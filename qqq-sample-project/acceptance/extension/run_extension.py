#!/usr/bin/env python3
"""Build a disposable, renamed first-party extension and exercise it in a host."""

import argparse
import re
import shutil
import subprocess
import tempfile
from pathlib import Path
import xml.etree.ElementTree as ET


HERE = Path(__file__).resolve().parent
PACKAGE = "com.qrunio.acceptance.extension"
GROUP = "com.qrunio.acceptance"
ARTIFACT = "orderdesk-extension"
VERSION = "0.1.0-SNAPSHOT"


def resolve_candidate_version(source, override):
    """Use the checked-out root revision unless CI supplies its isolated version."""
    root = ET.parse(source / "pom.xml").getroot()
    revision = root.find("./{*}properties/{*}revision")
    require(revision is not None and revision.text, "QQQ root revision is missing")
    version = override or revision.text.strip()
    require(re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]*", version) is not None,
            "invalid QQQ candidate version")
    return version


def rewrite_template_coordinates(value, candidate_version):
    for old, new in (("<groupId>com.kingsrook.qbits</groupId>", f"<groupId>{GROUP}</groupId>"),
                     ("<artifactId>qbit-example-extension</artifactId>", f"<artifactId>{ARTIFACT}</artifactId>"),
                     ("<qqq.version>4.0.0</qqq.version>",
                      f"<qqq.version>{candidate_version}</qqq.version>")):
        value = replace_exact(value, old, new)
    return value


def framework_install_command(base, candidate_version):
    return base + ["-pl", "qqq-bom,qqq-backend-core", "-am",
                   f"-Drevision={candidate_version}", "-DskipTests",
                   "-Dspotbugs.skip=true", "-Dpmd.skip=true", "install"]


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def run(command, cwd):
    subprocess.run(command, cwd=cwd, check=True)


def git_output(source, *args):
    return subprocess.run(["git", "-C", str(source), *args], check=True,
                          capture_output=True).stdout


def replace_exact(text, old, new):
    require(text.count(old) == 1, f"template marker changed: {old[:80]}")
    return text.replace(old, new)


def stage_template(source, destination, candidate_version):
    """Copy tracked template inputs; add consumer-owned behavior only in the copy."""
    require(not git_output(source, "status", "--porcelain"), "template checkout must be clean")
    destination.mkdir()
    for raw in git_output(source, "ls-files", "-z").split(b"\0"):
        if not raw:
            continue
        relative = Path(raw.decode())
        original = source / relative
        require(not original.is_symlink(), f"tracked symlink: {relative}")
        if original.is_file():
            target = destination / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(original, target)

    pom = destination / "pom.xml"
    value = rewrite_template_coordinates(pom.read_text(), candidate_version)
    for artifact in ("junit-jupiter", "assertj-core"):
        start = value.index("      <dependency>\n", value.index("<artifactId>qqq-backend-core</artifactId>"))
        while f"<artifactId>{artifact}</artifactId>" not in value[start:value.index("</dependency>", start)]:
            start = value.index("      <dependency>\n", start + 1)
        end = value.index("      </dependency>", start) + len("      </dependency>\n")
        value = value[:start] + value[end:]
    pom.write_text(value)

    old_root = destination / "src/main/java/com/kingsrook/qbits/example"
    new_root = destination / "src/main/java/com/qrunio/acceptance/extension"
    new_root.mkdir(parents=True)
    for original in old_root.rglob("*.java"):
        relative = original.relative_to(old_root)
        target = new_root / Path(*[part.replace("Example", "OrderDesk") for part in relative.parts])
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(original.read_text().replace("com.kingsrook.qbits.example", PACKAGE)
                          .replace("Example", "OrderDesk"))
    shutil.rmtree(destination / "src/main/java/com/kingsrook")

    config = new_root / "OrderDeskExtensionQBitConfig.java"
    value = config.read_text()
    value = replace_exact(value,
        'if(targetTableName != null && qInstance.getTable(targetTableName) == null)\n      {\n         errors.add("Target table not found: " + targetTableName);\n      }',
        'if(targetTableName == null || targetTableName.isBlank())\n      {\n         errors.add("Target table is required");\n      }\n      else if(qInstance.getTable(targetTableName) == null)\n      {\n         errors.add("Target table not found: " + targetTableName);\n      }\n      else if(!qInstance.getTable(targetTableName).getFields().containsKey("name"))\n      {\n         errors.add("Target table requires name field: " + targetTableName);\n      }')
    config.write_text(value)

    producer = new_root / "OrderDeskExtensionQBitProducer.java"
    value = producer.read_text()
    value = replace_exact(value, "import com.kingsrook.qqq.backend.core.exceptions.QException;",
                          f"import com.kingsrook.qqq.backend.core.exceptions.QException;\nimport {PACKAGE}.customizers.OrderDeskTableCustomizer;")
    value = replace_exact(value, '"com.kingsrook.qbits"', f'"{GROUP}"')
    value = replace_exact(value, '"qbit-example-extension"', f'"{ARTIFACT}"')
    value = replace_exact(value, "      QBitMetaData qBitMetaData =", "      config.validate(qInstance);\n      QBitMetaData qBitMetaData =")
    value = replace_exact(value, "if(config != null && config.getTargetTableName() != null)",
                          "if(Boolean.TRUE.equals(config.getEnableFeatureX()))")
    value = replace_exact(value, "      // Implement your extension logic here",
                          "      new OrderDeskTableCustomizer().customize(qInstance.getTable(tableName));")
    producer.write_text(value)

    customizer = new_root / "customizers/OrderDeskTableCustomizer.java"
    value = customizer.read_text()
    value = replace_exact(value, "import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;",
        "import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;\n"
        "import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;\n"
        "import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;")
    value = replace_exact(value,
        "      // OrderDesk: Add audit tracking fields\n      // table.withField(new QFieldMetaData(\"createdAt\", QFieldType.DATE_TIME));",
        "      table.withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(OrderDeskPreInsert.class));")
    customizer.write_text(value)
    shutil.copyfile(HERE / "OrderDeskPreInsert.java", customizer.with_name("OrderDeskPreInsert.java"))
    return new_root


def stage_host(destination, candidate_version):
    destination.mkdir()
    pom = destination / "pom.xml"
    pom.write_text(replace_exact((HERE / "pom.xml").read_text(),
                                 "@QQQ_VERSION@", candidate_version))
    test = destination / "src/test/java/com/qrunio/acceptance/extension/ExtensionHostAcceptanceTest.java"
    test.parent.mkdir(parents=True)
    shutil.copyfile(HERE / "ExtensionHostAcceptanceTest.java", test)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--qqq-source", required=True, type=Path)
    parser.add_argument("--template-source", required=True, type=Path)
    parser.add_argument("--candidate-version",
                        help="isolated Maven version for the exact checked-out QQQ source")
    parser.add_argument("--maven-repo", type=Path)
    parser.add_argument("--maven-settings", type=Path,
                        help="Maven settings for the candidate build environment")
    parser.add_argument("--report-dir", type=Path,
                        help="copy the separate host's JUnit XML into the sample report gate")
    args = parser.parse_args()
    qqq = args.qqq_source.resolve()
    template = args.template_source.resolve()
    candidate_version = resolve_candidate_version(qqq, args.candidate_version)
    print("QQQ:", git_output(qqq, "rev-parse", "HEAD").decode().strip(), flush=True)
    print("candidate version:", candidate_version, flush=True)
    print("template:", git_output(template, "rev-parse", "HEAD").decode().strip(), flush=True)
    # Public template/plugin versions differ from QQQ's own plugins.  Allow
    # release downloads, but never refresh the locally installed snapshot
    # artifacts built from this exact QQQ checkout and generated consumer.
    base = ["mvn", "-B", "-nsu"]
    if args.maven_repo:
        base.append(f"-Dmaven.repo.local={args.maven_repo.resolve()}")
    if args.maven_settings:
        base.extend(["-s", str(args.maven_settings.resolve())])
    with tempfile.TemporaryDirectory(prefix="qqq-extension-610-") as root:
        root = Path(root)
        extension = root / "generated-extension"
        host = root / "host"
        generated = stage_template(template, extension, candidate_version)
        require(len(list(generated.rglob("*.java"))) == 4, "generated source count changed")
        stage_host(host, candidate_version)
        run(framework_install_command(base, candidate_version), qqq)
        run(base + [f"-Dqqq.version={candidate_version}", "install"], extension)
        run(base + [f"-Dqqq.version={candidate_version}", "test"], host)
        missing_dependency = root / "host-without-extension"
        stage_host(missing_dependency, candidate_version)
        pom = missing_dependency / "pom.xml"
        value = pom.read_text()
        dependency = ("      <dependency>\n"
                      f"         <groupId>{GROUP}</groupId>\n"
                      f"         <artifactId>{ARTIFACT}</artifactId>\n"
                      f"         <version>{VERSION}</version>\n"
                      "      </dependency>\n")
        pom.write_text(replace_exact(value, dependency, ""))
        failed = subprocess.run(base + [f"-Dqqq.version={candidate_version}", "test-compile"], cwd=missing_dependency,
                                capture_output=True, text=True)
        require(failed.returncode != 0 and "com.qrunio.acceptance.extension" in
                failed.stdout + failed.stderr,
                "host unexpectedly compiled without the generated extension dependency")
        if args.report_dir:
            report = (host / "target/surefire-reports/"
                      "TEST-com.qrunio.acceptance.extension.ExtensionHostAcceptanceTest.xml")
            require(report.is_file(), "host JUnit report missing")
            args.report_dir.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(report, args.report_dir / report.name)
    print("PASS: generated extension, separate host runtime, config negatives and missing dependency")


if __name__ == "__main__":
    main()
