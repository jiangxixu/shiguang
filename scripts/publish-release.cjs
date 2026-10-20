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
 const notes="拾光 Android 1.6.7\n\n收藏跟随及适配本机按钮增加知乎、抖音（含极速版）、YouTube；同时扩展无障碍监听范围、平台独立的按钮规则和原内容链接校验。YouTube 支持尝试识别保存到播放列表/稍后观看的按钮及确认提示。每个平台的规则单独保存，B站和抖音的不同版本仅在对应平台内适配。\n\n升级：覆盖安装，不卸载；关闭再开启系统中的拾光无障碍服务。进入我→适配本机按钮，选择平台，分别记录分享和收藏/保存按钮；分享按钮出现复制链接菜单后规则才生效。完成平台收藏确认后，取得本次原链接，再进入分类窗口由你确认保存。\n\n保留1.6.6的滚动容错及原内容核对。未取得原链接不保存，支持范围不代表所有App版本都已在实际手机验证。自动测试及编译不能替代手机实测。\n\n下载 shiguang-android.apk。网站：https://jiangxixu.github.io/shiguang/ 。Android 8.0及以上，固定签名。";
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
