'use strict';
// Runs only on the published library, in the extension's isolated world.
let draining=false;
async function drain(){
 if(draining)return;draining=true;
 try{
  let totalAdded=0;
  while(true){
  const response=await chrome.runtime.sendMessage({type:'SHIGUANG_PENDING',owner:localStorage.getItem('shiguang.cloud.owner')});
  if(response.error)throw Error(response.error);
  if(!response.items?.length)break;
  const result=ShiguangCapture.save(localStorage,response.items);
  const ack=await chrome.runtime.sendMessage({type:'SHIGUANG_ACK',ids:response.items.map(x=>x.id)});
  if(!ack.ok)throw Error('归档确认失败');
  totalAdded+=result.added;
  }
  const entries=JSON.parse(localStorage.getItem(ShiguangCapture.KEY)||'[]');
  await chrome.runtime.sendMessage({type:'SHIGUANG_CATEGORIES',owner:localStorage.getItem('shiguang.cloud.owner'),tags:[...new Set(entries.flatMap(item=>item.tags||[]))]});
  if(!totalAdded)return;
  window.dispatchEvent(new StorageEvent('storage',{key:ShiguangCapture.KEY,newValue:localStorage.getItem(ShiguangCapture.KEY)}));
  const note=document.createElement('div');note.textContent=`拾光：快捷收藏已归入知识库（新增 ${totalAdded} 条）`;
  Object.assign(note.style,{position:'fixed',bottom:'24px',right:'24px',zIndex:'2147483647',background:'#354936',color:'#fff',padding:'14px 20px',borderRadius:'8px',font:'14px sans-serif'});
  document.body.append(note);setTimeout(()=>note.remove(),4000);
 }catch(e){
  // Keep the extension queue intact on corrupt/full/blocked site storage.
  console.warn('拾光：归档失败，收藏仍保留在扩展中。',e.message);
 }finally{draining=false;}
}
chrome.runtime.onMessage.addListener(message=>{if(message.type==='SHIGUANG_SYNC'){drain();return false;}});
window.addEventListener('storage',event=>{if(event.key==='shiguang.cloud.owner')drain();});
window.addEventListener('focus',drain);
setInterval(drain,30000);
drain();
