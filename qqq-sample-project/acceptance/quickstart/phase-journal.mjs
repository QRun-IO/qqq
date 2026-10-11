/* Copyright 2026 QRun.IO, Inc. Licensed under the Apache License, Version 2.0. */
import { renameSync, writeFileSync } from 'node:fs'

const phases = new Set(['runner', 'initial', 'restart', 'compile-failure', 'java-edit', 'material'])
const operations = new Set(['run', 'browser-launch', 'launcher-start', 'next-ready', 'context-create', 'page-create', 'page-goto', 'page-reload', 'screenshot', 'context-close', 'app-stop', 'browser-close', 'failure-screenshot', 'crud', 'process', 'material-checks', 'partial-report', 'application-log'])
const states = new Set(['start', 'success', 'error'])

/** Observe native Promise boundaries without replacing the caller's result or rejection. */
export function createPhaseJournal(file, { enabled = false, limit = 256 } = {}) {
  if (enabled !== true) return {
    mark() {},
    observe(_phase, _operation, callback) { return callback() },
    checkpoint() {},
    snapshot() { return null },
  }
  const capacity = Number.isInteger(limit) && limit > 0 ? Math.min(limit, 256) : 256
  const entries = []
  let seq = 0
  let dropped = 0
  let persistenceFailures = 0
  let finished = false
  const snapshot = () => ({ version: 1, entries: entries.map(entry => ({ ...entry })), dropped, persistenceFailures, incomplete: !finished || dropped > 0 || persistenceFailures > 0 })
  function mark(phase, operation, state) {
    if (!phases.has(phase) || !operations.has(operation) || !states.has(state) || entries.length >= capacity) {
      dropped++
    } else {
      entries.push({ seq: ++seq, at: Date.now(), phase, operation, state })
      if (phase === 'runner' && operation === 'run' && state !== 'start') finished = true
    }
    try {
      writeFileSync(`${file}.tmp`, JSON.stringify(snapshot()) + '\n', { mode: 0o600 })
      renameSync(`${file}.tmp`, file)
    } catch { persistenceFailures++ }
  }
  function observe(phase, operation, callback) {
    mark(phase, operation, 'start')
    let result
    try { result = callback() }
    catch (error) { mark(phase, operation, 'error'); throw error }
    if (result instanceof Promise) {
      try {
        Promise.prototype.then.call(result,
          () => mark(phase, operation, 'success'),
          () => mark(phase, operation, 'error'))
      } catch { mark(phase, undefined, 'error') }
    } else mark(phase, operation, 'success')
    return result
  }
  function checkpoint(phase, partialFile, report, copyApplicationLog) {
    // Each write is best effort; neither may prevent the original cleanup.
    try {
      observe(phase, 'partial-report', () => {
        const partial = { version: 1, at: Date.now(), complete: report.complete === true,
          completedPhases: report.phases.length, pageErrorCount: report.pageErrors.length,
          errorPresent: Boolean(report.error), cleanupErrorPresent: Boolean(report.cleanupError),
          incomplete: true }
        writeFileSync(`${partialFile}.tmp`, JSON.stringify(partial) + '\n', { mode: 0o600 })
        renameSync(`${partialFile}.tmp`, partialFile)
      })
    } catch { persistenceFailures++ }
    try { observe(phase, 'application-log', copyApplicationLog) }
    catch { persistenceFailures++ }
  }
  return { mark, observe, snapshot, checkpoint }
}
