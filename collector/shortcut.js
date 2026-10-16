(function(){
 'use strict';
 let space=false,used=false,disconnected=false;
 const editing=event=>event.composedPath().some(el=>el instanceof Element&&(el.matches('input,textarea,select,[role="textbox"]')||el.isContentEditable));
 document.addEventListener('keydown',event=>{
  if(disconnected||event.isComposing||event.ctrlKey||event.altKey||event.metaKey||editing(event))return;
  if(event.code==='Space'&&!event.repeat){space=true;used=false;return;}
  if(space&&!used&&event.code==='KeyS'&&!event.repeat){
   used=true;event.preventDefault();event.stopPropagation();
   try{Promise.resolve(chrome.runtime.sendMessage({type:'SHIGUANG_SHORTCUT',title:document.title,thumbnail:document.querySelector?.('meta[property="og:image"],meta[name="twitter:image"]')?.content||'',selection:getSelection().toString().slice(0,5000)})).catch(error=>{if(/extension context invalidated/i.test(error?.message||''))disconnected=true;});}catch(error){if(/extension context invalidated/i.test(error?.message||''))disconnected=true;}
  }
 },true);
 document.addEventListener('keyup',event=>{if(event.code==='Space'){space=false;used=false;}},true);
 window.addEventListener('blur',()=>{space=false;used=false;});
})();
