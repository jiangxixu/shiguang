(function(){
 'use strict';
 const cloud=window.ShiguangCloud,button=document.getElementById('cloud-account');
 const withTimeout=work=>new Promise((resolve,reject)=>{const timer=setTimeout(()=>reject(Error('连接超时，请检查网络后重试')),15000);Promise.resolve(work).then(resolve,reject).finally(()=>clearTimeout(timer));});
 function update(){button.textContent=cloud.user?'☁ '+cloud.message:'☁ 登录同步';}
 window.addEventListener('shiguang-cloud-status',update);cloud.ready.then(update);
 button.onclick=()=>{
  if(cloud.user){
   openModal('你的收藏，随身可见。',`<p class="help">已登录：${esc(cloud.user.email||'')}<br>${esc(cloud.message)}<br>手机 App、电脑扩展和网站使用同一个邮箱登录，即可同步收藏。离线时先保存在本机，联网后补传。</p><div class="data-actions"><button id="cloud-now">立即同步</button><button id="cloud-out">退出此设备</button></div><p id="cloud-feedback" class="help" role="status"></p>`);
   document.getElementById('cloud-now').onclick=async()=>{const feedback=document.getElementById('cloud-feedback');feedback.textContent='正在同步…';try{await withTimeout(cloud.sync());feedback.textContent=cloud.message;}catch{feedback.textContent='暂时无法同步，收藏仍保存在本机，请联网后重试。';}};
   document.getElementById('cloud-out').onclick=async()=>{try{await withTimeout(cloud.signOut());closeModal();}catch(e){document.getElementById('cloud-feedback').textContent=e.message;}};return;
  }
  openModal('登录，让收藏自动相遇。',`<p class="help">网站、手机 App 和电脑扩展使用同一个邮箱。登录后，本机真实收藏会上传至你的 Supabase 私有知识库，其他设备登录后自动同步。示例不会上传。</p><label class="field">邮箱<input id="cloud-email" type="email" autocomplete="email" placeholder="你的邮箱"></label><button id="cloud-send" class="primary">发送登录邮件</button><label class="field">邮件登录链接或验证码<input id="cloud-code" autocomplete="off" placeholder="长按邮件中的登录按钮，复制链接粘贴到这里"></label><button id="cloud-verify" class="primary">登录并同步本机收藏</button><p id="cloud-feedback" class="help" role="status"></p>`);
  const feedback=document.getElementById('cloud-feedback');
  document.getElementById('cloud-send').onclick=async event=>{const email=document.getElementById('cloud-email');if(!email.reportValidity()||!email.value.trim())return;event.target.disabled=true;feedback.textContent='正在发送登录邮件…';try{await withTimeout(cloud.requestCode(email.value.trim()));feedback.textContent='登录邮件已发送，请检查收件箱和垃圾邮件。网页可直接点邮件中的登录按钮；App 内请长按邮件按钮复制链接，粘贴在下方登录。60 秒内请勿重复发送。';}catch(e){feedback.textContent='发送失败：'+e.message;}finally{event.target.disabled=false;}};
  document.getElementById('cloud-verify').onclick=async event=>{event.target.disabled=true;feedback.textContent='正在登录与同步…';try{await withTimeout(cloud.verifyCode(document.getElementById('cloud-email').value.trim(),document.getElementById('cloud-code').value.trim()));closeModal();toast('已登录，收藏已同步');}catch(e){feedback.textContent='未完成同步：'+e.message;}finally{event.target.disabled=false;}};
 };
})();
