'use strict';
const accountStatus=document.getElementById('account-status');
async function command(data){const result=await chrome.runtime.sendMessage(data);if(result?.error)throw Error(result.error);return result;}
async function showAccount(){try{const result=await command({type:'SHIGUANG_CLOUD_STATUS'});accountStatus.textContent=result.email?'已登录：'+result.email+' · 收藏会自动上传':'未登录：收藏先保存在此浏览器扩展中';}catch(e){accountStatus.textContent=e.message;}}
document.getElementById('account-send').onclick=async event=>{const email=document.getElementById('account-email');if(!email.reportValidity()||!email.value.trim())return;event.target.disabled=true;try{await command({type:'SHIGUANG_CLOUD_SEND',email:email.value.trim()});accountStatus.textContent='登录邮件已发送。长按邮件中的登录按钮，复制链接后粘贴在输入框中。60 秒内请勿重复发送。';}catch(e){accountStatus.textContent=e.message;}finally{event.target.disabled=false;}};
document.getElementById('account-verify').onclick=async event=>{event.target.disabled=true;try{await command({type:'SHIGUANG_CLOUD_VERIFY',email:document.getElementById('account-email').value.trim(),token:document.getElementById('account-token').value.trim()});await showAccount();}catch(e){accountStatus.textContent='未完成：'+e.message;}finally{event.target.disabled=false;}};
document.getElementById('account-push').onclick=async()=>{try{const result=await command({type:'SHIGUANG_CLOUD_PUSH'});accountStatus.textContent=result.signedIn?'已同步到云端':'请先登录';}catch(e){accountStatus.textContent='暂未同步，内容保存在扩展中：'+e.message;}};
document.getElementById('account-out').onclick=async()=>{try{await command({type:'SHIGUANG_CLOUD_LOGOUT'});await showAccount();}catch(e){accountStatus.textContent='尚未退出：'+e.message;}};
showAccount();
