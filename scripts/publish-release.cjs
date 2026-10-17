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
 const notes='拾光 Android '+version+'\n\n修复收藏跟随中持续页面更新导致确认检查被不断推迟的问题。改为独立定时检查，重复的成功提示不推迟下一步。补充带按钮描述、数量的收藏/分享标签及点击容器子控件识别；继续区分新增收藏与取消收藏。没有取得对应平台本次的原内容链接仍不保存。\n\n在「我 → 跟随原 App 收藏（试用）」查看带时间的最近阶段：是否收到点击、是否确认收藏、是否找到分享/复制链接，以及系统是否真正显示校验窗口。窗口未显示、链接缺失和超时均显示原因。仅支持可识别的小红书/B站界面，尚未在你的手机真机验证。\n\n下载 shiguang-android.apk 直接覆盖安装，无需卸载。打开「我」确认 Android 1.6.1；更新后请在系统无障碍设置关闭再开启「拾光 · 收藏跟随（试用）」以应用新配置。进入原 App 内容详情页收藏；B站需要确认收藏夹。识别成功会提示「拾光已识别收藏」，随后尝试取得原链接并进入分类窗口，最终由你确认保存。若没有弹窗，请回到我页面查看最近状态。\n\n网站：https://jiangxixu.github.io/shiguang/ 。Android 8.0 及以上；固定签名，SHA256SUMS.txt 提供校验值。';
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
