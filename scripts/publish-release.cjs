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
 const notes="拾光 Android 1.6.9\n\n根据1.6.8 YouTube检测记录修正复制链接点击：允许语义明确的整行复制链接按钮，仍拒绝整页容器；查找父按钮时避免提升到过大的容器。分享流程兼容Android系统分享面板，仅在正在取链接或学习分享按钮期间读取；复制后仍校验新剪贴板时间戳和原平台内容链接。\n\n知乎：链接校验时保留分享文字中的标题，新增手机本地公开网页OG/title读取；已有域名占位标题按原补全规则重试。YouTube新增本地oEmbed标题获取，保留直接封面回退；抖音增加本地网页预览和modal_id内容链接校验。元数据请求仅在同一平台允许域名间跳转，不携带账号Cookie，有响应大小及时间限制。\n\n覆盖安装，不卸载；关闭再开启拾光无障碍服务。失败记录现在带平台名称，便于定位。提供的日志明确定位了YouTube复制动作失败，未包含抖音失败步骤；此次变更尚未在实际手机验证，不能保证所有抖音/YouTube页面版本均成功。原链接未取得不会保存假链接。下载shiguang-android.apk。";
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
