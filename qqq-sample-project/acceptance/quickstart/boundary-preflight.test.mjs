/* Copyright 2026 QRun.IO, Inc. Licensed under the Apache License, Version 2.0. */
import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createHash } from 'node:crypto'
import { createRequire } from 'node:module'
import { mkdtempSync, mkdirSync, writeFileSync, symlinkSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { hashNativeTree, validateEnvironment, hashLoadedPlaywrightCore } from './boundary-preflight.mjs'

const expected = { platform: 'darwin', architecture: 'arm64', osVersion: '15.7.9', nodeMajor: 22,
  playwrightVersion: '1.64.0-alpha-2026-10-01', installedDirectory: 'webkit-2369',
  toolsCommit: '2853b9b148282ae0e6d454adab6d0f534ba48280', publicSource: '7be255479bc39a3538a9597d1671c529b417bf18' }

test('preflight accepts only the source-bound macOS15 arm64 maintained WebKit environment', () => {
  assert.doesNotThrow(() => validateEnvironment(expected))
  assert.throws(() => validateEnvironment({ ...expected, installedDirectory: 'webkit-2251' }))
  for (const field of Object.keys(expected)) {
    assert.throws(() => validateEnvironment({ ...expected, [field]: field === 'nodeMajor' ? 24 : 'different' }))
  }
})

test('preflight rejects the retained macOS14 frozen-browser pairing for this qualification', () => {
  assert.throws(() => validateEnvironment({ ...expected, osVersion: '14.8.9', installedDirectory: 'webkit_mac14_arm64_special-2251' }))
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


function fakeSdk(root) {
  function file(name, text) {
    const target = path.join(root, name)
    mkdirSync(path.dirname(target), {recursive: true})
    writeFileSync(target, text)
  }
  file('package.json', '{}')
  file('node_modules/@playwright/test/package.json', JSON.stringify({name:'@playwright/test', main:'index.cjs'}))
  file('node_modules/@playwright/test/index.cjs', "module.exports = require('playwright/test')")
  file('node_modules/playwright/package.json', JSON.stringify({name:'playwright', main:'test.js'}))
  file('node_modules/playwright/test.js', "module.exports = require('playwright-core')")
  file('node_modules/playwright-core/package.json', JSON.stringify({name:'playwright-core', version:'1.64.0-alpha-2026-10-01', main:'index.js'}))
  file('node_modules/playwright-core/index.js', "module.exports = require('./lib/coreBundle.js')")
  file('node_modules/playwright-core/lib/coreBundle.js', 'module.exports = {safeFixture: true}\n')
  return createRequire(path.join(root, 'package.json'))
}

test('SDK receipt binds actual loaded dependency graph and bundle bytes without absolute paths', () => {
  const root = mkdtempSync(path.join(tmpdir(), 'qqq-loaded-sdk-'))
  const foreign = mkdtempSync(path.join(tmpdir(), 'qqq-unrelated-sdk-'))
  try {
    const other = fakeSdk(foreign); other('@playwright/test')
    const require = fakeSdk(root)
    assert.throws(() => hashLoadedPlaywrightCore(require))
    assert.equal(require('@playwright/test').safeFixture, true)
    const receipt = hashLoadedPlaywrightCore(require)
    assert.equal(receipt.package, 'playwright-core')
    assert.equal(receipt.version, '1.64.0-alpha-2026-10-01')
    assert.equal(receipt.entry, 'lib/coreBundle.js')
    assert.equal(receipt.sha256, createHash('sha256').update('module.exports = {safeFixture: true}\n').digest('hex'))
    assert.match(receipt.resolvedPathSHA256, /^[a-f0-9]{64}$/)
    assert.equal(JSON.stringify(receipt).includes(root), false)
    assert.equal(JSON.stringify(receipt).includes(foreign), false)
  } finally { rmSync(root, {recursive:true,force:true}); rmSync(foreign, {recursive:true,force:true}) }
})

test('SDK provenance fails closed for missing or ambiguous loaded bundles', () => {
  const root = mkdtempSync(path.join(tmpdir(), 'qqq-ambiguous-sdk-'))
  const otherRoot = mkdtempSync(path.join(tmpdir(), 'qqq-second-sdk-'))
  try {
    const require = fakeSdk(root); require('@playwright/test')
    const entry = require.cache[require.resolve('@playwright/test')]
    const saved = entry.children
    entry.children = []
    assert.throws(() => hashLoadedPlaywrightCore(require))
    entry.children = saved
    const other = fakeSdk(otherRoot); other('@playwright/test')
    entry.children = [...saved, other.cache[other.resolve('@playwright/test')]]
    assert.throws(() => hashLoadedPlaywrightCore(require))
    entry.children = saved
  } finally { rmSync(root, {recursive:true,force:true}); rmSync(otherRoot, {recursive:true,force:true}) }
})
