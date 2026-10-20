const {test}=require('node:test'),assert=require('node:assert/strict');
const capture=require('../dist/capture-store.js');
function storage(raw=null){return {raw,getItem(){return this.raw},setItem(k,v){this.raw=v}};}
test('quick capture persists metadata and repeated delivery preserves notes and read state',()=>{
 const s=storage();const input={url:'https://www.bilibili.com/video/BV123?vd_source=a',title:'学习内容',notes:'我的笔记',read:true};
 assert.equal(capture.save(s,[input]).added,1);
 const result=capture.save(s,[{...input,title:'新的标题',notes:'',read:false}]);
 assert.equal(result.added,0);assert.equal(result.duplicates,1);
 const saved=JSON.parse(s.raw)[0];assert.equal(saved.notes,'我的笔记');assert.equal(saved.read,true);
});
test('quick capture never overwrites malformed storage or acknowledges failed writes',()=>{
 const s=storage('{broken');assert.throws(()=>capture.save(s,[{url:'https://example.com'}]));assert.equal(s.raw,'{broken');
 const quota=storage();quota.setItem=()=>{throw Error('quota')};assert.throws(()=>capture.save(quota,[{url:'https://example.com'}]),/quota/);
});
test('invalid share URLs are rejected without a partial write',()=>{
 const s=storage();assert.throws(()=>capture.save(s,[{url:'https://example.com'},{url:'javascript:alert(1)'}]));assert.equal(s.raw,null);
});


test('recapturing Douyin enriches missing title and cover while preserving user fields',()=>{
 const s=storage();const url='https://v.douyin.com/abc/';capture.save(s,[{url,title:'v.douyin.com',tags:['Learning'],notes:'Keep',read:true}]);
 capture.save(s,[{url,title:'Real caption',thumbnail:'https://cdn.example.com/video.jpg',tags:['Other'],notes:''}]);
 const saved=JSON.parse(s.raw)[0];assert.equal(saved.title,'Real caption');assert.equal(saved.thumbnail,'https://cdn.example.com/video.jpg');assert.deepEqual(saved.tags,['Learning']);assert.equal(saved.notes,'Keep');assert.equal(saved.read,true);
 capture.save(s,[{url,title:'Replacement',thumbnail:'https://cdn.example.com/other.jpg'}]);assert.equal(JSON.parse(s.raw)[0].title,'Real caption');assert.equal(JSON.parse(s.raw)[0].thumbnail,'https://cdn.example.com/video.jpg');
});
