#!/usr/bin/env python3
"""Verify the committed sample against one published QQQ version in a fresh cache."""
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tarfile
import tempfile
import xml.etree.ElementTree as ET


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('version', help='Published release or RC version, for example 4.0.0-RC.3')
    parser.add_argument('--material-version', help='Published Material dashboard version; defaults to the sample POM')
    parser.add_argument('--next-version', help='Published Next dashboard version; defaults to the sample POM')
    parser.add_argument('--require-complete-coverage', action='store_true',
                        help='Require all recorded feature scenarios; otherwise report the deferred gaps')
    args = parser.parse_args()
    if not re.fullmatch(r'\d+\.\d+\.\d+(?:-RC\.\d+)?', args.version):
        parser.error('Use a literal release or RC version, not a SNAPSHOT or Maven expression')
    if args.material_version and not re.fullmatch(r'\d+\.\d+\.\d+(?:-RC\.\d+)?', args.material_version):
        parser.error('Use a literal published release or RC version for the Material dashboard')
    if args.next_version and not re.fullmatch(r'\d+\.\d+\.\d+(?:-RC\.\d+)?', args.next_version):
        parser.error('Use a literal published release or RC version for the Next dashboard')

    root = Path(__file__).resolve().parent.parent
    source = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root, text=True).strip()
    output_root = root / 'qqq-sample-project' / 'target'
    output_root.mkdir(parents=True, exist_ok=True)
    work = Path(tempfile.mkdtemp(prefix='published-' + args.version + '-', dir=output_root))
    archive = work / 'source.tar'
    with archive.open('wb') as stream:
        subprocess.run(['git', 'archive', source, 'qqq-sample-project', 'checkstyle', 'pmd', 'spotbugs'], cwd=root, stdout=stream, check=True)
    with tarfile.open(archive) as files:
        files.extractall(work, filter='data')

    sample = work / 'qqq-sample-project'
    # Inherited plugin paths resolve relative to the standalone sample project.
    for configuration in ('checkstyle', 'spotbugs'):
        shutil.copytree(work / configuration, sample / configuration)
    namespace = {'m': 'http://maven.apache.org/POM/4.0.0'}
    ET.register_namespace('', namespace['m'])
    pom = ET.parse(sample / 'pom.xml')
    parent = pom.getroot().find('m:parent', namespace)
    parent.find('m:version', namespace).text = args.version
    relative = parent.find('m:relativePath', namespace)
    if relative is None:
        relative = ET.SubElement(parent, '{' + namespace['m'] + '}relativePath')
    relative.text = None
    properties = pom.getroot().find('m:properties', namespace)
    revision = properties.find('m:revision', namespace)
    if revision is None:
        revision = ET.SubElement(properties, '{' + namespace['m'] + '}revision')
    revision.text = args.version
    material_version = properties.find('m:qqq.frontend.material-dashboard.version', namespace)
    if args.material_version:
        material_version.text = args.material_version
    next_version = properties.find('m:qqq.frontend.next.version', namespace)
    if args.next_version:
        next_version.text = args.next_version
    if not re.fullmatch(r'\d+\.\d+\.\d+(?:-RC\.\d+)?', next_version.text or ''):
        raise SystemExit('The Next dashboard version must be a published release or RC; pass --next-version')
    pom.write(sample / 'pom.xml', encoding='utf-8', xml_declaration=True)
    settings = work / 'settings.xml'
    settings.write_text('<settings/>\n')
    command = ['mvn', '-B', '-s', str(settings), '-gs', str(settings),
               '-Dmaven.repo.local=' + str(work / 'm2'), '-Drevision=' + args.version,
               '-f', str(sample / 'pom.xml'), '-Pacceptance-tests', 'clean', 'verify']
    result = {
        'source_sha': source, 'artifact_version': args.version,
        'started_at': datetime.now(timezone.utc).isoformat(), 'command': command,
        'source': 'git archive HEAD; working-tree changes are excluded',
        'cache': str(work / 'm2'), 'settings': 'empty user and global settings',
        'material_version': material_version.text,
        'next_version': next_version.text,
        'feature_coverage_required': args.require_complete_coverage,
        'feature_coverage_complete': False,
        'complete': False,
    }
    print('Published-artifact sample evidence: ' + str(work), flush=True)
    with (work / 'maven.log').open('w') as log:
        run = subprocess.run(command, cwd=work, stdout=log, stderr=subprocess.STDOUT)
    result['maven_exit_code'] = run.returncode
    if run.returncode == 0:
        coverage_command = [sys.executable, str(sample / 'verify-feature-coverage.py'), '--stage', 'published']
        if not args.require_complete_coverage:
            coverage_command.append('--report-only')
        coverage = subprocess.run(coverage_command, cwd=work)
        result['feature_coverage_exit_code'] = coverage.returncode
        coverage_report = sample / 'target' / 'feature-coverage-result.json'
        if coverage_report.exists():
            result['feature_coverage_complete'] = json.loads(coverage_report.read_text())['complete']
        result['complete'] = coverage.returncode == 0
    result['finished_at'] = datetime.now(timezone.utc).isoformat()
    (work / 'acceptance.json').write_text(json.dumps(result, indent=2) + '\n')
    print('Acceptance ' + ('passed' if result['complete'] else 'incomplete') + ': ' + str(work / 'acceptance.json'))
    return 0 if result['complete'] else 1


if __name__ == '__main__':
    sys.exit(main())
