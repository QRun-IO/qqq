"""Synthetic, sanitized native Playwright/gate receipts; never release evidence."""
import copy
import hashlib
import json
from pathlib import Path
import tempfile
import unittest

from next_acceptance import evaluate


PROJECTS = ('chromium', 'firefox', 'webkit', 'mobile', 'tablet')
TITLE = '[WID-028] data bag viewer lists versions newest first and shows the selected contents @mobile'
FILE = 'widgets/record-widgets.spec.ts'
NEXT_SHA = '1' * 40
RUN = 'https://github.com/QRun-IO/qqq-frontend-next/actions/runs/123456'


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value))


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def write_fixture(sample, features, qqq_sha):
    """Use the inspected report shape, with explicitly synthetic source/artifact bytes."""
    crosswalk = json.loads(Path(__file__).with_name('next-acceptance-crosswalk.json').read_text())
    crosswalk['next_sha'] = NEXT_SHA
    bundle = sample / 'target' / 'next-acceptance'
    sources = {}
    for path in crosswalk['sources']:
        target = bundle / 'sources' / path
        target.parent.mkdir(parents=True, exist_ok=True)
        if '/matrix/' in path:
            rows = [{'id': 'WID-028', 'required': True, 'feature': 'Synthetic viewer'}] if path.endswith('widgets.json') else []
            write_json(target, {'area': 'Synthetic', 'prefixes': ['WID'], 'rows': rows})
        else:
            target.write_text('// Synthetic source fixture only\n')
        sources[path] = digest(target)
    crosswalk['sources'] = sources
    for row in crosswalk['features']:
        # Negative coverage is synthetic only, deliberately complete to exercise the gate's success path.
        for requirement in row['requirements']:
            requirement['pending'] = None
            requirement['tests'] = [{'matrix_id': 'WID-028', 'file': FILE, 'title': TITLE, 'projects': list(PROJECTS)}]
    jobs = []
    for name, projects in [('acceptance-chromium-touch', ['chromium', 'mobile', 'tablet']),
                           ('acceptance-firefox', ['firefox']), ('acceptance-webkit', ['webkit'])]:
        tests = [{'timeout': 90000, 'annotations': [], 'expectedStatus': 'passed',
                  'projectId': project, 'projectName': project, 'status': 'expected',
                  'results': [{'workerIndex': 0, 'parallelIndex': 0, 'status': 'passed', 'duration': 10,
                               'errors': [], 'stdout': [], 'stderr': [], 'retry': 0,
                               'startTime': '2026-09-28T00:00:00Z', 'annotations': [], 'attachments': []}]}
                 for project in projects]
        report = {'config': {'forbidOnly': True, 'grep': {}, 'grepInvert': None, 'shard': None,
                             'projects': [{'name': p, 'id': p, 'retries': 0, 'repeatEach': 1} for p in projects],
                             'metadata': {'ci': {'commitHash': NEXT_SHA, 'buildHref': RUN},
                                          'gitCommit': {'hash': NEXT_SHA}}},
                  'suites': [{'title': FILE, 'file': FILE, 'suites': [], 'specs': [
                      {'title': TITLE, 'ok': True, 'tags': ['mobile'], 'id': 'synthetic-spec',
                       'file': FILE, 'line': 88, 'column': 5, 'tests': tests}]}],
                  'errors': [], 'stats': {'startTime': '2026-09-28T00:00:00Z', 'duration': 100,
                                         'expected': len(projects), 'unexpected': 0, 'skipped': 0, 'flaky': 0}}
        summary = {'passed': 1, 'failed': 0, 'missing': 0, 'excluded': 0}
        if 'mobile' in projects:
            summary['phone'] = {'covered': 1, 'desktopOnly': 0, 'uncovered': 0}
        gate = {'partial': False, 'generatedAt': '2026-09-28T00:01:00Z', 'summary': summary,
                'byProject': {p: {'passed': 1, 'failed': 0, 'skipped': 0, 'flaky': 0} for p in projects},
                'problems': [], 'rows': [{'id': 'WID-028', 'results': [
                    {'title': TITLE, 'project': p, 'outcome': 'passed'} for p in projects]}]}
        refs = {}
        for kind, value in [('report', report), ('gate', gate)]:
            path = bundle / name / (kind + '.json')
            write_json(path, value)
            refs[kind] = {'path': str(path.relative_to(bundle)), 'sha256': digest(path)}
        artifacts = {}
        for kind in ('sample', 'api', 'next'):
            path = bundle / name / (kind + '.jar')
            path.write_bytes(b'SYNTHETIC UNIT FIXTURE, NOT A RELEASE ARTIFACT: ' + kind.encode())
            artifacts[kind] = {'path': str(path.relative_to(bundle)), 'sha256': digest(path)}
        checkout = bundle / name / 'checkout.txt'
        checkout.write_text('Synthetic fixture checkout evidence only\n')
        jobs.append({'name': name, **refs, 'artifacts': artifacts,
                     'checkout_evidence': {'path': str(checkout.relative_to(bundle)), 'sha256': digest(checkout)}})
    write_json(bundle / 'receipt.json', {'schema_version': 1, 'next_sha': NEXT_SHA, 'qqq_sha': qqq_sha,
                                       'mode': 'javalin', 'run_url': RUN, 'jobs': jobs})
    write_json(sample / 'next-acceptance-crosswalk.json', crosswalk)
    return crosswalk, bundle


class NextAcceptanceTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.sample = Path(self.tmp.name)
        self.features = json.loads(Path(__file__).with_name('feature-coverage.json').read_text())['features']
        self.sha = '2' * 40
        self.crosswalk, self.bundle = write_fixture(self.sample, self.features, self.sha)
        self.receipt_sha256 = digest(self.bundle / 'receipt.json')

    def result(self):
        return evaluate(self.sample, self.features, self.sha, self.receipt_sha256)

    def assert_rejected(self, text=None):
        result = self.result()
        self.assertTrue(all(row['reasons'] for row in result['features'].values()), result)
        if text:
            self.assertIn(text, json.dumps(result))

    def repin(self):
        receipt_path = self.bundle / 'receipt.json'
        receipt = json.loads(receipt_path.read_text())
        for job in receipt['jobs']:
            for key in ('report', 'gate'):
                job[key]['sha256'] = digest(self.bundle / job[key]['path'])
        write_json(receipt_path, receipt)
        self.receipt_sha256 = digest(receipt_path)
        write_json(self.sample / 'next-acceptance-crosswalk.json', self.crosswalk)

    def mutate_report(self, change):
        path = self.bundle / 'acceptance-webkit' / 'report.json'
        report = json.loads(path.read_text())
        change(report)
        write_json(path, report)
        self.repin()

    def test_native_first_attempt_results_cover_each_required_browser(self):
        result = self.result()
        self.assertEqual(7, len(result['features']))
        self.assertFalse(result['problems'])
        for row in result['features'].values():
            self.assertEqual([], row['reasons'])
            self.assertEqual(set(PROJECTS), {t['project'] for t in row['tests']})

    def test_missing_or_unreviewed_receipt_cannot_certify(self):
        self.receipt_sha256 = None
        write_json(self.sample / 'next-acceptance-crosswalk.json', self.crosswalk)
        self.assert_rejected('provenance')
        (self.bundle / 'receipt.json').unlink()
        self.assert_rejected()

    def test_native_failure_skip_retry_expected_failure_and_missing_attempt_fail(self):
        changes = [lambda t: t['results'][0].update(status='failed'),
                   lambda t: t.update(status='skipped'),
                   lambda t: t['results'].append(copy.deepcopy(t['results'][0])),
                   lambda t: t['results'][0].update(retry=1),
                   lambda t: t.update(expectedStatus='failed'),
                   lambda t: t.update(results=[]), lambda t: t.update(status='flaky'),
                   lambda t: t['results'][0].update(errors=[{'message': 'Synthetic failure'}])]
        good = (self.bundle / 'acceptance-webkit/report.json').read_text()
        for change in changes:
            with self.subTest(change=changes.index(change)):
                (self.bundle / 'acceptance-webkit/report.json').write_text(good)
                self.mutate_report(lambda r: change(r['suites'][0]['specs'][0]['tests'][0]))
                self.assert_rejected()

    def test_green_totals_do_not_hide_missing_duplicate_or_wrong_case(self):
        good = (self.bundle / 'acceptance-webkit/report.json').read_text()
        for change in [lambda r: r.update(suites=[]),
                       lambda r: r['suites'][0]['specs'].append(copy.deepcopy(r['suites'][0]['specs'][0])),
                       lambda r: r['suites'][0]['specs'][0].update(title='[WID-028] unrelated green test'),
                       lambda r: r['suites'][0]['specs'][0].update(file='wrong.spec.ts'),
                       lambda r: r.update(errors=[{'message': 'Synthetic interrupted run'}]),
                       lambda r: r['config'].update(shard={'current': 1, 'total': 2})]:
            with self.subTest(change=change):
                (self.bundle / 'acceptance-webkit/report.json').write_text(good)
                self.mutate_report(change)
                self.assert_rejected()

    def test_partial_or_disagreeing_gate_fails(self):
        path = self.bundle / 'acceptance-webkit/gate.json'
        good = json.loads(path.read_text())
        for change in [{'partial': True}, {'problems': ['Synthetic failure']}, {'rows': []},
                       {'byProject': {}}, {'summary': {'passed': 999}}]:
            with self.subTest(change=change):
                write_json(path, good | change)
                self.repin()
                self.assert_rejected()

    def test_missing_browser_job_stale_source_and_changed_artifacts_fail(self):
        path = self.bundle / 'receipt.json'
        good = path.read_text()
        for key, value in [('jobs', json.loads(good)['jobs'][:2]), ('qqq_sha', '3' * 40), ('next_sha', '4' * 40)]:
            with self.subTest(key=key):
                write_json(path, json.loads(good) | {key: value})
                self.repin()
                self.assert_rejected()
        path.write_text(good)
        self.repin()
        (self.bundle / 'acceptance-webkit/api.jar').write_bytes(b'changed')
        self.assert_rejected('hash')

    def test_report_source_sha_must_match_reviewed_target(self):
        self.mutate_report(lambda r: r['config']['metadata']['ci'].update(commitHash='9' * 40))
        self.assert_rejected()

    def test_unmapped_negatives_survive_green_browser_receipt(self):
        self.crosswalk['features'][0]['requirements'][1].update(tests=[], pending='No type-specific malformed/denied proof')
        write_json(self.sample / 'next-acceptance-crosswalk.json', self.crosswalk)
        result = self.result()['features']
        self.assertTrue(result['core.widget.data_bag_viewer']['reasons'])
        self.assertFalse(result['core.widget.row_builder']['reasons'])

    def test_contract_omission_and_source_drift_fail_closed(self):
        self.crosswalk['features'][0]['requirements'].pop()
        write_json(self.sample / 'next-acceptance-crosswalk.json', self.crosswalk)
        self.assert_rejected()
        self.crosswalk, self.bundle = write_fixture(self.sample, self.features, self.sha)
        (self.bundle / 'sources/tests/acceptance/specs' / FILE).write_text('changed')
        self.assert_rejected('hash')

    def test_path_escape_and_malformed_json_fail_closed(self):
        receipt = json.loads((self.bundle / 'receipt.json').read_text())
        receipt['jobs'][0]['artifacts']['sample']['path'] = '../outside.jar'
        write_json(self.bundle / 'receipt.json', receipt)
        self.repin()
        self.assert_rejected()
        (self.sample / 'next-acceptance-crosswalk.json').write_text('{invalid')
        self.assert_rejected()

    def test_sources_directory_symlink_cannot_move_the_trust_root(self):
        self.assertFalse(self.result()['problems'])
        sources = self.bundle / 'sources'
        outside = self.sample / 'outside-sources'
        sources.rename(outside)
        sources.symlink_to(outside, target_is_directory=True)
        self.assert_rejected('escapes bundle')


if __name__ == '__main__':
    unittest.main()
