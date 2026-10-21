/* Copyright 2026 QRun.IO, Inc. Licensed under the Apache License, Version 2.0. */
import assert from 'node:assert/strict'
import { spawn, spawnSync } from 'node:child_process'
import { createHash } from 'node:crypto'
import { copyFileSync, existsSync, lstatSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import net from 'node:net'
import os from 'node:os'
import path from 'node:path'
import { createPhaseJournal } from './phase-journal.mjs'
import { setTimeout as delay } from 'node:timers/promises'
import { parseArgs } from 'node:util'

const { values: args } = parseArgs({ options: {
  'playwright-project': { type: 'string' }, output: { type: 'string' },
  version: { type: 'string', default: '4.1.0-RC.1' }, 'expected-sha': { type: 'string' },
  'rehearsal-source': { type: 'string' }, cold: { type: 'boolean', default: false },
  browser: { type: 'string', default: 'chromium' }, channel: { type: 'string' },
} })
assert(args.output && args['playwright-project'], '--output NEW_DIRECTORY and --playwright-project INSTALLED_PROJECT are required')
assert(/^\d+\.\d+\.\d+-RC\.\d+$/.test(args.version), 'Use an exact RC version')
assert(['chromium', 'firefox', 'webkit'].includes(args.browser), 'Unknown browser')
assert(!args.cold || !args['rehearsal-source'], 'A source rehearsal cannot be a cold public result')
if (!args['rehearsal-source']) assert(/^[0-9a-f]{40}$/.test(args['expected-sha'] ?? ''), 'Public run requires --expected-sha')
const output = path.resolve(args.output)
assert(!existsSync(output), 'Evidence directory must not already exist')
const project = path.join(output, 'editable app')
const { [args.browser]: browserType, expect } = createRequire(path.resolve(args['playwright-project'], 'package.json'))('@playwright/test')
const base = 'http://127.0.0.1:8000'
const report = { scope: 'quickstart-lifecycle', startedAt: new Date().toISOString(),
  version: args.version, provenance: args['rehearsal-source'] ? 'SOURCE REHEARSAL; not public acceptance' : 'public launcher and source tag',
  cold: args.cold, platform: `${os.platform()}/${os.arch()}`, browser: args.browser,
  complete: false, releaseAcceptanceComplete: false, phases: [], pageErrors: [],
  remainingAcceptance: ['full screen/action/browser/platform matrix', 'public Maven/BOM provenance', 'all required recovery cases', 'Material browser feature compatibility'] }
let journal
let app, browser, page, phaseName
let started, usableAt
let interrupted = false
for (const signal of ['SIGINT', 'SIGTERM']) process.on(signal, () => {
  interrupted = true
  app?.kill('SIGTERM')
})
const childErrors = []

function command(executable, arguments_, cwd = output) {
  const result = spawnSync(executable, arguments_, { cwd, encoding: 'utf8', timeout: 180_000 })
  assert.equal(result.status, 0, `${executable} failed: ${result.error?.message ?? result.stderr}`)
  return result.stdout.trim()
}
async function portOpen(port) {
  return new Promise((resolve) => {
    const socket = net.connect({ host: '127.0.0.1', port })
    const done = (open) => { socket.destroy(); resolve(open) }
    socket.once('connect', () => done(true)).once('error', () => done(false))
    socket.setTimeout(500, () => done(false))
  })
}
async function json(route) {
  const response = await fetch(base + route, { signal: AbortSignal.timeout(10_000) })
  assert.equal(response.status, 200, `${route}: ${response.status}`)
  return response.json()
}
function appLog(name) {
  if (existsSync(path.join(project, 'quickstart.log'))) copyFileSync(path.join(project, 'quickstart.log'), path.join(output, `${name}-application.log`))
}
function checkpoint() {
  journal?.checkpoint(phaseName ?? 'runner', path.join(output, 'lifecycle-partial.json'), report,
    () => appLog(phaseName ?? 'runner'))
}
function groupAlive(pid) {
  try { process.kill(-pid, 0); return true }
  catch (error) {
    if (error.code === 'ESRCH') return false
    if (error.code === 'EPERM') return true
    throw error
  }
}
async function stop(signal) {
  assert(app, 'No owned launcher')
  const owned = app
  let forced = false
  if (owned.exitCode === null && owned.signalCode === null) owned.kill(signal)
  const deadline = Date.now() + 20_000
  while (owned.exitCode === null && owned.signalCode === null && Date.now() < deadline) await delay(100)
  if (owned.exitCode === null && owned.signalCode === null) {
    forced = true
    process.kill(-owned.pid, 'SIGKILL')
  }
  const portsDeadline = Date.now() + 10_000
  while ((groupAlive(owned.pid) || await portOpen(8000) || await portOpen(61616)) && Date.now() < portsDeadline) await delay(100)
  const ownedResourcesRemain = groupAlive(owned.pid) || await portOpen(8000) || await portOpen(61616)
  // This group was created by this runner; never kill a process merely because it owns a port.
  if (ownedResourcesRemain) {
    try { process.kill(-owned.pid, 'SIGKILL') } catch (error) { if (error.code !== 'ESRCH') throw error }
  }
  app = undefined
  appLog(phaseName)
  assert(!forced && !ownedResourcesRemain, 'Launcher cleanup failed; owned process group required forced cleanup')
  assert.equal(childErrors.length, 0, childErrors.join('\n'))
}
async function launch(name, frontend = 'next', first = false, expectedCompileFailure = false) {
  assert(!interrupted, 'Runner interrupted')
  phaseName = name
  for (const port of [8000, 61616]) assert(!await portOpen(port), `Port ${port} is occupied`)
  const env = { ...process.env, QQQ_NO_BROWSER: 'true' }
  if (frontend === 'material') env.QQQ_FRONTEND = 'material'
  if (args['rehearsal-source']) env.QQQ_QUICKSTART_VERSION = '4.1.0-SNAPSHOT'
  const script = first ? path.join(output, 'quickstart.sh') : path.join(project, 'quickstart.sh')
  const args_ = ['bash', script, ...(first ? [project] : [])]
  started = performance.now()
  app = spawn(args_[0], args_.slice(1), { cwd: first ? output : project, env, detached: true, stdio: ['ignore', 'pipe', 'pipe'] })
  app.on('error', (error) => childErrors.push(error.message))
  let launcherLog = ''
  for (const stream of [app.stdout, app.stderr]) stream.on('data', (data) => {
    launcherLog += data.toString()
    writeFileSync(path.join(output, `${name}-launcher.log`), launcherLog)
  })
  const deadline = Date.now() + 360_000
  while (!launcherLog.includes('Ready in ')) {
    assert(!interrupted, 'Runner interrupted')
    if (expectedCompileFailure && app.exitCode !== null) {
      assert.notEqual(app.exitCode, 0, 'Invalid Java unexpectedly compiled')
      assert(readFileSync(path.join(project, 'quickstart.log'), 'utf8').includes('COMPILATION ERROR'), 'Expected a real Java compilation failure')
      return { name, frontend, ownedProcessGroup: app.pid, expectedCompileFailure: true, exitCode: app.exitCode }
    }
    assert(app.exitCode === null && app.signalCode === null, `Launcher exited before readiness (${name}); see logs`)
    assert.equal(childErrors.length, 0, childErrors.join('\n'))
    assert(Date.now() < deadline, `Launcher readiness timed out (${name})`)
    await delay(200)
  }
  assert(!expectedCompileFailure, 'Invalid Java unexpectedly reached readiness')
  return { name, frontend, ownedProcessGroup: app.pid, launcherReadySeconds: (performance.now() - started) / 1000 }
}
async function nextReady(phase) {
  const context = await journal.observe(phaseName ?? 'runner', 'context-create', () => browser.newContext({ viewport: { width: 1440, height: 1000 }, serviceWorkers: 'block' }))
  page = await journal.observe(phaseName ?? 'runner', 'page-create', () => context.newPage())
  const errors = []
  page.on('pageerror', (error) => { errors.push(error.message); report.pageErrors.push({ phase: phase.name, message: error.message }) })
  page.setDefaultTimeout(20_000)
  await journal.observe(phaseName ?? 'runner', 'page-goto', () => page.goto(base + '/app/person'))
  await expect(page.getByText('Avery', { exact: true }).first()).toBeVisible()
  usableAt = performance.now()
  phase.usableSeconds = (usableAt - started) / 1000
  assert.equal((await json('/qqq/v1/table/person/1')).record.values.firstName, 'Avery')
  const person = (await json('/qqq/v1/table/person/1?includeAssociations=true')).record
  assert.deepEqual(person.associatedRecords.pets.map((pet) => pet.values.id).sort(), [1, 2, 3, 4])
  await journal.observe(phaseName ?? 'runner', 'page-goto', () => page.goto(base + '/app/person/1'))
  await expect(page.getByRole('heading', { name: 'Avery Sample', exact: true }).first()).toBeVisible()
  await journal.observe(phaseName ?? 'runner', 'page-reload', () => page.reload())
  await expect(page.getByRole('heading', { name: 'Avery Sample', exact: true }).first()).toBeVisible()
  assert.deepEqual(errors, [])
  return context
}

try {
  for (const port of [8000, 61616]) assert(!await portOpen(port), `Port ${port} is occupied; stop the existing app yourself`)
  if (!args['rehearsal-source']) {
    for (const name of ['QQQ_FRONTEND', 'QQQ_QUICKSTART_REF', 'QQQ_QUICKSTART_VERSION', 'MAVEN_ARGS', 'MAVEN_OPTS', 'MAVEN_USER_HOME', 'MVNW_REPOURL', 'MVNW_USERNAME', 'MVNW_PASSWORD', 'JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS'])
      assert(!process.env[name], `Public acceptance disallows ${name} overrides`)
    if (args.cold) {
      for (const item of ['.m2', '.netrc', '.git-credentials']) assert(!existsSync(path.join(os.homedir(), item)), `Cold environment must not contain ${item}`)
    }
  }
  mkdirSync(output)
  journal = createPhaseJournal(path.join(output, 'phase-journal.json'), { enabled: process.env.QQQ_QUICKSTART_BOUNDARY_CAPTURE === '1' })
  journal.mark('runner', 'run', 'start')
  browser = await journal.observe('runner', 'browser-launch', () => browserType.launch(args.channel ? { channel: args.channel } : {}))
  report.browserVersion = browser.version()
  let firstPublic = !args['rehearsal-source']
  const coldStarted = performance.now()
  if (args['rehearsal-source']) {
    const source = path.resolve(args['rehearsal-source'])
    report.source = command('git', ['rev-parse', 'HEAD'], source)
    report.sourceDirty = Boolean(command('git', ['status', '--porcelain'], source))
    const files = command('git', ['ls-files', '-z', 'quickstart.sh', 'mvnw', 'pom.xml', '.mvn', 'checkstyle', 'pmd', 'qqq-sample-project'], source).split('\0').filter(Boolean)
    for (const file of files) {
      assert(lstatSync(path.join(source, file)).isFile(), `Expected regular tracked file: ${file}`)
      mkdirSync(path.dirname(path.join(project, file)), { recursive: true })
      copyFileSync(path.join(source, file), path.join(project, file))
    }
  } else {
    const url = `https://raw.githubusercontent.com/QRun-IO/qqq/quickstart-${args.version}/quickstart.sh`
    command('curl', ['-fsSL', '--max-time', '120', url, '-o', 'quickstart.sh'])
    report.launcherUrl = url
    report.launcherSha256 = createHash('sha256').update(readFileSync(path.join(output, 'quickstart.sh'))).digest('hex')
  }
  let phase = await journal.observe('initial', 'launcher-start', () => launch('initial', 'next', firstPublic))
  const initialContext = await journal.observe(phaseName ?? 'runner', 'next-ready', () => nextReady(phase))
  if (firstPublic) {
    report.source = command('git', ['rev-parse', 'HEAD'], project)
    assert.equal(report.source, args['expected-sha'])
    assert.equal(readFileSync(path.join(project, 'quickstart.sh'), 'utf8'), readFileSync(path.join(output, 'quickstart.sh'), 'utf8'))
    phase.totalDownloadToUsableSeconds = (usableAt - coldStarted) / 1000
    if (args.cold) assert(phase.totalDownloadToUsableSeconds <= 90, `Cold public quickstart took ${phase.totalDownloadToUsableSeconds.toFixed(3)} seconds; limit is 90`)
  }
  journal.mark('initial', 'crud', 'start')
  await journal.observe(phaseName ?? 'runner', 'page-goto', () => page.goto(base + '/app/person/create'))
  for (const [field, value] of Object.entries({ firstName: 'Lifecycle', lastName: 'Verification', email: 'lifecycle@example.invalid' })) await page.locator(`#field-${field}`).fill(value)
  await page.getByRole('button', { name: 'Save', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Lifecycle Verification', exact: true }).first()).toBeVisible()
  const id = new URL(page.url()).pathname.split('/').filter(Boolean).at(-1)
  assert(/^\d+$/.test(id), 'Created record ID missing')
  await journal.observe(phaseName ?? 'runner', 'page-goto', () => page.goto(`${base}/app/person/${id}/edit`))
  await page.locator('#field-firstName').fill('Lifecycle Updated')
  await page.getByRole('button', { name: 'Save', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Lifecycle Updated Verification', exact: true }).first()).toBeVisible()
  await journal.observe(phaseName ?? 'runner', 'page-reload', () => page.reload())
  assert.equal((await json(`/qqq/v1/table/person/${id}`)).record.values.firstName, 'Lifecycle Updated')
  phase.createdRecord = id
  journal.mark('initial', 'crud', 'success')
  await journal.observe(phaseName ?? 'runner', 'screenshot', () => page.screenshot({ path: path.join(output, 'created-edited.png'), fullPage: true }))
  journal.mark('initial', 'process', 'start')
  await page.getByRole('button', { name: 'Record actions menu', exact: true }).click()
  await page.getByRole('menuitem', { name: 'Greet Interactive', exact: true }).click()
  await page.getByRole('textbox', { name: /Greeting Prefix/ }).fill('Hello')
  await page.getByRole('textbox', { name: /Greeting Suffix/ }).fill('QQQ')
  await page.getByRole('button', { name: 'Submit', exact: true }).click()
  await expect(page.getByRole('cell', { name: 'Hello Lifecycle Updated QQQ', exact: true })).toBeVisible()
  await expect(page.locator('[data-qqq-id="process-record-list-range"]')).toHaveText('1–1 of 1')
  await journal.observe(phaseName ?? 'runner', 'screenshot', () => page.screenshot({ path: path.join(output, 'next-process-results.png'), fullPage: true }))
  await page.getByRole('button', { name: 'Return', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Lifecycle Updated Verification', exact: true }).first()).toBeVisible()
  await expect(page).toHaveURL(new RegExp(`/app/person/${id}/?$`))
  phase.greetingProcess = 'Exactly one selected record reached greeting result and returned to record'
  journal.mark('initial', 'process', 'success')
  checkpoint()
  await journal.observe(phaseName ?? 'runner', 'context-close', () => initialContext.close())
  checkpoint()
  await journal.observe(phaseName ?? 'runner', 'app-stop', () => stop('SIGINT'))
  checkpoint()
  report.phases.push(phase)
  phase = await journal.observe('restart', 'launcher-start', () => launch('restart'))
  const restartContext = await journal.observe(phaseName ?? 'runner', 'next-ready', () => nextReady(phase))
  const reset = await fetch(`${base}/qqq/v1/table/person/${id}`, { signal: AbortSignal.timeout(10_000) })
  assert.equal(reset.status, 404, 'Temporary record survived restart, contradicting documented reset')
  checkpoint()
  await journal.observe(phaseName ?? 'runner', 'context-close', () => restartContext.close())
  checkpoint()
  await journal.observe(phaseName ?? 'runner', 'app-stop', () => stop('SIGTERM'))
  checkpoint()
  report.phases.push(phase)
  const javaFile = path.join(project, 'qqq-sample-project/src/main/java/com/kingsrook/sampleapp/metadata/SampleMetaDataProvider.java')
  const before = readFileSync(javaFile, 'utf8')
  assert.equal(before.split('.withAppName("QQQ Sample")').length, 2, 'Java edit anchor must match exactly once')
  const brokenSource = before.replace('.withAppName("QQQ Sample")', '.withAppName(QQQ_ACCEPTANCE_UNDEFINED_APP_NAME)')
  writeFileSync(javaFile, brokenSource)
  phase = await journal.observe('compile-failure', 'launcher-start', () => launch('compile-failure', 'next', false, true))
  checkpoint()
  await journal.observe(phaseName ?? 'runner', 'app-stop', () => stop('SIGTERM'))
  checkpoint()
  assert.equal(readFileSync(javaFile, 'utf8'), brokenSource, 'Failed compilation discarded the local edit')
  report.phases.push(phase)
  const edited = before.replace('.withAppName("QQQ Sample")', '.withAppName("QQQ Lifecycle Edit")')
  writeFileSync(javaFile, edited)
  phase = await journal.observe('java-edit', 'launcher-start', () => launch('java-edit'))
  const editedContext = await journal.observe(phaseName ?? 'runner', 'next-ready', () => nextReady(phase))
  assert.equal((await json('/qqq/v1/metaData')).branding.appName, 'QQQ Lifecycle Edit')
  await expect(page.getByRole('img', { name: 'QQQ Lifecycle Edit', exact: true })).toBeVisible()
  checkpoint()
  await journal.observe(phaseName ?? 'runner', 'context-close', () => editedContext.close())
  checkpoint()
  await journal.observe(phaseName ?? 'runner', 'app-stop', () => stop('SIGTERM'))
  checkpoint()
  assert.equal(readFileSync(javaFile, 'utf8'), edited, 'Restart discarded local Java edits')
  report.phases.push(phase)
  phase = await journal.observe('material', 'launcher-start', () => launch('material', 'material'))
  journal.mark('material', 'material-checks', 'start')
  const html = await (await fetch(base, { signal: AbortSignal.timeout(10_000) })).text()
  assert(html.includes('/static/js/'), 'Material assets were not selected')
  assert(!html.includes('/_next/static/'), 'Next served despite Material selection')
  assert.equal((await json('/qqq/v1/table/person/1')).record.values.firstName, 'Avery')
  const materialContext = await journal.observe(phaseName ?? 'runner', 'context-create', () => browser.newContext({ viewport: { width: 1440, height: 1000 }, serviceWorkers: 'block' }))
  page = await journal.observe(phaseName ?? 'runner', 'page-create', () => materialContext.newPage())
  page.setDefaultTimeout(20_000)
  page.on('pageerror', (error) => report.pageErrors.push({ phase: 'material', message: error.message }))
  const materialPerson = base + '/peopleApp/greetingsApp/person'
  await journal.observe(phaseName ?? 'runner', 'page-goto', () => page.goto(materialPerson))
  await expect(page.getByText('Avery', { exact: true }).first()).toBeVisible()
  await journal.observe(phaseName ?? 'runner', 'page-goto', () => page.goto(base + '/peopleApp/greetingsApp/pet/1'))
  await expect(page.getByText('Viewing Pet: Charlie', { exact: true })).toBeVisible()
  await page.getByRole('link', { name: 'Avery Sample', exact: true }).click()
  await expect(page.getByText('Viewing Person: Avery Sample', { exact: true })).toBeVisible()
  await journal.observe(phaseName ?? 'runner', 'page-reload', () => page.reload())
  await expect(page.getByText('Viewing Person: Avery Sample', { exact: true })).toBeVisible()
  await journal.observe(phaseName ?? 'runner', 'page-goto', () => page.goto(materialPerson + '/create'))
  for (const [field, value] of Object.entries({ firstName: 'Material', lastName: 'Verification', email: 'material@example.invalid' }))
    await page.locator(`input[name="${field}"]`).fill(value)
  await page.getByRole('button', { name: /Save$/i }).click()
  await expect(page.getByText('Viewing Person: Material Verification', { exact: true })).toBeVisible()
  const materialId = new URL(page.url()).pathname.split('/').filter(Boolean).at(-1)
  assert(/^\d+$/.test(materialId), 'Material created record ID missing')
  await page.getByRole('button', { name: /Edit$/i }).click()
  await page.locator('input[name="firstName"]').fill('Material Updated')
  await page.getByRole('button', { name: /Save$/i }).click()
  await expect(page.getByText('Viewing Person: Material Updated Verification', { exact: true })).toBeVisible()
  await journal.observe(phaseName ?? 'runner', 'page-reload', () => page.reload())
  await expect(page.getByText('Viewing Person: Material Updated Verification', { exact: true })).toBeVisible()
  assert.equal((await json(`/qqq/v1/table/person/${materialId}`)).record.values.firstName, 'Material Updated')
  await page.getByRole('button', { name: /actions/i }).click()
  await page.getByRole('menuitem', { name: /Greet Interactive/ }).click()
  await page.locator('input[name="greetingPrefix"]').fill('Hello')
  await page.locator('input[name="greetingSuffix"]').fill('QQQ')
  await page.getByRole('button', { name: /Submit$/i }).click()
  await expect(page.getByText('Hello Material Updated QQQ', { exact: true })).toBeVisible()
  await expect(page.getByText('1–1 of 1', { exact: true })).toBeVisible()
  await journal.observe(phaseName ?? 'runner', 'screenshot', () => page.screenshot({ path: path.join(output, 'material-process-results.png'), fullPage: true }))
  await page.getByRole('button', { name: /Close$/i }).click()
  await expect(page.getByText('Viewing Person: Material Updated Verification', { exact: true })).toBeVisible()
  await expect(page).toHaveURL(`${materialPerson}/${materialId}`)
  phase.createdRecord = materialId
  phase.browserChecks = ['seeded query', 'Pet-to-Person link and refresh', 'create/edit/HTTP readback', 'selected-record greeting result and close']
  journal.mark('material', 'material-checks', 'success')
  checkpoint()
  await journal.observe(phaseName ?? 'runner', 'context-close', () => materialContext.close())
  checkpoint()
  await journal.observe(phaseName ?? 'runner', 'app-stop', () => stop('SIGTERM'))
  checkpoint()
  report.phases.push(phase)
  assert.deepEqual(report.pageErrors, [], 'Browser reported unhandled JavaScript errors')
  report.complete = true
} catch (error) {
  report.error = error.stack
  process.exitCode = 1
  checkpoint()
  if (page && !page.isClosed()) await journal.observe(phaseName ?? 'runner', 'failure-screenshot', () => page.screenshot({ path: path.join(output, 'failure.png'), fullPage: true })).catch(() => {})
} finally {
  checkpoint()
  if (app) await journal.observe(phaseName ?? 'runner', 'app-stop', () => stop('SIGTERM')).catch((error) => { report.cleanupError = error.message; report.complete = false; process.exitCode = 1 })
  checkpoint()
  if (browser) await journal.observe(phaseName ?? 'runner', 'browser-close', () => browser.close())
  checkpoint()
  journal?.mark('runner', 'run', report.complete ? 'success' : 'error')
  report.finishedAt = new Date().toISOString()
  if (existsSync(output)) writeFileSync(path.join(output, 'lifecycle-result.json'), JSON.stringify(report, null, 2) + '\n')
  console.log(JSON.stringify(report, null, 2))
}
