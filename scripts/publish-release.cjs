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
 const notes="在 1.6.27 基础修复包上针对小红书、微信公众号和 YouTube 调整。小红书优先使用作者行右侧 moreOperateIV 分享箭头，避免旧分享适配遮蔽已知入口。支持 YouTube 完整保存菜单名称、辅助功能标注及已保存入口，排除播放列表复选框和明确移除动作。已适配的微信 WebView 可在 DOM 标记缺失时保留文章上下文；原生菜单不再被文章标记检查提前拦截。更多菜单与分类窗口分开处理，适配时不会把已识别的更多按钮记成收藏。允许记忆短的整行菜单按钮，仍拒绝整块面板。微信公众号保存前仍严格核对原链接为 mp.weixin.qq.com 文章。自动测试与签名构建通过后发布；没有连接到用户手机，尚待实际验证。请覆盖安装。1.6.27 恢复包仍为默认下载。";
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
