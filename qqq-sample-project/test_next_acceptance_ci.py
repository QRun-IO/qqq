"""CI receipt transport/command contracts using owned synthetic native evidence."""
import contextlib
import hashlib
import io
import json
import shutil
import stat
import unittest
from unittest.mock import patch
import zipfile

import next_acceptance_ci as ci
import test_feature_coverage


class NextAcceptanceCiTest(unittest.TestCase):
    def setUp(self):
        self.fixture = test_feature_coverage.FeatureCoverageGateTest()
        self.fixture.setUp()
        self.addCleanup(self.fixture.doCleanups)
        self.sample = self.fixture.sample
        self.assertEqual(0, self.fixture.run_gate(stage='source')[0])
        self.bundle = self.sample / 'target/next-acceptance'
        self.archive = self.sample / 'input.zip'
        with zipfile.ZipFile(self.archive, 'w', zipfile.ZIP_DEFLATED) as archive:
            for path in self.bundle.rglob('*'):
                if path.is_file():
                    archive.write(path, path.relative_to(self.bundle))
        self.env = {'QQQ_NEXT_CI_BUNDLE_URL': 'https://evidence.example.invalid/owned.zip',
                    'QQQ_NEXT_BUNDLE_SHA256': hashlib.sha256(self.archive.read_bytes()).hexdigest(),
                    'QQQ_NEXT_RECEIPT_SHA256': self.fixture.next_receipt_sha,
                    'CIRCLE_SHA1': self.fixture.sha}
        self.marker = self.sample / 'target/next-acceptance-stage.json'

    def stage(self):
        # Only the remote stream is replaced; digest, archive, filesystem and receipt checks run normally.
        with patch.object(ci, 'open_https', return_value=io.BytesIO(self.archive.read_bytes())):
            result = ci.main(['stage'], self.sample, self.env)
        self.assertTrue(self.marker.exists(), 'staging must produce a bounded diagnostic receipt')
        return result

    def coverage(self, report_only=False):
        return ci.main(['report' if report_only else 'strict'], self.sample, self.env)

    def change_archive(self, name, content, mode=None):
        with zipfile.ZipFile(self.archive, 'a') as archive:
            entry = zipfile.ZipInfo(name)
            if mode is not None:
                entry.create_system = 3
                entry.external_attr = mode << 16
            archive.writestr(entry, content)
        self.env['QQQ_NEXT_BUNDLE_SHA256'] = hashlib.sha256(self.archive.read_bytes()).hexdigest()

    def test_verified_bundle_stages_after_clean_and_strict_forwards_receipt_pin(self):
        shutil.rmtree(self.bundle)
        self.assertEqual(0, self.stage())
        self.assertTrue(json.loads(self.marker.read_text())['ready'])
        self.assertEqual(0, self.coverage())
        result = json.loads((self.sample / 'target/feature-coverage-result.json').read_text())
        self.assertTrue(result['stage_passed'])
        self.assertEqual([], result['next_acceptance']['problems'])

    def test_absent_inputs_clear_old_receipt_and_report_without_certifying(self):
        self.env = {}
        self.assertEqual(0, self.stage())
        self.assertFalse(self.bundle.exists())
        self.assertEqual(0, self.coverage(report_only=True))
        result = json.loads((self.sample / 'target/feature-coverage-result.json').read_text())
        self.assertFalse(result['stage_passed'])
        self.assertFalse(result['complete'])
        self.assertEqual(1, self.coverage())

    def test_explicit_candidate_acceptance_does_not_need_a_fabricated_receipt(self):
        self.fixture.configure_accepted_next()
        self.env = {'QQQ_ACCEPTED_NEXT_CANDIDATE': '4.1.0-RC.1'}
        self.stage()
        self.assertFalse(self.bundle.exists())
        self.assertEqual(0, self.coverage())
        result = json.loads((self.sample / 'target/feature-coverage-result.json').read_text())
        self.assertFalse(result['complete'])
        self.assertEqual(7, len(result['accepted_next_features']))
        self.env = {}
        self.assertEqual(1, self.coverage())

    def test_candidate_exception_checks_actual_publication_branch_and_source_version(self):
        self.fixture.configure_accepted_next()
        self.env = {'QQQ_ACCEPTED_NEXT_CANDIDATE': '4.1.0-RC.1', 'CIRCLE_BRANCH': 'release/4.1'}
        self.assertEqual(0, self.coverage())
        for branch in ('release/4.2', 'develop', 'hotfix/4.1.1'):
            self.env['CIRCLE_BRANCH'] = branch
            self.assertEqual(1, self.coverage())
        for extra in ({'CIRCLE_TAG': 'v4.1.0'}, {'CIRCLECI': 'true'}, {'CIRCLE_BRANCH': ''}):
            self.env = {'QQQ_ACCEPTED_NEXT_CANDIDATE': '4.1.0-RC.1', **extra}
            self.assertEqual(1, self.coverage())
        self.env = {'QQQ_ACCEPTED_NEXT_CANDIDATE': '4.1.0-RC.1', 'CIRCLE_BRANCH': 'release/4.1'}
        root_pom = self.sample.parent / 'pom.xml'
        root_pom.write_text(root_pom.read_text().replace('4.1.0-SNAPSHOT', '4.1.0-RC.1'))
        self.assertEqual(1, self.coverage(), 'the publisher would increment existing RC1 to RC2')

    def test_partial_inputs_and_wrong_archive_or_receipt_hash_fail_closed(self):
        for key in ('QQQ_NEXT_CI_BUNDLE_URL', 'QQQ_NEXT_BUNDLE_SHA256', 'QQQ_NEXT_RECEIPT_SHA256'):
            original = self.env[key]
            with self.subTest(key=key):
                self.env[key] = '' if key.endswith('URL') else '0' * 64
                self.assertEqual(0, self.stage())
                self.assertFalse(json.loads(self.marker.read_text())['ready'])
                self.assertFalse(self.bundle.exists())
                self.assertEqual(1, self.coverage())
            self.env[key] = original

    def test_stale_checkout_binding_rejects_bundle(self):
        self.env['CIRCLE_SHA1'] = '9' * 40
        self.stage()
        self.assertFalse(json.loads(self.marker.read_text())['ready'])
        self.assertEqual(1, self.coverage())

    def test_changed_pins_cannot_reuse_previously_staged_success(self):
        self.stage()
        self.env['QQQ_NEXT_RECEIPT_SHA256'] = '0' * 64
        self.assertEqual(1, self.coverage())

    def test_replaced_bundle_root_symlink_cannot_reuse_staging_success(self):
        self.stage()
        outside = self.sample / 'outside-bundle'
        self.bundle.rename(outside)
        self.bundle.symlink_to(outside, target_is_directory=True)
        self.assertEqual(1, self.coverage())

    def test_receipt_backend_sha_must_match_even_when_trigger_sha_matches(self):
        with zipfile.ZipFile(self.archive) as archive:
            entries = {name: archive.read(name) for name in archive.namelist()}
        receipt = json.loads(entries['receipt.json'])
        receipt['qqq_sha'] = '9' * 40
        entries['receipt.json'] = json.dumps(receipt).encode()
        with zipfile.ZipFile(self.archive, 'w') as archive:
            for name, content in entries.items():
                archive.writestr(name, content)
        self.env['QQQ_NEXT_BUNDLE_SHA256'] = hashlib.sha256(self.archive.read_bytes()).hexdigest()
        self.env['QQQ_NEXT_RECEIPT_SHA256'] = hashlib.sha256(entries['receipt.json']).hexdigest()
        self.stage()
        self.assertFalse(json.loads(self.marker.read_text())['ready'])
        self.assertEqual(1, self.coverage())

    def test_archive_traversal_absolute_backslash_and_link_members_are_rejected(self):
        original = self.archive.read_bytes()
        for name, mode in [('../escaped', None), ('/absolute', None), ('sources\\escape', None),
                           ('sources/link', stat.S_IFLNK | 0o777), ('sources/device', stat.S_IFCHR | 0o600)]:
            with self.subTest(name=name):
                self.archive.write_bytes(original)
                self.change_archive(name, b'outside', mode)
                self.stage()
                self.assertFalse(json.loads(self.marker.read_text())['ready'])
                self.assertFalse(self.bundle.exists())
                self.assertFalse((self.sample / 'escaped').exists())

    def test_duplicate_members_and_oversized_expansion_are_rejected(self):
        original = self.archive.read_bytes()
        with contextlib.redirect_stderr(io.StringIO()):
            self.change_archive('receipt.json', b'{}')
        self.stage()
        self.assertFalse(json.loads(self.marker.read_text())['ready'])
        self.archive.write_bytes(original)
        self.env['QQQ_NEXT_BUNDLE_SHA256'] = hashlib.sha256(original).hexdigest()
        for limit in ('MAX_EXPANDED_BYTES', 'MAX_ARCHIVE_BYTES', 'MAX_FILES'):
            with self.subTest(limit=limit), patch.object(ci, limit, 1):
                self.stage()
                self.assertFalse(json.loads(self.marker.read_text())['ready'])
                self.assertFalse(self.bundle.exists())

    def test_external_destination_symlink_is_removed_without_touching_target(self):
        outside = self.sample / 'outside'
        outside.mkdir()
        sentinel = outside / 'keep'
        sentinel.write_text('owned sentinel')
        shutil.rmtree(self.bundle)
        self.bundle.symlink_to(outside, target_is_directory=True)
        self.stage()
        self.assertEqual('owned sentinel', sentinel.read_text())
        self.assertFalse(self.bundle.is_symlink())
        self.assertTrue(json.loads(self.marker.read_text())['ready'])

    def test_insecure_url_credentials_and_redirect_downgrade_are_rejected(self):
        for url in ('http://example.invalid/a.zip', 'file:///tmp/a.zip',
                    'https://user:password@example.invalid/a.zip', 'https://example.invalid/a.zip\nheader: x'):
            with self.subTest(url=url):
                self.env['QQQ_NEXT_CI_BUNDLE_URL'] = url
                self.stage()
                self.assertFalse(json.loads(self.marker.read_text())['ready'])
        with self.assertRaises(ValueError):
            ci.HttpsRedirect().redirect_request(None, None, 302, '', {}, 'http://example.invalid/a.zip')

    def test_signed_url_is_not_exposed_in_status_or_error_output(self):
        self.env['QQQ_NEXT_CI_BUNDLE_URL'] = ''
        self.env['QQQ_NEXT_BUNDLE_URL'] = 'https://evidence.example.invalid/a.zip?signature=SYNTHETIC_SECRET'
        output = io.StringIO()
        with contextlib.redirect_stdout(output), patch.object(ci, 'open_https', side_effect=OSError('SYNTHETIC_SECRET')):
            ci.main(['stage'], self.sample, self.env)
        self.assertTrue(self.marker.exists())
        self.assertNotIn('SYNTHETIC_SECRET', output.getvalue() + self.marker.read_text())
        self.assertEqual(1, self.coverage())

    def test_shell_metacharacters_are_never_executed(self):
        sentinel = self.sample / 'shell-ran'
        self.env['QQQ_NEXT_RECEIPT_SHA256'] = '$(touch ' + str(sentinel) + ')'
        self.stage()
        self.assertEqual(1, self.coverage())
        self.assertFalse(sentinel.exists())

    def test_pending_ledger_review_still_blocks_with_valid_staging(self):
        self.stage()
        path = self.sample / 'feature-coverage.json'
        inventory = json.loads(path.read_text())
        next(f for f in inventory['features'] if f['id'] == 'core.widget.row_builder')['acceptance_status'] = 'pending'
        path.write_text(json.dumps(inventory))
        self.assertEqual(1, self.coverage())


if __name__ == '__main__':
    unittest.main()
