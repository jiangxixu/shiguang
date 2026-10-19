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
 const notes='拾光 Android '+version+'\n\n改进「复制链接 → 点击悬浮球 → 分类收藏」：从整段分享文案提取网址，处理中文标点、缺少协议的常见短链接和紧贴网址的分享文字。平台链接优先于附带的普通网址；出现多个平台链接时让用户选择。分类窗口显示识别出的平台；已有收藏的已知平台归属按真实域名纠正，保留原链接、笔记和分类。\n\n小红书短链接在打开时先解析为笔记地址，再通过官方原生笔记协议打开对应 App；保留原链接的访问参数。移除把短链接装进小红书内置浏览器的兜底。解析失败明确提示重试，浏览器打开需用户自行选择。短链解析只请求小红书域名、限制跳转和超时、不发送账号 Cookie。\n\n下载 shiguang-android.apk 直接覆盖安装，无需卸载。在我页面确认版本 1.6.2。此次针对悬浮球复制收藏及原内容打开；无需为此开启收藏跟随无障碍服务。保留分类和原因选填。自动化解析/跳转测试通过不代表已在你的手机验证；手机平台版本、原内容可见性及网络可能影响打开。\n\n网站：https://jiangxixu.github.io/shiguang/ 。Android 8.0 及以上，固定签名；SHA256SUMS.txt 提供校验值。';
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
