/* Copyright 2026 QRun.IO, Inc. Licensed under the Apache License, Version 2.0. */
import assert from 'node:assert/strict'
import { test } from 'node:test'
import { mkdtempSync, readFileSync, writeFileSync, existsSync, rmSync, statSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { createPhaseJournal } from './phase-journal.mjs'

const source = readFileSync(new URL('./run-public.mjs', import.meta.url), 'utf8')
const AsyncFunction = Object.getPrototypeOf(async function () {}).constructor
const catchBody = source.slice(source.lastIndexOf('} catch (error) {') + '} catch (error) {'.length, source.lastIndexOf('} finally {'))
const finallyBody = source.slice(source.lastIndexOf('} finally {') + '} finally {'.length, source.lastIndexOf('\n}'))

function fixture() {
  const output = mkdtempSync(path.join(tmpdir(), 'qqq-cleanup-journal-'))
  const journalFile = path.join(output, 'phase-journal.json')
  const partialFile = path.join(output, 'lifecycle-partial.json')
  const applicationFile = path.join(output, 'initial-application.log')
  const journal = createPhaseJournal(journalFile, { enabled: true })
  journal.mark('runner', 'run', 'start')
  const report = { phases: [], pageErrors: [], complete: false }
  let copied = 0
  const checkpoint = () => journal.checkpoint('initial', partialFile, report, () => { copied++; writeFileSync(applicationFile, 'fixture only', { mode: 0o600 }) })
  const disk = () => JSON.parse(readFileSync(journalFile, 'utf8'))
  return { output, journal, report, checkpoint, disk, partialFile, applicationFile, copied: () => copied, cleanup: () => rmSync(output, { recursive: true, force: true }) }
}

const executeCatch = new AsyncFunction('error', 'report', 'process', 'checkpoint', 'page', 'journal', 'phaseName', 'path', 'output', catchBody)
const executeFinally = new AsyncFunction('app', 'stop', 'browser', 'journal', 'phaseName', 'checkpoint', 'report', 'process', 'existsSync', 'writeFileSync', 'path', 'output', 'console', finallyBody)

test('actual catch body persists partial error and application log before a screenshot never settles', () => {
  const f = fixture()
  try {
    let calls = 0
    const page = { isClosed: () => false, screenshot(options) {
      assert.equal(this, page); calls++
      assert.deepEqual(options, { path: path.join(f.output, 'failure.png'), fullPage: true })
      assert(existsSync(f.partialFile)); assert(existsSync(f.applicationFile))
      assert.equal(JSON.parse(readFileSync(f.partialFile, 'utf8')).errorPresent, true)
      return new Promise(() => {})
    } }
    const processStub = {}
    executeCatch({ stack: 'private fake error' }, f.report, processStub, f.checkpoint, page, f.journal, 'initial', path, f.output)
    assert.equal(processStub.exitCode, 1)
    assert.equal(calls, 1)
    assert.equal(f.copied(), 1)
    assert.equal(f.disk().entries.at(-1).operation, 'failure-screenshot')
    assert.equal(f.disk().entries.at(-1).state, 'start')
    assert.equal(f.disk().incomplete, true)
    assert(!JSON.stringify(f.disk()).includes('private'))
  } finally { f.cleanup() }
})

test('actual finally body leaves an app-stop start receipt when stop never settles', () => {
  const f = fixture()
  try {
    let calls = 0
    const stop = signal => {
      calls++; assert.equal(signal, 'SIGTERM')
      assert(existsSync(f.partialFile)); assert(existsSync(f.applicationFile))
      return new Promise(() => {})
    }
    const browser = { close() { assert.fail('must not change original sequential cleanup order') } }
    executeFinally({}, stop, browser, f.journal, 'initial', f.checkpoint, f.report, {}, existsSync, writeFileSync, path, f.output, { log() {} })
    assert.equal(calls, 1)
    assert.equal(f.disk().entries.at(-1).operation, 'app-stop')
    assert.equal(f.disk().entries.at(-1).state, 'start')
    assert.equal(f.disk().incomplete, true)
  } finally { f.cleanup() }
})

test('actual finally preserves rejected stop handling and records the subsequent browser-close hang', async () => {
  const f = fixture()
  try {
    const rejected = Promise.reject({ message: 'fake private cleanup error' })
    const processStub = {}
    let closeCalls = 0
    const browser = { close() {
      assert.equal(this, browser); closeCalls++
      assert.equal(JSON.parse(readFileSync(f.partialFile, 'utf8')).cleanupErrorPresent, true)
      return new Promise(() => {})
    } }
    executeFinally({}, () => rejected, browser, f.journal, 'initial', f.checkpoint, f.report, processStub, existsSync, writeFileSync, path, f.output, { log() {} })
    // Drain only the original rejection handler and await continuation; no timers.
    await rejected.catch(() => {})
    await Promise.resolve()
    assert.equal(closeCalls, 1)
    assert.equal(processStub.exitCode, 1)
    assert.equal(f.report.cleanupError, 'fake private cleanup error')
    assert.equal(f.disk().entries.at(-1).operation, 'browser-close')
    assert.equal(f.disk().entries.at(-1).state, 'start')
    assert.equal(f.copied(), 2)
  } finally { f.cleanup() }
})

test('actual finally saves the post-cleanup checkpoint and original final report after successful cleanup', async () => {
  const f = fixture()
  try {
    f.report.complete = true
    let stopped = 0
    let closed = 0
    let logged = 0
    await executeFinally({}, async signal => { stopped++; assert.equal(signal, 'SIGTERM') }, { async close() { closed++ } }, f.journal, 'initial', f.checkpoint, f.report, {}, existsSync, writeFileSync, path, f.output, { log(value) { logged++; assert.deepEqual(JSON.parse(value), f.report) } })
    assert.equal(stopped, 1); assert.equal(closed, 1); assert.equal(logged, 1)
    assert.equal(f.copied(), 3)
    assert.equal(f.disk().incomplete, false)
    assert.equal(f.disk().entries.at(-1).state, 'success')
    assert.equal(JSON.parse(readFileSync(f.partialFile, 'utf8')).incomplete, true)
    assert.equal(JSON.parse(readFileSync(path.join(f.output, 'lifecycle-result.json'), 'utf8')).complete, true)
    assert.equal(statSync(f.partialFile).mode & 0o777, 0o600)
  } finally { f.cleanup() }
})
