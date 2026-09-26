"""Fast checks for the disposable first-party consumer fixture."""

import tempfile
import unittest
import os
import subprocess
from unittest.mock import patch
from pathlib import Path

from run_starter_application import GENERATED_PACKAGE, copy_tracked_source, main, stage_starter, stage_template
from live_starter_application import exercise


class DisposableCopyTest(unittest.TestCase):
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
