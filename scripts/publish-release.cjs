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
 const notes="仅针对抖音精选与 YouTube 补充适配。恢复 com.ss.android.yumme.video 的无障碍事件与独立适配入口，不与普通抖音共用按钮选择器。按用户提供的抖音精选竖屏界面识别右侧收藏轨道，以及底部位于全屏左侧的分享控件。YouTube 分别识别普通视频横向操作栏和 Shorts 保存/分享轨道；从可访问标题或描述保留原视频锚点；处理图标与保存文字分离的菜单行，并在分类确认后关闭可核对的保存面板，再核对原视频取链接。仅点击更多或普通分享不会触发收藏。新增逻辑按目标包名限定，微信、小红书、B站、知乎和普通抖音沿用现有流程。针对截图比例、重复控件、广告更多按钮、横屏拒绝、原视频过期上下文等增加回归用例。自动测试和签名构建通过后发布；未连接用户手机，尚待真机验证。覆盖安装保留数据。1.6.27 恢复包继续作为默认下载。";
 const response=await fetch(base+'/releases',{method:'POST',headers:{...headers,'Content-Type':'application/json'},body:JSON.stringify({tag_name:tag,target_commitish:process.env.GITHUB_SHA,name:'拾光 '+version+' · 1.6.27 基础修复测试包',body:notes,draft:false,prerelease:true})});
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
