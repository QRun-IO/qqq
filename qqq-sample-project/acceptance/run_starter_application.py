#!/usr/bin/env python3
"""Build a renamed first-party application QBit in a copied starter host.

Inputs must be checkouts of QRun-IO/qbit-template-application and
QRun-IO/qqq-app-starter. All writes stay in a disposable work directory.
"""

import argparse
import contextlib
import os
import re
import shutil
import socket
import subprocess
import tempfile
import time
import uuid
import xml.etree.ElementTree as ET
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import urlopen

from live_starter_application import exercise, request


HERE = Path(__file__).resolve().parent
QBIT_COORDINATES = "com.qrunio.acceptance:orderdesk-app:0.1.0-SNAPSHOT"
ORIGINAL_PACKAGE = "com.kingsrook.qbits.example"
GENERATED_PACKAGE = "com.qrunio.acceptance.orderdesk"


def run(command, cwd):
    subprocess.run(command, cwd=cwd, check=True)


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def revision(source):
    return subprocess.run(["git", "-C", str(source), "rev-parse", "HEAD"],
                          capture_output=True, text=True, check=True).stdout.strip()


def qqq_version(source):
    root = ET.parse(source / "pom.xml").getroot()
    namespace = "{http://maven.apache.org/POM/4.0.0}"
    value = root.findtext(f"{namespace}properties/{namespace}revision")
    require(value is not None and re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+(?:-[A-Za-z0-9][A-Za-z0-9.-]*)?", value),
            "QQQ source must declare a literal root revision")
    return value


def copy_tracked_source(source, destination):
    """Copy worktree content of safe tracked paths, never local untracked state."""
    paths = subprocess.run(["git", "-C", str(source), "ls-files", "-z"],
                           capture_output=True, check=True).stdout.split(b"\0")
    destination.mkdir(parents=True, exist_ok=False)
    for raw in paths:
        if not raw:
            continue
        relative = Path(raw.decode())
        name = relative.name.lower()
        lower_parts = tuple(part.lower() for part in relative.parts)
        if (name.startswith(".env") or name in (".npmrc", ".netrc", "settings.xml", "id_rsa")
                or ".local." in name
                or any(word in part for part in lower_parts
                       for word in ("secret", "credential", "password"))
                or relative.suffix.lower() in (".pem", ".key", ".p12", ".pfx", ".jks")):
            continue
        original = source / relative
        require(not original.is_symlink(), f"tracked symlink is not safe to copy: {relative}")
        if original.is_file():
            target = destination / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(original, target)


def require_free_port(port):
    with socket.socket() as probe:
        probe.bind(("127.0.0.1", port))


def expect_server_error(method, path):
    try:
        request("http://127.0.0.1:8000", method, path, {} if method == "POST" else None)
    except HTTPError as error:
        require(error.code == 500, f"expected 500 for {path}, received {error.code}")
        body = error.read().decode()
        require("\"record\"" not in body and "\"records\"" not in body,
                f"failed {path} disclosed a record")
    else:
        raise AssertionError(f"{path} unexpectedly succeeded")


def run_live_mysql(base, starter, workdir, version):
    require_free_port(3306)
    require_free_port(8000)
    name = "qqq-acceptance-" + uuid.uuid4().hex[:12]
    container_started = False
    server = None
    environment = os.environ.copy()
    environment.update(RDBMS_VENDOR="mysql", RDBMS_HOSTNAME="127.0.0.1", RDBMS_PORT="3306",
                       RDBMS_DATABASE_NAME="qqq_starter_test", RDBMS_USERNAME="test", RDBMS_PASSWORD="test")
    try:
        run(["docker", "run", "-d", "--rm", "--name", name,
             "-p", "127.0.0.1:3306:3306", "-e", "MYSQL_ROOT_PASSWORD=disposable-root",
             "-e", "MYSQL_DATABASE=qqq_starter_test", "-e", "MYSQL_USER=test",
             "-e", "MYSQL_PASSWORD=test", "mysql:8.4"], workdir)
        container_started = True
        for _ in range(90):
            ready = subprocess.run(["docker", "exec", name, "mysql", "-utest", "-ptest",
                                    "qqq_starter_test", "-e", "SELECT 1"],
                                   capture_output=True)
            if ready.returncode == 0:
                break
            time.sleep(1)
        else:
            raise AssertionError("disposable MySQL did not become ready")
        with (HERE / "starter-application-mysql.sql").open("rb") as schema:
            subprocess.run(["docker", "exec", "-i", name, "mysql", "-utest", "-ptest",
                            "qqq_starter_test"], stdin=schema, check=True, capture_output=True)
        run(base + [f"-Dqqq.versions.bom={version}", "-DskipTests", "package"], starter)
        run(base + [f"-Dqqq.versions.bom={version}",
                    "-Dtest=StarterApplicationLivePermissionTest", "test"], starter)
        jars = list((starter / "target").glob("qqq-app-starter-*.jar"))
        require(len(jars) == 1, f"expected one shaded starter jar, found {len(jars)}")
        with (workdir / "starter-server.log").open("wb") as log:
            server = subprocess.Popen(["java", "-jar", str(jars[0])], cwd=starter,
                                      env=environment, stdout=log, stderr=subprocess.STDOUT)
            for _ in range(90):
                require(server.poll() is None, "starter exited before HTTP readiness")
                try:
                    with urlopen("http://127.0.0.1:8000/metaData", timeout=1) as response:
                        if response.status == 200:
                            break
                except (HTTPError, URLError, TimeoutError):
                    time.sleep(1)
            else:
                raise AssertionError("starter HTTP did not become ready")
            exercise("http://127.0.0.1:8000")
            subprocess.run(["docker", "exec", name, "mysql", "-utest", "-ptest",
                            "qqq_starter_test", "-e", "DROP TABLE orderDeskChildEntity"],
                           check=True, capture_output=True)
            expect_server_error("POST", "/qqq/v1/table/orderDeskChildEntity/query")
            subprocess.run(["docker", "stop", name], check=True, capture_output=True)
            container_started = False
            expect_server_error("POST", "/qqq/v1/table/orderDeskEntity/query")
        server.terminate()
        server.wait(timeout=15)
        server = None
        no_database_environment = {key: value for key, value in os.environ.items()
                                   if not key.startswith("RDBMS_")}
        missing_env = subprocess.run(["java", "-jar", str(jars[0])], cwd=starter,
                                     env=no_database_environment, capture_output=True,
                                     text=True, timeout=30)
        require(missing_env.returncode != 0 and "Missing at least one env. var" in
                missing_env.stdout + missing_env.stderr, "missing DB environment did not fail closed")
        print("PASS: live MySQL CRUD, allowed/denied permission with DB readback, schema/DB/env failures")
    finally:
        if server is not None:
            server.terminate()
            try:
                server.wait(timeout=10)
            except subprocess.TimeoutExpired:
                server.kill()
                server.wait()
        if container_started:
            subprocess.run(["docker", "stop", name], capture_output=True)


def stage_template(source, destination):
    copy_tracked_source(source, destination)
    pom = destination / "pom.xml"
    content = pom.read_text()
    for old, new in (
        ("<groupId>com.kingsrook.qbits</groupId>", "<groupId>com.qrunio.acceptance</groupId>"),
        ("<artifactId>qbit-example-app</artifactId>", "<artifactId>orderdesk-app</artifactId>"),
        ("<name>QBit Example Application</name>", "<name>Order Desk Acceptance QBit</name>"),
    ):
        require(content.count(old) == 1, old)
        content = content.replace(old, new)
    pom.write_text(content)

    new_root = destination / "src/main/java/com/qrunio/acceptance/orderdesk"
    apache_header = (HERE / "StarterApplicationAcceptanceTest.java").read_text().split("package ", 1)[0]
    for source_set in ("main", "test"):
        old_root = destination / f"src/{source_set}/java/com/kingsrook/qbits/example"
        if not old_root.exists():
            continue
        target_root = destination / f"src/{source_set}/java/com/qrunio/acceptance/orderdesk"
        for source_file in old_root.rglob("*.java"):
            relative = source_file.relative_to(old_root)
            target = target_root / Path(*[part.replace("Example", "OrderDesk") for part in relative.parts])
            target.parent.mkdir(parents=True, exist_ok=True)
            java = source_file.read_text()
            java = java.replace(ORIGINAL_PACKAGE, GENERATED_PACKAGE)
            java = java.replace("Example", "OrderDesk").replace("example", "orderDesk")
            target.write_text(java if java.startswith(apache_header) else apache_header + java)
        shutil.rmtree(destination / f"src/{source_set}/java/com/kingsrook")
    return new_root


def stage_starter(source, destination):
    copy_tracked_source(source, destination)
    pom = destination / "pom.xml"
    content = pom.read_text()
    marker = "   <dependencies>\n      <!-- qqq modules deps -->"
    require(content.count(marker) == 1, marker)
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
    require(java.count(import_marker) == 1, import_marker)
    java = java.replace(import_marker, import_marker + "\nimport com.qrunio.acceptance.orderdesk.OrderDeskAppQBitConfig;\nimport com.qrunio.acceptance.orderdesk.OrderDeskAppQBitProducer;")
    registration_marker = "      qInstance.addApp(defineSampleApp(qInstance));"
    require(java.count(registration_marker) == 1, registration_marker)
    registration = """      new OrderDeskAppQBitProducer()
         .withConfig(new OrderDeskAppQBitConfig().withBackendName(RDBMS_BACKEND_NAME))
         .produce(qInstance, "acceptance");"""
    provider.write_text(java.replace(registration_marker, registration_marker + "\n" + registration))
    test = destination / "src/test/java/com/kingsrook/qqq/starterapp/StarterApplicationAcceptanceTest.java"
    shutil.copyfile(HERE / "StarterApplicationAcceptanceTest.java", test)
    shutil.copyfile(HERE / "StarterApplicationLivePermissionTest.java", test.with_name("StarterApplicationLivePermissionTest.java"))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--starter-source", type=Path, required=True)
    parser.add_argument("--template-source", type=Path, required=True)
    parser.add_argument("--qqq-source", type=Path, required=True)
    parser.add_argument("--maven-repo", type=Path, required=True)
    parser.add_argument("--workdir", type=Path)
    parser.add_argument("--junit-dir", type=Path,
                        help="new directory for durable consumer and live-runner JUnit reports")
    parser.add_argument("--online", action="store_true",
                        help="resolve dependencies into the dedicated Maven repository")
    parser.add_argument("--maven-settings", type=Path)
    parser.add_argument("--live-mysql", action="store_true",
                        help="run disposable MySQL HTTP, permission, and failure probes")
    args = parser.parse_args()
    if args.junit_dir:
        require(args.live_mysql, "--junit-dir requires --live-mysql")
        require(not args.junit_dir.exists(), "--junit-dir must not already exist")
        args.junit_dir.mkdir(parents=True)
    if args.workdir:
        workdir = args.workdir.resolve()
        temp_roots = (Path(tempfile.gettempdir()).resolve(), Path("/private/tmp").resolve())
        require(any(workdir.is_relative_to(root) and workdir != root for root in temp_roots),
                "--workdir must be under the OS temporary directory")
        require(not workdir.exists(), "--workdir must not already exist")
        workdir.mkdir(parents=True)
        cleanup = contextlib.nullcontext()
    else:
        cleanup = tempfile.TemporaryDirectory(prefix="qqq-starter-application-")
    with cleanup as disposable:
        if not args.workdir:
            workdir = Path(disposable)
        failure = None
        try:
            execute(args, workdir)
        except BaseException as error:
            failure = error
            raise
        finally:
            if args.junit_dir:
                collect_junit(workdir, args.junit_dir, failure)


def collect_junit(workdir, destination, failure):
    for project in ("orderdesk-app", "starter"):
        for report_type in ("surefire-reports", "failsafe-reports"):
            for report in (workdir / project / "target" / report_type).glob("TEST-*.xml"):
                shutil.copyfile(report, destination / f"TEST-{project}-{report.name[5:]}")
    suite = ET.Element("testsuite", name="StarterApplicationSourceAcceptance", tests="1",
                       failures="1" if failure else "0")
    case = ET.SubElement(suite, "testcase", classname="qqq.acceptance.StarterApplicationSourceAcceptance",
                         name="sourceGeneratedMySqlCrudAndNegativeProbes")
    if failure:
        ET.SubElement(case, "failure", message=str(failure), type=type(failure).__name__)
    ET.ElementTree(suite).write(destination / "TEST-starter-live.xml", encoding="utf-8",
                                xml_declaration=True)


def execute(args, workdir):
    version = qqq_version(args.qqq_source)
    print("QQQ source:", revision(args.qqq_source), flush=True)
    print("starter source:", revision(args.starter_source), flush=True)
    print("application template source:", revision(args.template_source), flush=True)
    base = ["mvn"] + ([] if args.online else ["-o"]) + ["-B", "-q"]
    if args.maven_settings:
        base += ["-s", str(args.maven_settings)]
    base += [f"-Dmaven.repo.local={args.maven_repo}"]
    modules = ",".join(("qqq-bom", "qqq-backend-core", "qqq-backend-module-rdbms",
                        "qqq-backend-module-api", "qqq-backend-module-filesystem",
                        "qqq-middleware-javalin", "qqq-middleware-api",
                        "qqq-middleware-picocli", "qqq-language-support-javascript"))
    run(base + ["-DskipTests", "-Dspotbugs.skip=true", "-Dpmd.skip=true",
                "-pl", modules, "-am", "install"], args.qqq_source)
    template = workdir / "orderdesk-app"
    starter = workdir / "starter"
    generated = stage_template(args.template_source, template)
    stage_starter(args.starter_source, starter)
    require(len(list(generated.rglob("*.java"))) == 8, "generated QBit source count changed")
    run(base + [f"-Dqqq.version={version}", "clean", "install"], template)
    run(base + [f"-Dqqq.versions.bom={version}",
                "-Dtest=StarterApplicationAcceptanceTest,StarterAppTest", "test"], starter)
    missing_dependency = workdir / "starter-without-qbit-dependency"
    stage_starter(args.starter_source, missing_dependency)
    pom = missing_dependency / "pom.xml"
    content = pom.read_text()
    dependency = """      <dependency>
         <groupId>com.qrunio.acceptance</groupId>
         <artifactId>orderdesk-app</artifactId>
         <version>0.1.0-SNAPSHOT</version>
      </dependency>
"""
    require(content.count(dependency) == 1, "expected one QBit dependency")
    pom.write_text(content.replace(dependency, ""))
    failed_build = subprocess.run(base + [f"-Dqqq.versions.bom={version}", "test-compile"],
                                  cwd=missing_dependency, capture_output=True, text=True)
    require(failed_build.returncode != 0, "host compiled without its QBit dependency")
    require("com.qrunio.acceptance.orderdesk" in failed_build.stdout + failed_build.stderr,
            (failed_build.stdout + failed_build.stderr)[-1000:])
    print(f"PASS: generated 8 Java sources; built {QBIT_COORDINATES}; integrated starter tests passed")
    print("PASS: removing host QBit dependency fails compilation")
    if args.live_mysql:
        run_live_mysql(base, starter, workdir, version)
    if args.workdir:
        print(f"fixture retained: {workdir}")
    else:
        print("disposable fixture removed")


if __name__ == "__main__":
    main()
