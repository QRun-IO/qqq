#!/usr/bin/env python3
"""Check the sample's feature ledger against actual Maven test reports."""
import argparse
import hashlib
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET


# Reviewed 128-feature scope at 28d4e22fe; change only after reviewing the source
# inventory delta. The digest prevents accidental removal/renaming from passing.
INVENTORY_IDS_SHA256 = 'b6fa55382bb7100e936a24aaf93d7f9398c290b8e46b06138a9a5e97558ed5aa'
PUBLISHED_FEATURES = {'train.bom'}
UNSUPPORTED_FEATURES = {'core.widget.generic'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--stage', choices=('source', 'published'), default='published',
                        help='Source checks defer only explicitly marked public-artifact acceptance')
    parser.add_argument('--report-only', action='store_true', help='List gaps without certifying acceptance')
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
        if reasons:
            gaps.append({'id': feature['id'], 'reasons': reasons})

    result = {
        'inventory_entries': len(features), 'features': len(features) - len(unsupported) - len(deferred),
        'verified': len(features) - len(unsupported) - len(deferred) - len(gaps),
        'unsupported': unsupported, 'deferred': deferred, 'stage': args.stage,
        'stage_passed': not gaps,
        'complete': not args.report_only and not gaps and not deferred, 'gaps': gaps,
        'scope': 'This checks recorded scenarios against these reports. Inventory completeness requires source review; use clean verify to avoid stale reports.',
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, indent=2) + '\n')
    print(f"Sample features verified ({args.stage}): {result['verified']}/{result['features']}; report: {output}")
    return 0 if args.report_only or result['stage_passed'] else 1


if __name__ == '__main__':
    sys.exit(main())
