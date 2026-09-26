"""Fast checks for the disposable first-party consumer fixture."""

import tempfile
import unittest
import os
from pathlib import Path

from run_starter_application import GENERATED_PACKAGE, stage_starter, stage_template


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

    def test_stages_host_registration_and_dependency(self):
        stage_starter(self.starter, self.root / "starter")
        pom = (self.root / "starter/pom.xml").read_text()
        provider = (self.root / "starter/src/main/java/com/kingsrook/qqq/starterapp/StarterAppMetaDataProvider.java").read_text()
        self.assertIn("<artifactId>orderdesk-app</artifactId>", pom)
        self.assertIn('produce(qInstance, "acceptance")', provider)
        self.assertIn("withBackendName(RDBMS_BACKEND_NAME)", provider)
        self.assertNotIn("orderdesk-app", (self.starter / "pom.xml").read_text())


if __name__ == "__main__":
    unittest.main()
