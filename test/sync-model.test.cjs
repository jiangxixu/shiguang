const {test}=require('node:test'),assert=require('node:assert/strict');
const model=require('../dist/sync-model.js'),SG=require('../dist/core.js'),auth=require('../dist/auth-input.js');
const item=SG.normalize({url:'https://example.com/tutorial',id:'one',title:'教程',notes:'自己的笔记',read:true});
const empty=()=>({last:{},pending:{}});let count=0;const id=()=>String(++count);
test('cloud retry retains edits made while the previous upload is in flight',()=>{
 let state=model.observe(empty(),[item],id);const snapshot={...state.pending};
 state=model.observe(state,[{...item,notes:'新笔记'}],id);
 state=model.acknowledge(state,snapshot);assert.equal(Object.keys(state.pending).length,1);assert.equal(state.pending[item.url].payload.notes,'新笔记');
 state=model.acknowledge(state,{...state.pending});assert.equal(Object.keys(state.pending).length,0);
});
test('remote records merge without overwriting unsent local edits; tombstones prevent resurrection',()=>{
 const initial={last:{[item.url]:item},pending:{}};
 let dirty=model.observe(initial,[{...item,notes:'离线编辑'}],id);
 const merged=model.pull(dirty,[{...item,notes:'离线编辑'}],[{url:item.url,payload:{...item,notes:'远端旧笔记'},deleted:false}]);
 assert.equal(merged.items[0].notes,'离线编辑');
 const deleted=model.pull(initial,[item],[{url:item.url,payload:null,deleted:true}]);assert.equal(deleted.items.length,0);
 dirty=model.observe(initial,[],id);assert.equal(dirty.pending[item.url].kind,'delete');
});
test('duplicate capture uses canonical URLs and new operations remain new until acknowledged',()=>{
 let state=model.observe(empty(),[item],id);state=model.observe(state,[{...item,title:'改过的标题'}],id);assert.equal(state.pending[item.url].kind,'new');
 const rows=[{url:item.url,payload:item,deleted:false}];state=model.acknowledge(state,{...state.pending});const pulled=model.pull(state,[item],rows);assert.equal(model.observe(pulled.state,pulled.items,id).pending[item.url],undefined);
});
test('email links are accepted only for this project and supported sign-in types',()=>{
 const project='https://example.supabase.co';assert.equal(auth.parse(project+'/auth/v1/verify?token=abc&type=magiclink',project,'me@example.com').token_hash,'abc');
 assert.equal(auth.parse('123456',project,'me@example.com').email,'me@example.com');
 assert.throws(()=>auth.parse('https://evil.example/auth/v1/verify?token=abc&type=magiclink',project));assert.throws(()=>auth.parse(project+'/auth/v1/verify?token=abc&type=recovery',project));
});
