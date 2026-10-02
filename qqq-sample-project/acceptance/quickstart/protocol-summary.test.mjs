/* Copyright 2026 QRun.IO, Inc. Licensed under the Apache License, Version 2.0. */
import test from 'node:test'
import assert from 'node:assert/strict'
import { summarizeProtocol } from './protocol-summary.mjs'
const line = (direction, message, time = '2026-10-02T11:37:12.000Z') => `${time} pw:protocol ${direction === 'send' ? 'SEND ►' : '◀ RECV'} ${JSON.stringify(message)}\n`
const setup = () => [
 line('send', {id: 1, method: 'Playwright.createContext'}),
 line('receive', {id: 1, result: {browserContextId: 'secret-context'}}),
 line('send', {id: 2, method: 'Playwright.createPage', params: {browserContextId: 'secret-context'}}),
 line('receive', {method: 'Playwright.pageProxyCreated', params: {browserContextId: 'secret-context', pageProxyId: 'secret-proxy'}}),
 line('receive', {id: 2, result: {pageProxyId: 'secret-proxy'}}),
]
const nested = (direction, id, targetId, message) => line(direction, { ...(direction === 'send' ? {id} : {}), pageProxyId: 'secret-proxy', method: direction === 'send' ? 'Target.sendMessageToTarget' : 'Target.dispatchMessageFromTarget', params: {targetId, message: JSON.stringify(message)}})
test('outer acknowledgement leaves inner command pending until its target response', () => {
 const input = [...setup(), nested('send', 8, 'secret-target', {id: 7, method: 'Page.enable'}), line('receive', {id: 8, pageProxyId: 'secret-proxy', result: {}})]
 const result = summarizeProtocol(input)
 assert.deepEqual(result.pending.map(x => [x.method,x.id,x.scope]), [['Page.enable',7,'target']])
 assert.equal(result.pageCreation, 'pending')
 assert.equal(JSON.stringify(result).includes('secret-'), false)
 const complete = summarizeProtocol([...input, nested('receive', undefined, 'secret-target', {id: 7, result: {}})])
 assert.deepEqual(complete.pending, [])
 assert.equal(complete.pageCreation, 'pending')
})
test('stops before any navigation payload and does not infer API return from createPage reply', () => {
 const result = summarizeProtocol([...setup(), line('send',{id:99,method:'Playwright.navigate',params:{pageProxyId:'secret-proxy',url:'SECRET_URL'}}), nested('send',90,'late',{id:91,method:'Page.enable'})])
 assert.equal(result.scopeEnd,'first-navigation'); assert.equal(result.pageCreation,'pending'); assert.equal(result.events.length,5)
 assert.equal(JSON.stringify(result).includes('SECRET'),false)
})
test('journal settlement cuts later messages and preserves error presence without error data', () => {
 const at = Date.parse('2026-10-02T11:37:12.000Z')
 const input=[...setup(),nested('send',8,'t',{id:7,method:'Page.enable'}), nested('receive',undefined,'t',{id:7,error:{message:'SECRET_ERROR',data:{token:'SECRET'}}}),line('send',{id:20,method:'Playwright.createContext'},'2026-10-02T11:37:12.001Z')]
 const result=summarizeProtocol(input,{pageCreateEnd:{at,state:'error'}})
 assert.equal(result.scopeEnd,'page-create-settled');assert.equal(result.pageCreation,'threw')
 assert.equal(result.events.find(e=>e.id===7&&e.kind==='response').error,true)
 assert.equal(JSON.stringify(result).includes('SECRET'),false)
})
test('normalizes IDs and captures paused/frame/blank metadata only', () => {
 const result=summarizeProtocol([...setup(),line('receive',{pageProxyId:'secret-proxy',method:'Target.targetCreated',params:{targetInfo:{targetId:'secret-frame',type:'frame',isPaused:true,isProvisional:false,url:'SECRET'}}}),nested('send',8,'secret-frame',{id:7,method:'Page.getResourceTree'}),nested('receive',undefined,'secret-frame',{id:7,result:{frameTree:{frame:{id:'secret-frame-id',url:'about:blank',securityOrigin:'SECRET'},resources:[{url:'SECRET'}]}}})])
 const created=result.events.find(e=>e.method==='Target.targetCreated');assert.equal(created.isPaused,true);assert.equal(created.isProvisional,false);assert.equal(created.targetType,'frame')
 assert.equal(result.events.at(-1).urlKind,'about-blank');assert.match(result.events.at(-1).frame,/^frame-\d+$/)
 assert.equal(JSON.stringify(result).includes('secret'),false);assert.equal(JSON.stringify(result).includes('SECRET'),false)
})
test('empty and private URL are reduced to fixed kinds', () => {
 const result=summarizeProtocol([...setup(),...['','https://user:SECRET@example.invalid/private?token=SECRET#SECRET'].map(url=>nested('receive',undefined,'t',{method:'Page.frameNavigated',params:{frame:{id:'f',url}}}))])
 assert.deepEqual(result.events.filter(e=>e.method==='Page.frameNavigated').map(e=>e.urlKind),['empty','other'])
 assert.equal(JSON.stringify(result).includes('SECRET'),false)
})
test('same numeric command ID in different targets has independent completion', () => {
 const input=[...setup(),nested('send',8,'a',{id:7,method:'Page.enable'}),nested('send',10,'b',{id:7,method:'Console.enable'}),nested('receive',undefined,'b',{id:7,result:{}})]
 const result=summarizeProtocol(input);assert.equal(result.pending.filter(e=>e.scope==='target').length,1);assert.equal(result.pending.find(e=>e.scope==='target').method,'Page.enable')
})
test('other proxy traffic cannot settle first proxy request', () => {
 const foreign=line('receive',{pageProxyId:'other',method:'Target.dispatchMessageFromTarget',params:{targetId:'t',message:JSON.stringify({id:7,result:{}})}})
 const result=summarizeProtocol([...setup(),nested('send',8,'t',{id:7,method:'Page.enable'}),foreign])
 assert.equal(result.pending.some(e=>e.id===7),true)
})
test('secrets in every raw field stay out, including IDs, unknown fields and methods', () => {
 const marker='SECRET_MARKER'
 const input=[...setup(),nested('send',8,marker,{id:7,method:'Network.setExtraHTTPHeaders',params:{headers:{Authorization:marker},cookies:[marker],source:marker,body:marker,url:marker}}),line('receive',{method:marker,params:{message:marker}}),nested('receive',undefined,marker,{id:7,result:{message:marker,headers:marker}})]
 assert.equal(JSON.stringify(summarizeProtocol(input)).includes(marker),false)
})
test('malformed, producer truncation and partial tail explicitly make evidence incomplete', () => {
 const result=summarizeProtocol([...setup(), '2026-10-02T11:37:12.000Z pw:protocol SEND ► { <<<<<( LOG TRUNCATED )>>>>> }\n',line('receive',{method:'Page.enable'}).trimEnd()],{pageCreateEnd:{at:Date.parse('2026-10-02T11:37:13.000Z'),state:'success'}})
 assert.equal(result.counts.malformed,1);assert.equal(result.counts.truncated,1);assert.equal(result.incomplete,true)
})
test('event, byte and line limits bound retention and mark incomplete', () => {
 for(const options of [{limit:2},{byteLimit:1},{lineLimit:1}]) { const result=summarizeProtocol(setup(),options);assert.equal(result.incomplete,true);assert.ok(result.events.length<=2) }
})
test('caller cannot disable hard caps with Infinity or negative options', () => {
 const input=[...setup(),...Array.from({length:2100},()=>nested('receive',undefined,'t',{method:'Page.frameNavigated',params:{frame:{id:'f',url:''}}}))]
 const result=summarizeProtocol(input,{limit:Infinity,lineLimit:Infinity,byteLimit:Infinity}); assert.ok(result.events.length<=2048);assert.equal(result.incomplete,true)
})
test('unexpected send method during initialization marks unsupported capture instead of complete',()=>{
 const result=summarizeProtocol([...setup(),nested('send',8,'t',{id:7,method:'Runtime.UNRECOGNIZED',params:{secret:'SECRET'}})],{pageCreateEnd:{at:Date.parse('2026-10-02T11:37:13.000Z'),state:'success'}})
 assert.equal(result.incomplete,true);assert.equal(result.counts.unrecognizedCommands,1);assert.equal(JSON.stringify(result).includes('UNRECOGNIZED'),false)
})

test('even valid pageCreation return does not equate pending acknowledgements to success',()=>{const result=summarizeProtocol([...setup(),nested('send',8,'t',{id:7,method:'Page.enable'})],{pageCreateEnd:{at:Date.parse('2026-10-02T11:37:13.000Z'),state:'success'}});assert.equal(result.pageCreation,'returned');assert.equal(result.incomplete,true);assert.equal(result.pending.some(e=>e.method==='Page.enable'),true)})
test('private native stream retains only fixed PID and exit metadata',()=>{
 const result=summarizeProtocol(['2026-10-02T11:37:11.000Z pw:browser <launching> SECRET\n','2026-10-02T11:37:11.001Z pw:browser <launched> pid=28815\n',...setup(),'2026-10-02T11:37:13.000Z pw:browser [pid=28815] <process did exit: exitCode=null, signal=SIGTERM>\n'])
 assert.deepEqual(result.native,{launchedPids:[28815],exits:[{pid:28815,exitCode:null,signal:'SIGTERM'}]});assert.equal(JSON.stringify(result).includes('SECRET'),false)
})
test('journal success without any protocol creation remains incomplete',()=>{const result=summarizeProtocol([],{pageCreateEnd:{at:1790941032000,state:'success'}});assert.equal(result.pageCreation,'not-observed');assert.equal(result.incomplete,true)})
test('another page navigation cannot truncate the selected page handshake',()=>{const result=summarizeProtocol([...setup(),line('send',{id:99,method:'Playwright.navigate',params:{pageProxyId:'other-proxy',url:'SECRET'}}),nested('send',8,'t',{id:7,method:'Page.enable'})]);assert.equal(result.scopeEnd,'input-ended');assert.equal(result.pending.some(e=>e.method==='Page.enable'),true)})
