#!/usr/bin/env python3
"""Check the sample's feature ledger against actual Maven test reports."""
import argparse
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--stage', choices=('source', 'published'), default='published',
                        help='Source checks defer only explicitly marked public-artifact acceptance')
    parser.add_argument('--report-only', action='store_true', help='List gaps without certifying acceptance')
    args = parser.parse_args()
    sample = Path(__file__).resolve().parent
    features = json.loads((sample / 'feature-coverage.json').read_text())['features']
    identifiers = [feature['id'] for feature in features]
    if not identifiers or len(identifiers) != len(set(identifiers)):
        parser.error('The inventory must contain unique feature IDs')

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
            if (support.get('status') == 'enum_only' and support.get('detail')
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
        if reasons:
            gaps.append({'id': feature['id'], 'reasons': reasons})

    result = {
        'inventory_entries': len(features), 'features': len(features) - len(unsupported) - len(deferred),
        'verified': len(features) - len(unsupported) - len(deferred) - len(gaps),
        'unsupported': unsupported, 'deferred': deferred, 'stage': args.stage,
        'stage_passed': not gaps,
        'complete': not gaps and not deferred, 'gaps': gaps,
        'scope': 'This checks recorded scenarios against these reports. Inventory completeness requires source review; use clean verify to avoid stale reports.',
    }
    output = sample / 'target' / 'feature-coverage-result.json'
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, indent=2) + '\n')
    print(f"Sample features verified ({args.stage}): {result['verified']}/{result['features']}; report: {output}")
    return 0 if args.report_only or result['stage_passed'] else 1


if __name__ == '__main__':
    sys.exit(main())
