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
 const notes='拾光 Android '+version+'\n\n下载 shiguang-android.apk 安装（Android 8.0 及以上）。独立应用支持离线收藏、搜索、笔记、JSON/CSV 导入导出，以及接收其他 App 分享的链接。新版选择“分享 → 拾光”即可保存并返回原 App，无需填写表单。电脑一键收藏扩展点击图标即保存，下次打开网站自动归档。\n\n网页版：https://jiangxixu.github.io/shiguang/\n\n网站、App 和扩展使用同一个邮箱登录，可通过 Supabase 私有知识库自动同步收藏。首次登录后，手机“分享 → 拾光”、电脑点扩展即可保存；不需要每次打开网站。离线时先保存在设备上，再次联网/打开 App/分享时补传。扩展在浏览器运行期间自动重试。登录邮件可直接在网站打开；App/扩展复制邮件按钮链接到登录框验证。卸载/清除数据前请导出备份。未发布到应用商店；安装需要在 Android 上允许所用浏览器安装此 APK。\n\nAPK 使用项目固定签名证书；SHA256SUMS.txt 提供校验值。';
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
