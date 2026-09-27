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
        subprocess.run(['git', 'init', '-q', str(self.sample)], check=True)
        subprocess.run(['git', '-C', str(self.sample), '-c', 'user.name=Fixture',
                        '-c', 'user.email=fixture@example.invalid', '-c', 'commit.gpgsign=false',
                        'commit', '--allow-empty',
                        '-qm', 'fixture'], check=True)
        self.sha = subprocess.check_output(['git', '-C', str(self.sample), 'rev-parse', 'HEAD'], text=True).strip()
        shutil.copy(Path(__file__).with_name('verify-feature-coverage.py'), self.sample)
        shutil.copy(Path(__file__).with_name('release-deferrals.json'), self.sample)
        self.inventory = json.loads(Path(__file__).with_name('feature-coverage.json').read_text())
        for feature in self.inventory['features']:
            if feature['acceptance_status'] != 'unsupported' and feature['id'] != 'sample.bootstrap':
                feature['acceptance_status'] = 'verified'
                feature['verified_tests'] = ['SampleTest#testExample']
        self.feature = self.inventory['features'][0]
        self.supported_count = len(self.inventory['features']) - 1
        self.reports = self.sample / 'target' / 'surefire-reports'
        self.reports.mkdir(parents=True)
        bootstrap = next(f for f in self.inventory['features'] if f['id'] == 'sample.bootstrap')
        cases = ''.join('<testcase classname="' + name.split('#')[0] + '" name="'
                        + name.split('#')[1] + '"/>' for name in bootstrap['verified_tests'])
        (self.reports / 'TEST-bootstrap.xml').write_text('<testsuite>' + cases + '</testsuite>')
        self.bootstrap_report = self.sample / 'target' / 'bootstrap-acceptance.json'
        self.bootstrap_report.write_text(json.dumps({
            'stage': 'source', 'source_sha': self.sha, 'worktree_dirty': False, 'complete': True,
            'cache_origin': 'new empty cache', 'library_count': 17,
            'root_install': {'exit_code': 0}, 'candidate_resolution': {'exit_code': 0},
            'sample_verify': {'exit_code': 0},
            'negative_models': {'missing_bom': {'exit_code': 1},
                                'mismatched_candidate': {'exit_code': 1}},
            'test_reports': {'SampleBootstrapTest': {'tests': 1},
                             'SampleJavalinServerTest': {'tests': 1},
                             'SamplePackagedConfigurationIT': {'tests': 4}},
        }))

    def run_gate(self, features=None, outcome='', report_only=False, stage='published', required=()):
        inventory = dict(self.inventory)
        if features is not None:
            inventory['features'] = features
        (self.sample / 'feature-coverage.json').write_text(json.dumps(inventory))
        (self.reports / 'TEST-sample.xml').write_text(
            '<testsuite><testcase classname="SampleTest" name="testExample">'
            + outcome + '</testcase></testsuite>')
        if isinstance(required, str):
            required = (required,)
        run = subprocess.run([sys.executable, str(self.sample / 'verify-feature-coverage.py')]
                             + ['--stage', stage] + (['--report-only'] if report_only else [])
                             + [option for feature in required for option in ('--require-feature', feature)],
                             capture_output=True, text=True)
        result = self.sample / 'target' / 'feature-coverage-result.json'
        return run.returncode, json.loads(result.read_text()) if result.exists() else None

    def set_deferrals(self, *entries):
        (self.sample / 'release-deferrals.json').write_text(json.dumps({
            'schema_version': 1, 'deferrals': list(entries),
        }))

    def approved_deferral(self, feature_id=None):
        return {'id': feature_id or self.feature['id'],
                'owner_approval': 'https://github.com/QRun-IO/qqq/issues/790#issuecomment-123456',
                'rationale': 'Scenario awaits a supported fixture', 'target_release': '4.1.1'}

    def test_reviewed_passing_test_is_required(self):
        code, result = self.run_gate()
        self.assertEqual(0, code)
        self.assertTrue(result['complete'])
        self.assertEqual(self.supported_count, result['verified'])
        self.feature['acceptance_status'] = 'pending'
        self.assertEqual(1, self.run_gate()[0])

    def test_source_pending_requires_explicit_approved_deferral(self):
        self.feature['acceptance_status'] = 'pending'
        self.feature['verified_tests'] = []
        self.assertEqual(1, self.run_gate(stage='source')[0])
        self.set_deferrals(self.approved_deferral())
        code, result = self.run_gate(stage='source')
        self.assertEqual(0, code)
        self.assertTrue(result['stage_passed'])
        self.assertEqual([self.feature['id']], result['release_deferrals'])
        self.assertFalse(result['complete'])

    def test_unlisted_pending_scenario_still_blocks_release(self):
        pending = [feature for feature in self.inventory['features']
                   if feature['acceptance_stage'] == 'source'][:2]
        for feature in pending:
            feature['acceptance_status'] = 'pending'
            feature['verified_tests'] = []
        self.set_deferrals(self.approved_deferral(pending[0]['id']))
        code, result = self.run_gate(stage='source')
        self.assertEqual(1, code)
        self.assertEqual([pending[1]['id']], [gap['id'] for gap in result['gaps']])

    def test_missing_malformed_and_stale_deferrals_fail_closed(self):
        manifest = self.sample / 'release-deferrals.json'
        manifest.unlink()
        self.assertNotEqual(0, self.run_gate(stage='source')[0])
        manifest.write_text('{invalid json')
        self.assertNotEqual(0, self.run_gate(stage='source')[0])
        for entries in ([{'id': self.feature['id']}],
                        [self.approved_deferral('unknown.feature')],
                        [self.approved_deferral(), self.approved_deferral()],
                        [self.approved_deferral('core.widget.generic')],
                        [self.approved_deferral('train.bom')],
                        [self.approved_deferral()]):
            with self.subTest(entries=entries):
                self.set_deferrals(*entries)
                self.assertNotEqual(0, self.run_gate(stage='source')[0])

    def test_deferral_never_excuses_failed_or_skipped_verified_test(self):
        self.set_deferrals(self.approved_deferral())
        for outcome in ('<failure/>', '<skipped/>'):
            with self.subTest(outcome=outcome):
                self.assertNotEqual(0, self.run_gate(stage='source', outcome=outcome)[0])

    def test_pending_deferral_cannot_hide_failed_skipped_or_missing_mapped_test(self):
        self.feature['acceptance_status'] = 'pending'
        self.set_deferrals(self.approved_deferral())
        for outcome in ('<failure/>', '<skipped/>', '<error/>'):
            with self.subTest(outcome=outcome):
                code, result = self.run_gate(stage='source', outcome=outcome)
                self.assertEqual(1, code)
                self.assertEqual([], result['release_deferrals'])
                self.assertIn('test did not pass in these reports: SampleTest#testExample',
                              result['gaps'][0]['reasons'])
        self.feature['verified_tests'] = ['SampleTest#missing']
        code, result = self.run_gate(stage='source')
        self.assertEqual(1, code)
        self.assertIn('test did not pass in these reports: SampleTest#missing',
                      result['gaps'][0]['reasons'])

    def test_approval_requires_direct_qqq_owner_review_record(self):
        self.feature['acceptance_status'] = 'pending'
        self.feature['verified_tests'] = []
        for reference in ('QQQ-41 release owner review',
                          'https://example.com/QRun-IO/qqq/issues/790#issuecomment-123456',
                          'https://github.com/QRun-IO/qqq/issues/790',
                          'https://github.com/Kingsrook/qqq/issues/790#issuecomment-123456',
                          'https://github.com/other/qqq/issues/790#issuecomment-123456'):
            with self.subTest(reference=reference):
                entry = self.approved_deferral()
                entry['owner_approval'] = reference
                self.set_deferrals(entry)
                self.assertNotEqual(0, self.run_gate(stage='source')[0])
        entry = self.approved_deferral()
        entry['owner_approval'] = 'https://github.com/QRun-IO/qqq/pull/798#pullrequestreview-123456'
        self.set_deferrals(entry)
        self.assertEqual(0, self.run_gate(stage='source')[0])

    def test_source_release_rejects_skipped_verified_test(self):
        code, result = self.run_gate(stage='source', outcome='<skipped/>')
        self.assertEqual(1, code)
        self.assertIn('test did not pass in these reports: SampleTest#testExample',
                      result['gaps'][0]['reasons'])

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
        code, result = self.run_gate(report_only=True)
        self.assertEqual(0, code)
        self.assertTrue(result['stage_passed'])
        self.assertFalse(result['complete'])

    def test_source_stage_defers_public_artifact_checks_without_claiming_completion(self):
        published = next(f for f in self.inventory['features'] if f['id'] == 'train.bom')
        published['acceptance_status'] = 'pending'
        published['verified_tests'] = []
        bootstrap_published = next(f for f in self.inventory['features'] if f['id'] == 'sample.bootstrap.published')
        bootstrap_published['acceptance_status'] = 'pending'
        bootstrap_published['verified_tests'] = []
        code, result = self.run_gate(stage='source')
        self.assertEqual(0, code)
        self.assertTrue(result['stage_passed'])
        self.assertFalse(result['complete'])
        self.assertEqual(['sample.bootstrap.published', 'train.bom'], result['deferred'])
        self.assertEqual(1, self.run_gate()[0])
        self.assertEqual(1, self.run_gate(stage='source', required='sample.bootstrap.published')[0])

    def test_source_features_cannot_be_deferred(self):
        self.feature['acceptance_stage'] = 'published'
        self.assertNotEqual(0, self.run_gate(stage='source')[0])
        for feature in self.inventory['features']:
            feature['acceptance_stage'] = 'published'
        self.assertNotEqual(0, self.run_gate(stage='source')[0])

    def test_source_bootstrap_requires_current_complete_report_and_negative_boundaries(self):
        code, result = self.run_gate(stage='source', required='sample.bootstrap')
        self.assertEqual(0, code)
        self.assertTrue(result['required_passed'])
        self.bootstrap_report.unlink()
        self.assertEqual(1, self.run_gate(stage='source', required='sample.bootstrap')[0])

    def test_source_bootstrap_rejects_stale_and_incomplete_evidence(self):
        good = json.loads(self.bootstrap_report.read_text())
        for change in ({'source_sha': '0' * 40}, {'complete': False}, {'worktree_dirty': True},
                       {'negative_models': {'missing_bom': {'exit_code': 0}}},
                       {'test_reports': {'SampleBootstrapTest': {'tests': 1}}}):
            with self.subTest(change=change):
                self.bootstrap_report.write_text(json.dumps(good | change))
                self.assertEqual(1, self.run_gate(stage='source', required='sample.bootstrap')[0])
        self.bootstrap_report.write_text(json.dumps(good))

    def test_required_features_override_report_only_and_all_must_pass(self):
        first = self.feature['id']
        required = (first, 'sample.bootstrap')
        code, result = self.run_gate(stage='source', report_only=True, required=required)
        self.assertEqual(0, code)
        self.assertEqual(list(required), result['required_features'])
        self.assertTrue(result['required_passed'])
        self.assertFalse(result['complete'])
        self.feature['acceptance_status'] = 'pending'
        code, result = self.run_gate(stage='source', report_only=True, required=required)
        self.assertEqual(1, code)
        self.assertFalse(result['required_passed'])
        self.feature['acceptance_status'] = 'verified'
        for outcome in ('<failure/>', '<error/>', '<skipped/>'):
            with self.subTest(outcome=outcome):
                self.assertEqual(1, self.run_gate(stage='source', report_only=True,
                                                  required=required, outcome=outcome)[0])
        self.assertEqual(1, self.run_gate(stage='source', report_only=True,
                                          required=['sample.bootstrap.published'])[0])
        self.assertEqual(1, self.run_gate(stage='source', report_only=True,
                                          required=['core.widget.generic'])[0])

    def test_required_pending_row_cannot_use_approved_release_deferral(self):
        self.feature['acceptance_status'] = 'pending'
        self.feature['verified_tests'] = []
        self.set_deferrals(self.approved_deferral())
        code, result = self.run_gate(stage='source', report_only=True, required=self.feature['id'])
        self.assertEqual(1, code)
        self.assertEqual([self.feature['id']], result['release_deferrals'])
        self.assertFalse(result['required_passed'])
        self.assertFalse(result['complete'])

    def test_shrinking_or_renaming_scope_cannot_certify(self):
        self.assertNotEqual(0, self.run_gate(features=[self.feature])[0])
        self.assertNotEqual(0, self.run_gate(features=[])[0])
        self.feature['id'] = 'renamed'
        self.assertNotEqual(0, self.run_gate()[0])

    def test_invalid_inventory_does_not_leave_a_previous_success_report(self):
        self.assertEqual(0, self.run_gate()[0])
        code, result = self.run_gate(features=[], report_only=True)
        self.assertNotEqual(0, code)
        self.assertIsNone(result)
        self.inventory['schema_version'] = 99
        self.assertNotEqual(0, self.run_gate()[0])

    def test_duplicate_inventory_is_rejected(self):
        self.assertNotEqual(0, self.run_gate(features=[self.feature, self.feature])[0])

    def test_failure_in_either_report_set_wins(self):
        failsafe = self.sample / 'target' / 'failsafe-reports'
        failsafe.mkdir()
        (failsafe / 'TEST-sample.xml').write_text(
            '<testsuite><testcase classname="SampleTest" name="testExample"><failure/></testcase></testsuite>')
        self.assertEqual(1, self.run_gate()[0])

    def test_only_reviewed_enum_placeholders_can_be_excluded(self):
        unsupported = next(f for f in self.inventory['features'] if f['id'] == 'core.widget.generic')
        code, result = self.run_gate()
        self.assertEqual(0, code)
        self.assertEqual(self.supported_count, result['features'])
        self.assertEqual(1, len(result['unsupported']))
        self.feature.update({key: unsupported[key] for key in ('support', 'support_review', 'source_paths')})
        self.feature['acceptance_status'] = 'unsupported'
        self.feature['verified_tests'] = []
        self.assertEqual(1, self.run_gate()[0])
        self.feature['acceptance_status'] = 'verified'
        self.feature['verified_tests'] = ['SampleTest#testExample']
        unsupported['support']['status'] = 'implemented_frontend_contract'
        self.assertEqual(1, self.run_gate()[0])
        unsupported['support']['status'] = 'enum_only'
        unsupported['support_review'] = {}
        self.assertEqual(1, self.run_gate()[0])


if __name__ == '__main__':
    unittest.main()
