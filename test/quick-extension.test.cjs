const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const SG=require('../dist/core.js'),Capture=require('../dist/capture-store.js');
const path=require('node:path');
function worker(){
 const events={},db={},api={alarms:{create(){},onAlarm:{addListener(){}}},runtime:{id:'test-extension',onMessage:{addListener:fn=>events.message=fn},onInstalled:{addListener(){}}},action:{onClicked:{addListener:fn=>events.click=fn},setBadgeBackgroundColor:async()=>{},setBadgeText:async()=>{},setTitle:async()=>{}},tabs:{query:async()=>[]},storage:{local:{get:async key=>structuredClone({[key]:db[key]}),set:async values=>Object.assign(db,structuredClone(values))}},scripting:{executeScript:async options=>options.args?[]:[{result:{url:'https://www.bilibili.com/video/BV123?vd_source=a',title:'学习视频',notes:'选中的内容'}}]}};
 const ctx={chrome:api,importScripts(){},SG,console,getCloud:()=>({auth:{getSession:async()=>({data:{session:null}})}}),pushCloud:async()=>({signedIn:false})};vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../collector/background.js'),'utf8'),ctx);
 const sender={id:'test-extension',tab:{url:'https://jiangxixu.github.io/shiguang/'}};
 const message=data=>new Promise(resolve=>events.message(data,sender,resolve));
 return {events,db,message};
}
test('toolbar click queues durably without opening the website; duplicate clicks and ACK are safe',async()=>{
 const w=worker();await Promise.all([w.events.click({id:1}),w.events.click({id:1})]);
 const response=await w.message({type:'SHIGUANG_PENDING'});assert.equal(response.items.length,1);assert.equal(response.items[0].notes,'选中的内容');
 await w.message({type:'SHIGUANG_ACK',ids:['unknown']});assert.equal((await w.message({type:'SHIGUANG_PENDING'})).items.length,1);
 await w.message({type:'SHIGUANG_ACK',ids:response.items.map(x=>x.id)});assert.equal((await w.message({type:'SHIGUANG_PENDING'})).items.length,0);
});
test('opening library drains to site storage; full storage retains queued capture for retry',async()=>{
 const w=worker();await w.events.click({id:1});let raw=null,full=true;const uiEvents=[];
 const localStorage={getItem:()=>raw,setItem:(key,value)=>{if(full)throw Error('quota');raw=value}};
 const page={chrome:{runtime:{sendMessage:w.message,onMessage:{addListener(){}}}},localStorage,ShiguangCapture:Capture,window:{dispatchEvent:event=>uiEvents.push(event),addEventListener(){}},StorageEvent:function(type,props){Object.assign(this,props)},console:{warn(){}},document:{createElement:()=>({style:{},remove(){}}),body:{append(){}}},setTimeout(){},setInterval(){}};
 const source=fs.readFileSync(path.join(__dirname,'../collector/library-bridge.js'),'utf8');
 vm.createContext(page);vm.runInContext(source,page);
 // Await the actual drain instead of sleeping; it shares a single in-flight guard.
 await new Promise(resolve=>setImmediate(resolve));
 assert.equal((await w.message({type:'SHIGUANG_PENDING'})).items.length,1);assert.equal(raw,null);
 full=false;await vm.runInContext('drain()',page);
 assert.equal(JSON.parse(raw).length,1);assert.equal((await w.message({type:'SHIGUANG_PENDING'})).items.length,0);assert.equal(uiEvents.length,1);
});
