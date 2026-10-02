/* Copyright 2026 QRun.IO, Inc. Licensed under the Apache License, Version 2.0. */
import assert from 'node:assert/strict'
import { test } from 'node:test'
import { mkdtempSync, mkdirSync, writeFileSync, symlinkSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { hashNativeTree, validateEnvironment } from './boundary-preflight.mjs'

const expected = { platform: 'darwin', architecture: 'arm64', osVersion: '14.8.2', nodeMajor: 22,
  playwrightVersion: '1.64.0-alpha-2026-10-01', installedDirectory: 'webkit-2251',
  toolsCommit: '2853b9b148282ae0e6d454adab6d0f534ba48280', publicSource: '7be255479bc39a3538a9597d1671c529b417bf18' }

test('preflight accepts only the source-bound macOS14 arm64 frozen WebKit environment', () => {
  assert.doesNotThrow(() => validateEnvironment(expected))
  for (const field of Object.keys(expected)) {
    assert.throws(() => validateEnvironment({ ...expected, [field]: field === 'nodeMajor' ? 24 : 'different' }))
  }
})

test('native tree hash covers file bytes and relative names without following symlinks', () => {
  const root = mkdtempSync(path.join(tmpdir(), 'qqq-preflight-'))
  try {
    mkdirSync(path.join(root, 'nested'))
    writeFileSync(path.join(root, 'pw_run.sh'), 'do not execute')
    writeFileSync(path.join(root, 'nested', 'native'), 'fixture-native')
    symlinkSync('nested/native', path.join(root, 'native-link'))
    const first = hashNativeTree(root)
    assert.equal(first.files, 2); assert.equal(first.symlinks, 1)
    assert.match(first.sha256, /^[a-f0-9]{64}$/)
    assert.deepEqual(hashNativeTree(root), first)
    writeFileSync(path.join(root, 'nested', 'native'), 'changed-native')
    assert.notEqual(hashNativeTree(root).sha256, first.sha256)
    // An external symlink must be hashed as a link, never traversed.
    symlinkSync('/missing/private-target', path.join(root, 'external-link'))
    const linked = hashNativeTree(root)
    assert.equal(linked.files, 2); assert.equal(linked.symlinks, 2)
    assert.deepEqual(Object.keys(linked), ['sha256', 'files', 'symlinks', 'bytes'])
    assert(!JSON.stringify(linked).includes('private-target'))
  } finally { rmSync(root, { recursive: true, force: true }) }
})
