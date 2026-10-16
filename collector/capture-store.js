(function(root){
 'use strict';
 const SG=typeof module!=='undefined'?require('./core.js'):root.SG;
 const KEY='shiguang.library.v1';
 function save(storage,incoming){
  if(!Array.isArray(incoming)||!incoming.length)throw Error('没有可保存的链接');
  const normalized=incoming.map(SG.normalize);
  if(normalized.some(x=>!x))throw Error('链接无效，仅支持 HTTP/HTTPS 网页');
  const raw=storage.getItem(KEY),existing=raw===null?[]:JSON.parse(raw);
  if(!Array.isArray(existing)||existing.some(x=>!SG.normalize(x)))throw Error('现有知识库格式异常，请先备份修复');
  const result=SG.merge(existing,normalized);
  // Acknowledgement is returned only after durable storage succeeds.
  storage.setItem(KEY,JSON.stringify(result.items));
  return {added:result.added,duplicates:result.duplicates,total:result.items.length};
 }
 const api={save,KEY};root.ShiguangCapture=api;
 if(typeof module!=='undefined')module.exports=api;
})(typeof window!=='undefined'?window:globalThis);
