/* Copyright 2026 QRun.IO, Inc. Licensed under the Apache License, Version 2.0. */
const methods = new Set([
  'Playwright.enable', 'Playwright.createContext', 'Playwright.setDownloadBehavior', 'Playwright.createPage',
  'Playwright.pageProxyCreated', 'Playwright.pageProxyDestroyed', 'Playwright.provisionalLoadFailed',
  'Target.targetCreated', 'Target.targetDestroyed', 'Target.sendMessageToTarget', 'Target.dispatchMessageFromTarget', 'Target.resume',
  'Dialog.enable', 'Emulation.setActiveAndFocused', 'Emulation.setDeviceMetricsOverride', 'Emulation.setAuthCredentials',
  'Page.enable', 'Page.getResourceTree', 'Page.createUserWorld', 'Page.overrideUserAgent', 'Page.setEmulatedMedia',
  'Page.overrideUserPreference', 'Page.setBootstrapScript', 'Page.setScreenSizeOverride', 'Page.setTouchEmulationEnabled',
  'Page.overrideSetting', 'Page.frameNavigated', 'Runtime.enable', 'Network.enable', 'Network.setExtraHTTPHeaders',
  'Worker.enable', 'Console.enable',
])
const idOK = value => Number.isSafeInteger(value) && value >= 0
const opaqueOK = value => typeof value === 'string' && value.length > 0 && value.length <= 256
const record = value => value !== null && typeof value === 'object' && !Array.isArray(value)
const urlKind = value => value === '' ? 'empty' : value === 'about:blank' ? 'about-blank' : 'other'

/** Derive only first-page handshake metadata; never return raw protocol fields. */
export function summarizeProtocol(lines, { pageCreateEnd, limit = 2048, lineLimit = 1_048_576, byteLimit = 67_108_864 } = {}) {
  limit = Number.isInteger(limit) && limit > 0 ? Math.min(limit, 2048) : 2048
  lineLimit = Number.isInteger(lineLimit) && lineLimit > 0 ? Math.min(lineLimit, 1_048_576) : 1_048_576
  byteLimit = Number.isInteger(byteLimit) && byteLimit > 0 ? Math.min(byteLimit, 67_108_864) : 67_108_864
  const events = [], pending = new Map(), aliases = new Map()
  const native = { launchedPids: [], exits: [] }
  const counts = { malformed: 0, truncated: 0, oversize: 0, dropped: 0, ignored: 0, unmatched: 0, ambiguous: 0, unrecognizedCommands: 0 }
  let context, proxy, pageRequested = false, bytes = 0, scopeEnd = 'input-ended'
  const end = record(pageCreateEnd) && Number.isSafeInteger(pageCreateEnd.at) && ['success','error'].includes(pageCreateEnd.state) ? pageCreateEnd : undefined
  function alias(value, kind) {
    if (!opaqueOK(value)) return undefined
    const key = `${kind}:${value}`
    if (!aliases.has(key)) aliases.set(key, `${kind}-${aliases.size + 1}`)
    return aliases.get(key)
  }
  function processMessage(direction, message, at, target) {
    if (!record(message)) { counts.malformed++; return }
    const params = record(message.params) ? message.params : {}
    const isResponse = idOK(message.id) && !Object.hasOwn(message, 'method')
    const scope = target ? 'target' : message.pageProxyId ? 'proxy' : 'root'
    const key = `${scope}:${target ?? message.pageProxyId ?? ''}:${message.id}`
    let method = message.method
    if (isResponse) {
      const sent = pending.get(key)
      if (!sent) { counts.unmatched++; return }
      method = sent.method
      pending.delete(key)
    }
    if (!methods.has(method)) {
      if (direction === 'send' && pageRequested) counts.unrecognizedCommands++
      else counts.ignored++
      return
    }
    if (message.pageProxyId && message.pageProxyId !== proxy) { counts.ignored++; return }
    if (method === 'Playwright.createContext' && isResponse && !message.error) {
      if (context) { counts.ambiguous++; return }
      context = message.result?.browserContextId
      if (!opaqueOK(context)) { counts.malformed++; return }
    }
    if (method === 'Playwright.createPage' && !isResponse) {
      if (pageRequested || !context || params.browserContextId !== context) { counts.ambiguous++; return }
      pageRequested = true
    }
    if (method === 'Playwright.pageProxyCreated' || (method === 'Playwright.createPage' && isResponse && !message.error)) {
      const candidate = isResponse ? message.result?.pageProxyId : params.pageProxyId
      if (!pageRequested || (!isResponse && params.browserContextId !== context) || !opaqueOK(candidate)) { counts.ignored++; return }
      if (proxy && proxy !== candidate) { counts.ambiguous++; return }
      proxy = candidate
    }
    if (method === 'Playwright.pageProxyDestroyed' && params.pageProxyId !== proxy) { counts.ignored++; return }
    if (method.startsWith('Target.') && !proxy) { counts.ignored++; return }
    const event = { seq: events.length + 1, at, direction, scope, method, kind: isResponse ? 'response' : idOK(message.id) ? 'request' : 'event' }
    if (idOK(message.id)) event.id = message.id
    if (context) event.context = alias(context, 'context')
    if (proxy) event.proxy = alias(proxy, 'proxy')
    if (target) event.target = alias(target, 'target')
    if (isResponse) event.error = Object.hasOwn(message, 'error')
    const info = method === 'Target.targetCreated' ? params.targetInfo : params
    if (record(info) && method.startsWith('Target.')) {
      if (opaqueOK(info.targetId)) event.target = alias(info.targetId, 'target')
      if (['page','frame','worker','service-worker'].includes(info.type)) event.targetType = info.type
      for (const flag of ['isPaused','isProvisional','crashed']) if (typeof info[flag] === 'boolean') event[flag] = info[flag]
    }
    const frame = method === 'Page.getResourceTree' && isResponse ? message.result?.frameTree?.frame : method === 'Page.frameNavigated' ? params.frame : undefined
    if (record(frame)) {
      if (opaqueOK(frame.id)) event.frame = alias(frame.id, 'frame')
      if (typeof frame.url === 'string') event.urlKind = urlKind(frame.url)
    }
    if (events.length >= limit) { counts.dropped++; return }
    events.push(event)
    if (direction === 'send' && idOK(message.id)) {
      if (pending.has(key)) counts.ambiguous++
      pending.set(key, event)
    }
    if (['Target.sendMessageToTarget','Target.dispatchMessageFromTarget'].includes(method) && !isResponse) {
      if (!opaqueOK(params.targetId) || typeof params.message !== 'string') { counts.malformed++; return }
      try { processMessage(direction, JSON.parse(params.message), at, params.targetId) } catch { counts.malformed++ }
    }
  }
  for (const line of lines) {
    bytes += Buffer.byteLength(line)
    if (bytes > byteLimit || events.length >= limit) { counts.dropped++; scopeEnd = 'limit'; break }
    if (Buffer.byteLength(line) > lineLimit) { counts.oversize++; continue }
    if (!line.endsWith('\n')) { counts.truncated++; continue }
    const lineTime = Date.parse(line.slice(0, 24))
    if (end && Number.isFinite(lineTime) && lineTime > end.at) { scopeEnd = 'page-create-settled'; break }
    const launched = /^\d{4}-\d{2}-\d{2}T[^ ]+ pw:browser <launched> pid=(\d{1,10})\n$/.exec(line)
    const exited = /^\d{4}-\d{2}-\d{2}T[^ ]+ pw:browser \[pid=(\d{1,10})\] <process did exit: exitCode=(null|\d{1,3}), signal=(null|SIGTERM|SIGKILL|SIGSEGV|SIGABRT)>\n$/.exec(line)
    if (launched || exited) {
      if (native.launchedPids.length + native.exits.length >= 16) counts.dropped++
      else if (launched) native.launchedPids.push(Number(launched[1]))
      else native.exits.push({ pid: Number(exited[1]), exitCode: exited[2] === 'null' ? null : Number(exited[2]), signal: exited[3] === 'null' ? null : exited[3] })
      continue
    }
    const match = /^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z) pw:protocol (SEND ►|◀ RECV) (.*)\n$/.exec(line)
    if (!match) { if (line.includes('pw:protocol')) counts.malformed++; else counts.ignored++; continue }
    const at = Date.parse(match[1])
    if (!Number.isFinite(at)) { counts.malformed++; continue }
    if (end && at > end.at) { scopeEnd = 'page-create-settled'; break }
    let message
    try { message = JSON.parse(match[3]) } catch { counts.malformed++; continue }
    if (pageRequested && proxy && message?.method === 'Playwright.navigate' && message.params?.pageProxyId === proxy) { scopeEnd = 'first-navigation'; break }
    processMessage(match[2] === 'SEND ►' ? 'send' : 'receive', message, at)
  }
  const pageCreation = pageRequested ? end ? end.state === 'success' ? 'returned' : 'threw' : 'pending' : 'not-observed'
  return { version: 1, pageCreation, scopeEnd, native, incomplete: !pageRequested || !end || pending.size > 0 || Object.entries(counts).some(([key,n]) => !['ignored'].includes(key) && n > 0), counts, events, pending: [...pending.values()].map(({method,id,scope,target,proxy}) => ({method,id,scope,...(target ? {target} : {}),...(proxy ? {proxy} : {})})) }
}
