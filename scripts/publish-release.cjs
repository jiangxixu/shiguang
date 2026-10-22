const fs=require('node:fs');
async function main(){
 const repo=process.env.GITHUB_REPOSITORY,token=process.env.GH_TOKEN;
 if(!/^[-\w]+\/[-\w.]+$/.test(repo||'')||!token)throw Error('Missing repository or release token');
 const version=fs.readFileSync('android/app/build.gradle','utf8').match(/versionName '([\d.]+)'/)?.[1];
 if(!version)throw Error('Missing versionName');
 const tag='v'+version,base='https://api.github.com/repos/'+repo;
 const headers={Authorization:'Bearer '+token,Accept:'application/vnd.github+json','User-Agent':'shiguang-release','X-GitHub-Api-Version':'2022-11-28'};
 const existing=await fetch(base+'/releases/tags/'+tag,{headers});
 if(existing.status!==404)throw Error('This release tag already exists or cannot be checked. Bump versionName before publishing.');
 const notes="拾光 Android 1.6.24\n\n自动收藏流程调整：识别原App收藏/保存动作后直接显示分类表单，不再先等待平台成功提示或先取链接。用户选分类并点击保存收藏后，自动尝试取得本次原内容链接并校验，成功后沿用已选分类和原因完成保存。微信公众号不再依赖“收藏成功”提示才能打开分类界面。\n\n取消自动流程中的独立手动分享引导、二次选择分类提示和额外提示卡片。取链接失败只在原表单内显示错误，保留分类和原因，允许重试或关闭。分类表单只显示一套标题和关闭按钮。普通分享不触发收藏，缺少有效原链接不写入占位收藏；标题封面补全仍沿用已有逻辑。\n\n取链接时仍可能短暂显示原App菜单，无法保证第三方App隐藏的控件能被识别。已验证分类先行、失败不写入、重试保留分类及标题封面的自动测试；尚未在用户手机逐平台实测。覆盖安装后关闭并重新开启无障碍服务。";
 const response=await fetch(base+'/releases',{method:'POST',headers:{...headers,'Content-Type':'application/json'},body:JSON.stringify({tag_name:tag,target_commitish:process.env.GITHUB_SHA,name:'拾光 '+version+' · Android App',body:notes,draft:false,prerelease:false})});
 if(!response.ok)throw Error('Release creation failed: '+response.status);
 const release=await response.json();
 for(const [name,type] of [['shiguang-android.apk','application/vnd.android.package-archive'],['shiguang-collector.zip','application/zip'],['SHA256SUMS.txt','text/plain']]){
  const file=fs.readFileSync('release/'+name);
  const uploaded=await fetch(release.upload_url.replace(/\{.*$/,'')+'?name='+encodeURIComponent(name),{method:'POST',headers:{...headers,'Content-Type':type},body:file});
  if(!uploaded.ok)throw Error('Asset upload failed: '+uploaded.status);
 }
 console.log('Release published: '+release.html_url);
}
main().catch(e=>{console.error(e.message);process.exit(1)});
