/* Copyright 2026 QRun.IO, Inc. Licensed under the Apache License, Version 2.0. */
import test from 'node:test'
import assert from 'node:assert/strict'
import { mkdtempSync, writeFileSync, readFileSync, statSync, rmSync, symlinkSync, chmodSync, truncateSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { createHash } from 'node:crypto'
import { spawnSync } from 'node:child_process'
import { summarizeFiles } from './protocol-files.mjs'
const raw = '2026-10-02T11:37:12.000Z pw:protocol SEND ► {"id":1,"method":"Playwright.createContext"}\n'
function files(t) {const dir=mkdtempSync(join(tmpdir(),'protocol-contract-'));t.after(()=>rmSync(dir,{recursive:true,force:true}));const input=join(dir,'raw.private'),journal=join(dir,'journal.json');writeFileSync(input,raw,{mode:0o600});writeFileSync(journal,JSON.stringify({entries:[{seq:1,at:1790941032000,phase:'initial',operation:'page-create',state:'start'},{seq:2,at:1790941032010,phase:'initial',operation:'page-create',state:'success'}]}),{mode:0o600});return{dir,input,journal}}
test('bounded file adapter preserves bytes and hashes input; no input fields leak',t=>{const f=files(t);const result=summarizeFiles(f.input,f.journal);assert.equal(result.rawSHA256,createHash('sha256').update(raw).digest('hex'));assert.equal(result.rawBytes,Buffer.byteLength(raw));assert.equal(readFileSync(f.input,'utf8'),raw);assert.equal(result.journalAvailable,true);assert.equal(result.summary.events[0].method,'Playwright.createContext')})
test('unavailable/private-mode/symlink inputs yield fixed failure without arbitrary error/path',t=>{const f=files(t);assert.deepEqual(summarizeFiles(join(f.dir,'SECRET'),f.journal),{version:1,available:false,reason:'raw-unavailable'});const link=join(f.dir,'link');symlinkSync(f.input,link);assert.equal(summarizeFiles(link,f.journal).available,false);chmodSync(f.input,0o644);assert.equal(summarizeFiles(f.input,f.journal).available,false);})
test('missing or malformed journal makes settlement unknown without arbitrary contents',t=>{const f=files(t);writeFileSync(f.journal,'SECRET');const r=summarizeFiles(f.input,f.journal);assert.equal(r.journalAvailable,false);assert.equal(r.summary.incomplete,true);assert.equal(JSON.stringify(r).includes('SECRET'),false)})
test('CLI keeps raw data out of both stdout and stderr and writes private summary',t=>{const f=files(t),output=join(f.dir,'summary.json');const child=spawnSync(process.execPath,['protocol-files.mjs',f.input,f.journal,output],{cwd:new URL('.',import.meta.url),encoding:'utf8'});assert.equal(child.status,0);assert.equal(child.stdout,'');assert.equal(child.stderr,'');assert.equal(statSync(output).mode&0o777,0o600);assert.equal(JSON.parse(readFileSync(output)).available,true)})

test('oversized raw file is refused without loading or changing it',t=>{const f=files(t);truncateSync(f.input,67_108_865);const result=summarizeFiles(f.input,f.journal);assert.deepEqual(result,{version:1,available:false,reason:'raw-unavailable'});assert.equal(statSync(f.input).size,67_108_865)})
test('CLI persistence failure is fixed text with nonzero exit and input unchanged',t=>{const f=files(t);const child=spawnSync(process.execPath,['protocol-files.mjs',f.input,f.journal,join(f.dir,'SECRET','out')],{cwd:new URL('.',import.meta.url),encoding:'utf8'});assert.equal(child.status,1);assert.equal(child.stdout,'');assert.equal(child.stderr,'protocol-summary-unavailable\n');assert.equal(readFileSync(f.input,'utf8'),raw)})
