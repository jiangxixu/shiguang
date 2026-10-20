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
 const notes="拾光 Android 1.6.16\n\n修复小红书分享链接标题及封面：按手机分享网页请求，兼容 noteData.data.noteData 与桌面 noteDetailMap 数据结构，并核对原笔记 ID。已用反馈中的公开短链接确认手机网页包含原标题及首图，而桌面请求跳转登录页。\n\n修复 YouTube 短链转正式链接后，youtu.be 被误当作有效标题而跳过补全的问题；已有占位标题也可补全。扩展嵌套保存按钮识别，保存/取消分类后恢复检测。\n\n改善抖音等平台的等待逻辑：在原窗口中核对原内容文字是否仍存在，不再仅比较第一段文字；确认收藏后等待页面返回最多约 5 秒，避免无意义等待到 17 秒。成功打开复制菜单后会记住分享控件。\n\n诊断按平台分别保留，补充未识别点击及完整右侧按钮区域。没有取得本次有效原内容链接仍不保存。覆盖安装并重连无障碍服务，已有收藏及按钮适配保留。自动化回归测试与签名构建通过；未在用户实体手机上验证抖音和 YouTube 的全部布局。";
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
