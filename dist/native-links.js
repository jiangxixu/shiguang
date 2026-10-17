(function(){
 'use strict';
 // Use the native opener for target=_blank links as well as ordinary anchors.
 document.addEventListener('click',event=>{
  if(event.defaultPrevented||event.button>0||event.ctrlKey||event.metaKey||event.shiftKey||event.altKey||!window.ShiguangNative)return;
  const anchor=event.target.closest?.('a[href]');if(!anchor||anchor.hasAttribute('download'))return;
  let url;try{url=new URL(anchor.href,location.href);}catch{return;}
  if(!['https:','http:'].includes(url.protocol)||url.origin===location.origin||url.username||url.password)return;
  try{window.ShiguangNative.postMessage(JSON.stringify({action:'openLink',url:url.href}));event.preventDefault();}catch{}
 });
})();
