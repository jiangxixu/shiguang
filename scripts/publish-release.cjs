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
 const notes="拾光 Android 1.6.31 · 知乎取链接改进\n\n已明确识别为分享/更多的知乎入口不再依赖标题提取成功；仍先核对原内容窗口。复制链接或分享按钮存在嵌套父子控件时，合并为同一个目标，不再把同一按钮误判成多个候选；不同目标仍拒绝猜测。\n\n按用户提供的截图补充作者行右侧、关注按钮旁的分享控件布局识别，并排除顶部搜索和底部更多菜单。核对原内容时跳过动态赞同人数文字。补充知乎“分享问题/回答/文章”、“复制回答/文章/问题链接”及更多菜单动作名称，普通分享仍不触发收藏。仅在用户收藏并确认分类之后尝试取链接。\n\n保留抖音精选底部分享适配和后台保存。测试与构建通过后发布；尚未收到本次知乎失败的完整控件诊断，也未完成用户 iQOO 真机验证，因此不保证覆盖所有知乎版本。覆盖安装保留已有数据和按钮适配。";
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
