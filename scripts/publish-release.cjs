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
 const notes="拾光 Android 1.6.23\n\n稳定性修复：其他已支持App的后台事件不再直接取消正在进行的收藏；已经打开的分类表单不再被其他平台事件关闭。无障碍中断后首次重新收到事件会恢复定时检测，服务重连清理旧任务。窗口短暂不可读时保留检测上下文，等待最长2秒；收藏确认后，取链接入口暂未出现时最多等待2秒再提示手动继续，不重复点击原App收藏。\n\n收藏控件确认增加本次点击的临时结构匹配，可容忍按钮轻微位移；仍要求同一原内容和唯一按钮匹配，不通过放宽原链接校验来提高成功率。检测记录保留最近60条，复制菜单超时记录当前平台结构，便于定位具体失败阶段。\n\n保留标题封面、原链接和分类保存。普通分享仍不独立触发收藏，不恢复悬浮球和下拉快捷入口。覆盖安装后关闭再开启无障碍服务，已有适配及收藏保留。\n\n自动回归测试、构建和签名检查不能代替各平台真机测试；第三方App隐藏控件或变更界面时仍可能无法自动识别。";
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
