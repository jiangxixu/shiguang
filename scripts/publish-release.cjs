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
 const notes='拾光 Android '+version+'\n\n新版：手机分享后可选择已有分类、创建新分类并填写原因，再保存返回原 App；右下角浮动收藏按钮和粘贴链接；卡片直接删除；电脑手机优先显示原内容封面，登录后自动补全已有收藏封面。原平台限制访问时显示备用封面。已有用户直接安装新版 APK 更新，请勿先卸载。\n\n下载 shiguang-android.apk 安装（Android 8.0 及以上）。独立应用支持离线收藏、搜索、笔记、JSON/CSV 导入导出，以及接收其他 App 分享的链接。选择“分享 → 拾光”，分类和原因选填，点击保存后返回原 App。电脑按住空格再按 S，或网页右键选择“收藏到拾光…”，弹窗选填分类和收藏原因后保存。网站封面和标题直接打开原内容，卡片提供独立编辑入口。\n\n网页版：https://jiangxixu.github.io/shiguang/\n\n网站、App 和扩展使用同一个邮箱登录，可通过 Supabase 私有知识库自动同步收藏。首次登录后，手机“分享 → 拾光”及电脑快捷键都可打开收藏窗口并保存；不需要每次打开网站。离线时先保存在设备上，再次联网/打开 App/分享时补传。扩展在浏览器运行期间自动重试。登录邮件可直接在网站打开；App/扩展复制邮件按钮链接到登录框验证。卸载/清除数据前请导出备份。未发布到应用商店；安装需要在 Android 上允许所用浏览器安装此 APK。\n\nAPK 使用项目固定签名证书；SHA256SUMS.txt 提供校验值。';
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
