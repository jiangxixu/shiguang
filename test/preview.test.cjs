const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const SG=require('../dist/core.js'),Capture=require('../dist/capture-store.js');
test('thumbnail survives normalize, backup and cloud merge; unsafe images rejected',()=>{
 const input={url:'https://www.bilibili.com/video/BV123',thumbnail:'https://i0.hdslb.com/bfs/archive/test.jpg'};
 assert.equal(SG.normalize(input).thumbnail,input.thumbnail);
 assert.equal(SG.parseFile(JSON.stringify([input]),'backup.json')[0].thumbnail,input.thumbnail);
 for(const url of ['javascript:alert(1)','http://example.com/img.jpg','https://127.0.0.1/a','https://10.1.2.3/a','https://user:pass@example.com/a'])assert.equal(SG.safeImage(url),'');
 assert.equal(SG.thumbnailFor({url:'https://youtu.be/fNk_zzaMoSs'}),'https://i.ytimg.com/vi/fNk_zzaMoSs/hqdefault.jpg');
 assert.equal(SG.thumbnailFor({url:'https://fake-youtube.com/watch?v=fNk_zzaMoSs'}),'');
});
test('metadata parser accepts varied OG attribute order and rejects outbound private hosts and redirect escapes',async()=>{
 const {parseMeta,allowed,preview}=await import('../supabase/functions/link-preview/preview.mjs');
 const result=parseMeta(`<meta content='https://cdn.example.com/a.jpg?a=1&amp;b=2' property='og:image'><meta property="og:title" content="原视频 &amp; 标题">`,'https://linux.do/t/topic/123');
 assert.equal(result.thumbnail,'https://cdn.example.com/a.jpg?a=1&b=2');assert.equal(result.title,'原视频 & 标题');
 assert.equal(parseMeta('<title>无封面</title>','https://linux.do').thumbnail,'');
 for(const url of ['https://127.0.0.1','http://linux.do','https://linux.do.evil.com','https://user@linux.do','https://linux.do:1234'])assert.equal(allowed(url),false);
 let calls=0;await assert.rejects(preview('https://b23.tv/test',async()=>{calls++;return new Response('',{status:302,headers:{location:'http://169.254.169.254/latest'}});}),/unsupported_redirect/);assert.equal(calls,1);
 const data=await preview('https://www.bilibili.com/video/BV123',async url=>{assert.match(url,/api.bilibili.com/);return Response.json({code:0,data:{pic:'http://i0.hdslb.com/test.jpg',title:'原视频'}});});assert.equal(data.thumbnail,'https://i0.hdslb.com/test.jpg');
});
test('mobile share waits for confirmation and preserves categories, optional reason and cover',async()=>{
 let raw=JSON.stringify([{url:'https://example.com/existing',title:'已有',tags:['机器人']}]);const reports=[];
 const page={window:null,SG,ShiguangCapture:Capture,ShiguangCloud:{ready:Promise.resolve(),sync:async()=>({signedIn:true,pending:0})},ShiguangPreview:{fetch:async()=>({})},ShiguangQuickResult:{postMessage:text=>reports.push(JSON.parse(text))},localStorage:{getItem:key=>key===Capture.KEY?raw:null,setItem:(k,v)=>raw=v},setTimeout:()=>{},document:{getElementById:()=>({})}};page.window=page;
 vm.createContext(page);vm.runInContext(fs.readFileSync('dist/share-form.js','utf8'),page);
 page.shiguangReceiveShare({url:'https://www.bilibili.com/video/BV123',title:'视频',thumbnail:'https://i0.hdslb.com/test.jpg'});
 const response=await page.ShiguangCaptureTransport.sendMessage({type:'SHIGUANG_CAPTURE_GET'});assert.deepEqual([...response.tags],['机器人']);assert.equal(JSON.parse(raw).length,1);
 await page.ShiguangCaptureTransport.sendMessage({type:'SHIGUANG_CAPTURE_SAVE',tags:'机器人，新分类',reason:''});
 const saved=JSON.parse(raw)[0];assert.deepEqual(saved.tags,['机器人','新分类']);assert.equal(saved.notes,'');assert.equal(saved.thumbnail,'https://i0.hdslb.com/test.jpg');assert.equal(reports[0].saved,true);assert.equal(reports[1].cloud,true);
});
test('mobile share reports no success when durable storage fails',async()=>{
 const reports=[],page={window:null,SG,ShiguangCapture:Capture,ShiguangCloud:{ready:Promise.resolve()},ShiguangPreview:{fetch:async()=>({})},ShiguangQuickResult:{postMessage:text=>reports.push(text)},localStorage:{getItem:()=>null,setItem:()=>{throw Error('quota');}}};page.window=page;vm.createContext(page);vm.runInContext(fs.readFileSync('dist/share-form.js','utf8'),page);page.shiguangReceiveShare({url:'https://example.com'});await page.ShiguangCaptureTransport.sendMessage({type:'SHIGUANG_CAPTURE_GET'});
 await assert.rejects(page.ShiguangCaptureTransport.sendMessage({type:'SHIGUANG_CAPTURE_SAVE'}),/quota/);assert.equal(reports.length,0);
});
