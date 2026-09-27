"""Fail-closed checks for candidate-resolved sample bootstrap acceptance."""

import tempfile
import unittest
from pathlib import Path
import zipfile

from bootstrap_acceptance import (expected_libraries, make_consumer, require_artifacts,
                                  require_new_workdir, require_reports, validate_version)


ROOT = Path(__file__).resolve().parent.parent


class BootstrapAcceptanceTest(unittest.TestCase):
    def test_bom_covers_prior_sixteen_libraries_and_new_esb(self):
        libraries = expected_libraries(ROOT / "qqq-bom/pom.xml")
        self.assertEqual(17, len(libraries))
        self.assertIn("qqq-esb", libraries)
        self.assertIn("qqq-utility-lambdas", libraries)

    def test_bom_drift_requires_review(self):
        with tempfile.TemporaryDirectory() as root:
            bom = Path(root) / "pom.xml"
            original = (ROOT / "qqq-bom/pom.xml").read_text()
            bom.write_text(original.replace("<artifactId>qqq-esb</artifactId>",
                                            "<artifactId>qqq-unreviewed</artifactId>"))
            with self.assertRaisesRegex(ValueError, "inventory"):
                expected_libraries(bom)

    def test_published_version_must_be_literal_and_immutable(self):
        for version in ("4.1.0-SNAPSHOT", "${revision}", "LATEST", "4.1.0-RC.1-broken"):
            with self.subTest(version=version), self.assertRaises(ValueError):
                validate_version(version, "published")
        self.assertEqual("4.1.0-RC.1", validate_version("4.1.0-RC.1", "published"))
        self.assertEqual("4.1.0", validate_version("4.1.0", "source"))
        self.assertEqual("4.1.0-RC.1", validate_version("4.1.0-RC.1", "source"))
        self.assertEqual("4.1.0-SNAPSHOT", validate_version("4.1.0-SNAPSHOT", "source"))
        with self.assertRaises(ValueError):
            validate_version("${revision}", "source")

    def test_consumer_imports_candidate_bom_and_declares_every_library_without_versions(self):
        with tempfile.TemporaryDirectory() as root:
            path = Path(root) / "pom.xml"
            libraries = expected_libraries(ROOT / "qqq-bom/pom.xml")
            make_consumer(path, "4.1.0-RC.1", libraries)
            xml = path.read_text()
            self.assertIn("qqq-bom-pom", xml)
            self.assertEqual(1, xml.count("<version>4.1.0-RC.1</version>"))
            for library in libraries:
                self.assertEqual(1, xml.count(f"<artifactId>{library}</artifactId>"))

    def test_missing_or_stale_candidate_artifact_fails(self):
        with tempfile.TemporaryDirectory() as root:
            directory = Path(root)
            libraries = ("qqq-backend-core", "qqq-esb")
            with zipfile.ZipFile(directory / "qqq-backend-core-4.0.0.jar", "w") as jar:
                jar.writestr("marker", "stale")
            with self.assertRaisesRegex(AssertionError, "qqq-backend-core"):
                require_artifacts(directory, libraries, "4.1.0")
            with zipfile.ZipFile(directory / "qqq-backend-core-4.1.0.jar", "w") as jar:
                jar.writestr("marker", "candidate")
            with self.assertRaisesRegex(AssertionError, "qqq-esb"):
                require_artifacts(directory, libraries, "4.1.0")
            with zipfile.ZipFile(directory / "qqq-esb-4.1.0.jar", "w") as jar:
                jar.writestr("marker", "candidate")
            require_artifacts(directory, libraries, "4.1.0")

    def test_existing_fixture_is_not_overwritten(self):
        with tempfile.TemporaryDirectory() as root:
            with self.assertRaisesRegex(ValueError, "already exists"):
                require_new_workdir(Path(root))
        with self.assertRaisesRegex(ValueError, "OS temporary"):
            require_new_workdir(Path(ROOT.anchor) / "unowned-bootstrap-fixture")

    def test_skipped_or_missing_packaged_startup_report_cannot_pass(self):
        with tempfile.TemporaryDirectory() as root:
            sample = Path(root)
            reports = sample / "target/failsafe-reports"
            reports.mkdir(parents=True)
            (reports / "TEST-com.kingsrook.sampleapp.SamplePackagedConfigurationIT.xml").write_text(
                '<testsuite tests="4" errors="0" failures="0" skipped="1"/>')
            with self.assertRaises(AssertionError):
                require_reports(sample)


if __name__ == "__main__":
    unittest.main()
