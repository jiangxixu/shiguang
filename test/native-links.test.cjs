const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const source=fs.readFileSync('dist/native-links.js','utf8');
function page(bridge){let click;vm.runInNewContext(source,{document:{addEventListener:(type,fn)=>click=fn},window:{ShiguangNative:bridge},location:new URL('https://appassets.androidplatform.net/assets/index.html'),URL});return click;}
function tap(href,extra={}){const result={prevented:false};result.event={button:0,target:{closest:()=>({href,hasAttribute:()=>false})},preventDefault:()=>result.prevented=true,...extra};return result;}
test('Android links including new-tab covers go through native app-first opening with full tokens',()=>{
 let message;const click=page({postMessage:raw=>message=JSON.parse(raw)});
 const href='https://www.xiaohongshu.com/explore/123?xsec_token=a%2Bb&xsec_source=pc#part';
 const {event,...unused}=tap(href);let prevented=false;event.preventDefault=()=>prevented=true;click(event);
 assert.deepEqual(message,{action:'openLink',url:href});assert.ok(prevented);
});
test('website, native bridge failure, internal pages, downloads and modified clicks retain default handling',()=>{
 const ordinary=tap('https://b23.tv/abc');page(undefined)(ordinary.event);assert.equal(ordinary.prevented,false);
 const failed=tap('https://b23.tv/abc');page({postMessage:()=>{throw Error('bridge unavailable')}})(failed.event);assert.equal(failed.prevented,false);
 const click=page({postMessage:()=>{throw Error('must not intercept')}});
 for(const href of ['https://appassets.androidplatform.net/assets/index.html','javascript:alert(1)','mailto:example@example.com','https://user@bilibili.com/video/a']){const t=tap(href);click(t.event);assert.equal(t.prevented,false);}
 for(const extra of [{ctrlKey:true},{metaKey:true},{shiftKey:true},{button:1},{defaultPrevented:true},{target:{closest:()=>({href:'https://example.com/a.json',hasAttribute:()=>true})}}]){const t=tap('https://b23.tv/abc',extra);click(t.event);assert.equal(t.prevented,false);}
});
