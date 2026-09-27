"""Focused contracts for the public BOM consumer verifier."""

import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
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
            (lambda t: t["children"][0].update(scope="runtime"), "scope"),
            (lambda t: t["children"][0]["children"].append(
                dict(groupId="other", artifactId="transitive", version="1-SNAPSHOT", type="jar")), "SNAPSHOT"),
        )
        for mutate, error in cases:
            with self.subTest(error=error):
                changed = self.tree()
                mutate(changed)
                with self.assertRaisesRegex(ValueError, error):
                    verifier.check_tree(changed, "4.1.0-RC.1", inventory)

    def test_published_bom_matches_complete_normalized_source_management(self):
        published = POM.replace("${revision}", "4.1.0-RC.1")
        root = ET.fromstring(published)
        dependencies = root.find("m:dependencyManagement/m:dependencies", verifier.NAMESPACE)
        dependencies[:] = list(reversed(dependencies))
        for dependency in dependencies:
            ET.SubElement(dependency, "{http://maven.apache.org/POM/4.0.0}type").text = "jar"
            ET.SubElement(dependency, "{http://maven.apache.org/POM/4.0.0}scope").text = "compile"
        expected, observed = verifier.compare_boms(POM, ET.tostring(root, encoding="unicode"), "4.1.0-RC.1")
        self.assertEqual(expected, observed)
        self.assertEqual(3, len(expected))

    def test_published_bom_rejects_extra_changed_and_missing_management(self):
        published = POM.replace("${revision}", "4.1.0-RC.1")
        extra = ('<dependency><groupId>other.group</groupId><artifactId>extra</artifactId>'
                 '<version>1.0.0</version></dependency>')
        changed = published.replace("<version>0.2.1</version>", "<version>0.2.2</version>")
        changed_type = published.replace("<version>0.2.1</version>",
                                         "<version>0.2.1</version><type>pom</type>")
        changed_scope = published.replace("<version>0.2.1</version>",
                                          "<version>0.2.1</version><scope>runtime</scope>")
        missing = published.replace('<dependency><groupId>com.kingsrook.qqq</groupId>'
                                    '<artifactId>qqq-esb</artifactId><version>4.1.0-RC.1</version></dependency>', '')
        for candidate in (published.replace("</dependencies>", extra + "</dependencies>"),
                          changed, changed_type, changed_scope, missing):
            with self.subTest(candidate=candidate), self.assertRaisesRegex(ValueError, "BOM management mismatch"):
                verifier.compare_boms(POM, candidate, "4.1.0-RC.1")

    def test_repository_marker_is_supplemental_evidence(self):
        with tempfile.TemporaryDirectory() as directory:
            jar = Path(directory) / "qqq-esb-4.1.0-RC.1.jar"
            jar.write_text("fixture")
            marker = jar.parent / "_remote.repositories"
            marker.write_text(f"{jar.name}>private=\n")
            self.assertEqual(["private"], verifier.repository_markers(jar))
            marker.unlink()
            self.assertEqual([], verifier.repository_markers(jar))
            marker.write_text(f"# Maven Resolver\n{jar.name}>central=\n")
            self.assertEqual(["central"], verifier.repository_markers(jar))
            verifier.check_repository_markers(jar)
            marker.write_text(f"{jar.name}>private=\n")
            with self.assertRaisesRegex(ValueError, "Repository provenance mismatch"):
                verifier.check_repository_markers(jar)
            marker.unlink()
            verifier.check_repository_markers(jar)

    @staticmethod
    def tree():
        return {"groupId": "qqq.verification", "artifactId": "published-bom-consumer",
                "version": "1.0.0", "type": "jar", "children": [
                    {"groupId": "com.kingsrook.qqq", "artifactId": name,
                     "version": version, "type": "jar", "scope": "compile", "children": []}
                    for name, version in (("qqq-backend-core", "4.1.0-RC.1"),
                                          ("qqq-esb", "4.1.0-RC.1"),
                                          ("qqq-frontend-next", "0.2.1"))]}

    def test_fake_maven_uses_isolated_settings_cache_and_keeps_evidence(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            fake = root / "mvn"
            fake.write_text("#!/usr/bin/env python3\n"
                            "import json, os, pathlib, sys\n"
                            "args = sys.argv[1:]\n"
                            "repo = pathlib.Path(next(x.split('=', 1)[1] for x in args if x.startswith('-Dmaven.repo.local=')))\n"
                            "assert os.environ['HOME'] == str(repo.parent)\n"
                            "assert 'MAVEN_ARGS' not in os.environ and 'JAVA_TOOL_OPTIONS' not in os.environ\n"
                            "out = pathlib.Path(next(x.split('=', 1)[1] for x in args if x.startswith('-DoutputFile=')))\n"
                            "for artifact, version, ext in [('qqq-bom-pom','4.1.0-RC.1','pom'),"
                            "('qqq-backend-core','4.1.0-RC.1','jar'),('qqq-esb','4.1.0-RC.1','jar'),"
                            "('qqq-frontend-next','0.2.1','jar')]:\n"
                            "  path = repo / 'com/kingsrook/qqq' / artifact / version / (artifact+'-'+version+'.'+ext)\n"
                            "  path.parent.mkdir(parents=True, exist_ok=True)\n"
                            f"  path.write_text({json.dumps(POM.replace('${revision}', '4.1.0-RC.1'))} if ext == 'pom' else 'fixture')\n"
                            "  (path.parent / '_remote.repositories').write_text(path.name + '>central=\\n')\n"
                            f"out.write_text({json.dumps(json.dumps(self.tree()))})\n")
            fake.chmod(0o755)
            seen_urls = []
            def matching_digest(url, path):
                seen_urls.append(url)
                return verifier.sha256(path)
            with patch.dict(os.environ, {"MAVEN_ARGS": "-o", "JAVA_TOOL_OPTIONS": "-Dfixture=true"}):
                evidence = verifier.verify("4.1.0-RC.1", POM, root / "evidence", str(fake),
                                           "test-sha", remote_digest=matching_digest)
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
            self.assertEqual(4, len(seen_urls))
            self.assertTrue(all(url.startswith("https://repo.maven.apache.org/maven2/") for url in seen_urls))
            self.assertEqual("central", evidence["artifacts"]["qqq-esb"]["repository_markers"][0])
            self.assertEqual("test-sha", evidence["source_commit"])
            self.assertIn("MAVEN_ARGS", evidence["removed_environment_keys"])
            self.assertIn("JAVA_TOOL_OPTIONS", evidence["removed_environment_keys"])
            self.assertEqual(64, len(evidence["published_bom_sha256"]))
            self.assertEqual('<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"/>',
                             (run_dir / "settings.xml").read_text().strip())

    def test_remote_central_digest_mismatch_fails_closed(self):
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
                            "  path.parent.mkdir(parents=True, exist_ok=True)\n"
                            f"  path.write_text({json.dumps(POM.replace('${revision}', '4.1.0-RC.1'))} if ext == 'pom' else 'fixture')\n"
                            "  (path.parent / '_remote.repositories').write_text(path.name + '>central=\\n')\n"
                            f"out.write_text({json.dumps(json.dumps(self.tree()))})\n")
            fake.chmod(0o755)
            with self.assertRaisesRegex(ValueError, "Central checksum mismatch"):
                verifier.verify("4.1.0-RC.1", POM, root / "evidence", str(fake), "test-sha",
                                remote_digest=lambda url, path: "0" * 64)
            evidence = json.loads(next((root / "evidence").glob("*/evidence.json")).read_text())
            self.assertEqual("fail", evidence["status"])
            self.assertIn("central", evidence["artifacts"]["qqq-bom-pom"]["repository_markers"])

    def test_verifier_records_extra_published_management_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            fake = root / "mvn"
            extra = ('<dependency><groupId>other.group</groupId><artifactId>extra</artifactId>'
                     '<version>1.0.0</version></dependency>')
            published = POM.replace("${revision}", "4.1.0-RC.1").replace(
                "</dependencies>", extra + "</dependencies>")
            fake.write_text("#!/usr/bin/env python3\n"
                            "import pathlib, sys\n"
                            "args = sys.argv[1:]\n"
                            "repo = pathlib.Path(next(x.split('=', 1)[1] for x in args if x.startswith('-Dmaven.repo.local=')))\n"
                            "out = pathlib.Path(next(x.split('=', 1)[1] for x in args if x.startswith('-DoutputFile=')))\n"
                            "bom = repo / 'com/kingsrook/qqq/qqq-bom-pom/4.1.0-RC.1/qqq-bom-pom-4.1.0-RC.1.pom'\n"
                            "bom.parent.mkdir(parents=True, exist_ok=True)\n"
                            f"bom.write_text({json.dumps(published)})\n"
                            "out.write_text('{}')\n")
            fake.chmod(0o755)
            with self.assertRaisesRegex(ValueError, "BOM management mismatch"):
                verifier.verify("4.1.0-RC.1", POM, root / "evidence", str(fake), "test-sha",
                                remote_digest=lambda url, path: verifier.sha256(path))
            evidence = json.loads(next((root / "evidence").glob("*/evidence.json")).read_text())
            self.assertEqual("fail", evidence["status"])
            self.assertEqual(4, len(evidence["published_management"]))

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
