'use strict';
const id=new URLSearchParams(location.search).get('draft'),status=document.getElementById('status'),save=document.getElementById('save');
document.getElementById('cancel').onclick=()=>window.close();
async function load(){
 try{
  const draft=await chrome.runtime.sendMessage({type:'SHIGUANG_CAPTURE_GET',id});
  if(draft?.error)throw Error(draft.error);if(!draft?.url)throw Error('未找到待收藏网页');
  document.getElementById('title').textContent=draft.title;document.getElementById('url').textContent=draft.url;
  for(const tag of draft.tags||[]){const option=document.createElement('option');option.value=tag;document.getElementById('categories').append(option);}
  if(draft.selection){document.getElementById('excerpt').hidden=false;document.getElementById('selection').textContent=draft.selection;}
  save.disabled=false;document.getElementById('tags').focus();
 }catch(error){status.textContent=error.message;}
}
document.getElementById('capture-form').onsubmit=async event=>{
 event.preventDefault();if(save.disabled)return;save.disabled=true;status.textContent='正在保存…';
 try{
  const result=await chrome.runtime.sendMessage({type:'SHIGUANG_CAPTURE_SAVE',id,tags:document.getElementById('tags').value,reason:document.getElementById('reason').value});
  if(!result?.ok)throw Error(result?.error||'未保存，请重试');
  status.textContent=result.duplicate?'这条内容已收藏，保留原有笔记。':'已收下，联网后自动同步。';
  setTimeout(()=>window.close(),900);
 }catch(error){status.textContent=error.message;save.disabled=false;}
};
document.addEventListener('keydown',event=>{if(event.key==='Escape')window.close();if((event.ctrlKey||event.metaKey)&&event.key==='Enter'){event.preventDefault();document.getElementById('capture-form').requestSubmit();}});
load();
