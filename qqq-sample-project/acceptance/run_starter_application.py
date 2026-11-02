#!/usr/bin/env python3
"""Build a renamed first-party application QBit in a copied starter host.

Inputs must be checkouts of QRun-IO/qbit-template-application and
QRun-IO/qqq-app-starter. All writes stay in a disposable work directory.
"""

import argparse
import contextlib
import hashlib
import ipaddress
import os
import re
import secrets
import shutil
import socket
import struct
import subprocess
import sys
import tempfile
import time
import uuid
import xml.etree.ElementTree as ET
from xml.sax.saxutils import escape
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urlparse
from urllib.request import urlopen

from live_starter_application import exercise, request


HERE = Path(__file__).resolve().parent
QBIT_COORDINATES = "com.qrunio.acceptance:orderdesk-app:0.1.0-SNAPSHOT"
ORIGINAL_PACKAGE = "com.kingsrook.qbits.example"
GENERATED_PACKAGE = "com.qrunio.acceptance.orderdesk"
# The image the PostgreSQL module and sample integration tests already use.
POSTGRES_IMAGE = "postgres:17-alpine"
DATABASE_NAME = "qqq_starter_test"
DATABASE_USER = "qqq_test"
LOOPBACK_HOSTS = ("127.0.0.1", "localhost")
JUNIT_CASE = "sourceGeneratedPostgresCrudAndNegativeProbes"


def run(command, cwd, env=None):
    subprocess.run(command, cwd=cwd, check=True, env=env)


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


def resolve_candidate_version(source, override):
    version = qqq_version(source) if override is None else override
    require(re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+(?:-[A-Za-z0-9][A-Za-z0-9.-]*)?", version),
            "invalid QQQ candidate version")
    return version


def source_install_command(base, modules, version):
    # Source-artifact install is not a test gate: skipped tests leave no (or stale partial) jacoco.exec,
    # so the coverage check must be skipped here too, and only here.
    return base + [f"-Drevision={version}", "-DskipTests", "-Dspotbugs.skip=true", "-Dpmd.skip=true",
                   "-Djacoco.skip=true", "-pl", modules, "-am", "install"]


def maven_base(repo, online, settings):
    # subprocess does not apply PATHEXT: resolve mvn (mvn.cmd on Windows) explicitly.
    maven = shutil.which("mvn")
    require(maven is not None, "Maven (mvn) was not found on PATH; install it or add it to PATH")
    command = [maven, "-B", "-q", "-nsu" if online else "-o"]
    if settings:
        command += ["-s", str(settings.resolve())]
    return command + [f"-Dmaven.repo.local={repo.resolve()}"]


def file_sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.digest()


def assert_source_install(source, repo, version, artifacts):
    bom = repo / "com/kingsrook/qqq/qqq-bom-pom" / version / f"qqq-bom-pom-{version}.pom"
    require(bom.is_file(), f"source candidate BOM missing: {bom}")
    for artifact in artifacts:
        filename = f"{artifact}-{version}.jar"
        reactor = source / artifact / "target" / filename
        installed = repo / "com/kingsrook/qqq" / artifact / version / filename
        require(reactor.is_file() and installed.is_file(), f"source candidate jar missing: {artifact}")
        require(file_sha256(reactor) == file_sha256(installed),
                f"installed candidate {artifact} does not match reactor source jar")
    print(f"PASS: {len(artifacts)} QQQ candidate jars match checked-out reactor artifacts at {version}", flush=True)


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


def container_gateway():
    """Default-route address inside a container: where its Docker host publishes ports."""
    if not Path("/.dockerenv").exists():
        return None
    try:
        for line in Path("/proc/net/route").read_text().splitlines()[1:]:
            fields = line.split()
            if len(fields) > 2 and fields[1] == "00000000":
                return socket.inet_ntoa(struct.pack("<L", int(fields[2], 16)))
    except (OSError, ValueError):
        pass
    return None


def database_host(override=None, environ=None, gateway=container_gateway):
    """Address the acceptance process uses to reach a port the Docker daemon publishes.

    Same precedence Testcontainers uses for the sample integration tests: an explicit
    override, TESTCONTAINERS_HOST_OVERRIDE, a tcp DOCKER_HOST, the default gateway when
    running inside a container, then loopback.
    """
    environ = os.environ if environ is None else environ
    if override:
        return override
    if environ.get("TESTCONTAINERS_HOST_OVERRIDE"):
        return environ["TESTCONTAINERS_HOST_OVERRIDE"]
    docker_host = urlparse(environ.get("DOCKER_HOST", ""))
    if docker_host.scheme == "tcp" and docker_host.hostname:
        return docker_host.hostname
    return gateway() or "127.0.0.1"


def database_environment(host, port, password):
    return {"RDBMS_VENDOR": "postgres", "RDBMS_HOSTNAME": host, "RDBMS_PORT": str(port),
            "RDBMS_DATABASE_NAME": DATABASE_NAME, "RDBMS_USERNAME": DATABASE_USER,
            "RDBMS_PASSWORD": password}


def apply_database_environment(starter, values):
    """Point the copied starter's Surefire environment at the live database.

    Surefire's environmentVariables in the starter pom override the parent process
    environment, so this is what the test JVM actually reads. The packaged server gets
    the same values through its own process environment. The staged copy is rewritten
    and then read back, so a pom that stops declaring a variable fails the run.
    """
    pom = starter / "pom.xml"
    content = pom.read_text()
    for key, value in values.items():
        content, count = re.subn(rf"(<{key}>)[^<]*(</{key}>)",
                                 lambda match, text=escape(value): match.group(1) + text + match.group(2),
                                 content)
        require(count == 1, f"expected one Surefire {key}")
    pom.write_text(content)
    namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
    declared = ET.parse(pom)
    for key, value in values.items():
        found = declared.findall(f".//m:environmentVariables/m:{key}", namespace)
        require([element.text for element in found] == [value], f"Surefire {key} does not match the database")


def published_port(name):
    output = subprocess.run(["docker", "port", name, "5432/tcp"],
                            capture_output=True, text=True, check=True).stdout
    for line in output.splitlines():
        match = re.fullmatch(r"\[?[0-9a-fA-F:.]+\]?:([0-9]+)", line.strip())
        if match:
            return int(match.group(1))
    raise AssertionError("Docker did not report the published PostgreSQL port")


def wait_for_database(name):
    # TCP on the container loopback only answers from the final server, not from the
    # socket-only server the image runs while it initializes.
    for _ in range(90):
        ready = subprocess.run(["docker", "exec", name, "psql", "-h", "127.0.0.1", "-U", DATABASE_USER,
                                "-d", DATABASE_NAME, "-tAc", "SELECT 1"], capture_output=True)
        if ready.returncode == 0:
            return
        time.sleep(1)
    raise AssertionError("disposable PostgreSQL did not become ready")


def wait_for_endpoint(host, port):
    for _ in range(30):
        try:
            with socket.create_connection((host, port), timeout=2):
                return
        except OSError:
            time.sleep(1)
    raise AssertionError(
        f"disposable PostgreSQL is running but port {port} is not reachable from the acceptance "
        f"process at {host}; set TESTCONTAINERS_HOST_OVERRIDE or --postgres-host (and "
        "--postgres-bind-address) for this Docker setup")


def stop_server(server):
    if server is None:
        return
    server.terminate()
    try:
        server.wait(timeout=10)
    except subprocess.TimeoutExpired:
        server.kill()
        server.wait()


def cleanup_quietly(server, name, container_started):
    """Release owned processes and the container without hiding the primary failure."""
    for action in (lambda: stop_server(server),
                   lambda: container_started and subprocess.run(["docker", "rm", "-f", name], capture_output=True)):
        try:
            action()
        except Exception as error:
            print(f"cleanup warning: {error}", file=sys.stderr)


def run_live_postgres(base, starter, workdir, version, postgres_host=None, postgres_bind_address=None):
    require_free_port(8000)
    host = database_host(postgres_host)
    require(re.fullmatch(r"[A-Za-z0-9.-]+", host), f"unsupported database host: {host}")
    bind = postgres_bind_address or ("127.0.0.1" if host in LOOPBACK_HOSTS else "0.0.0.0")
    try:
        ipaddress.ip_address(bind)
    except ValueError:
        raise AssertionError(f"--postgres-bind-address must be an IP address: {bind}") from None
    password = secrets.token_hex(16)
    name = "qqq-acceptance-" + uuid.uuid4().hex[:12]
    container_started = False
    server = None
    try:
        # The password reaches Docker through the environment, never the command line.
        docker_environment = dict(os.environ, POSTGRES_PASSWORD=password)
        # Set first: a failed `docker run` can still leave a created container to remove.
        container_started = True
        run(["docker", "run", "-d", "--name", name, "-p", f"{bind}::5432",
             "-e", "POSTGRES_PASSWORD", "-e", f"POSTGRES_USER={DATABASE_USER}",
             "-e", f"POSTGRES_DB={DATABASE_NAME}", POSTGRES_IMAGE], workdir, env=docker_environment)
        wait_for_database(name)
        with (HERE / "starter-application-postgres.sql").open("rb") as schema:
            loaded = subprocess.run(["docker", "exec", "-i", name, "psql", "-v", "ON_ERROR_STOP=1",
                                     "-U", DATABASE_USER, "-d", DATABASE_NAME],
                                    stdin=schema, capture_output=True)
        require(loaded.returncode == 0, "starter schema did not load: " + str(loaded.stderr))
        port = published_port(name)
        wait_for_endpoint(host, port)
        values = database_environment(host, port, password)
        environment = dict(os.environ, **values)
        apply_database_environment(starter, values)
        run(base + [f"-Dqqq.versions.bom={version}", "-DskipTests", "package"], starter, env=environment)
        run(base + [f"-Dqqq.versions.bom={version}",
                    "-Dtest=StarterApplicationLivePermissionTest", "test"], starter, env=environment)
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
            subprocess.run(["docker", "exec", name, "psql", "-U", DATABASE_USER, "-d", DATABASE_NAME,
                            "-c", 'DROP TABLE "orderDeskChildEntity"'],
                           check=True, capture_output=True)
            expect_server_error("POST", "/qqq/v1/table/orderDeskChildEntity/query")
            subprocess.run(["docker", "stop", name], check=True, capture_output=True)
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
        print("PASS: live PostgreSQL CRUD, allowed/denied permission with DB readback, schema/DB/env failures")
    finally:
        # Never raises: a cleanup problem must not replace the failure being reported.
        cleanup_quietly(server, name, container_started)


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


def stage_starter(source, destination, postgres=False):
    """Copy the starter and register the generated QBit in the copy.

    With postgres, the copy also gets the PostgreSQL backend module and builds a
    PostgreSQL backend when RDBMS_VENDOR is postgres: the starter's plain RDBMS backend
    only knows MySQL and H2 URLs. The upstream checkout is never modified.
    """
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
    postgres_dependency = """      <dependency>
         <groupId>com.kingsrook.qqq</groupId>
         <artifactId>qqq-backend-module-postgres</artifactId>
      </dependency>
""" if postgres else ""
    pom.write_text(content.replace(marker, marker + "\n" + dependency + postgres_dependency))

    provider = destination / "src/main/java/com/kingsrook/qqq/starterapp/StarterAppMetaDataProvider.java"
    java = provider.read_text()
    import_marker = "import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;"
    require(java.count(import_marker) == 1, import_marker)
    postgres_import = ("\nimport com.kingsrook.qqq.backend.module.postgres.model.metadata.PostgreSQLBackendMetaData;"
                       if postgres else "")
    java = java.replace(import_marker, import_marker + postgres_import
                        + "\nimport com.qrunio.acceptance.orderdesk.OrderDeskAppQBitConfig;\nimport com.qrunio.acceptance.orderdesk.OrderDeskAppQBitProducer;")
    if postgres:
        backend_marker = "      return new RDBMSBackendMetaData()\n         .withName(RDBMS_BACKEND_NAME)"
        require(java.count(backend_marker) == 1, backend_marker)
        java = java.replace(backend_marker, """      RDBMSBackendMetaData backend = "postgres".equals(vendor) ? new PostgreSQLBackendMetaData() : new RDBMSBackendMetaData();
      return backend
         .withName(RDBMS_BACKEND_NAME)""")
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
    parser.add_argument("--candidate-version",
                        help="isolated Maven version for the exact checked-out QQQ source")
    parser.add_argument("--maven-repo", type=Path, required=True)
    parser.add_argument("--workdir", type=Path)
    parser.add_argument("--junit-dir", type=Path,
                        help="new directory for durable consumer and live-runner JUnit reports")
    parser.add_argument("--online", action="store_true",
                        help="resolve dependencies into the dedicated Maven repository")
    parser.add_argument("--maven-settings", type=Path)
    parser.add_argument("--live-postgres", action="store_true",
                        help="run disposable PostgreSQL HTTP, permission, and failure probes")
    parser.add_argument("--postgres-host",
                        help="only when automatic discovery is wrong: address the acceptance process "
                             "uses to reach the published database port (default: "
                             "TESTCONTAINERS_HOST_OVERRIDE, a tcp DOCKER_HOST, the container's "
                             "default gateway, then 127.0.0.1)")
    parser.add_argument("--postgres-bind-address",
                        help="only when needed: Docker host address the database port is published "
                             "on (default: 127.0.0.1 for a loopback host, otherwise 0.0.0.0)")
    for removed in ("--live-mysql", "--mysql-host", "--mysql-bind-address"):
        parser.add_argument(removed, nargs="?", const="", help=argparse.SUPPRESS)
    args = parser.parse_args()
    if any(getattr(args, option.lstrip("-").replace("-", "_")) is not None
           for option in ("--live-mysql", "--mysql-host", "--mysql-bind-address")):
        parser.error("the MySQL live acceptance was replaced; use --live-postgres "
                     "(optional --postgres-host/--postgres-bind-address)")
    if args.junit_dir:
        require(args.live_postgres, "--junit-dir requires --live-postgres")
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
                         name=JUNIT_CASE)
    if failure:
        ET.SubElement(case, "failure", message=str(failure), type=type(failure).__name__)
    ET.ElementTree(suite).write(destination / "TEST-starter-live.xml", encoding="utf-8",
                                xml_declaration=True)


def execute(args, workdir):
    version = resolve_candidate_version(args.qqq_source, args.candidate_version)
    print("QQQ source:", revision(args.qqq_source), flush=True)
    print("QQQ root revision:", qqq_version(args.qqq_source), flush=True)
    print("QQQ candidate version:", version, flush=True)
    print("starter source:", revision(args.starter_source), flush=True)
    print("application template source:", revision(args.template_source), flush=True)
    repo = args.maven_repo.resolve()
    base = maven_base(repo, args.online, args.maven_settings)
    artifacts = ("qqq-backend-core", "qqq-backend-module-rdbms", "qqq-backend-module-api",
                 "qqq-backend-module-filesystem", "qqq-middleware-javalin", "qqq-middleware-api",
                 "qqq-middleware-picocli", "qqq-language-support-javascript")
    if args.live_postgres:
        artifacts += ("qqq-backend-module-postgres",)
    modules = ",".join(("qqq-bom",) + artifacts)
    run(source_install_command(base, modules, version), args.qqq_source)
    assert_source_install(args.qqq_source, repo, version, artifacts)
    template = workdir / "orderdesk-app"
    starter = workdir / "starter"
    generated = stage_template(args.template_source, template)
    stage_starter(args.starter_source, starter, postgres=args.live_postgres)
    require(len(list(generated.rglob("*.java"))) == 8, "generated QBit source count changed")
    run(base + [f"-Dqqq.version={version}", "clean", "install"], template)
    run(base + [f"-Dqqq.versions.bom={version}",
                "-Dtest=StarterApplicationAcceptanceTest,StarterAppTest", "test"], starter)
    missing_dependency = workdir / "starter-without-qbit-dependency"
    stage_starter(args.starter_source, missing_dependency, postgres=args.live_postgres)
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
    if args.live_postgres:
        run_live_postgres(base, starter, workdir, version, postgres_host=args.postgres_host,
                          postgres_bind_address=args.postgres_bind_address)
    if args.workdir:
        print(f"fixture retained: {workdir}")
    else:
        print("disposable fixture removed")


if __name__ == "__main__":
    main()
