#!/usr/bin/env python3
"""Build a renamed first-party application QBit in a copied starter host.

Inputs must be checkouts of QRun-IO/qbit-template-application and
QRun-IO/qqq-app-starter. All writes stay in a disposable work directory.
"""

import argparse
import shutil
import subprocess
import tempfile
from pathlib import Path


HERE = Path(__file__).resolve().parent
SNAPSHOT = "4.1.0-SNAPSHOT"
QBIT_COORDINATES = "com.qrunio.acceptance:orderdesk-app:0.1.0-SNAPSHOT"
ORIGINAL_PACKAGE = "com.kingsrook.qbits.example"
GENERATED_PACKAGE = "com.qrunio.acceptance.orderdesk"


def run(command, cwd):
    subprocess.run(command, cwd=cwd, check=True)


def revision(source):
    return subprocess.run(["git", "-C", str(source), "rev-parse", "HEAD"],
                          capture_output=True, text=True, check=True).stdout.strip()


def stage_template(source, destination):
    shutil.copytree(source, destination, ignore=shutil.ignore_patterns(".git", "target", ".env"))
    pom = destination / "pom.xml"
    content = pom.read_text()
    for old, new in (
        ("<groupId>com.kingsrook.qbits</groupId>", "<groupId>com.qrunio.acceptance</groupId>"),
        ("<artifactId>qbit-example-app</artifactId>", "<artifactId>orderdesk-app</artifactId>"),
        ("<name>QBit Example Application</name>", "<name>Order Desk Acceptance QBit</name>"),
    ):
        assert content.count(old) == 1, old
        content = content.replace(old, new)
    pom.write_text(content)

    old_root = destination / "src/main/java/com/kingsrook/qbits/example"
    new_root = destination / "src/main/java/com/qrunio/acceptance/orderdesk"
    apache_header = (HERE / "StarterApplicationAcceptanceTest.java").read_text().split("package ", 1)[0]
    for source_file in old_root.rglob("*.java"):
        relative = source_file.relative_to(old_root)
        target = new_root / Path(*[part.replace("Example", "OrderDesk") for part in relative.parts])
        target.parent.mkdir(parents=True, exist_ok=True)
        java = source_file.read_text()
        java = java.replace(ORIGINAL_PACKAGE, GENERATED_PACKAGE)
        java = java.replace("Example", "OrderDesk").replace("example", "orderDesk")
        target.write_text(apache_header + java)
    shutil.rmtree(destination / "src/main/java/com/kingsrook")
    return new_root


def stage_starter(source, destination):
    shutil.copytree(source, destination, ignore=shutil.ignore_patterns(".git", "target", ".env"))
    pom = destination / "pom.xml"
    content = pom.read_text()
    marker = "   <dependencies>\n      <!-- qqq modules deps -->"
    assert content.count(marker) == 1
    dependency = """      <dependency>
         <groupId>com.qrunio.acceptance</groupId>
         <artifactId>orderdesk-app</artifactId>
         <version>0.1.0-SNAPSHOT</version>
      </dependency>
"""
    pom.write_text(content.replace(marker, marker + "\n" + dependency))

    provider = destination / "src/main/java/com/kingsrook/qqq/starterapp/StarterAppMetaDataProvider.java"
    java = provider.read_text()
    import_marker = "import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;"
    assert java.count(import_marker) == 1
    java = java.replace(import_marker, import_marker + "\nimport com.qrunio.acceptance.orderdesk.OrderDeskAppQBitConfig;\nimport com.qrunio.acceptance.orderdesk.OrderDeskAppQBitProducer;")
    registration_marker = "      qInstance.addApp(defineSampleApp(qInstance));"
    assert java.count(registration_marker) == 1
    registration = """      new OrderDeskAppQBitProducer()
         .withConfig(new OrderDeskAppQBitConfig().withBackendName(RDBMS_BACKEND_NAME))
         .produce(qInstance, "acceptance");"""
    provider.write_text(java.replace(registration_marker, registration_marker + "\n" + registration))
    test = destination / "src/test/java/com/kingsrook/qqq/starterapp/StarterApplicationAcceptanceTest.java"
    shutil.copyfile(HERE / "StarterApplicationAcceptanceTest.java", test)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--starter-source", type=Path, required=True)
    parser.add_argument("--template-source", type=Path, required=True)
    parser.add_argument("--qqq-source", type=Path, required=True)
    parser.add_argument("--maven-repo", type=Path, required=True)
    parser.add_argument("--workdir", type=Path)
    args = parser.parse_args()
    workdir = args.workdir or Path(tempfile.mkdtemp(prefix="qqq-starter-application-"))
    workdir.mkdir(parents=True, exist_ok=True)
    assert "<revision>4.1.0-SNAPSHOT</revision>" in (args.qqq_source / "pom.xml").read_text()
    print("QQQ source:", revision(args.qqq_source), flush=True)
    print("starter source:", revision(args.starter_source), flush=True)
    print("application template source:", revision(args.template_source), flush=True)
    base = ["mvn", "-o", "-B", "-q", f"-Dmaven.repo.local={args.maven_repo}"]
    modules = ",".join(("qqq-bom", "qqq-backend-core", "qqq-backend-module-rdbms",
                        "qqq-backend-module-api", "qqq-backend-module-filesystem",
                        "qqq-middleware-javalin", "qqq-middleware-api",
                        "qqq-middleware-picocli", "qqq-language-support-javascript"))
    run(base + ["-Dmaven.test.skip=true", "-Dspotbugs.skip=true", "-Dpmd.skip=true",
                "-pl", modules, "-am", "install"], args.qqq_source)
    template = workdir / "orderdesk-app"
    starter = workdir / "starter"
    generated = stage_template(args.template_source, template)
    stage_starter(args.starter_source, starter)
    assert len(list(generated.rglob("*.java"))) == 8
    run(base + [f"-Dqqq.version={SNAPSHOT}", "clean", "install"], template)
    run(base + [f"-Dqqq.versions.bom={SNAPSHOT}",
                "-Dtest=StarterApplicationAcceptanceTest,StarterAppTest", "test"], starter)
    print(f"PASS: generated 8 Java sources; built {QBIT_COORDINATES}; integrated starter tests passed")
    print(f"fixture: {workdir}")


if __name__ == "__main__":
    main()
