/* Copyright 2026 QRun.IO, Inc. Licensed under the Apache License, Version 2.0. */
import assert from 'node:assert/strict'
import { test } from 'node:test'
import { readFileSync } from 'node:fs'

const workflow = readFileSync(new URL('../../../.github/workflows/quickstart-lifecycle.yml', import.meta.url), 'utf8')
const runner = readFileSync(new URL('./run-public.mjs', import.meta.url), 'utf8')

test('diagnostic dispatch defaults off and selects one cell while normal matrix retains all six', () => {
  assert.match(workflow, /boundary_capture:\n\s+description:[^\n]+\n\s+required: false\n\s+type: boolean\n\s+default: false/)
  const expression = workflow.match(/matrix: \$\{\{ fromJSON\(inputs\.boundary_capture && '(.*?)' \|\| '(.*?)'\) \}\}/)
  assert(expression)
  const cells = value => JSON.parse(value).os.flatMap(os => JSON.parse(value).browser.map(browser => [os, browser]))
  assert.deepEqual(cells(expression[1]), [['macos-14', 'webkit']])
  assert.deepEqual(cells(expression[2]), ['ubuntu-24.04', 'macos-14'].flatMap(os => ['chromium', 'firefox', 'webkit'].map(browser => [os, browser])))
})

test('preflight and pure tests run only on diagnostic dispatch with fixed tools and unchanged runtime budgets', () => {
  const diagnostic = workflow.slice(workflow.indexOf('      - name: Diagnostic boundary preflight'), workflow.indexOf('      - name: Run the public launcher lifecycle'))
  assert.match(diagnostic, /if: \$\{\{ inputs\.boundary_capture \}\}/)
  assert.match(diagnostic, /EXPECTED_SHA: \$\{\{ inputs\.source_sha \}\}/)
  assert.match(diagnostic, /QQQ_QUICKSTART_PREFLIGHT_OUTPUT: \$\{\{ runner\.temp \}\}\/qqq-quickstart-diagnostic/)
  assert.match(diagnostic, /node --test qqq-sample-project\/acceptance\/quickstart\/\*\.test\.mjs/)
  assert.match(diagnostic, /boundary-preflight\.mjs/)
  for (const value of ["timeout-minutes: 20", "java-version: '21'", "node-version: '22'", 'pnpm@9.15.9', 'ref: 2853b9b148282ae0e6d454adab6d0f534ba48280']) assert(workflow.includes(value))
})

test('runner captures only exact flag1 and retains the public argument and output-directory contract', () => {
  assert.match(runner, /enabled: process\.env\.QQQ_QUICKSTART_BOUNDARY_CAPTURE === '1'/)
  assert(runner.includes("assert(!existsSync(output), 'Evidence directory must not already exist')"))
  assert(workflow.includes('EXPECTED_SHA="$(git rev-parse HEAD)"'))
  assert(workflow.includes('--version "$CANDIDATE_VERSION" --expected-sha "$EXPECTED_SHA"'))
  assert(workflow.includes('--browser "$BROWSER"'))
  assert(workflow.includes('if: always()'))
  for (const extension of ['json', 'log', 'png']) assert(workflow.includes('${{ runner.temp }}/qqq-public-lifecycle/*.' + extension))
})
