const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
function client(storage,send){
 const c={localStorage:{getItem:k=>storage[k]??null,setItem:(k,v)=>storage[k]=v},ShiguangCloudConfig:{url:'https://example.supabase.co',key:'public'},ShiguangSupabase:{createClient:()=>({auth:{getSession:async()=>({data:{session:null}}),onAuthStateChange(){},signInWithOtp:send}})},ShiguangSyncModel:{},navigator:{},location:{pathname:'/quick-share.html'},document:{},setTimeout(){},clearTimeout(){},Event:function(){},CustomEvent:function(){},addEventListener(){},dispatchEvent(){}};
 c.window=c;vm.runInNewContext(fs.readFileSync('dist/cloud-client.js','utf8'),c);return c.ShiguangCloud;
}
test('mail requests are blocked across reopened clients and failed delivery without touching collections',async()=>{
 const storage={'shiguang.library.v1':'private collection'};let calls=0;
 const send=async()=>{calls++;return {error:{code:'over_email_send_rate_limit',message:'email rate limit exceeded'}};};
 const first=client(storage,send);await assert.rejects(first.requestCode('a@example.com'));
 assert.ok(first.mailRemaining()>0);
 const reopened=client(storage,send);await assert.rejects(reopened.requestCode('b@example.com'),/稍后/);
 assert.equal(calls,1);assert.equal(storage['shiguang.library.v1'],'private collection');
 storage['shiguang.auth.mail-next']=String(Date.now()-1);
 await assert.rejects(reopened.requestCode('a@example.com'));assert.equal(calls,2);
});
test('pending mail cannot be duplicated even after cooldown expires',async()=>{
 const storage={};let complete,calls=0;
 const cloud=client(storage,()=>{calls++;return new Promise(r=>complete=r);});
 const pending=cloud.requestCode('a@example.com');storage['shiguang.auth.mail-next']='0';
 await assert.rejects(cloud.requestCode('a@example.com'),/正在发送/);assert.equal(calls,1);complete({error:null});await pending;
});
test('mail limit feedback explains service quota and leaves received link login usable',async()=>{
 const elements={};const element=id=>elements[id]??={value:'a@example.com',disabled:false,textContent:'',reportValidity:()=>true};
 const c={window:{ShiguangCloud:{ready:new Promise(()=>{}),requestCode:async()=>{throw {code:'over_email_send_rate_limit',message:'email rate limit exceeded'};}},addEventListener(){}},document:{getElementById:element},esc:x=>x,openModal(){},setTimeout,clearTimeout};
 vm.runInNewContext(fs.readFileSync('dist/cloud-ui.js','utf8'),c);element('cloud-account').onclick();await element('cloud-send').onclick();
 assert.match(element('cloud-feedback').textContent,/本次未发送/);assert.match(element('cloud-feedback').textContent,/SMTP/);assert.equal(element('cloud-verify').disabled,false);
});
