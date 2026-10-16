(function(root){
 'use strict';
 const domains=['bilibili.com','b23.tv','youtube.com','youtu.be','linux.do','zhihu.com','zhihu.link','xiaohongshu.com','xhslink.com','douyin.com','iesdouyin.com','github.com'];
 function supported(url){try{const u=new URL(url);return u.protocol==='https:'&&domains.some(d=>u.hostname===d||u.hostname.endsWith('.'+d));}catch{return false;}}
 async function fetchPreview(item){
  const direct=SG.thumbnailFor(item);if(direct)return {thumbnail:direct};
  const cloud=root.ShiguangCloud;if(!cloud||!supported(item.url))return {};
  await cloud.ready;const {data}=await cloud.client.auth.getSession();if(!data.session)return {};
  const response=await fetch(root.ShiguangCloudConfig.url+'/functions/v1/link-preview',{method:'POST',headers:{'Content-Type':'application/json',apikey:root.ShiguangCloudConfig.key,Authorization:'Bearer '+data.session.access_token},body:JSON.stringify({url:item.url}),signal:AbortSignal.timeout(10000)});
  if(!response.ok)return {};const result=await response.json();return {thumbnail:SG.safeImage(result.thumbnail),title:String(result.title||'').slice(0,500)};
 }
 root.ShiguangPreview={fetch:fetchPreview,supported};
})(window);
