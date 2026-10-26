const {test}=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs');
const SG=require('../dist/core.js'),Capture=require('../dist/capture-store.js'),crypto=require('node:crypto');
const KEY=Capture.KEY,OWNER='shiguang.cloud.owner';
function storage(){const map=new Map();return {get length(){return map.size;},key:i=>[...map.keys()][i],getItem:k=>map.get(k)??null,setItem:(k,v)=>map.set(k,v),removeItem:k=>map.delete(k)};}
function boot(localStorage,{preview=async()=>({title:'Original title',thumbnail:'https://example.com/original.jpg'}),sync=async()=>({signedIn:true,pending:0}),native}={}){
 const page={SG,crypto,localStorage,URL,AbortSignal,setTimeout,clearTimeout,location:{pathname:'/assets/quick-share.html'},addEventListener(){},ShiguangBackground:native,ShiguangBackgroundRunner:!native,ShiguangCloud:{ready:Promise.resolve(),sync},window:null};page.window=page;
 vm.createContext(page);vm.runInContext(fs.readFileSync('dist/preview-client.js','utf8'),page);page.ShiguangPreview.fetch=preview;
 vm.runInContext(fs.readFileSync('dist/background-capture.js','utf8'),page);return page;
}
async function capture(store,item,owner='a'){
 if(owner!==null)store.setItem(OWNER,owner);
 Capture.save(store,[item]);
 const native={postMessage(raw){queueMicrotask(()=>this.onmessage({data:JSON.stringify({id:JSON.parse(raw).id,ok:true})}));}};
 const page=boot(store,{native});await page.ShiguangBackgroundCapture.enqueue(item,owner);return page;
}
test('native save waits for durable scheduler acknowledgement, not metadata or network',async()=>{
 const store=storage(),reports=[];let acknowledge;
 const native={postMessage(raw){acknowledge=()=>this.onmessage({data:JSON.stringify({id:JSON.parse(raw).id,ok:true})});}};
 const page=boot(store,{native,preview:()=>new Promise(()=>{}),sync:()=>{throw Error('must not sync in editor');}});
 Object.assign(page,{ShiguangCapture:Capture,ShiguangQuickResult:{postMessage:raw=>reports.push(JSON.parse(raw))},document:{getElementById:()=>({})}});
 vm.runInContext(fs.readFileSync('dist/share-form.js','utf8'),page);
 page.shiguangReceiveShare({url:'https://v.douyin.com/example/',title:'v.douyin.com'});
 await page.ShiguangCaptureTransport.sendMessage({type:'SHIGUANG_CAPTURE_GET'});
 const done=page.ShiguangCaptureTransport.sendMessage({type:'SHIGUANG_CAPTURE_SAVE',tags:'学习',reason:'稍后复习'});
 await new Promise(setImmediate);assert.equal(reports.length,0);assert.equal(JSON.parse(store.getItem(KEY))[0].notes,'稍后复习');assert.equal(page.ShiguangBackgroundCapture.jobs().length,1);
 acknowledge();await done;assert.equal(reports.length,1);assert.equal(reports[0].background,true);assert.equal(reports[0].saved,true);
});
test('fresh runner resumes durable jobs after editor/process loss, network failure is retryable',async()=>{
 const store=storage();await capture(store,{url:'https://v.douyin.com/example/',tags:['学习'],notes:'保留原因'});
 const failed=boot(store,{sync:async()=>{throw Error('offline');}});
 await assert.rejects(failed.ShiguangBackgroundCapture.drain(),/offline/);
 assert.equal(failed.ShiguangBackgroundCapture.jobs().length,1);
 const restarted=boot(store);await restarted.ShiguangBackgroundCapture.drain();
 assert.equal(restarted.ShiguangBackgroundCapture.jobs().length,0);
 const item=JSON.parse(store.getItem(KEY))[0];assert.equal(item.title,'Original title');assert.equal(item.thumbnail,'https://example.com/original.jpg');assert.deepEqual(item.tags,['学习']);assert.equal(item.notes,'保留原因');
});
for(const scenario of ['delete','recreate','edit','account'])test('independent background runner respects '+scenario,async()=>{
 const store=storage();await capture(store,{url:'https://v.douyin.com/example/'});
 let reply;const page=boot(store,{preview:()=>new Promise(resolve=>reply=resolve)}),work=page.ShiguangBackgroundCapture.drain();await new Promise(setImmediate);
 if(scenario==='delete')store.setItem(KEY,'[]');
 if(scenario==='recreate'){store.setItem(KEY,'[]');Capture.save(store,[{url:'https://v.douyin.com/example/',title:'New capture'}]);}
 if(scenario==='edit'){const items=JSON.parse(store.getItem(KEY));items[0].title='My title';items[0].notes='My reason';store.setItem(KEY,JSON.stringify(items));}
 if(scenario==='account'){store.setItem(OWNER,'b');store.setItem(KEY,'[]');}
 reply({title:'Fetched title',thumbnail:'https://example.com/cover.jpg'});await work;
 const items=JSON.parse(store.getItem(KEY));
 if(scenario==='edit'){assert.equal(items[0].title,'My title');assert.equal(items[0].notes,'My reason');}
 else if(scenario==='recreate'){assert.equal(items[0].title,'New capture');assert.equal(items[0].thumbnail,'');}
 else assert.deepEqual(items,[]);
});
test('logged-out work stays local and guest work follows only the same imported record',async()=>{
 const store=storage();await capture(store,{url:'https://example.com/article'},null);
 const guest=boot(store,{sync:async()=>({signedIn:false})});await guest.ShiguangBackgroundCapture.drain();assert.equal(guest.ShiguangBackgroundCapture.jobs().length,1);
 store.setItem(OWNER,'first-account');const loggedIn=boot(store);await loggedIn.ShiguangBackgroundCapture.drain();assert.equal(loggedIn.ShiguangBackgroundCapture.jobs().length,0);
});
test('unavailable metadata is retained with backoff, and multiple captures remain independent',async()=>{
 const store=storage();await capture(store,{url:'https://v.douyin.com/one/'});await capture(store,{url:'https://v.douyin.com/two/'});
 let calls=0;const page=boot(store,{preview:async()=>{calls++;return {};}});await page.ShiguangBackgroundCapture.drain();assert.equal(calls,2);
 assert.equal(page.ShiguangBackgroundCapture.jobs().length,2);await page.ShiguangBackgroundCapture.drain();assert.equal(calls,2);
 assert.ok(page.ShiguangBackgroundCapture.jobs().every(job=>job.nextAt>Date.now()));
});
test('scheduler rejection retains local data and job for retry without reporting success',async()=>{
 const store=storage();Capture.save(store,[{url:'https://example.com/item'}]);
 const native={postMessage(raw){queueMicrotask(()=>this.onmessage({data:JSON.stringify({id:JSON.parse(raw).id,ok:false})}));}};
 const page=boot(store,{native});await assert.rejects(page.ShiguangBackgroundCapture.enqueue({url:'https://example.com/item'},null),/后台任务未能建立/);
 assert.equal(JSON.parse(store.getItem(KEY)).length,1);assert.equal(page.ShiguangBackgroundCapture.jobs().length,1);
});
