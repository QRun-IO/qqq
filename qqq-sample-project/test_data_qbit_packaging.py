"""Keep source-only data-QBit acceptance out of the published sample model."""

from pathlib import Path
import unittest
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parent.parent
NS = {"m": "http://maven.apache.org/POM/4.0.0"}


class DataQBitPackagingTest(unittest.TestCase):
    def test_release_reactor_and_default_sample_do_not_require_fixture(self):
        root = ET.parse(ROOT / "pom.xml").getroot()
        modules = [item.text for item in root.findall("m:modules/m:module", NS)]
        self.assertNotIn("qqq-sample-data-qbit", modules)

        sample = ET.parse(ROOT / "qqq-sample-project/pom.xml").getroot()
        dependencies = [item.findtext("m:artifactId", namespaces=NS)
                        for item in sample.findall("m:dependencies/m:dependency", NS)]
        self.assertNotIn("qqq-sample-data-qbit", dependencies)
        source = (ROOT / "qqq-sample-project/src/main/java/com/kingsrook/sampleapp/metadata/"
                  "SampleMetaDataProvider.java").read_text()
        self.assertNotIn("ReferenceDataQBit", source)

    def test_source_profile_keeps_explicit_fixture_dependency(self):
        sample = ET.parse(ROOT / "qqq-sample-project/pom.xml").getroot()
        profiles = {item.findtext("m:id", namespaces=NS): item
                    for item in sample.findall("m:profiles/m:profile", NS)}
        profile = profiles["data-qbit-acceptance"]
        dependencies = [item.findtext("m:artifactId", namespaces=NS)
                        for item in profile.findall("m:dependencies/m:dependency", NS)]
        self.assertIn("qqq-sample-data-qbit", dependencies)


if __name__ == "__main__":
    unittest.main()
