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
 const notes="拾光 Android 1.6.17\n\n修复抖音分享文字标题提取：正确读取“复制打开抖音，看看【作者的作品】”之后的原文案，保留平台提供的截断标记。已将用户反馈的“记住这四点，能让您比同龄人更年轻”实际分享文字加入回归测试；不再把整段分享文字过滤成 v.douyin.com。补充原页面作者下方完整文案读取。\n\n兼容 YouTube 三点菜单中的整行保存按钮：将无标签外层按钮与内部的保存标签关联；打开原视频更多菜单前临时记录页面，确认保存后核对返回的原内容，再继续获取链接及分类。没有可靠原链接仍不会保存。\n\n抖音网页预览补充手机 _ROUTER_DATA 数据结构，按视频 ID 核对标题及封面，兼容桌面 RENDER_DATA。部分公开分享网页只返回通用页面，没有视频详情；这时使用原平台分享文案，无法保证每条都能补全封面或完整标题。\n\n覆盖安装保留已有收藏、登录及按钮适配。安装后请关闭再开启一次拾光无障碍服务。构建与自动化回归测试验证代码行为；尚未在用户实体手机上验证新版 YouTube 菜单流程。";
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
