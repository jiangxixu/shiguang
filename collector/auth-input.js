(function(root){
 'use strict';
 function parse(value,projectUrl,email){
  value=String(value||'').trim();
  if(/^\d{6,10}$/.test(value))return {email,token:value,type:'email'};
  let url;try{url=new URL(value);}catch{throw Error('请粘贴邮件中登录按钮的链接，或输入邮件验证码');}
  const expected=new URL(projectUrl);
  if(url.origin!==expected.origin||url.pathname!=='/auth/v1/verify')throw Error('这不是拾光的登录链接，请复制本次登录邮件中的按钮链接');
  const token=url.searchParams.get('token'),type=url.searchParams.get('type');
  if(!token||!['email','magiclink','signup'].includes(type))throw Error('登录链接不完整，请重新复制邮件按钮链接');
  return {token_hash:token,type};
 }
 root.ShiguangAuthInput={parse};if(typeof module!=='undefined')module.exports={parse};
})(typeof window!=='undefined'?window:globalThis);
