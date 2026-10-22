"""Candidate-version boundaries for the generated extension acceptance runner."""

import tempfile
import unittest
from pathlib import Path

import run_extension


class CandidateVersionTest(unittest.TestCase):
    def test_derives_release_candidate_and_ga_from_source(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory)
            for version in ("4.1.0-RC.2", "4.1.0"):
                (source / "pom.xml").write_text(
                    f"<project><properties><revision>{version}</revision></properties></project>")
                self.assertEqual(version, run_extension.resolve_candidate_version(source, None))

    def test_explicit_ci_candidate_overrides_source_revision(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory)
            (source / "pom.xml").write_text(
                "<project><properties><revision>4.1.0-SNAPSHOT</revision></properties></project>")
            self.assertEqual("0.0.0-sample-acceptance",
                             run_extension.resolve_candidate_version(source, "0.0.0-sample-acceptance"))
            with self.assertRaises(AssertionError):
                run_extension.resolve_candidate_version(source, "bad</version>")

    def test_generated_and_host_poms_use_same_candidate(self):
        version = "4.1.0-RC.2"
        template = ("<groupId>com.kingsrook.qbits</groupId>"
                    "<artifactId>qbit-example-extension</artifactId>"
                    "<qqq.version>4.0.0</qqq.version>")
        generated = run_extension.rewrite_template_coordinates(template, version)
        self.assertIn(f"<qqq.version>{version}</qqq.version>", generated)
        with tempfile.TemporaryDirectory() as directory:
            destination = Path(directory) / "host"
            run_extension.stage_host(destination, version)
            host_pom = (destination / "pom.xml").read_text()
            self.assertIn(f"<artifactId>qqq-backend-core</artifactId>\n         <version>{version}</version>", host_pom)
            self.assertNotIn("@QQQ_VERSION@", host_pom)

    def test_root_install_uses_candidate_revision(self):
        command = run_extension.framework_install_command(["mvn", "-B", "-nsu"], "4.1.0-RC.2")
        self.assertIn("-Drevision=4.1.0-RC.2", command)
        self.assertLess(command.index("-Drevision=4.1.0-RC.2"), command.index("install"))


if __name__ == "__main__":
    unittest.main()
