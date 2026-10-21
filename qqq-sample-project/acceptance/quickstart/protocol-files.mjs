/* Copyright 2026 QRun.IO, Inc. Licensed under the Apache License, Version 2.0. */
import { openSync, closeSync, readSync, fstatSync, constants, writeFileSync, readFileSync } from 'node:fs'
import { createHash } from 'node:crypto'
import { resolve } from 'node:path'
import { pathToFileURL } from 'node:url'
import { summarizeProtocol } from './protocol-summary.mjs'

function privateRead(file, cap) {
  const fd = openSync(file, constants.O_RDONLY | constants.O_NOFOLLOW)
  try {
    const before = fstatSync(fd)
    if (!before.isFile() || (before.mode & 0o077) !== 0 || before.size > cap) return undefined
    const data = Buffer.alloc(before.size + 1)
    let length = 0, n
    while (length < data.length && (n = readSync(fd, data, length, data.length - length, null)) > 0) length += n
    const after = fstatSync(fd)
    if (length !== before.size || after.size !== before.size || after.mtimeMs !== before.mtimeMs) return undefined
    return data.subarray(0, length)
  } finally { closeSync(fd) }
}

/** File adapter has fixed failure aliases and never serializes exceptions. */
export function summarizeFiles(rawFile, journalFile) {
  let raw
  try { raw = privateRead(rawFile, 67_108_864) } catch {}
  if (!raw) return { version: 1, available: false, reason: 'raw-unavailable' }
  let journalAvailable = false, pageCreateEnd
  try {
    const journal = JSON.parse(privateRead(journalFile, 262_144)?.toString('utf8') ?? '')
    if (Array.isArray(journal.entries) && journal.entries.length <= 256) {
      let started = false
      for (let i = 0; i < journal.entries.length; i++) {
        const e = journal.entries[i]
        if (!e || e.seq !== i + 1 || !Number.isSafeInteger(e.at)) throw new Error()
        if (e.phase === 'initial' && e.operation === 'page-create') {
          if (e.state === 'start') { if (started) throw new Error(); started = true }
          else if (started && ['success','error'].includes(e.state) && !pageCreateEnd) pageCreateEnd = { at: e.at, state: e.state }
        }
      }
      journalAvailable = true
    }
  } catch { pageCreateEnd = undefined }
  function* lines() {
    let start = 0
    for (let end = raw.indexOf(10); end !== -1; end = raw.indexOf(10, start)) {
      yield raw.subarray(start, end + 1).toString('utf8')
      start = end + 1
    }
    if (start < raw.length) yield raw.subarray(start).toString('utf8')
  }
  const sanitizerSHA256 = Object.fromEntries(['protocol-files.mjs','protocol-summary.mjs'].map(name => [name, createHash('sha256').update(readFileSync(new URL(name, import.meta.url))).digest('hex')]))
  return { version: 1, available: true, sanitizerSHA256, rawBytes: raw.length, rawSHA256: createHash('sha256').update(raw).digest('hex'), journalAvailable, summary: summarizeProtocol(lines(), { pageCreateEnd }) }
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  try {
    const result = summarizeFiles(process.argv[2], process.argv[3])
    writeFileSync(process.argv[4], JSON.stringify(result, null, 2) + '\n', { mode: 0o600, flag: 'wx' })
    if (!result.available) process.exitCode = 1
  } catch {
    process.stderr.write('protocol-summary-unavailable\n')
    process.exitCode = 1
  }
}
