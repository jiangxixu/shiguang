(async function(){
 try{
  const run=()=>ShiguangBackgroundCapture.drain();
  const result=await (navigator.locks?navigator.locks.request('shiguang-capture-worker',run):run());
  ShiguangWorkResult.postMessage(JSON.stringify(result));
 }catch{ShiguangWorkResult.postMessage(JSON.stringify({retry:true}));}
})();
