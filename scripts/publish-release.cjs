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
 const notes="拾光 Android 1.6.20\n\n新增微信公众号文章支持：平台筛选、分类保存、原链接识别、标题和封面补全；网站和电脑扩展同步识别微信公众号来源。支持 /s/ 短地址及带 __biz、mid、idx、sn 的文章地址，保留原链接参数。\n\n自动收藏适配：进入公众号文章阅读页，点右上角更多菜单里的收藏，确认收藏后取得原文章链接并弹出分类窗口。适配本机按钮新增“微信公众号”。只接受具有公众号文章标记的 WebView 及其短时关联菜单；普通聊天消息、图片及日常分享不作为文章收藏触发。没有可验证的原文章链接不保存。\n\n已用用户提供的 See-and-Reach 公众号文章核对原标题和原封面，并加入真实元数据回归样例。读取网页前 512KB 元数据，避免附带大量脚本导致整篇信息丢失。标题及封面从手机可访问的公开文章页面解析，验证/删除页面不会冒充内容。取得的信息随收藏同步；微信未暴露文章标记、菜单按钮或网页要求验证时，可能仍无法自动采集。回归测试与构建通过不代表已在用户微信版本实测。\n\n覆盖安装后关闭再开启一次拾光无障碍服务，使新增微信监听生效。已有平台按钮适配和收藏保留。";
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
