"""Fast checks for the disposable first-party consumer fixture."""

import tempfile
import unittest
import os
import subprocess
import xml.etree.ElementTree as ET
from unittest.mock import patch
from pathlib import Path

from run_starter_application import (GENERATED_PACKAGE, assert_source_install, copy_tracked_source,
                                     main, maven_base, qqq_version, resolve_candidate_version,
                                     source_install_command, stage_starter, stage_template)
from live_starter_application import exercise


class DisposableCopyTest(unittest.TestCase):
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
            self.assertIn("-nsu", maven_base(Path(root) / "m2", True, None))
            self.assertNotIn("-o", maven_base(Path(root) / "m2", True, None))
            self.assertIn("-o", maven_base(Path(root) / "m2", False, None))

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
                         "--live-mysql", "--junit-dir", str(reports)]

            def completed(args, workdir):
                report = workdir / "starter/target/surefire-reports/TEST-starter.xml"
                report.parent.mkdir(parents=True)
                report.write_text('<testsuite><testcase classname="StarterTest" name="works"/></testsuite>')

            with patch("sys.argv", arguments), patch("run_starter_application.execute", side_effect=completed):
                main()
            self.assertTrue((reports / "TEST-starter-starter.xml").exists())
            case = ET.parse(reports / "TEST-starter-live.xml").getroot().find("testcase")
            self.assertEqual("sourceGeneratedMySqlCrudAndNegativeProbes", case.attrib["name"])
            self.assertIsNone(case.find("failure"))
            with patch("sys.argv", arguments), self.assertRaisesRegex(AssertionError, "must not already exist"):
                main()

    def test_failed_live_runner_writes_failed_junit(self):
        with tempfile.TemporaryDirectory() as root:
            reports = Path(root) / "reports"
            arguments = ["runner", "--qqq-source", "/unused", "--starter-source", "/unused",
                         "--template-source", "/unused", "--maven-repo", "/unused",
                         "--live-mysql", "--junit-dir", str(reports)]
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

    def test_live_probe_rejects_non_loopback_urls(self):
        with self.assertRaises(ValueError):
            exercise("https://example.invalid")


if __name__ == "__main__":
    unittest.main()
