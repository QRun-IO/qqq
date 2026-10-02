/* Copyright 2026 QRun.IO, Inc. Licensed under the Apache License, Version 2.0. */
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { createHash } from 'node:crypto'
import { lstatSync, mkdirSync, readFileSync, readdirSync, readlinkSync, realpathSync, writeFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const toolsCommit = '2853b9b148282ae0e6d454adab6d0f534ba48280'
const publicSource = '7be255479bc39a3538a9597d1671c529b417bf18'
const sha256 = value => createHash('sha256').update(value).digest('hex')

export function validateEnvironment(value) {
  assert.equal(value.platform, 'darwin')
  assert.equal(value.architecture, 'arm64')
  assert.match(value.osVersion, /^14\./)
  assert.equal(value.nodeMajor, 22)
  assert.equal(value.playwrightVersion, '1.64.0-alpha-2026-10-01')
  assert.equal(value.installedDirectory, 'webkit-2251')
  assert.equal(value.toolsCommit, toolsCommit)
  assert.equal(value.publicSource, publicSource)
}

/** Hash installed bytes and link targets without executing or following native links. */
export function hashNativeTree(root) {
  const digest = createHash('sha256')
  let files = 0; let symlinks = 0; let bytes = 0
  function visit(relative) {
    const directory = path.join(root, relative)
    for (const name of readdirSync(directory).sort()) {
      const child = path.join(relative, name)
      const absolute = path.join(root, child)
      const stat = lstatSync(absolute)
      if (stat.isDirectory()) visit(child)
      else if (stat.isSymbolicLink()) {
        symlinks++
        digest.update(JSON.stringify([child, 'link', readlinkSync(absolute)]) + '\n')
      } else {
        assert(stat.isFile(), 'unsupported-native-entry')
        files++; bytes += stat.size
        digest.update(JSON.stringify([child, 'file', stat.mode & 0o777, stat.size, sha256(readFileSync(absolute))]) + '\n')
      }
    }
  }
  visit('')
  assert(files > 0, 'empty-native-tree')
  return { sha256: digest.digest('hex'), files, symlinks, bytes }
}

function main() {
  const output = process.env.QQQ_QUICKSTART_PREFLIGHT_OUTPUT
  assert(output, 'preflight-output-required')
  mkdirSync(output, { recursive: true, mode: 0o700 })
  let stage = 'source'
  const receipt = { version: 1, complete: false, browserLaunched: false }
  try {
    const root = process.cwd()
    const gitHead = cwd => execFileSync('git', ['rev-parse', 'HEAD'], { cwd, encoding: 'utf8' }).trim()
    const tools = path.resolve('acceptance-tools')
    receipt.harnessCommit = gitHead(root)
    receipt.toolsCommit = gitHead(tools)
    receipt.publicSource = process.env.EXPECTED_SHA
    assert.match(receipt.harnessCommit, /^[a-f0-9]{40}$/)
    stage = 'environment'
    const require = createRequire(path.join(tools, 'package.json'))
    const { webkit } = require('@playwright/test')
    const launcher = realpathSync(webkit.executablePath())
    const nativeRoot = path.dirname(launcher)
    Object.assign(receipt, { platform: os.platform(), architecture: os.arch(),
      osVersion: execFileSync('/usr/bin/sw_vers', ['-productVersion'], { encoding: 'utf8' }).trim(),
      nodeMajor: Number(process.versions.node.split('.')[0]), nodeVersion: process.version,
      playwrightVersion: require('@playwright/test/package.json').version,
      installedDirectory: path.basename(nativeRoot) })
    validateEnvironment(receipt)
    stage = 'hashes'
    receipt.launcherSHA256 = sha256(readFileSync(launcher))
    receipt.nativeTree = hashNativeTree(nativeRoot)
    receipt.toolsLockSHA256 = sha256(readFileSync(path.join(tools, 'pnpm-lock.yaml')))
    receipt.sourceSHA256 = Object.fromEntries([
      'run-public.mjs', 'phase-journal.mjs', 'boundary-preflight.mjs',
      'phase-journal.test.mjs', 'cleanup-contract.test.mjs', 'boundary-preflight.test.mjs',
      'workflow-contract.test.mjs',
    ].map(name => [name, sha256(readFileSync(new URL(name, import.meta.url)))]))
    receipt.workflowSHA256 = sha256(readFileSync(path.join(root, '.github/workflows/quickstart-lifecycle.yml')))
    receipt.complete = true
  } catch {
    receipt.failedStage = stage
    process.exitCode = 1
    process.stderr.write('quickstart-boundary-preflight-failed\n')
  }
  writeFileSync(path.join(output, 'preflight.json'), JSON.stringify(receipt, null, 2) + '\n', { mode: 0o600 })
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) main()
