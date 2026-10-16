'use strict';
importScripts('core.js','cloud-config.js','auth-input.js','supabase.js','cloud-worker.js');
const SITE='https://jiangxixu.github.io/shiguang/';
const QUEUE='quickCaptureQueue';
let serial=Promise.resolve();
function locked(work){const next=serial.then(work);serial=next.catch(()=>{});return next;}
async function pending(){return (await chrome.storage.local.get(QUEUE))[QUEUE]||[];}
async function badge(text,color='#47654e'){await chrome.action.setBadgeBackgroundColor({color});await chrome.action.setBadgeText({text});}
async function tell(tabId,text){
 try{await chrome.scripting.executeScript({target:{tabId},func:message=>{
  const notice=document.createElement('div');notice.textContent=message;
  Object.assign(notice.style,{position:'fixed',top:'24px',right:'24px',zIndex:'2147483647',padding:'15px 22px',background:'#354936',color:'#fff',borderRadius:'10px',boxShadow:'0 8px 30px #0003',font:'15px sans-serif',pointerEvents:'none'});
  document.documentElement.append(notice);setTimeout(()=>notice.remove(),3500);
 },args:[text]});}catch{}
}
async function enqueue(raw,owner){
 const item=SG.normalize(raw);if(!item)throw Error('这个页面无法收藏，请打开普通网页后重试');if(owner)item.cloudOwner=owner;
 return locked(async()=>{const queue=await pending();const duplicate=queue.some(x=>SG.canonical(x.url)===item.url&&(x.cloudOwner||null)===(owner||null));
  if(!duplicate){queue.push(item);await chrome.storage.local.set({[QUEUE]:queue});}
  return {duplicate,count:queue.length};
 });
}
chrome.action.onClicked.addListener(async tab=>{
 try{
  if(!tab.id)throw Error('没有活动网页');
  const [{result}] = await chrome.scripting.executeScript({target:{tabId:tab.id},func:()=>({url:location.href,title:document.title,notes:getSelection().toString().slice(0,5000),createdAt:new Date().toISOString()})});
  const ownership=await chrome.storage.local.get('shiguang.extension.owner');const saved=await enqueue(result,ownership['shiguang.extension.owner']);await badge('✓');
  await chrome.action.setTitle({title:'已收下 · 打开拾光即可查看'});
  await tell(tab.id,saved.duplicate?'拾光：已收下过这条内容':'拾光：已收下，稍后打开知识库即可查看');
  try{const cloud=await pushCloud();if(cloud.signedIn){await chrome.action.setTitle({title:'已收藏并同步到云端 · 拾光'});await tell(tab.id,'拾光：已收下并同步到云端');}}catch{await chrome.action.setTitle({title:'已保存在本机扩展，联网后点击扩展或打开选项重试同步'});}
  const tabs=await chrome.tabs.query({url:SITE+'*'});
  for(const target of tabs){try{await chrome.tabs.sendMessage(target.id,{type:'SHIGUANG_SYNC'});}catch{}}
 }catch(e){await badge('!','#a44d3c');await chrome.action.setTitle({title:'未保存：'+e.message});if(tab.id)await tell(tab.id,'拾光：未保存，'+e.message);}
});
chrome.runtime.onMessage.addListener((message,sender,respond)=>{
 if(sender.id!==chrome.runtime.id)return;
 const fromLibrary=sender.tab?.url?.startsWith(SITE);
 if(message.type?.startsWith('SHIGUANG_CLOUD_')&&sender.url===chrome.runtime.getURL('popup.html')){cloudMessage(message).then(respond).catch(e=>respond({error:e.message}));return true;}
 if(message.type==='SHIGUANG_PENDING'&&fromLibrary){locked(pending).then(items=>respond({items:items.filter(item=>!item.cloudOwner||item.cloudOwner===message.owner)})).catch(()=>respond({error:'无法读取待归档收藏'}));return true;}
 if(message.type==='SHIGUANG_ACK'&&fromLibrary&&Array.isArray(message.ids)){
  locked(async()=>{const ids=new Set(message.ids);const queue=(await pending()).filter(x=>!ids.has(x.id));await chrome.storage.local.set({[QUEUE]:queue});await badge(queue.length?String(queue.length):'');return{ok:true};}).then(respond).catch(()=>respond({error:'无法确认归档'}));return true;
 }
});
chrome.runtime.onInstalled.addListener(()=>{chrome.action.setTitle({title:'点击即收藏当前网页 · 拾光'});});

chrome.alarms.create('shiguang-cloud-retry',{periodInMinutes:1});
chrome.alarms.onAlarm.addListener(alarm=>{if(alarm.name==='shiguang-cloud-retry')pushCloud().catch(()=>{});});
