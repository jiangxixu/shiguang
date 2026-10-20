(function(root){
 'use strict';
 const domains=['bilibili.com','b23.tv','youtube.com','youtu.be','linux.do','zhihu.com','zhihu.link','xiaohongshu.com','xhslink.com','xhslink.cn','douyin.com','iesdouyin.com','github.com'];
 function needsTitle(item){try{const title=String(item.title||'').trim(),u=new URL(item.url);return !title||title===item.url||title===u.hostname||title===u.hostname.replace(/^www\./,'')||title==='小红书';}catch{return false;}}
 function apply(item,meta){let changed=false;if(!item.thumbnail&&SG.safeImage(meta.thumbnail)){item.thumbnail=SG.safeImage(meta.thumbnail);changed=true;}if(needsTitle(item)&&meta.title&&!needsTitle({...item,title:meta.title})){item.title=String(meta.title).slice(0,500);changed=true;}return changed;}
 function supported(url){try{const u=new URL(url);return u.protocol==='https:'&&domains.some(d=>u.hostname===d||u.hostname.endsWith('.'+d));}catch{return false;}}
 let sequence=0;const requests=new Map();
 function phonePreview(url){
  return new Promise(resolve=>{
   const id=String(++sequence),timer=setTimeout(()=>{requests.delete(id);resolve({});},12000);
   requests.set(id,value=>{clearTimeout(timer);resolve({thumbnail:SG.safeImage(value.thumbnail),title:String(value.title||'').slice(0,500),requiresLogin:!!value.requiresLogin});});
   root.ShiguangPreviewNative.onmessage=event=>{try{const value=JSON.parse(event.data),done=requests.get(value.id);if(done){requests.delete(value.id);done(value);}}catch{}};
   try{root.ShiguangPreviewNative.postMessage(JSON.stringify({id,url}));}catch{requests.delete(id);clearTimeout(timer);resolve({});}
  });
 }
 async function fetchPreview(item){
  const direct=SG.thumbnailFor(item);if(direct&&!needsTitle(item))return {thumbnail:direct};
  if(root.ShiguangPreviewNative&&/^https:\/\/(?:[^/]+\.)?(?:bilibili\.com|b23\.tv|xiaohongshu\.com|xhslink\.com|xhslink\.cn)\//i.test(item.url)){
   const native=await phonePreview(item.url);if(native.thumbnail||native.title||native.requiresLogin)return native;
  }
  const cloud=root.ShiguangCloud;if(!cloud||!supported(item.url))return {};
  await cloud.ready;const {data}=await cloud.client.auth.getSession();if(!data.session)return {};
  const response=await fetch(root.ShiguangCloudConfig.url+'/functions/v1/link-preview',{method:'POST',headers:{'Content-Type':'application/json',apikey:root.ShiguangCloudConfig.key,Authorization:'Bearer '+data.session.access_token},body:JSON.stringify({url:item.url}),signal:AbortSignal.timeout(10000)});
  if(!response.ok)return {};const result=await response.json();return {thumbnail:SG.safeImage(result.thumbnail),title:String(result.title||'').slice(0,500)};
 }
 root.ShiguangPreview={fetch:fetchPreview,supported,needsTitle,apply};
})(window);
