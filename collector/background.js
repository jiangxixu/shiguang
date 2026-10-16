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
async function openCapture(tab,info={}){
 try{
  if(!tab.id)throw Error('没有活动网页');
  const result=info.raw||((await chrome.scripting.executeScript({target:{tabId:tab.id},func:()=>({url:location.href,title:document.title,selection:getSelection().toString().slice(0,5000),createdAt:new Date().toISOString()})}))[0].result);
  if(info.linkUrl){result.url=info.linkUrl;result.title=info.selectionText||info.linkUrl;}
  if(!SG.safeUrl(result.url))throw Error('请在普通网页上收藏');
  const ownership=await chrome.storage.local.get('shiguang.extension.owner');
  const id=crypto.randomUUID();
  await locked(async()=>{
   const drafts=(await chrome.storage.local.get('captureDrafts')).captureDrafts||{};
   for(const [key,draft] of Object.entries(drafts))if(Date.now()-draft.time>86400000)delete drafts[key];
   drafts[id]={raw:result,tabId:tab.id,owner:ownership['shiguang.extension.owner']||null,time:Date.now()};
   await chrome.storage.local.set({captureDrafts:drafts});
  });
  await chrome.windows.create({url:chrome.runtime.getURL('capture.html')+'?draft='+encodeURIComponent(id),type:'popup',width:480,height:620});
 }catch(e){await badge('!','#a44d3c');await chrome.action.setTitle({title:'未保存：'+e.message});if(tab?.id)await tell(tab.id,'拾光：未保存，'+e.message);}
}
async function finishCapture(tabId,saved){
 try{
  await badge('✓');
  await chrome.action.setTitle({title:'已收下 · 打开拾光即可查看'});
  await tell(tabId,saved.duplicate?'拾光：已收下过这条内容，保留原笔记':'拾光：已收下，稍后打开知识库即可查看');
  const tabs=await chrome.tabs.query({url:SITE+'*'});
  for(const target of tabs){try{await chrome.tabs.sendMessage(target.id,{type:'SHIGUANG_SYNC'});}catch{}}
  try{const cloud=await pushCloud();if(cloud.signedIn)await chrome.action.setTitle({title:'已收藏并同步到云端 · 拾光'});}catch{await chrome.action.setTitle({title:'已保存在本机扩展，联网后自动重试同步'});}
 }catch{}
}
chrome.action.onClicked.addListener(tab=>openCapture(tab));
chrome.commands.onCommand.addListener(async command=>{
 if(command==='save-current-page'){const [tab]=await chrome.tabs.query({active:true,currentWindow:true});if(tab)await openCapture(tab);}
});
chrome.contextMenus.onClicked.addListener((info,tab)=>{if(info.menuItemId==='shiguang-save')return openCapture(tab,info);});
async function saveDraft(message){
 const result=await locked(async()=>{
  const drafts=(await chrome.storage.local.get('captureDrafts')).captureDrafts||{},draft=drafts[message.id];
  if(!draft)throw Error('收藏窗口已过期，请回到网页重新收藏');
  const owner=(await chrome.storage.local.get('shiguang.extension.owner'))['shiguang.extension.owner']||null;
  if(owner!==draft.owner)throw Error('账号已切换，请重新打开收藏窗口');
  const item=SG.normalize({...draft.raw,tags:String(message.tags||''),notes:[String(message.reason||'').trim(),draft.raw.selection?'摘录：'+draft.raw.selection:''].filter(Boolean).join('\n\n')});
  if(!item)throw Error('链接无效，尚未保存');if(owner)item.cloudOwner=owner;
  const queue=await pending();const duplicate=queue.some(x=>SG.canonical(x.url)===item.url&&(x.cloudOwner||null)===owner);
  if(!duplicate){queue.push(item);await chrome.storage.local.set({[QUEUE]:queue});}
  delete drafts[message.id];await chrome.storage.local.set({captureDrafts:drafts});
  return {duplicate,tabId:draft.tabId};
 });
 return result;
}
chrome.runtime.onMessage.addListener((message,sender,respond)=>{
 if(sender.id!==chrome.runtime.id)return;
 if(message.type==='SHIGUANG_SHORTCUT'&&sender.tab?.id&&SG.safeUrl(sender.tab.url)){
  openCapture(sender.tab,{raw:{url:sender.tab.url,title:String(message.title||sender.tab.url).slice(0,500),selection:String(message.selection||'').slice(0,5000),createdAt:new Date().toISOString()}}).then(()=>respond({ok:true})).catch(e=>respond({error:e.message}));return true;
 }
 const fromCapture=sender.url?.startsWith(chrome.runtime.getURL('capture.html')+'?');
 if(fromCapture&&message.type==='SHIGUANG_CAPTURE_GET'){
  locked(async()=>{const drafts=(await chrome.storage.local.get('captureDrafts')).captureDrafts||{};const draft=drafts[message.id];if(!draft)throw Error('收藏窗口已过期');const items=await pending();return {title:draft.raw.title,url:draft.raw.url,selection:draft.raw.selection||'',tags:[...new Set(items.flatMap(x=>x.tags||[]))].slice(0,50)};}).then(respond).catch(e=>respond({error:e.message}));return true;
 }
 if(fromCapture&&message.type==='SHIGUANG_CAPTURE_SAVE'){
  saveDraft(message).then(result=>{respond({ok:true,duplicate:result.duplicate});return finishCapture(result.tabId,result);}).catch(e=>respond({error:e.message}));return true;
 }
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
function registerMenu(){chrome.contextMenus.removeAll(()=>{chrome.contextMenus.create({id:'shiguang-save',title:'收藏到拾光…',contexts:['page','selection','link'],documentUrlPatterns:['http://*/*','https://*/*']});});}
chrome.runtime.onInstalled.addListener(()=>{chrome.action.setTitle({title:'收藏到拾光 · Alt+Shift+S'});registerMenu();});
chrome.runtime.onStartup.addListener(registerMenu);

chrome.alarms.create('shiguang-cloud-retry',{periodInMinutes:1});
chrome.alarms.onAlarm.addListener(alarm=>{if(alarm.name==='shiguang-cloud-retry')pushCloud().catch(()=>{});});
