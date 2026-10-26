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
 const notes="拾光 Android 1.6.28 · 后台补全与同步\n\n本版仅优化取得有效原链接后的保存阶段，保留 1.6.27 的收藏/保存识别及自动取链接流程。\n\n原链接、分类及原因先写入本机；后台任务经 WorkManager 持久化确认后，立即关闭分类界面，不再等待标题/封面和云端同步，不再显示同步等待窗口。后台使用独立执行器，关闭分类界面不会取消任务；断网重试，进程重启后由系统恢复调度。\n\n后台只补全缺失标题及封面，保留手动修改、分类和原因，不恢复已删除记录；任务按账号隔离。未登录先保存在本机。系统省电、强制停止及网络限制仍会影响后台执行时机。\n\n此项不取消自动打开分享/复制链接的前台操作，取得原链接期间仍需短暂等待。原平台需要登录才能返回的标题/封面，后台也无法保证获取。\n\n已通过自动测试与构建检查；尚未完成 iQOO 真机验收。请覆盖安装，不要卸载旧版，以保留已有数据、登录和按钮适配。";
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
