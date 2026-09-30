#!/usr/bin/env python3
"""Stage reviewed Next evidence and invoke the source coverage gate in CI."""
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import stat
import subprocess
import sys
import tempfile
import urllib.parse
import urllib.request
import zipfile

from next_acceptance import accepted_release, read_json, require

MAX_ARCHIVE_BYTES = 2 * 1024**3
MAX_EXPANDED_BYTES = 4 * 1024**3
MAX_FILES = 10000


def valid_https(url):
    require(isinstance(url, str) and not any(ord(c) < 33 for c in url), 'invalid bundle URL')
    parsed = urllib.parse.urlsplit(url)
    require(parsed.scheme == 'https' and parsed.hostname and parsed.username is None
            and parsed.password is None and not parsed.fragment, 'bundle URL must be HTTPS without userinfo or fragment')
    return url


class HttpsRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, response, code, message, headers, newurl):
        valid_https(newurl)
        return super().redirect_request(request, response, code, message, headers, newurl)


def open_https(url):
    return urllib.request.build_opener(HttpsRedirect()).open(url, timeout=60)


def digest(path):
    value = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            value.update(chunk)
    return value.hexdigest()


def pins(environment):
    archive = environment.get('QQQ_NEXT_BUNDLE_SHA256', '')
    receipt = environment.get('QQQ_NEXT_RECEIPT_SHA256', '')
    require(all(re.fullmatch('[0-9a-f]{64}', value) for value in (archive, receipt)), 'reviewed SHA256 inputs are missing or invalid')
    return archive, receipt


def source_sha(sample, environment):
    sha = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=sample,
                                  text=True, stderr=subprocess.DEVNULL).strip()
    require(not environment.get('CIRCLE_SHA1') or environment['CIRCLE_SHA1'] == sha, 'CI checkout differs from trigger SHA')
    return sha


def extract_bundle(archive_path, destination):
    with zipfile.ZipFile(archive_path) as archive:
        entries = archive.infolist()
        require(len(entries) <= MAX_FILES and sum(e.file_size for e in entries) <= MAX_EXPANDED_BYTES,
                'bundle exceeds extraction limits')
        seen = set()
        for entry in entries:
            name = entry.filename
            path = PurePosixPath(name)
            require(name == entry.orig_filename and name and not path.is_absolute() and '\\' not in name
                    and all(p not in ('', '.', '..') for p in name.rstrip('/').split('/')),
                    'unsafe ZIP member path')
            require(path not in seen and not entry.flag_bits & 1, 'duplicate or encrypted ZIP member')
            seen.add(path)
            kind = stat.S_IFMT(entry.external_attr >> 16)
            require(kind in (0, stat.S_IFDIR if entry.is_dir() else stat.S_IFREG), 'ZIP member is not a regular file/directory')
            target = destination / path
            if entry.is_dir():
                target.mkdir(parents=True, exist_ok=True)
            else:
                target.parent.mkdir(parents=True, exist_ok=True)
                with archive.open(entry) as incoming, target.open('xb') as outgoing:
                    shutil.copyfileobj(incoming, outgoing, 1024 * 1024)


def stage(sample, environment):
    target = sample / 'target'
    require(not target.is_symlink(), 'target directory must not be a symlink')
    target.mkdir(parents=True, exist_ok=True)
    bundle = target / 'next-acceptance'
    marker = target / 'next-acceptance-stage.json'
    marker.unlink(missing_ok=True)
    # Remove only the owned destination; never follow a pre-existing destination symlink.
    if bundle.is_symlink() or bundle.is_file():
        bundle.unlink()
    elif bundle.exists():
        shutil.rmtree(bundle)
    result = {'ready': False, 'scope': 'Transport integrity only; native acceptance and ledger review remain separate gates.'}
    try:
        archive_sha, receipt_sha = pins(environment)
        url = valid_https(environment.get('QQQ_NEXT_CI_BUNDLE_URL') or environment.get('QQQ_NEXT_BUNDLE_URL', ''))
        sha = source_sha(sample, environment)
        with tempfile.TemporaryDirectory(prefix='next-stage-', dir=target) as work:
            work = Path(work)
            archive_path = work / 'bundle.zip'
            with open_https(url) as incoming, archive_path.open('wb') as outgoing:
                total = 0
                while chunk := incoming.read(1024 * 1024):
                    total += len(chunk)
                    require(total <= MAX_ARCHIVE_BYTES, 'bundle exceeds download limit')
                    outgoing.write(chunk)
            require(digest(archive_path) == archive_sha, 'bundle archive hash mismatch')
            unpacked = work / 'unpacked'
            unpacked.mkdir()
            extract_bundle(archive_path, unpacked)
            require(digest(unpacked / 'receipt.json') == receipt_sha, 'reviewed receipt hash mismatch')
            receipt = read_json(unpacked / 'receipt.json')
            crosswalk = read_json(sample / 'next-acceptance-crosswalk.json')
            require(receipt['schema_version'] == 1 and receipt['qqq_sha'] == sha
                    and receipt['next_sha'] == crosswalk['next_sha'], 'stale source receipt')
            unpacked.rename(bundle)
        result.update(ready=True, source_sha=sha, archive_sha256=archive_sha, receipt_sha256=receipt_sha)
    except (OSError, ValueError, KeyError, TypeError, zipfile.BadZipFile, RuntimeError, subprocess.CalledProcessError) as error:
        # URLs (including signed queries) and exception text can contain credentials.
        result['error'] = 'Next bundle not staged: ' + type(error).__name__
    marker.write_text(json.dumps(result, indent=2) + '\n')
    print('Next receipt staging: ' + ('ready for native validation' if result['ready'] else 'pending/invalid; see staging diagnostic'))


def coverage(sample, environment, report_only):
    if (sample / 'target').is_symlink():
        print('Coverage target directory must not be a symlink', file=sys.stderr)
        return 1
    ready = False
    receipt_sha = None
    try:
        archive_sha, receipt_sha = pins(environment)
        staged = read_json(sample / 'target/next-acceptance-stage.json')
        ready = (not (sample / 'target/next-acceptance').is_symlink()
                 and staged['ready'] is True and staged['source_sha'] == source_sha(sample, environment)
                 and staged['archive_sha256'] == archive_sha and staged['receipt_sha256'] == receipt_sha)
    except (OSError, ValueError, KeyError, TypeError, subprocess.CalledProcessError):
        # Missing or invalid staging must leave ready=False so strict coverage fails below.
        pass
    command = [sys.executable, '-B', str(sample / 'verify-feature-coverage.py'), '--stage', 'source']
    candidate_version = environment.get('QQQ_ACCEPTED_NEXT_CANDIDATE', '')
    try:
        accepted_next = accepted_release(sample, candidate_version, before_publish=True)
        if accepted_next:
            require(not environment.get('CIRCLE_TAG'), 'RC1 exception is not allowed in a tag pipeline')
            if environment.get('CIRCLECI') or 'CIRCLE_BRANCH' in environment:
                require(environment.get('CIRCLE_BRANCH') in ('release/4.1', 'release/4.1.0'),
                        'RC1 exception is only allowed before the first 4.1 candidate publication')
    except (OSError, ValueError, KeyError, TypeError):
        print('Invalid candidate binding for the approved Next RC1 exception', file=sys.stderr)
        return 1
    if candidate_version:
        command += ['--candidate-version', candidate_version]
    if ready:
        command += ['--next-receipt-sha256', receipt_sha]
    if report_only:
        command += ['--report-only']
    status = subprocess.run(command, cwd=sample).returncode
    return status if report_only or ready or accepted_next else 1


def main(argv=None, sample=None, environment=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mode', choices=('stage', 'report', 'strict'))
    args = parser.parse_args(argv)
    sample = sample or Path(__file__).resolve().parent
    environment = os.environ if environment is None else environment
    if args.mode == 'stage':
        try:
            stage(sample, environment)
        except (OSError, ValueError):
            print('Next receipt staging could not safely prepare its owned target directory', file=sys.stderr)
            return 1
        # Reporting jobs keep running to expose gaps; strict coverage cannot pass an invalid stage.
        return 0
    return coverage(sample, environment, args.mode == 'report')


if __name__ == '__main__':
    sys.exit(main())
