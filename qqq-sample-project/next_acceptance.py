"""Validate reviewed Next receipts without manufacturing JUnit or promoting rows."""
from collections import Counter
import hashlib
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET

FEATURE_IDS = ('core.widget.data_bag_viewer', 'core.widget.pivot_table_setup',
               'core.widget.filter_and_columns_setup', 'core.widget.row_builder',
               'core.widget.script_viewer', 'core.widget.blocks', 'dashboard.acceptance')
JOB_LAYOUTS = (
    {'acceptance-chromium': ('chromium',), 'acceptance-touch': ('mobile', 'tablet'),
     'acceptance-firefox': ('firefox',), 'acceptance-webkit': ('webkit',)},
    {'acceptance-chromium-touch': ('chromium', 'mobile', 'tablet'),
     'acceptance-firefox': ('firefox',), 'acceptance-webkit': ('webkit',)},
)
PROJECTS = {'chromium', 'firefox', 'webkit', 'mobile', 'tablet'}
MATRICES = ('navigation', 'performance', 'processes', 'query', 'records', 'security', 'widgets')
EXCLUSIONS = {'WID-033', 'SEC-030', 'PRC-039'}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, 'duplicate JSON key')
        result[key] = value
    return result


def read_json(path):
    return json.loads(path.read_text(), object_pairs_hook=unique_object)



def accepted_release(sample, candidate_version, before_publish=False):
    """Read the explicit candidate exception without changing native evidence."""
    if not candidate_version:
        return None
    policy = read_json(sample / 'release-deferrals.json').get('accepted_next_release')
    require(isinstance(policy, dict), 'candidate has no approved Next release exception')
    require(candidate_version == policy['qqq_version'] == '4.1.0-RC.1', 'Next exception is only approved for QQQ4.1.0-RC.1')
    require(policy['next_version'] == '1.0.0-RC.11'
            and policy['next_sha'] == '2853b9b148282ae0e6d454adab6d0f534ba48280'
            and policy['jar_sha256'] == '0c89bc0a104131010e5607e8a3669f8579048fab43170d05344dd47721188d99',
            'Next exception must identify the accepted immutable public Next RC11')
    require(policy['owner_approval'] == 'https://github.com/QRun-IO/qqq/issues/798#issuecomment-5944789583',
            'Next exception lacks the reviewed maintainer decision')
    require(len(policy['features']) == len(FEATURE_IDS) and set(policy['features']) == set(FEATURE_IDS),
            'Next exception must preserve the seven successor coverage contracts')
    require(policy.get('rationale') and policy.get('target_release'), 'Next exception needs rationale and follow-up target')
    ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
    root_pom = ET.parse(sample.parent / 'pom.xml').getroot()
    revision = root_pom.findtext('m:properties/m:revision', namespaces=ns)
    allowed_revisions = ('4.1.0-SNAPSHOT',) if before_publish else ('4.1.0-SNAPSHOT', '4.1.0-RC.1')
    require(revision in allowed_revisions, 'QQQ source revision cannot use the RC1 exception')
    sample_pom = ET.parse(sample / 'pom.xml').getroot()
    require(sample_pom.findtext('m:properties/m:qqq.frontend.next.version', namespaces=ns) == policy['next_version'],
            'sample Next pin differs from the accepted release')
    bom = ET.parse(sample.parent / 'qqq-bom/pom.xml').getroot()
    next_pins = [d.findtext('m:version', namespaces=ns)
                 for d in bom.findall('m:dependencyManagement/m:dependencies/m:dependency', ns)
                 if d.findtext('m:artifactId', namespaces=ns) == 'qqq-frontend-next']
    require(next_pins == [policy['next_version']], 'BOM Next pin differs from the accepted release')
    return policy


def hashed_file(root, reference):
    require(set(reference) == {'path', 'sha256'}, 'invalid file reference')
    relative = Path(reference['path'])
    require(not relative.is_absolute() and '..' not in relative.parts, 'receipt path escapes bundle')
    path = root / relative
    require(path.resolve().is_relative_to(root.resolve()), 'receipt symlink escapes bundle')
    require(re.fullmatch('[0-9a-f]{64}', reference['sha256']) is not None, 'invalid SHA256')
    require(hashlib.sha256(path.read_bytes()).hexdigest() == reference['sha256'], 'receipt file hash mismatch: ' + str(relative))
    return path


def crosswalk_rows(crosswalk, features):
    require(crosswalk['schema_version'] == 1, 'unsupported Next crosswalk schema')
    rows = crosswalk['features']
    require(len(rows) == len(FEATURE_IDS) and {r['id'] for r in rows} == set(FEATURE_IDS), 'Next crosswalk must retain seven IDs')
    inventory = {f['id']: f for f in features}
    for row in rows:
        feature = inventory[row['id']]
        historical = row['historical']
        for field in ('acceptance_cases', 'negative_cases', 'acceptance_stage', 'release_disposition',
                      'feature', 'source_paths', 'owning_module'):
            require(historical[field] == feature[field], 'Next crosswalk differs from original contract: ' + row['id'])
        expected = [(kind, text) for kind in ('acceptance_cases', 'negative_cases') for text in feature[kind]]
        require([(c['kind'], c['text']) for c in row['requirements']] == expected, 'Next crosswalk omits or changes required cases')
        for case in row['requirements']:
            require(case['pending'] is None or isinstance(case['pending'], str) and case['pending'].strip(), 'invalid pending reason')
            require(isinstance(case['tests'], list), 'invalid Next case mapping')
            for test in case['tests']:
                require(set(test['projects']) == PROJECTS and len(test['projects']) == 5,
                        'mapped behavior must retain all five browser projects')
                require('tests/acceptance/specs/' + test['file'] in crosswalk['sources'], 'mapped test lacks source hash')
                require('[' + test['matrix_id'] + ']' in test['title'], 'mapped test lacks matrix ID')
    return rows


def matrix_rows(bundle, sources):
    for path, sha in sources.items():
        hashed_file(bundle, {'path': 'sources/' + path, 'sha256': sha})
    rows = {}
    for name in MATRICES:
        path = 'tests/acceptance/matrix/' + name + '.json'
        require(path in sources, 'missing pinned matrix source')
        matrix = read_json(bundle / 'sources' / path)
        for row in matrix['rows']:
            require(row['id'] not in rows, 'duplicate matrix ID')
            require(type(row['required']) is bool, 'invalid required matrix disposition')
            require(any(row['id'].startswith(p + '-') for p in matrix['prefixes']), 'matrix prefix mismatch')
            if not row['required']:
                require(row['id'] in EXCLUSIONS and row.get('approval'), 'unreviewed matrix exclusion')
            if 'desktopOnly' in row:
                require(isinstance(row['desktopOnly'], str) and row['desktopOnly'].strip(), 'invalid desktopOnly rationale')
            rows[row['id']] = row
    require(rows, 'empty Next matrix')
    return rows


def native_results(report, gate, projects, matrix, receipt):
    require(gate['partial'] is False and gate['problems'] == [], 'Next gate is partial or failed')
    config = report['config']
    require(report['errors'] == [], 'Playwright run has errors')
    require(config['forbidOnly'] is True and config.get('shard') is None
            and config.get('grep') in ({}, None) and config.get('grepInvert') is None, 'filtered or sharded Playwright run')
    configured = config['projects']
    require(len(configured) == len(projects) and {p['name'] for p in configured} == set(projects), 'missing or duplicate browser project')
    require(all(p['retries'] == 0 and p['repeatEach'] == 1 for p in configured), 'retry/repeat configuration is not acceptance')
    metadata = config['metadata']
    require(metadata['ci']['commitHash'] == receipt['next_sha']
            and metadata['gitCommit']['hash'] == receipt['next_sha']
            and metadata['ci']['buildHref'] == receipt['run_url'], 'native report source/run provenance mismatch')
    results = []
    row_results = {key: [] for key in matrix}
    counts = {p: {'passed': 0, 'failed': 0, 'skipped': 0, 'flaky': 0} for p in projects}
    seen = set()

    def walk(suite, titles=()):
        for child in suite.get('suites', []):
            walk(child, (*titles, child['title']))
        for spec in suite.get('specs', []):
            ids = re.findall(r'\[([A-Z]{3}-\d{3})\]', ' '.join((*titles, spec['title'])))
            require(ids and set(ids) <= matrix.keys(), 'missing or unknown matrix ID in native report')
            require(spec['ok'] is True and spec['tests'], 'failed or empty native spec')
            for test in spec['tests']:
                project = test['projectName']
                identity = (spec['file'], *titles, spec['title'], project)
                require(project in projects and identity not in seen, 'unknown project or duplicate native testcase')
                seen.add(identity)
                attempts = test['results']
                require(test['status'] == 'expected' and test['expectedStatus'] == 'passed'
                        and len(attempts) == 1 and attempts[0]['status'] == 'passed'
                        and attempts[0]['retry'] == 0 and attempts[0]['errors'] == []
                        and not attempts[0].get('error'), 'native testcase failed, skipped, flaky or incomplete')
                counts[project]['passed'] += 1
                for mid in ids:
                    row_results[mid].append({'title': spec['title'], 'project': project, 'outcome': 'passed'})
                results.append({'file': spec['file'], 'title': spec['title'], 'project': project,
                                'matrix_ids': ids, 'outcome': 'passed'})

    for suite in report['suites']:
        walk(suite)
    require(all(c['passed'] for c in counts.values()), 'browser ran no tests')
    require(report['stats']['expected'] == len(results)
            and all(report['stats'][k] == 0 for k in ('unexpected', 'skipped', 'flaky')), 'native report totals disagree')
    require(gate['byProject'] == counts, 'native report and gate project counts disagree')
    require(len(gate['rows']) == len(matrix) and {r['id'] for r in gate['rows']} == set(matrix), 'native gate row scope differs')
    for row in gate['rows']:
        normalize = lambda values: Counter(json.dumps(v, sort_keys=True) for v in values)
        require(normalize(row['results']) == normalize(row_results[row['id']]), 'native report and gate testcase results disagree')
    summary = {'passed': 0, 'failed': 0, 'missing': 0, 'excluded': 0}
    phone = {'covered': 0, 'desktopOnly': 0, 'uncovered': 0}
    touch = set(projects) & {'mobile', 'tablet'}
    for mid, row in matrix.items():
        if not row['required']:
            summary['excluded'] += 1
            continue
        present = {r['project'] for r in row_results[mid]}
        needed = set(projects) - ({'mobile', 'tablet'} if 'desktopOnly' in row else set())
        require(needed <= present, 'required matrix row missing browser coverage: ' + mid)
        summary['passed'] += 1
        if touch:
            phone['desktopOnly' if 'desktopOnly' in row else 'covered'] += 1
    if touch:
        summary['phone'] = phone
    require(gate['summary'] == summary, 'native report and gate summary disagree')
    return results


def evaluate(sample, features, source_sha, receipt_sha256=None):
    """Fail closed; a reviewed receipt hash is an integrity anchor, not a build attestation."""
    result = {'features': {key: {'reasons': [], 'tests': [], 'historical_tests': []} for key in FEATURE_IDS}, 'problems': []}
    try:
        crosswalk = read_json(sample / 'next-acceptance-crosswalk.json')
        rows = crosswalk_rows(crosswalk, features)
        for row in rows:
            result['features'][row['id']]['historical_tests'] = row['historical']['verified_tests']
            for case in row['requirements']:
                if case['pending'] or not case['tests']:
                    result['features'][row['id']]['reasons'].append('Next behavior mapping pending: ' + case['text'])
        require(receipt_sha256 is not None, 'Next source/artifact provenance receipt is pending review')
        bundle = sample / 'target' / 'next-acceptance'
        receipt = read_json(hashed_file(bundle, {'path': 'receipt.json', 'sha256': receipt_sha256}))
        require(receipt['schema_version'] == 1 and receipt['mode'] == 'javalin', 'invalid Next import receipt schema/mode')
        require(re.fullmatch('[0-9a-f]{40}', crosswalk['next_sha']) is not None
                and isinstance(source_sha, str) and re.fullmatch('[0-9a-f]{40}', source_sha) is not None
                and receipt['next_sha'] == crosswalk['next_sha'] and receipt['qqq_sha'] == source_sha,
                'stale Next/QQQ source provenance')
        require(re.fullmatch(r'https://github\.com/QRun-IO/qqq-frontend-next/actions/runs/[1-9]\d*', receipt['run_url']) is not None,
                'invalid Next run provenance')
        matrix = matrix_rows(bundle, crosswalk['sources'])
        jobs = receipt['jobs']
        layout = next((candidate for candidate in JOB_LAYOUTS
                       if len(jobs) == len(candidate) and {j['name'] for j in jobs} == set(candidate)), None)
        require(layout is not None, 'missing, duplicate or unsupported Next browser job layout')
        tests = []
        for job in jobs:
            require(set(job['artifacts']) == {'sample', 'api', 'next'}, 'missing runtime artifact provenance')
            for reference in job['artifacts'].values():
                require(hashed_file(bundle, reference).stat().st_size > 0, 'empty runtime artifact')
            require(hashed_file(bundle, job['checkout_evidence']).stat().st_size > 0, 'missing checkout/build evidence')
            report = read_json(hashed_file(bundle, job['report']))
            gate = read_json(hashed_file(bundle, job['gate']))
            tests.extend(native_results(report, gate, layout[job['name']], matrix, receipt))
        for row in rows:
            for case in row['requirements']:
                for required in case['tests']:
                    require(matrix[required['matrix_id']]['required'], 'mapped requirement refers to an excluded matrix row')
                    for project in required['projects']:
                        matches = [t for t in tests if t['file'] == required['file'] and t['title'] == required['title']
                                   and t['project'] == project and required['matrix_id'] in t['matrix_ids']]
                        require(len(matches) == 1, 'required Next testcase missing or ambiguous: ' + required['matrix_id'] + '/' + project)
                        if matches[0] not in result['features'][row['id']]['tests']:
                            result['features'][row['id']]['tests'].append(matches[0])
    except (OSError, ValueError, TypeError, KeyError, AttributeError) as error:
        # Do not echo untrusted report/log contents; only bounded schema/provenance diagnostics.
        message = str(error) if isinstance(error, ValueError) and not isinstance(error, json.JSONDecodeError) else type(error).__name__
        result['problems'].append('Next evidence rejected: ' + message)
        for row in result['features'].values():
            row['reasons'].extend(result['problems'])
    return result
