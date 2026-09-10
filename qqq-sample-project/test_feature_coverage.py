"""Acceptance gate regressions: missing, skipped or fabricated evidence must fail."""
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest


class FeatureCoverageGateTest(unittest.TestCase):
    def setUp(self):
        self.work = tempfile.TemporaryDirectory()
        self.addCleanup(self.work.cleanup)
        self.sample = Path(self.work.name)
        shutil.copy(Path(__file__).with_name('verify-feature-coverage.py'), self.sample)
        self.feature = {'id': 'example', 'acceptance_status': 'verified',
                        'verified_tests': ['SampleTest#testExample']}
        self.reports = self.sample / 'target' / 'surefire-reports'
        self.reports.mkdir(parents=True)

    def run_gate(self, features=None, outcome='', report_only=False, stage='published'):
        (self.sample / 'feature-coverage.json').write_text(json.dumps({'features': features or [self.feature]}))
        (self.reports / 'TEST-sample.xml').write_text(
            '<testsuite><testcase classname="SampleTest" name="testExample">'
            + outcome + '</testcase></testsuite>')
        run = subprocess.run([sys.executable, str(self.sample / 'verify-feature-coverage.py')]
                             + ['--stage', stage] + (['--report-only'] if report_only else []), capture_output=True, text=True)
        result = self.sample / 'target' / 'feature-coverage-result.json'
        return run.returncode, json.loads(result.read_text()) if result.exists() else None

    def test_reviewed_passing_test_is_required(self):
        code, result = self.run_gate()
        self.assertEqual(0, code)
        self.assertTrue(result['complete'])
        self.assertEqual(1, result['verified'])
        self.feature['acceptance_status'] = 'pending'
        self.assertEqual(1, self.run_gate()[0])

    def test_failed_skipped_and_missing_tests_cannot_certify(self):
        for outcome in ('<failure/>', '<error/>', '<skipped/>'):
            with self.subTest(outcome=outcome):
                self.assertEqual(1, self.run_gate(outcome=outcome)[0])
        self.feature['verified_tests'] = ['SampleTest#missing']
        self.assertEqual(1, self.run_gate()[0])
        self.feature['verified_tests'] = []
        self.assertEqual(1, self.run_gate()[0])

    def test_report_only_never_claims_completion(self):
        code, result = self.run_gate(outcome='<failure/>', report_only=True)
        self.assertEqual(0, code)
        self.assertFalse(result['complete'])

    def test_source_stage_defers_public_artifact_checks_without_claiming_completion(self):
        published = {'id': 'public-artifacts', 'acceptance_stage': 'published',
                     'acceptance_status': 'pending', 'verified_tests': []}
        code, result = self.run_gate(features=[self.feature, published], stage='source')
        self.assertEqual(0, code)
        self.assertTrue(result['stage_passed'])
        self.assertFalse(result['complete'])
        self.assertEqual(['public-artifacts'], result['deferred'])
        self.assertEqual(1, self.run_gate(features=[self.feature, published])[0])

    def test_duplicate_inventory_is_rejected(self):
        self.assertNotEqual(0, self.run_gate(features=[self.feature, self.feature])[0])

    def test_failure_in_either_report_set_wins(self):
        failsafe = self.sample / 'target' / 'failsafe-reports'
        failsafe.mkdir()
        (failsafe / 'TEST-sample.xml').write_text(
            '<testsuite><testcase classname="SampleTest" name="testExample"><failure/></testcase></testsuite>')
        self.assertEqual(1, self.run_gate()[0])

    def test_only_reviewed_enum_placeholders_can_be_excluded(self):
        unsupported = {'id': 'placeholder', 'acceptance_status': 'unsupported', 'verified_tests': [],
                       'source_paths': ['WidgetType.java'],
                       'support': {'status': 'enum_only', 'detail': 'No implementation exists'},
                       'support_review': {'source_sha': 'reviewed-source', 'reason': 'Enum only', 'evidence': 'Source review'}}
        code, result = self.run_gate(features=[self.feature, unsupported])
        self.assertEqual(0, code)
        self.assertEqual(2, result['inventory_entries'])
        self.assertEqual(1, result['features'])
        self.assertEqual(1, len(result['unsupported']))
        unsupported['support']['status'] = 'implemented_frontend_contract'
        self.assertEqual(1, self.run_gate(features=[self.feature, unsupported])[0])
        unsupported['support']['status'] = 'enum_only'
        unsupported['support_review'] = {}
        self.assertEqual(1, self.run_gate(features=[self.feature, unsupported])[0])


if __name__ == '__main__':
    unittest.main()
