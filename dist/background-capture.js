(function(root){
 'use strict';
 const PREFIX='shiguang.capture.work.',OWNER='shiguang.cloud.owner',KEY='shiguang.library.v1';
 const read=()=>JSON.parse(localStorage.getItem(KEY)||'[]');
 function jobs(){const result=[];for(let i=0;i<localStorage.length;i++){const key=localStorage.key(i);if(key.startsWith(PREFIX)){try{result.push({key,...JSON.parse(localStorage.getItem(key))});}catch{}}}return result;}
 let sequence=0;const requests=new Map();
 function wake(){
  if(!root.ShiguangBackground)return Promise.reject(Error('后台保存组件不可用，请更新 App'));
  return new Promise((resolve,reject)=>{
   const id=String(++sequence),timer=setTimeout(()=>{requests.delete(id);reject(Error('已存本机，后台任务尚未确认，请重试保存'));},8000);
   requests.set(id,{resolve,reject,timer});
   root.ShiguangBackground.onmessage=event=>{try{const value=JSON.parse(event.data),request=requests.get(value.id);if(!request)return;requests.delete(value.id);clearTimeout(request.timer);value.ok?request.resolve():request.reject(Error('已存本机，后台任务未能建立，请重试保存'));}catch{}};
   try{root.ShiguangBackground.postMessage(JSON.stringify({id}));}catch(error){clearTimeout(timer);requests.delete(id);reject(error);}
  });
 }
 async function enqueue(item,owner){
  // Each capture owns a key, so concurrent captures never replace another job.
  const existing=read().find(row=>SG.canonical(row.url)===SG.canonical(item.url));
  if(!existing)throw Error('收藏尚未写入本机');
  const key=PREFIX+crypto.randomUUID();
  localStorage.setItem(key,JSON.stringify({owner,id:existing.id,url:existing.url,attempt:0,nextAt:0}));
  await wake(); // Native acknowledgement follows WorkManager's database commit.
 }
 function find(job){return read().find(item=>item.id===job.id&&SG.canonical(item.url)===SG.canonical(job.url));}
 function owns(job){return localStorage.getItem(OWNER)===job.owner;}
 async function drain(){
  await ShiguangCloud.ready;
  let retry=false,processed=0;
  for(const job of jobs()){
   // Guest records are imported by the normal login flow. Match the actual
   // record ID before adopting that job; never adopt an old user's job.
   if(job.owner===null&&localStorage.getItem(OWNER)&&find(job)){
    job.owner=localStorage.getItem(OWNER);localStorage.setItem(job.key,JSON.stringify(job));
   }
   if(!owns(job))continue; // Park another account's jobs; never switch accounts here.
   if(!find(job)){localStorage.removeItem(job.key);continue;}
   if(job.nextAt>Date.now())continue;
   if(++processed>4){retry=true;break;}
   const before=find(job);
   if(SG.needsTitle(before)||!before.thumbnail){
    const meta=await ShiguangPreview.fetch(before);
    if(!owns(job))continue;
    const items=read(),current=items.find(item=>item.id===job.id&&SG.canonical(item.url)===SG.canonical(job.url));
    if(!current){localStorage.removeItem(job.key);continue;}
    if(ShiguangPreview.apply(current,meta))localStorage.setItem(KEY,JSON.stringify(items));
   }
   if(!owns(job))continue;
   const current=find(job);
   if(!current){localStorage.removeItem(job.key);continue;}
   // Keep unavailable metadata queued, but do not hammer login walls/offline servers.
   if(SG.needsTitle(current)||!current.thumbnail){
    const attempt=(job.attempt||0)+1;
    localStorage.setItem(job.key,JSON.stringify({...job,attempt,nextAt:Date.now()+Math.min(21600000,60000*2**Math.min(attempt,9))}));
   }
  }
  const owner=localStorage.getItem(OWNER);
  const result=await ShiguangCloud.sync();
  if(localStorage.getItem(OWNER)!==owner)return {retry:true};
  if(result.signedIn&&result.pending===0){
   for(const job of jobs())if(owns(job)){const item=find(job);if(!item||(!SG.needsTitle(item)&&item.thumbnail))localStorage.removeItem(job.key);}
  }
  return {retry:retry||(result.signedIn&&result.pending>0)};
 }
 root.ShiguangBackgroundCapture={enqueue,drain,wake,jobs};
 if(root.ShiguangBackground&&!root.ShiguangBackgroundRunner){
  root.addEventListener('online',()=>wake().catch(()=>{}));
  // Also resume parked work after opening the library or completing login.
  if(!location.pathname.endsWith('quick-share.html')){
   ShiguangCloud.ready.then(()=>wake().catch(()=>{}));
   ShiguangCloud.client.auth.onAuthStateChange(event=>{if(event==='SIGNED_IN')setTimeout(()=>wake().catch(()=>{}),0);});
  }
 }
})(window);
