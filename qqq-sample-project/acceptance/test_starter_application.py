"""Fast checks for the disposable first-party consumer fixture."""

import tempfile
import unittest
import os
import subprocess
import xml.etree.ElementTree as ET
from unittest.mock import patch, MagicMock
from types import SimpleNamespace
from pathlib import Path

from run_starter_application import (GENERATED_PACKAGE, JUNIT_CASE, apply_database_environment,
                                     assert_source_install, copy_tracked_source, database_host,
                                     main, maven_base, qqq_version, resolve_candidate_version,
                                     source_install_command, stage_starter, stage_template,
                                     run_live_postgres)
from live_starter_application import exercise

DATABASE_KEYS = ("RDBMS_VENDOR", "RDBMS_HOSTNAME", "RDBMS_PORT", "RDBMS_DATABASE_NAME",
                 "RDBMS_USERNAME", "RDBMS_PASSWORD")
SUREFIRE_POM = ('<project xmlns="http://maven.apache.org/POM/4.0.0"><build><plugins><plugin><configuration>'
                "<environmentVariables>" + "".join(f"<{key}>old</{key}>" for key in DATABASE_KEYS)
                + "</environmentVariables></configuration></plugin></plugins></build></project>")


class DisposableCopyTest(unittest.TestCase):
    def live_run(self, work, options=None, fail_exercise=None, cleanup_failure=None):
        starter = work / "starter"
        (starter / "target").mkdir(parents=True)
        (starter / "target/qqq-app-starter-test.jar").touch()
        (starter / "pom.xml").write_text(SUREFIRE_POM)
        server = MagicMock()
        server.poll.return_value = None
        response = MagicMock()
        response.__enter__.return_value.status = 200
        commands = []

        def child(command, **kwargs):
            commands.append(command)
            if command[0] == "java":
                self.assertFalse(any(key.startswith("RDBMS_") for key in kwargs["env"]))
                return SimpleNamespace(returncode=1, stdout="Missing at least one env. var", stderr="")
            if command[:2] == ["docker", "port"]:
                return SimpleNamespace(returncode=0, stdout="0.0.0.0:49153\n[::]:49153\n")
            if command[:3] == ["docker", "rm", "-f"] and cleanup_failure:
                raise cleanup_failure
            return SimpleNamespace(returncode=0, stdout="", stderr=b"")

        with patch("run_starter_application.require_free_port") as ports, \
             patch("run_starter_application.run") as run, \
             patch("run_starter_application.subprocess.run", side_effect=child), \
             patch("run_starter_application.subprocess.Popen", return_value=server) as launch, \
             patch("run_starter_application.urlopen", return_value=response), \
             patch("run_starter_application.wait_for_endpoint") as endpoint, \
             patch("run_starter_application.exercise", side_effect=fail_exercise) as exercise_live, \
             patch("run_starter_application.expect_server_error") as negative:
            try:
                run_live_postgres(["mvn"], starter, work, "4.1.0-SNAPSHOT", **(options or {}))
            finally:
                self.captured = SimpleNamespace(starter=starter, ports=ports, run=run, launch=launch,
                                                endpoint=endpoint, exercise=exercise_live,
                                                negative=negative, commands=commands)

    def test_live_postgres_endpoint_reaches_maven_server_and_docker_cleanup(self):
        for host in ("127.0.0.1", "172.17.0.1"):
            with self.subTest(host=host), tempfile.TemporaryDirectory() as root:
                self.live_run(Path(root), {"postgres_host": host})
                seen = self.captured
                docker = seen.run.call_args_list[0].args[0]
                bind = "127.0.0.1" if host == "127.0.0.1" else "0.0.0.0"
                # Dynamic host port, never a fixed 3306/5432 on the Docker host.
                self.assertEqual(f"{bind}::5432", docker[docker.index("-p") + 1])
                password = seen.run.call_args_list[0].kwargs["env"]["POSTGRES_PASSWORD"]
                self.assertNotIn(password, " ".join(docker))
                # The Maven (and therefore Surefire) process, the staged Surefire block and the
                # packaged server all receive the same endpoint and credentials.
                maven_runs = seen.run.call_args_list[1:3]
                self.assertEqual(2, len(maven_runs))
                server_env = seen.launch.call_args.kwargs["env"]
                staged = ET.parse(seen.starter / "pom.xml")
                ns = {"m": "http://maven.apache.org/POM/4.0.0"}
                for key in DATABASE_KEYS:
                    declared = [e.text for e in staged.findall(f".//m:environmentVariables/m:{key}", ns)]
                    self.assertEqual([server_env[key]], declared, key)
                    for call in maven_runs:
                        self.assertEqual(server_env[key], call.kwargs["env"][key], key)
                self.assertEqual(("postgres", host, "49153", password),
                                 tuple(server_env[key] for key in ("RDBMS_VENDOR", "RDBMS_HOSTNAME",
                                                                   "RDBMS_PORT", "RDBMS_PASSWORD")))
                seen.endpoint.assert_called_once_with(host, 49153)
                self.assertEqual([8000], [call.args[0] for call in seen.ports.call_args_list])
                seen.exercise.assert_called_once_with("http://127.0.0.1:8000")
                self.assertEqual(2, seen.negative.call_count)
                name = docker[docker.index("--name") + 1]
                self.assertIn(["docker", "rm", "-f", name], seen.commands)

    def test_failed_probe_still_removes_container_and_is_not_masked_by_cleanup(self):
        with tempfile.TemporaryDirectory() as root:
            with self.assertRaisesRegex(AssertionError, "probe failed"):
                self.live_run(Path(root), {"postgres_host": "127.0.0.1"},
                              fail_exercise=AssertionError("probe failed"),
                              cleanup_failure=OSError("docker unavailable"))
            self.assertTrue(any(command[:3] == ["docker", "rm", "-f"] for command in self.captured.commands))
            self.captured.launch.return_value.terminate.assert_called()

    def test_database_host_prefers_override_then_testcontainers_then_docker_host(self):
        gateway = lambda: "172.19.0.1"
        self.assertEqual("db.example", database_host("db.example", {"TESTCONTAINERS_HOST_OVERRIDE": "x"}, gateway))
        self.assertEqual("10.0.0.5", database_host(None, {"TESTCONTAINERS_HOST_OVERRIDE": "10.0.0.5",
                                                         "DOCKER_HOST": "tcp://10.9.9.9:2375"}, gateway))
        self.assertEqual("10.9.9.9", database_host(None, {"DOCKER_HOST": "tcp://10.9.9.9:2375"}, gateway))
        self.assertEqual("172.19.0.1", database_host(None, {"DOCKER_HOST": "unix:///var/run/docker.sock"}, gateway))
        self.assertEqual("127.0.0.1", database_host(None, {}, lambda: None))

    def test_database_environment_is_written_to_every_surefire_variable_or_fails(self):
        with tempfile.TemporaryDirectory() as root:
            starter = Path(root)
            (starter / "pom.xml").write_text(SUREFIRE_POM)
            values = {key: f"v-{index}" for index, key in enumerate(DATABASE_KEYS)}
            apply_database_environment(starter, values)
            declared = ET.parse(starter / "pom.xml").getroot().iter()
            self.assertEqual(set(values.values()),
                             {element.text for element in declared if element.tag.endswith(tuple(DATABASE_KEYS))})
            (starter / "pom.xml").write_text(SUREFIRE_POM.replace("<RDBMS_PORT>old</RDBMS_PORT>", ""))
            with self.assertRaisesRegex(AssertionError, "expected one Surefire RDBMS_PORT"):
                apply_database_environment(starter, values)

    def test_qqq_revision_uses_literal_source_version_across_release_cycles(self):
        with tempfile.TemporaryDirectory() as root:
            pom = Path(root) / "pom.xml"
            prefix = '<project xmlns="http://maven.apache.org/POM/4.0.0"><properties><revision>'
            suffix = '</revision></properties></project>'
            for version in ("4.1.0-SNAPSHOT", "4.1.0-RC.1", "4.1.0", "4.2.0-SNAPSHOT"):
                pom.write_text(prefix + version + suffix)
                self.assertEqual(version, qqq_version(Path(root)))
            pom.write_text(prefix + '${revision}' + suffix)
            with self.assertRaisesRegex(AssertionError, "literal root revision"):
                qqq_version(Path(root))

    def test_candidate_override_is_used_for_source_install(self):
        with tempfile.TemporaryDirectory() as root:
            pom = Path(root) / "pom.xml"
            pom.write_text('<project xmlns="http://maven.apache.org/POM/4.0.0">'
                           '<properties><revision>4.2.0-SNAPSHOT</revision></properties></project>')
            self.assertEqual("4.2.0-SNAPSHOT", resolve_candidate_version(Path(root), None))
            version = resolve_candidate_version(Path(root), "0.0.0-sample-acceptance")
            self.assertEqual("0.0.0-sample-acceptance", version)
            command = source_install_command(["mvn", "-B", "-nsu"], "qqq-bom,qqq-backend-core", version)
            self.assertIn("-Drevision=0.0.0-sample-acceptance", command)
            self.assertLess(command.index("-Drevision=0.0.0-sample-acceptance"), command.index("install"))
            with self.assertRaisesRegex(AssertionError, "invalid QQQ candidate version"):
                resolve_candidate_version(Path(root), "$(bad)")
            with patch("run_starter_application.shutil.which", return_value=r"C:\tools\maven\bin\mvn.cmd") as which:
                self.assertIn("-nsu", maven_base(Path(root) / "m2", True, None))
                self.assertNotIn("-o", maven_base(Path(root) / "m2", True, None))
                self.assertIn("-o", maven_base(Path(root) / "m2", False, None))
                # The resolved executable (mvn.cmd on Windows) is what subprocess launches.
                self.assertEqual(r"C:\tools\maven\bin\mvn.cmd", maven_base(Path(root) / "m2", True, None)[0])
                which.assert_called_with("mvn")
            with patch("run_starter_application.shutil.which", return_value=None):
                with self.assertRaisesRegex(AssertionError, r"Maven \(mvn\) was not found on PATH"):
                    maven_base(Path(root) / "m2", True, None)

    def test_installed_candidate_jar_must_match_reactor_source_jar(self):
        with tempfile.TemporaryDirectory() as root:
            root = Path(root)
            source = root / "source"
            repo = root / "m2"
            artifact = "qqq-backend-core"
            version = "0.0.0-sample-acceptance"
            bom = repo / "com/kingsrook/qqq/qqq-bom-pom" / version / f"qqq-bom-pom-{version}.pom"
            bom.parent.mkdir(parents=True)
            bom.write_text("<project/>")
            jar = source / artifact / "target" / f"{artifact}-{version}.jar"
            installed = repo / "com/kingsrook/qqq" / artifact / version / jar.name
            jar.parent.mkdir(parents=True)
            installed.parent.mkdir(parents=True)
            jar.write_bytes(b"reactor")
            installed.write_bytes(b"reactor")
            assert_source_install(source, repo, version, (artifact,))
            installed.write_bytes(b"different")
            with self.assertRaisesRegex(AssertionError, "does not match reactor"):
                assert_source_install(source, repo, version, (artifact,))

    def test_copies_only_safe_tracked_files(self):
        with tempfile.TemporaryDirectory() as root:
            source = Path(root) / "source"
            destination = Path(root) / "copy"
            source.mkdir()
            subprocess.run(["git", "init", "-q", str(source)], check=True)
            for name in ("pom.xml", ".env", ".env.local", "config/credentials.json",
                         "config/key.pem", "config/keystore.p12", ".npmrc", "settings.xml",
                         "secrets/config.json", "application.local.properties", "id_rsa"):
                path = source / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(name)
            subprocess.run(["git", "-C", str(source), "add", "-f", "."], check=True)
            (source / "untracked-secret.txt").write_text("secret")
            copy_tracked_source(source, destination)
            self.assertEqual(["pom.xml"], [str(path.relative_to(destination))
                                           for path in destination.rglob("*") if path.is_file()])

    def test_default_fixture_is_removed_after_runner(self):
        captured = []
        arguments = ["runner", "--qqq-source", "/unused", "--starter-source", "/unused",
                     "--template-source", "/unused", "--maven-repo", "/unused"]
        with patch("sys.argv", arguments), patch("run_starter_application.execute",
                                                side_effect=lambda args, path: captured.append(path)):
            main()
        self.assertEqual(1, len(captured))
        self.assertFalse(captured[0].exists())

    def test_existing_explicit_fixture_is_rejected(self):
        with tempfile.TemporaryDirectory() as existing:
            arguments = ["runner", "--qqq-source", "/unused", "--starter-source", "/unused",
                         "--template-source", "/unused", "--maven-repo", "/unused",
                         "--workdir", existing]
            with patch("sys.argv", arguments), self.assertRaisesRegex(AssertionError, "must not already exist"):
                main()

    def test_live_junit_survives_disposable_fixture_and_copies_real_reports(self):
        with tempfile.TemporaryDirectory() as root:
            reports = Path(root) / "reports"
            arguments = ["runner", "--qqq-source", "/unused", "--starter-source", "/unused",
                         "--template-source", "/unused", "--maven-repo", "/unused",
                         "--live-postgres", "--junit-dir", str(reports)]

            def completed(args, workdir):
                report = workdir / "starter/target/surefire-reports/TEST-starter.xml"
                report.parent.mkdir(parents=True)
                report.write_text('<testsuite><testcase classname="StarterTest" name="works"/></testsuite>')

            with patch("sys.argv", arguments), patch("run_starter_application.execute", side_effect=completed):
                main()
            self.assertTrue((reports / "TEST-starter-starter.xml").exists())
            case = ET.parse(reports / "TEST-starter-live.xml").getroot().find("testcase")
            self.assertEqual(JUNIT_CASE, case.attrib["name"])
            self.assertEqual("sourceGeneratedPostgresCrudAndNegativeProbes", JUNIT_CASE)
            self.assertIsNone(case.find("failure"))
            with patch("sys.argv", arguments), self.assertRaisesRegex(AssertionError, "must not already exist"):
                main()

    def test_failed_live_runner_writes_failed_junit(self):
        with tempfile.TemporaryDirectory() as root:
            reports = Path(root) / "reports"
            arguments = ["runner", "--qqq-source", "/unused", "--starter-source", "/unused",
                         "--template-source", "/unused", "--maven-repo", "/unused",
                         "--live-postgres", "--junit-dir", str(reports)]
            with patch("sys.argv", arguments), patch("run_starter_application.execute",
                                                    side_effect=AssertionError("failed probe")):
                with self.assertRaisesRegex(AssertionError, "failed probe"):
                    main()
            case = ET.parse(reports / "TEST-starter-live.xml").getroot().find("testcase")
            self.assertIn("failed probe", case.find("failure").attrib["message"])


@unittest.skipUnless(os.environ.get("QQQ_STARTER_SOURCE") and os.environ.get("QQQ_TEMPLATE_SOURCE"),
                     "provide first-party source checkouts")
class StarterApplicationFixtureTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.acceptance = Path(__file__).resolve().parent
        self.template = Path(os.environ["QQQ_TEMPLATE_SOURCE"])
        self.starter = Path(os.environ["QQQ_STARTER_SOURCE"])

    def test_stages_distinct_application_sources_with_apache_headers(self):
        generated = stage_template(self.template, self.root / "application")
        sources = list(generated.rglob("*.java"))
        self.assertEqual(8, len(sources))
        for source in sources:
            contents = source.read_text()
            self.assertIn("Licensed under the Apache License, Version 2.0", contents)
            self.assertIn("package " + GENERATED_PACKAGE, contents)
            self.assertNotIn("com.kingsrook.qbits.example", contents)
        self.assertIn("<artifactId>orderdesk-app</artifactId>",
                      (self.root / "application/pom.xml").read_text())
        upstream_test = self.template / "src/test/java/com/kingsrook/qbits/example/ExampleAppQBitProducerTest.java"
        if upstream_test.exists():
            renamed_test = self.root / "application/src/test/java/com/qrunio/acceptance/orderdesk/OrderDeskAppQBitProducerTest.java"
            self.assertTrue(renamed_test.exists())
            self.assertIn("class OrderDeskAppQBitProducerTest", renamed_test.read_text())

    def test_stages_host_registration_and_dependency(self):
        stage_starter(self.starter, self.root / "starter")
        pom = (self.root / "starter/pom.xml").read_text()
        provider = (self.root / "starter/src/main/java/com/kingsrook/qqq/starterapp/StarterAppMetaDataProvider.java").read_text()
        self.assertIn("<artifactId>orderdesk-app</artifactId>", pom)
        self.assertIn('produce(qInstance, "acceptance")', provider)
        self.assertIn("withBackendName(RDBMS_BACKEND_NAME)", provider)
        self.assertNotIn("orderdesk-app", (self.starter / "pom.xml").read_text())

    def test_stages_postgres_backend_and_live_environment_without_changing_source(self):
        original_pom = (self.starter / "pom.xml").read_bytes()
        provider_path = "src/main/java/com/kingsrook/qqq/starterapp/StarterAppMetaDataProvider.java"
        original_provider = (self.starter / provider_path).read_bytes()
        staged = self.root / "postgres-starter"
        stage_starter(self.starter, staged, postgres=True)
        provider = (staged / provider_path).read_text()
        self.assertIn("new PostgreSQLBackendMetaData()", provider)
        self.assertIn("<artifactId>qqq-backend-module-postgres</artifactId>", (staged / "pom.xml").read_text())
        # The real starter pom declares every variable the live run rewrites.
        values = {key: "x-" + key for key in DATABASE_KEYS}
        apply_database_environment(staged, values)
        ns = {"m": "http://maven.apache.org/POM/4.0.0"}
        for key in DATABASE_KEYS:
            found = ET.parse(staged / "pom.xml").findall(f".//m:environmentVariables/m:{key}", ns)
            self.assertEqual(["x-" + key], [element.text for element in found])
        self.assertEqual(original_pom, (self.starter / "pom.xml").read_bytes())
        self.assertEqual(original_provider, (self.starter / provider_path).read_bytes())

    def test_live_probe_rejects_non_loopback_urls(self):
        with self.assertRaises(ValueError):
            exercise("https://example.invalid")


if __name__ == "__main__":
    unittest.main()
