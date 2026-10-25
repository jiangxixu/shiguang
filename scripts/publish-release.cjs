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
 const notes="拾光 Android 1.6.27 · 事件处理测试版\n\n参考GKD对密集界面事件的过滤/合并思路，独立实现单窗口内容事件合并。点击、收藏成功提示、窗口切换立即处理，不进入内容事件等待队列。移除系统统一100毫秒通知等待，由拾光仅对普通内容变化合并80毫秒批次。\n\n分享排除判断前刷新当前页面收藏按钮适配，避免使用上一页的匹配对象。系统断开服务时清理连接状态；异常记录原因并重新开始检测。检测记录增加事件数量、最近事件时间和当前阶段，便于区分系统未送达事件和页面未匹配。\n\n已研究Shizuku官方API，本版没有接入Shizuku，也不要求新增授权。Shizuku本身不能提供第三方平台私有收藏链接。\n\n本版为未完成iQOO真机验证的测试版；不承诺已解决所有平台失效、标题或封面问题。覆盖安装，保留已有数据和适配记录。\n\n参考来源：https://github.com/gkd-kit/gkd ，https://github.com/RikkaApps/Shizuku-API";
 const response=await fetch(base+'/releases',{method:'POST',headers:{...headers,'Content-Type':'application/json'},body:JSON.stringify({tag_name:tag,target_commitish:process.env.GITHUB_SHA,name:'拾光 '+version+' · Android App',body:notes,draft:false,prerelease:true})});
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
