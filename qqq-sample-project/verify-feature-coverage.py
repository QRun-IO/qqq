#!/usr/bin/env python3
"""Check the sample's feature ledger against actual Maven test reports."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import xml.etree.ElementTree as ET


# Reviewed 130-feature scope, including separate source/published bootstrap; change only after reviewing the source
# inventory delta. The digest prevents accidental removal/renaming from passing.
INVENTORY_IDS_SHA256 = 'db7f1a1afd740afab1e3e5cc5287f93e9c2fe9cf8a567b4318d76d83d6e361bf'
PUBLISHED_FEATURES = {'sample.bootstrap.published', 'train.bom'}
UNSUPPORTED_FEATURES = {'core.widget.generic'}


def bootstrap_source_reasons(sample):
    path = sample / 'target' / 'bootstrap-acceptance.json'
    try:
        report = json.loads(path.read_text())
        sha = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=sample,
                                      text=True, stderr=subprocess.DEVNULL).strip()
    except (OSError, ValueError, subprocess.CalledProcessError):
        return ['source bootstrap acceptance report is missing or invalid']
    if (report.get('stage') != 'source' or report.get('source_sha') != sha
            or report.get('worktree_dirty') is not False or not report.get('complete')):
        return ['source bootstrap report is incomplete or from another revision']
    if report.get('cache_origin') != 'new empty cache' or report.get('library_count') != 17:
        return ['source bootstrap cache or reviewed library count is invalid']
    for step in ('root_install', 'candidate_resolution', 'sample_verify'):
        if report.get(step, {}).get('exit_code') != 0:
            return ['source bootstrap step did not pass: ' + step]
    for step in ('missing_bom', 'mismatched_candidate'):
        if report.get('negative_models', {}).get(step, {}).get('exit_code', 0) == 0:
            return ['source bootstrap negative boundary did not fail: ' + step]
    for suite in ('SampleBootstrapTest', 'SampleJavalinServerTest', 'SamplePackagedConfigurationIT'):
        if report.get('test_reports', {}).get(suite, {}).get('tests', 0) < 1:
            return ['source bootstrap test report is missing: ' + suite]
    return []


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--stage', choices=('source', 'published'), default='published',
                        help='Source checks defer only explicitly marked public-artifact acceptance')
    parser.add_argument('--report-only', action='store_true', help='List gaps without certifying acceptance')
    parser.add_argument('--require-feature', action='append', default=[],
                        help='Require each named reviewed feature even with --report-only')
    args = parser.parse_args()
    sample = Path(__file__).resolve().parent
    output = sample / 'target' / 'feature-coverage-result.json'
    output.unlink(missing_ok=True)
    inventory = json.loads((sample / 'feature-coverage.json').read_text())
    if inventory.get('schema_version') != 1:
        parser.error('Unsupported feature inventory schema_version')
    features = inventory['features']
    identifiers = [feature['id'] for feature in features]
    if not identifiers or len(identifiers) != len(set(identifiers)):
        parser.error('The inventory must contain unique feature IDs')
    digest = hashlib.sha256('\n'.join(sorted(identifiers)).encode()).hexdigest()
    if digest != INVENTORY_IDS_SHA256:
        parser.error('Feature IDs differ from the reviewed scope; review the source inventory delta before updating its digest')
    for required in args.require_feature:
        if required not in identifiers:
            parser.error('Unknown required feature: ' + required)
    for feature in features:
        expected_stage = 'published' if feature['id'] in PUBLISHED_FEATURES else 'source'
        if feature.get('acceptance_stage') != expected_stage:
            parser.error('Invalid acceptance stage for ' + feature['id'] + ': expected ' + expected_stage)

    outcomes = {}
    for directory in ('surefire-reports', 'failsafe-reports'):
        for report in (sample / 'target' / directory).glob('TEST-*.xml'):
            for case in ET.parse(report).getroot().iter('testcase'):
                name = case.attrib['classname'] + '#' + case.attrib['name']
                passed = not any(case.find(result) is not None for result in ('failure', 'error', 'skipped'))
                outcomes[name] = outcomes.get(name, True) and passed

    gaps = []
    unsupported = []
    deferred = []
    for feature in features:
        if args.stage == 'source' and feature.get('acceptance_stage') == 'published':
            deferred.append(feature['id'])
            continue
        tests = feature['verified_tests']
        reasons = []
        if feature['acceptance_status'] == 'unsupported':
            support = feature.get('support', {})
            review = feature.get('support_review', {})
            if (feature['id'] in UNSUPPORTED_FEATURES
                    and support.get('status') == 'enum_only' and support.get('detail')
                    and feature.get('source_paths') and review.get('source_sha')
                    and review.get('reason') and review.get('evidence') and not tests):
                unsupported.append({'id': feature['id'], 'reason': review['reason']})
                continue
            reasons.append('unsupported disposition requires a reviewed enum-only boundary without test claims')
        if feature['acceptance_status'] != 'verified':
            reasons.append('scenario review is pending')
        if not tests:
            reasons.append('no acceptance tests are mapped')
        for test in tests:
            if not outcomes.get(test, False):
                reasons.append('test did not pass in these reports: ' + test)
        if feature['id'] == 'sample.bootstrap' and args.stage == 'source':
            reasons.extend(bootstrap_source_reasons(sample))
        if reasons:
            gaps.append({'id': feature['id'], 'reasons': reasons})

    unavailable = set(deferred) | {item['id'] for item in unsupported} | {gap['id'] for gap in gaps}
    required_passed = all(required not in unavailable for required in args.require_feature)
    result = {
        'inventory_entries': len(features), 'features': len(features) - len(unsupported) - len(deferred),
        'verified': len(features) - len(unsupported) - len(deferred) - len(gaps),
        'unsupported': unsupported, 'deferred': deferred, 'stage': args.stage,
        'stage_passed': not gaps,
        'required_features': args.require_feature, 'required_passed': required_passed,
        'complete': not args.report_only and not gaps and not deferred, 'gaps': gaps,
        'scope': 'This checks recorded scenarios against these reports. Inventory completeness requires source review; use clean verify to avoid stale reports.',
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, indent=2) + '\n')
    print(f"Sample features verified ({args.stage}): {result['verified']}/{result['features']}; report: {output}")
    return 0 if (required_passed if args.require_feature else (args.report_only or result['stage_passed'])) else 1


if __name__ == '__main__':
    sys.exit(main())
