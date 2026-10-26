const {test}=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path');
const SG=require('../dist/core.js'),model=require('../dist/sync-model.js');
async function boot(values,uid,background=false){
 const data={...values};const storage={getItem:k=>data[k]??null,setItem:(k,v)=>{data[k]=v},removeItem:k=>{delete data[k]}};
 const session={user:{id:uid,email:'test@example.invalid'}};
 const context={ShiguangBackgroundRunner:background,SG,ShiguangCloudConfig:{url:'https://example.supabase.co',key:'public'},ShiguangSyncModel:model,ShiguangSupabase:{createClient:()=>({auth:{getSession:async()=>({data:{session}}),onAuthStateChange(){}}})},localStorage:storage,navigator:{},location:{pathname:'/quick-share.html'},document:{},setTimeout(){},clearTimeout(){},Event:function(){},CustomEvent:function(){}};
 context.window=context;context.dispatchEvent=()=>{};context.addEventListener=()=>{};
 vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../dist/cloud-client.js'),'utf8'),context);await context.ShiguangCloud.ready;if(background)await context.ShiguangCloud.sync();return data;
}
test('signing back in restores archived library instead of marking every cloud item deleted',async()=>{
 const stored=SG.normalize({url:'https://example.com/old',id:'old',title:'之前的收藏'}),guest=SG.normalize({url:'https://example.com/new',id:'new',title:'离线新收藏'});
 const data=await boot({'shiguang.library.v1':JSON.stringify([guest]),'shiguang.cloud.archive.owner-a':JSON.stringify([stored])},'owner-a');
 assert.equal(JSON.parse(data['shiguang.library.v1']).length,2);assert.equal(data['shiguang.cloud.owner'],'owner-a');
});
test('switching accounts archives the prior account and never copies its records to the next',async()=>{
 const stored=SG.normalize({url:'https://example.com/private',id:'private',title:'私有收藏'});
 const data=await boot({'shiguang.library.v1':JSON.stringify([stored]),'shiguang.cloud.owner':'owner-a'},'owner-b');
 assert.equal(JSON.parse(data['shiguang.library.v1']).length,0);assert.equal(JSON.parse(data['shiguang.cloud.archive.owner-a']).length,1);
});


test('background worker never switches the library to a different session identity',async()=>{
 const original=JSON.stringify([SG.normalize({url:'https://example.com/private',id:'a-private'})]);
 const data=await boot({'shiguang.library.v1':original,'shiguang.cloud.owner':'owner-a'},'owner-b',true);
 assert.equal(data['shiguang.library.v1'],original);assert.equal(data['shiguang.cloud.owner'],'owner-a');
 assert.equal(data['shiguang.cloud.archive.owner-a'],undefined);
});
