"""Focused contracts for the public BOM consumer verifier."""

import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import xml.etree.ElementTree as ET


SCRIPT = Path(__file__).with_name("verify-published-bom.py")
spec = importlib.util.spec_from_file_location("verify_published_bom", SCRIPT)
verifier = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verifier)

POM = """<project xmlns="http://maven.apache.org/POM/4.0.0">
  <dependencyManagement><dependencies>
    <dependency><groupId>com.kingsrook.qqq</groupId><artifactId>qqq-backend-core</artifactId><version>${revision}</version></dependency>
    <dependency><groupId>com.kingsrook.qqq</groupId><artifactId>qqq-esb</artifactId><version>${revision}</version></dependency>
    <dependency><groupId>com.kingsrook.qqq</groupId><artifactId>qqq-frontend-next</artifactId><version>0.2.1</version></dependency>
  </dependencies></dependencyManagement>
</project>"""


class PublishedBomTest(unittest.TestCase):
    def test_parser_uses_source_bom_modules_and_next_pin(self):
        inventory = verifier.parse_bom(POM)
        self.assertEqual(("qqq-backend-core", "qqq-esb"), inventory.modules)
        self.assertEqual("0.2.1", inventory.next_version)

    def test_parser_rejects_snapshot_next_pin(self):
        with self.assertRaisesRegex(ValueError, "Next.*SNAPSHOT"):
            verifier.parse_bom(POM.replace("0.2.1", "0.2.1-SNAPSHOT"))

    def test_parser_rejects_duplicate_and_unmanaged_source_entries(self):
        module = ('<dependency><groupId>com.kingsrook.qqq</groupId>'
                  '<artifactId>qqq-esb</artifactId><version>${revision}</version></dependency>')
        with self.assertRaisesRegex(ValueError, "Duplicate"):
            verifier.parse_bom(POM.replace("</dependencies>", module + "</dependencies>"))
        with self.assertRaisesRegex(ValueError, "not managed"):
            verifier.parse_bom(POM.replace("<version>${revision}</version>",
                                           "<version>4.0.0</version>", 1))

    def test_current_source_bom_drives_inventory(self):
        source = SCRIPT.parent.parent / "qqq-bom/pom.xml"
        inventory = verifier.parse_bom(source.read_text())
        self.assertIn("qqq-esb", inventory.modules)
        self.assertEqual("0.2.1", inventory.next_version)
        self.assertEqual(len(inventory.modules) + 1,
                         len(ET.fromstring(verifier.build_consumer_pom("4.1.0-RC.1", inventory))
                             .findall("m:dependencies/m:dependency", verifier.NAMESPACE)))

    def test_candidate_version_must_be_literal_release_or_rc(self):
        for invalid in ("4.1.0-SNAPSHOT", "${revision}", "4.1.0+local", "4.1", "4.1.0-RC.1-SNAPSHOT"):
            with self.subTest(invalid=invalid), self.assertRaisesRegex(ValueError, "literal"):
                verifier.validate_version(invalid)
        for valid in ("4.0.0", "4.1.0-RC.1"):
            with self.subTest(valid=valid):
                self.assertEqual(valid, verifier.validate_version(valid))

    def test_consumer_imports_candidate_bom_and_requests_every_managed_jar_without_versions(self):
        consumer = ET.fromstring(verifier.build_consumer_pom("4.1.0-RC.1", verifier.parse_bom(POM)))
        ns = {"m": "http://maven.apache.org/POM/4.0.0"}
        imported = consumer.find("m:dependencyManagement/m:dependencies/m:dependency", ns)
        self.assertEqual("qqq-bom-pom", imported.findtext("m:artifactId", namespaces=ns))
        self.assertEqual("4.1.0-RC.1", imported.findtext("m:version", namespaces=ns))
        self.assertEqual("import", imported.findtext("m:scope", namespaces=ns))
        dependencies = consumer.findall("m:dependencies/m:dependency", ns)
        self.assertEqual(["qqq-backend-core", "qqq-esb", "qqq-frontend-next"],
                         [dep.findtext("m:artifactId", namespaces=ns) for dep in dependencies])
        self.assertTrue(all(dep.find("m:version", ns) is None for dep in dependencies))
        self.assertEqual("https://repo.maven.apache.org/maven2/",
                         consumer.findtext("m:repositories/m:repository/m:url", namespaces=ns))

    def test_tree_rejects_missing_mismatched_and_snapshot_artifacts(self):
        inventory = verifier.parse_bom(POM)
        tree = self.tree()
        verifier.check_tree(tree, "4.1.0-RC.1", inventory)
        cases = (
            (lambda t: t["children"].pop(), "missing"),
            (lambda t: t["children"][1].update(version="4.0.0"), "version"),
            (lambda t: t["children"][0].update(version="4.1.0-SNAPSHOT"), "SNAPSHOT"),
            (lambda t: t["children"][0]["children"].append(
                dict(groupId="other", artifactId="transitive", version="1-SNAPSHOT", type="jar")), "SNAPSHOT"),
        )
        for mutate, error in cases:
            with self.subTest(error=error):
                changed = self.tree()
                mutate(changed)
                with self.assertRaisesRegex(ValueError, error):
                    verifier.check_tree(changed, "4.1.0-RC.1", inventory)

    @staticmethod
    def tree():
        return {"groupId": "qqq.verification", "artifactId": "published-bom-consumer",
                "version": "1.0.0", "type": "jar", "children": [
                    {"groupId": "com.kingsrook.qqq", "artifactId": name,
                     "version": version, "type": "jar", "children": []}
                    for name, version in (("qqq-backend-core", "4.1.0-RC.1"),
                                          ("qqq-esb", "4.1.0-RC.1"),
                                          ("qqq-frontend-next", "0.2.1"))]}

    def test_fake_maven_uses_isolated_settings_cache_and_keeps_evidence(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            fake = root / "mvn"
            fake.write_text("#!/usr/bin/env python3\n"
                            "import json, pathlib, sys\n"
                            "args = sys.argv[1:]\n"
                            "repo = pathlib.Path(next(x.split('=', 1)[1] for x in args if x.startswith('-Dmaven.repo.local=')))\n"
                            "out = pathlib.Path(next(x.split('=', 1)[1] for x in args if x.startswith('-DoutputFile=')))\n"
                            "for artifact, version, ext in [('qqq-bom-pom','4.1.0-RC.1','pom'),"
                            "('qqq-backend-core','4.1.0-RC.1','jar'),('qqq-esb','4.1.0-RC.1','jar'),"
                            "('qqq-frontend-next','0.2.1','jar')]:\n"
                            "  path = repo / 'com/kingsrook/qqq' / artifact / version / (artifact+'-'+version+'.'+ext)\n"
                            "  path.parent.mkdir(parents=True, exist_ok=True); path.write_text('fixture')\n"
                            f"out.write_text({json.dumps(json.dumps(self.tree()))})\n")
            fake.chmod(0o755)
            evidence = verifier.verify("4.1.0-RC.1", POM, root / "evidence", str(fake), "test-sha")
            self.assertEqual("pass", evidence["status"])
            run_dir = Path(evidence["directory"])
            self.assertTrue((run_dir / "maven.log").exists())
            self.assertTrue((run_dir / "dependency-tree.json").exists())
            self.assertTrue((run_dir / "evidence.json").exists())
            args = evidence["maven_command"]
            self.assertIn("-s", args)
            self.assertIn("-gs", args)
            self.assertTrue(any(x.startswith("-Dmaven.repo.local=") for x in args))
            self.assertEqual(4, len(evidence["artifacts"]))
            self.assertEqual("test-sha", evidence["source_commit"])
            self.assertEqual(64, len(evidence["published_bom_sha256"]))
            self.assertEqual('<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"/>',
                             (run_dir / "settings.xml").read_text().strip())

    def test_failed_maven_is_recorded_and_fails_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            fake = root / "mvn"
            fake.write_text("#!/usr/bin/env python3\nimport sys\nprint('fixture failure')\nsys.exit(3)\n")
            fake.chmod(0o755)
            with self.assertRaisesRegex(ValueError, "Maven failed"):
                verifier.verify("4.1.0-RC.1", POM, root / "evidence", str(fake), "test-sha")
            evidence = json.loads(next((root / "evidence").glob("*/evidence.json")).read_text())
            self.assertEqual("fail", evidence["status"])
            self.assertEqual(3, evidence["maven_exit_code"])


if __name__ == "__main__":
    unittest.main()
