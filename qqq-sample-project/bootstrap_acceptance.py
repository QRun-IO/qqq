#!/usr/bin/env python3
"""Verify source-installed or published QQQ sample bootstrap in an isolated cache."""
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tarfile
import tempfile
import xml.etree.ElementTree as ET
import zipfile

NS = "http://maven.apache.org/POM/4.0.0"
XML = {"m": NS}
BASE_LIBRARIES = frozenset((
    "qqq-backend-core", "qqq-backend-module-rdbms", "qqq-backend-module-mongodb",
    "qqq-backend-module-api", "qqq-backend-module-filesystem", "qqq-middleware-javalin",
    "qqq-middleware-slack", "qqq-middleware-api", "qqq-openapi", "qqq-middleware-picocli",
    "qqq-language-support-javascript", "qqq-backend-module-sqlite", "qqq-backend-module-postgres",
    "qqq-middleware-lambda", "qqq-middleware-health", "qqq-utility-lambdas",
))


def expected_libraries(bom_path):
    root = ET.parse(bom_path).getroot()
    libraries = [item.findtext("m:artifactId", namespaces=XML)
                 for item in root.findall("m:dependencyManagement/m:dependencies/m:dependency", XML)
                 if item.findtext("m:groupId", namespaces=XML) == "com.kingsrook.qqq"
                 and item.findtext("m:artifactId", namespaces=XML) != "qqq-frontend-next"]
    actual = set(libraries)
    reviewed = BASE_LIBRARIES | {"qqq-esb"}
    if len(libraries) != len(actual) or actual != reviewed:
        raise ValueError(f"Review changed QQQ library inventory: {sorted(actual ^ reviewed)}")
    return tuple(sorted(actual))


def validate_version(version, stage):
    pattern = (r"\d+\.\d+\.\d+(?:-RC\.\d+)?" if stage == "published"
               else r"\d+\.\d+\.\d+(?:-RC\.\d+|-SNAPSHOT)?")
    if not re.fullmatch(pattern, version or ""):
        raise ValueError(f"{stage} candidate must have a literal {'release or RC' if stage == 'published' else 'release, RC or SNAPSHOT'} version")
    return version


def require_new_workdir(path):
    path = path.resolve()
    allowed = (Path(tempfile.gettempdir()).resolve(), Path("/private/tmp").resolve())
    if not any(path.is_relative_to(root) and path != root for root in allowed):
        raise ValueError("Fixture must be below the OS temporary directory")
    if path.exists():
        raise ValueError(f"Fixture already exists: {path}")
    path.mkdir(parents=True)
    return path


def make_consumer(path, version, libraries):
    """Import the candidate BOM and request every QQQ library without explicit versions."""
    ET.register_namespace("", NS)
    tag = lambda name: f"{{{NS}}}{name}"
    project = ET.Element(tag("project"))
    for key, value in (("modelVersion", "4.0.0"), ("groupId", "com.qrunio.acceptance"),
                       ("artifactId", "qqq-bootstrap-consumer"), ("version", "1.0.0")):
        ET.SubElement(project, tag(key)).text = value
    managed = ET.SubElement(ET.SubElement(project, tag("dependencyManagement")), tag("dependencies"))
    bom = ET.SubElement(managed, tag("dependency"))
    for key, value in (("groupId", "com.kingsrook.qqq"), ("artifactId", "qqq-bom-pom"),
                       ("version", version), ("type", "pom"), ("scope", "import")):
        ET.SubElement(bom, tag(key)).text = value
    deps = ET.SubElement(project, tag("dependencies"))
    for library in libraries:
        dep = ET.SubElement(deps, tag("dependency"))
        ET.SubElement(dep, tag("groupId")).text = "com.kingsrook.qqq"
        ET.SubElement(dep, tag("artifactId")).text = library
    ET.ElementTree(project).write(path, encoding="utf-8", xml_declaration=True)


def require_artifacts(directory, libraries, version):
    for library in libraries:
        artifact = directory / f"{library}-{version}.jar"
        if not artifact.is_file() or not zipfile.is_zipfile(artifact):
            raise AssertionError(f"Missing exact candidate artifact: {library}:{version}")
        with zipfile.ZipFile(artifact) as jar:
            if jar.testzip() is not None:
                raise AssertionError(f"Corrupt candidate artifact: {library}:{version}")


def require_reports(sample):
    required = (("surefire-reports", "SampleBootstrapTest", None),
                ("surefire-reports", "SampleJavalinServerTest", None),
                ("failsafe-reports", "SamplePackagedConfigurationIT",
                 "testRepeatStartupReseedsInSeparateOwnedFixtures"))
    evidence = {}
    for folder, suite, required_case in required:
        path = sample / "target" / folder / f"TEST-com.kingsrook.sampleapp.{suite}.xml"
        if not path.is_file():
            raise AssertionError(f"Missing bootstrap test report: {suite}")
        xml = ET.parse(path).getroot()
        cases = [case.attrib.get("name") for case in xml.iter("testcase")]
        if (not cases or int(xml.attrib.get("tests", "0")) != len(cases)
                or any(int(xml.attrib.get(key, "0")) for key in ("errors", "failures", "skipped"))
                or required_case and required_case not in cases):
            raise AssertionError(f"Incomplete bootstrap test report: {suite}")
        evidence[suite] = {"tests": len(cases), "report": str(path)}
    return evidence


def maven(work, repository, pom, label, goals, expect_success=True, cwd=None):
    settings = work / "settings.xml"
    command = ["mvn", "-B", "-ntp", "-s", str(settings), "-gs", str(settings),
               f"-Dmaven.repo.local={repository}", "-f", str(pom)] + goals
    log = work / f"{label}.log"
    with log.open("w") as output:
        result = subprocess.run(command, cwd=cwd or pom.parent, stdout=output, stderr=subprocess.STDOUT)
    if (result.returncode == 0) != expect_success:
        raise AssertionError(f"{label} returned {result.returncode}; inspect {log}")
    return {"exit_code": result.returncode, "log": str(log), "command": command}


def require_negative_models(work, repository, version, libraries):
    consumer = work / "consumer"
    missing = consumer / "missing-bom.xml"
    make_consumer(missing, version, libraries)
    tree = ET.parse(missing)
    tree.getroot().remove(tree.getroot().find("m:dependencyManagement", XML))
    tree.write(missing, encoding="utf-8", xml_declaration=True)
    no_bom = maven(work, repository, missing, "missing-bom", ["-o", "validate"], False)
    if "'dependencies.dependency.version'" not in Path(no_bom["log"]).read_text():
        raise AssertionError("Missing-BOM negative failed for an unrelated reason")
    wrong = consumer / "wrong-version.xml"
    make_consumer(wrong, version + "-missing", libraries)
    mismatched = maven(work, repository, wrong, "wrong-version", ["-o", "validate"], False)
    if f"qqq-bom-pom:pom:{version}-missing" not in Path(mismatched["log"]).read_text():
        raise AssertionError("Mismatched-candidate negative failed for an unrelated reason")
    return {"missing_bom": no_bom, "mismatched_candidate": mismatched}


def rewrite_published_sample(path, version):
    ET.register_namespace("", NS)
    tree = ET.parse(path)
    root = tree.getroot()
    parent = root.find("m:parent", XML)
    parent.find("m:version", XML).text = version
    relative = parent.find("m:relativePath", XML)
    if relative is None:
        relative = ET.SubElement(parent, f"{{{NS}}}relativePath")
    relative.text = None
    properties = root.find("m:properties", XML)
    revision = properties.find("m:revision", XML)
    if revision is None:
        revision = ET.SubElement(properties, f"{{{NS}}}revision")
    revision.text = version
    tree.write(path, encoding="utf-8", xml_declaration=True)


def run_acceptance(args):
    root = Path(__file__).resolve().parent.parent
    sha = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=root, text=True).strip()
    dirty = subprocess.check_output(["git", "status", "--porcelain"], cwd=root, text=True)
    revision = ET.parse(root / "pom.xml").getroot().findtext("m:properties/m:revision", namespaces=XML)
    if args.stage == "source":
        if args.version:
            raise ValueError("Source stage uses the checkout revision; omit --version")
        version = validate_version(revision, "source")
    else:
        version = validate_version(args.version, "published")
        if dirty:
            raise ValueError("Published stage requires a clean committed checkout")
    libraries = expected_libraries(root / "qqq-bom/pom.xml")
    work = require_new_workdir(args.workdir.resolve()) if args.workdir else Path(
        tempfile.mkdtemp(prefix=f"qqq-bootstrap-{args.stage}-{version}-"))
    repository = work / "m2"
    (work / "settings.xml").write_text("<settings/>\n")
    report = {"stage": args.stage, "source_sha": sha, "candidate_version": version,
              "worktree_dirty": bool(dirty),
              "library_count": len(libraries), "libraries": libraries,
              "cache": str(repository), "cache_origin": "new empty cache",
              "started_at": datetime.now(timezone.utc).isoformat(), "complete": False}
    try:
        if args.stage == "source":
            report["root_install"] = maven(work, repository, root / "pom.xml", "root-install",
                ["-DskipTests", "-Dspotbugs.skip=true", "-Dpmd.skip=true", "clean", "install"])
            sample = root / "qqq-sample-project"
            report["provenance"] = "root reactor install; not public artifact evidence"
        else:
            archive = work / "source.tar"
            with archive.open("wb") as stream:
                subprocess.run(["git", "archive", sha, "qqq-sample-project", "checkstyle",
                                "pmd", "spotbugs", "qqq-bom"], cwd=root, stdout=stream, check=True)
            with tarfile.open(archive) as source_archive:
                source_archive.extractall(work, filter="data")
            if expected_libraries(work / "qqq-bom/pom.xml") != libraries:
                raise AssertionError("Archived BOM differs from the reviewed worktree inventory")
            sample = work / "qqq-sample-project"
            for configuration in ("checkstyle", "spotbugs"):
                shutil.copytree(work / configuration, sample / configuration)
            rewrite_published_sample(sample / "pom.xml", version)
            report["provenance"] = "git archive HEAD; empty settings/cache; public repositories"
        consumer = work / "consumer"
        consumer.mkdir()
        make_consumer(consumer / "pom.xml", version, libraries)
        copies = consumer / "libraries"
        report["candidate_resolution"] = maven(work, repository, consumer / "pom.xml", "candidate-resolution",
            ["org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies",
             "-DincludeGroupIds=com.kingsrook.qqq", f"-DoutputDirectory={copies}"])
        require_artifacts(copies, libraries, version)
        report["negative_models"] = require_negative_models(work, repository, version, libraries)
        report["sample_verify"] = maven(work, repository, sample / "pom.xml", "sample-verify",
            ["-Pacceptance-tests", "-Dcoverage.haltOnFailure=false",
             "-Dtest=SampleBootstrapTest,SampleJavalinServerTest",
             "-Dit.test=SamplePackagedConfigurationIT", "clean", "verify"],
            cwd=root if args.stage == "source" else sample)
        report["test_reports"] = require_reports(sample)
        report["complete"] = True
    except (AssertionError, OSError, ValueError, subprocess.CalledProcessError) as error:
        report["error"] = str(error)
    finally:
        report["finished_at"] = datetime.now(timezone.utc).isoformat()
        (work / "acceptance.json").write_text(json.dumps(report, indent=2) + "\n")
    print(f"{args.stage} bootstrap {'PASS' if report['complete'] else 'INCOMPLETE'}: {work / 'acceptance.json'}")
    return 0 if report["complete"] else 1


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--stage", choices=("source", "published"), required=True)
    parser.add_argument("--version", help="literal published QQQ release or RC")
    parser.add_argument("--workdir", type=Path, help="new evidence directory")
    args = parser.parse_args()
    try:
        return run_acceptance(args)
    except ValueError as error:
        parser.error(str(error))


if __name__ == "__main__":
    sys.exit(main())
