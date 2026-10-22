#!/usr/bin/env python3
"""Resolve a published QQQ BOM and every managed QQQ jar from public Maven Central."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
from typing import NamedTuple
from urllib.request import urlopen
import xml.etree.ElementTree as ET


NAMESPACE = {"m": "http://maven.apache.org/POM/4.0.0"}
GROUP = "com.kingsrook.qqq"
NEXT = "qqq-frontend-next"
VERSION = re.compile(r"\d+\.\d+\.\d+(?:-RC\.\d+)?\Z")
CENTRAL = "https://repo.maven.apache.org/maven2/"


class Inventory(NamedTuple):
    modules: tuple[str, ...]
    next_version: str
    scopes: dict[str, str]


def validate_version(version):
    if not VERSION.fullmatch(version):
        raise ValueError("Use a literal published release or RC version")
    return version


def parse_bom(xml_text):
    root = ET.fromstring(xml_text)
    dependencies = root.findall("m:dependencyManagement/m:dependencies/m:dependency", NAMESPACE)
    modules = []
    next_version = None
    scopes = {}
    seen = set()
    for dependency in dependencies:
        group = (dependency.findtext("m:groupId", namespaces=NAMESPACE) or "").strip()
        artifact = (dependency.findtext("m:artifactId", namespaces=NAMESPACE) or "").strip()
        version = (dependency.findtext("m:version", namespaces=NAMESPACE) or "").strip()
        if group != GROUP:
            continue
        if not artifact:
            raise ValueError("QQQ BOM dependency has no artifactId")
        if artifact in seen:
            raise ValueError(f"Duplicate BOM artifact: {artifact}")
        seen.add(artifact)
        scopes[artifact] = (dependency.findtext("m:scope", default="compile", namespaces=NAMESPACE) or "").strip()
        if artifact == NEXT:
            if not version or not VERSION.fullmatch(version):
                raise ValueError("Next pin must be a literal release or RC, not SNAPSHOT")
            next_version = version
        else:
            if version != "${revision}":
                raise ValueError(f"QQQ module {artifact} is not managed by revision")
            modules.append(artifact)
    if not modules or next_version is None:
        raise ValueError("Source BOM must manage QQQ modules and the Next dashboard")
    return Inventory(tuple(modules), next_version, scopes)


def managed_coordinates(xml_text, revision=None):
    root = ET.fromstring(xml_text)
    entries = []
    for dependency in root.findall("m:dependencyManagement/m:dependencies/m:dependency", NAMESPACE):
        def field(name, default=""):
            return (dependency.findtext(f"m:{name}", default=default, namespaces=NAMESPACE) or "").strip()

        group = field("groupId")
        artifact = field("artifactId")
        version = field("version")
        if version == "${revision}" and revision is not None:
            version = revision
        kind = field("type", "jar")
        scope = field("scope", "compile")
        if not all((group, artifact, version, kind, scope)) or "${" in version:
            raise ValueError(f"Unresolved or incomplete BOM management: {artifact}")
        entries.append((group, artifact, version, kind, scope))
    if len(entries) != len(set(entries)):
        raise ValueError("Duplicate BOM management coordinate")
    return sorted(entries)


def compare_boms(source_bom, published_bom, version):
    expected = managed_coordinates(source_bom, version)
    observed = managed_coordinates(published_bom)
    if expected != observed:
        raise ValueError(f"BOM management mismatch: missing {sorted(set(expected) - set(observed))}; "
                         f"extra or changed {sorted(set(observed) - set(expected))}")
    return expected, observed


def build_consumer_pom(version, inventory):
    validate_version(version)
    uri = NAMESPACE["m"]
    ET.register_namespace("", uri)

    def child(parent, name, value=None):
        element = ET.SubElement(parent, f"{{{uri}}}{name}")
        element.text = value
        return element

    project = ET.Element(f"{{{uri}}}project")
    for name, value in (("modelVersion", "4.0.0"), ("groupId", "qqq.verification"),
                        ("artifactId", "published-bom-consumer"), ("version", "1.0.0")):
        child(project, name, value)
    imported = child(child(child(project, "dependencyManagement"), "dependencies"), "dependency")
    for name, value in (("groupId", GROUP), ("artifactId", "qqq-bom-pom"),
                        ("version", version), ("type", "pom"), ("scope", "import")):
        child(imported, name, value)
    dependencies = child(project, "dependencies")
    for artifact in (*inventory.modules, NEXT):
        dependency = child(dependencies, "dependency")
        child(dependency, "groupId", GROUP)
        child(dependency, "artifactId", artifact)
    repositories = child(project, "repositories")
    repository = child(repositories, "repository")
    child(repository, "id", "central")
    child(repository, "url", CENTRAL)
    child(child(repository, "snapshots"), "enabled", "false")
    return ET.tostring(project, encoding="unicode")


def check_tree(tree, version, inventory):
    expected = {artifact: version for artifact in inventory.modules}
    expected[NEXT] = inventory.next_version
    observed = {}

    def reject_snapshots(node):
        if "SNAPSHOT" in node.get("version", "").upper():
            raise ValueError(f"SNAPSHOT dependency: {node.get('artifactId')}")
        for child in node.get("children", []):
            reject_snapshots(child)

    reject_snapshots(tree)
    for node in tree.get("children", []):
        artifact = node.get("artifactId")
        if artifact in observed:
            raise ValueError(f"Duplicate resolved dependency: {artifact}")
        observed[artifact] = node
    if set(observed) != set(expected):
        raise ValueError(f"Resolved dependency missing or unexpected: expected {sorted(expected)}, got {sorted(observed)}")
    for artifact, wanted in expected.items():
        node = observed[artifact]
        if (node.get("groupId") != GROUP or node.get("version") != wanted or node.get("type") != "jar"
                or node.get("scope") != inventory.scopes[artifact]):
            raise ValueError(f"Resolved artifact version/type/scope mismatch: {artifact}: {node}")
    return {artifact: observed[artifact]["version"] for artifact in expected}


def artifact_path(cache, artifact, version, extension):
    return cache / "com/kingsrook/qqq" / artifact / version / f"{artifact}-{version}.{extension}"


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def central_sha256(url, _path):
    digest = hashlib.sha256()
    with urlopen(url, timeout=60) as response:
        if response.geturl() != url:
            raise ValueError(f"Central redirected artifact request: {url}")
        while chunk := response.read(1024 * 1024):
            digest.update(chunk)
    return digest.hexdigest()


def repository_markers(path):
    """Retain Maven Resolver's internal marker as supplemental, non-authoritative evidence."""
    marker = path.parent / "_remote.repositories"
    if not marker.is_file():
        return []
    prefix = f"{path.name}>"
    return sorted(line[len(prefix):].split("=", 1)[0] for line in marker.read_text().splitlines()
                  if line.startswith(prefix) and "=" in line)


def check_repository_markers(path):
    markers = repository_markers(path)
    if markers and markers != ["central"]:
        raise ValueError(f"Repository provenance mismatch for {path.name}: {markers}")
    return markers


def verify(version, source_bom, evidence_root, maven="mvn", source_commit=None, remote_digest=None):
    validate_version(version)
    inventory = parse_bom(source_bom)
    evidence_root = Path(evidence_root)
    evidence_root.mkdir(parents=True, exist_ok=True)
    directory = Path(tempfile.mkdtemp(prefix=f"published-bom-{version}-", dir=evidence_root))
    remote_digest = remote_digest or central_sha256
    cache = directory / "m2"
    cache.mkdir()
    settings = directory / "settings.xml"
    settings.write_text('<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"/>\n')
    consumer = directory / "pom.xml"
    consumer.write_text(build_consumer_pom(version, inventory) + "\n")
    tree_file = directory / "dependency-tree.json"
    command = [maven, "-B", "-ntp", "-s", str(settings), "-gs", str(settings),
               f"-Dmaven.repo.local={cache}", "-f", str(consumer),
               "org.apache.maven.plugins:maven-dependency-plugin:3.9.0:resolve",
               "org.apache.maven.plugins:maven-dependency-plugin:3.9.0:tree",
               "-DoutputType=json", f"-DoutputFile={tree_file}"]
    evidence = {"status": "fail", "directory": str(directory), "candidate_version": version,
                "source_commit": source_commit, "source_bom_sha256": hashlib.sha256(source_bom.encode()).hexdigest(),
                "expected_modules": list(inventory.modules), "next_version": inventory.next_version,
                "maven_command": command, "maven_exit_code": None, "artifacts": {},
                "central_url": CENTRAL}
    try:
        env = os.environ.copy()
        rejected = [key for key in env if key.startswith("MAVEN_") or key in
                    ("M2", "M2_HOME", "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "CLASSPATH")]
        for key in rejected:
            env.pop(key)
        env["HOME"] = str(directory)
        env["MAVEN_SKIP_RC"] = "true"
        evidence["removed_environment_keys"] = sorted(rejected)
        with (directory / "maven.log").open("w") as log:
            result = subprocess.run(command, cwd=directory, stdout=log, stderr=subprocess.STDOUT,
                                    env=env, timeout=900, check=False)
        evidence["maven_exit_code"] = result.returncode
        if result.returncode:
            raise ValueError(f"Maven failed with exit code {result.returncode}; see {directory / 'maven.log'}")
        if not tree_file.is_file():
            raise ValueError("Maven did not write a dependency tree")
        published_bom = artifact_path(cache, "qqq-bom-pom", version, "pom")
        if not published_bom.is_file():
            raise ValueError("Published BOM missing from isolated cache")
        evidence["source_management"] = managed_coordinates(source_bom, version)
        evidence["published_management"] = managed_coordinates(published_bom.read_text())
        compare_boms(source_bom, published_bom.read_text(), version)
        tree = json.loads(tree_file.read_text())
        evidence["resolved_versions"] = check_tree(tree, version, inventory)
        for artifact, artifact_version, extension in (
                ("qqq-bom-pom", version, "pom"),
                *((name, version, "jar") for name in inventory.modules),
                (NEXT, inventory.next_version, "jar")):
            path = artifact_path(cache, artifact, artifact_version, extension)
            if not path.is_file():
                raise ValueError(f"Resolved artifact missing from isolated cache: {path}")
            url = CENTRAL + str(path.relative_to(cache))
            local_hash = sha256(path)
            item = {"path": str(path.relative_to(directory)), "sha256": local_hash,
                    "central_url": url, "repository_markers": repository_markers(path)}
            evidence["artifacts"][artifact] = item
            check_repository_markers(path)
            item["central_sha256"] = remote_digest(url, path)
            if item["central_sha256"] != local_hash:
                raise ValueError(f"Central checksum mismatch for {artifact}")
        evidence["status"] = "pass"
    except (OSError, ValueError, json.JSONDecodeError, subprocess.TimeoutExpired) as error:
        evidence["error"] = str(error)
        raise
    finally:
        published_bom = artifact_path(cache, "qqq-bom-pom", version, "pom")
        if published_bom.is_file():
            evidence["published_bom_sha256"] = sha256(published_bom)
        (directory / "evidence.json").write_text(json.dumps(evidence, indent=2, sort_keys=True) + "\n")
    return evidence


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("version", help="literal public release or RC version, e.g. 4.1.0-RC.1")
    args = parser.parse_args(argv)
    root = Path(__file__).resolve().parent.parent
    try:
        source_bom = subprocess.check_output(
            ["git", "show", "HEAD:qqq-bom/pom.xml"], cwd=root, text=True)
        source_commit = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=root, text=True).strip()
        evidence = verify(args.version, source_bom, root / "qqq-sample-project/target",
                          source_commit=source_commit)
        print(f"Published BOM consumer PASS: {evidence['directory']}")
        return 0
    except (OSError, ValueError, subprocess.CalledProcessError) as error:
        print(f"Published BOM consumer FAIL: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
