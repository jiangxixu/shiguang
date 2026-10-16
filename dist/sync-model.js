(function(root){
 'use strict';
 const SG=typeof module!=='undefined'?require('./core.js'):root.SG;
 const map=items=>Object.fromEntries(items.map(SG.normalize).filter(Boolean).map(x=>[x.url,x]));
 const equal=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
 function observe(state,items,id){
  const now=map(items),last=state.last||{},pending={...state.pending};
  for(const url of new Set([...Object.keys(now),...Object.keys(last)])){
   if(equal(now[url],last[url]))continue;
   const previous=pending[url];
   pending[url]={op:id(),kind:now[url]?(previous?.kind==='new'||!last[url]?'new':'edit'):'delete',payload:now[url]||null};
  }
  return {...state,last:now,pending};
 }
 function acknowledge(state,snapshot){
  const pending={...state.pending};
  for(const [url,op] of Object.entries(snapshot))if(pending[url]?.op===op.op)delete pending[url];
  return {...state,pending};
 }
 function pull(state,items,rows){
  const now=map(items);
  for(const row of rows){
   if(state.pending[row.url])continue;
   if(row.deleted)delete now[row.url];
   else {const item=SG.normalize(row.payload);if(item&&item.url===row.url)now[row.url]=item;}
  }
  return {state:{...state,last:now},items:Object.values(now)};
 }
 const api={observe,acknowledge,pull};root.ShiguangSyncModel=api;if(typeof module!=='undefined')module.exports=api;
})(typeof window!=='undefined'?window:globalThis);
