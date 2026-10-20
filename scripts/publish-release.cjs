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
 const notes="拾光 Android 1.6.8\n\n修复手机小红书收藏只有xhslink.cn域名标题及默认封面的问题：新增手机本地小红书公开笔记信息读取，识别xhslink.cn/.com短链并保留原访问参数，优先读取同一笔记的标题和首图，再使用网页OG信息。仅请求小红书允许域名，限制跳转次数、响应大小和等待时间，默认不携带账号Cookie、不执行网页脚本。遇到必须登录的笔记，新增“我→登录小红书补全信息”：经你同意并自行登录后，允许仅在本机向小红书使用该网页会话，不上传凭据；可关闭授权。收藏跟随也保留分享文字中明确的笔记标题，不再在链接校验时将标题丢弃。\n\n点保存时等候正在进行的标题/封面请求（有超时上限），减少先保存域名后返回真实信息的问题。打开知识库时，旧收藏缺失的封面及域名占位标题会尝试自动补全，保留分类、原因、手动标题及原链接；补全后沿用现有云端同步。网站和Android使用同一补全规则。\n\n覆盖安装，不卸载。联网打开拾光，等候补全及同步，再在电脑查看。小红书若要求登录、限制访问或不公开元信息，仍可能无法取得标题封面，不会把登录页当成原笔记。测试及编译不能替代你的实际链接和手机实测。\n\n下载shiguang-android.apk，Android8.0及以上，固定签名。";
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
