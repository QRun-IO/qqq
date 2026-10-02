/* Copyright 2026 QRun.IO, Inc. Licensed under the Apache License, Version 2.0. */
import assert from 'node:assert/strict'
import { test } from 'node:test'
import { mkdtempSync, readFileSync, existsSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { createPhaseJournal } from './phase-journal.mjs'

function fixture() {
  const dir = mkdtempSync(path.join(tmpdir(), 'qqq-phase-journal-'))
  const file = path.join(dir, 'phase-journal.json')
  return { file, cleanup: () => rmSync(dir, { recursive: true, force: true }) }
}

test('start is on disk before entering a never-resolving native operation', () => {
  const f = fixture()
  try {
    const journal = createPhaseJournal(f.file, { enabled: true })
    const pending = new Promise(() => {})
    const returned = journal.observe('initial', 'context-create', () => {
      assert(existsSync(f.file), 'start must already be persisted before callback')
      const disk = JSON.parse(readFileSync(f.file, 'utf8'))
      assert.deepEqual(disk.entries.map(e => [e.phase, e.operation, e.state]), [['initial', 'context-create', 'start']])
      return pending
    })
    assert.equal(returned, pending)
  } finally { f.cleanup() }
})

test('native receiver, argument identity, callback count and synchronous result are unchanged', () => {
  const f = fixture()
  try {
    const journal = createPhaseJournal(f.file, { enabled: true })
    const argument = { privateValue: 'do-not-copy' }
    const result = { unchanged: true }
    let calls = 0
    const receiver = { method(value) { calls++; assert.equal(this, receiver); assert.equal(value, argument); return result } }
    assert.equal(journal.observe('initial', 'page-create', () => receiver.method(argument)), result)
    assert.equal(calls, 1)
    assert.deepEqual(journal.snapshot().entries.map(e => e.state), ['start', 'success'])
  } finally { f.cleanup() }
})

test('synchronous throws and native Promise rejections preserve the identical opaque error', async () => {
  const f = fixture()
  try {
    const journal = createPhaseJournal(f.file, { enabled: true })
    const error = Object.create(null, { message: { get() { throw new Error('must not inspect') } }, stack: { get() { throw new Error('must not inspect') } } })
    let calls = 0
    assert.throws(() => journal.observe('initial', 'page-goto', () => { calls++; throw error }), value => value === error)
    const rejected = Promise.reject(error)
    const returned = journal.observe('initial', 'page-reload', () => { calls++; return rejected })
    assert.equal(returned, rejected)
    await assert.rejects(returned, value => value === error)
    assert.equal(calls, 2)
    assert.deepEqual(journal.snapshot().entries.map(e => e.state), ['start', 'error', 'start', 'error'])
  } finally { f.cleanup() }
})

test('sequential successful phases remain ordered and only final completion clears incomplete', async () => {
  const f = fixture()
  try {
    const journal = createPhaseJournal(f.file, { enabled: true })
    journal.mark('runner', 'run', 'start')
    for (const phase of ['initial', 'restart', 'compile-failure', 'java-edit', 'material']) {
      const result = {}
      const promise = Promise.resolve(result)
      assert.equal(journal.observe(phase, 'launcher-start', () => promise), promise)
      assert.equal(await promise, result)
      assert.equal(journal.snapshot().incomplete, true)
    }
    journal.mark('runner', 'run', 'success')
    const disk = JSON.parse(readFileSync(f.file, 'utf8'))
    assert.equal(disk.incomplete, false)
    assert.deepEqual(disk.entries.map(e => e.seq), Array.from({ length: 12 }, (_, i) => i + 1))
    assert.deepEqual(disk.entries.filter(e => e.state === 'start').map(e => e.phase), ['runner', 'initial', 'restart', 'compile-failure', 'java-edit', 'material'])
  } finally { f.cleanup() }
})

test('bounded journal rejects aliases without coercion, copies snapshots and never serializes result/error data', () => {
  const f = fixture()
  try {
    const journal = createPhaseJournal(f.file, { enabled: true, limit: 3 })
    const unknown = { toString() { throw new Error('must not coerce') } }
    journal.mark(unknown, 'page-goto', 'start')
    journal.mark('initial', 'https://private.invalid/?secret=hidden', 'start')
    journal.observe('initial', 'page-goto', () => ({ secret: 'not-for-journal' }))
    journal.mark('runner', 'run', 'success')
    journal.mark('material', 'page-create', 'start')
    const snapshot = journal.snapshot()
    snapshot.entries[0].phase = 'mutated'
    snapshot.entries.push({ secret: 'not-for-journal' })
    const disk = JSON.parse(readFileSync(f.file, 'utf8'))
    assert.equal(disk.entries.length, 3)
    assert.equal(disk.dropped, 3)
    assert.equal(disk.incomplete, true)
    for (const entry of disk.entries) assert.deepEqual(Object.keys(entry), ['seq', 'at', 'phase', 'operation', 'state'])
    assert(!readFileSync(f.file, 'utf8').includes('secret'))
    assert(!JSON.stringify(journal.snapshot()).includes('mutated'))
  } finally { f.cleanup() }
})

test('journal write failure cannot replace the original operation outcome', () => {
  const f = fixture()
  try {
    const journal = createPhaseJournal(path.join(f.file, 'missing', 'journal.json'), { enabled: true })
    const result = {}
    assert.equal(journal.observe('initial', 'page-create', () => result), result)
    assert.equal(journal.snapshot().persistenceFailures, 2)
    assert.equal(journal.snapshot().incomplete, true)
  } finally { f.cleanup() }
})

test('partial report and application log precede an unresolved screenshot and cleanup operation', () => {
  const f = fixture()
  try {
    const journal = createPhaseJournal(f.file, { enabled: true })
    const partial = path.join(path.dirname(f.file), 'partial.json')
    const application = path.join(path.dirname(f.file), 'application.log')
    const report = { complete: false, phases: [{}], pageErrors: [{ message: 'secret' }], error: 'opaque-secret', cleanupError: undefined }
    let copies = 0
    journal.checkpoint('initial', partial, report, () => { copies++; writeFileSync(application, 'private fake log') })
    const pending = new Promise(() => {})
    journal.observe('initial', 'failure-screenshot', () => {
      assert(existsSync(partial)); assert(existsSync(application)); return pending
    })
    journal.checkpoint('initial', partial, report, () => { copies++ })
    journal.observe('initial', 'app-stop', () => {
      const disk = JSON.parse(readFileSync(partial, 'utf8'))
      assert.equal(disk.completedPhases, 1)
      assert.equal(disk.pageErrorCount, 1)
      assert.equal(disk.errorPresent, true)
      assert.equal(disk.complete, false)
      assert(!JSON.stringify(disk).includes('secret'))
      return pending
    })
    assert.equal(copies, 2)
    const disk = JSON.parse(readFileSync(f.file, 'utf8'))
    assert.deepEqual(disk.entries.at(-1).operation, 'app-stop')
    assert.deepEqual(disk.entries.at(-1).state, 'start')
    assert.equal(disk.incomplete, true)
  } finally { f.cleanup() }
})

test('failure to attach an observer cannot replace an otherwise valid returned Promise', async () => {
  const f = fixture()
  try {
    const journal = createPhaseJournal(f.file, { enabled: true })
    const promise = Promise.resolve({ unchanged: true })
    Object.defineProperty(promise, 'constructor', { configurable: true, get() { throw new Error('private constructor getter') } })
    const returned = journal.observe('initial', 'page-create', () => promise)
    assert.equal(returned, promise)
    assert.equal(journal.snapshot().incomplete, true)
    assert.equal(journal.snapshot().dropped, 1)
    delete promise.constructor
    await promise
  } finally { f.cleanup() }
})


test('capture is disabled by default with no files, clocks or Promise observers', () => {
  const f = fixture()
  try {
    const journal = createPhaseJournal(f.file)
    let callbacks = 0
    let observed = 0
    const promise = Promise.resolve({ unchanged: true })
    Object.defineProperty(promise, 'constructor', { get() { observed++; throw new Error('must not inspect') } })
    assert.equal(journal.observe('initial', 'context-create', () => { callbacks++; return promise }), promise)
    journal.mark('runner', 'run', 'start')
    journal.checkpoint('initial', f.file, {}, () => { callbacks++ })
    assert.equal(callbacks, 1)
    assert.equal(observed, 0)
    assert.equal(existsSync(f.file), false)
    assert.equal(existsSync(f.file + '.tmp'), false)
  } finally { f.cleanup() }
})

test('disabled capture preserves receiver/args, result and thrown object', () => {
  const f = fixture()
  try {
    const journal = createPhaseJournal(f.file, { enabled: false })
    const result = {}; const argument = {}; const failure = {}
    const receiver = { method(value) { assert.equal(this, receiver); assert.equal(value, argument); return result } }
    assert.equal(journal.observe('initial', 'page-create', () => receiver.method(argument)), result)
    assert.throws(() => journal.observe('initial', 'page-create', () => { throw failure }), error => error === failure)
    assert.equal(existsSync(f.file), false)
  } finally { f.cleanup() }
})

test('failed partial persistence stays incomplete and cannot prevent log copy or cleanup', () => {
  const f = fixture()
  try {
    const journal = createPhaseJournal(f.file, { enabled: true })
    let copied = 0; let cleaned = 0
    journal.checkpoint('initial', path.join(f.file, 'missing', 'partial.json'), { phases: [], pageErrors: [], complete: false }, () => { copied++ })
    const result = {}
    assert.equal(journal.observe('initial', 'app-stop', () => { cleaned++; return result }), result)
    journal.mark('runner', 'run', 'success')
    assert.equal(copied, 1); assert.equal(cleaned, 1)
    assert.equal(journal.snapshot().persistenceFailures, 1)
    assert.equal(journal.snapshot().incomplete, true)
  } finally { f.cleanup() }
})

test('failed application-log copy records a bounded fault without serializing the exception', () => {
  const f = fixture()
  try {
    const journal = createPhaseJournal(f.file, { enabled: true })
    const error = { toString() { throw new Error('do not inspect') } }
    journal.checkpoint('initial', path.join(path.dirname(f.file), 'partial.json'), { phases: [], pageErrors: [], complete: false }, () => { throw error })
    journal.mark('runner', 'run', 'success')
    assert.equal(journal.snapshot().persistenceFailures, 1)
    assert.equal(journal.snapshot().incomplete, true)
    assert.equal(journal.snapshot().entries.some(e => e.operation === 'application-log' && e.state === 'error'), true)
  } finally { f.cleanup() }
})
