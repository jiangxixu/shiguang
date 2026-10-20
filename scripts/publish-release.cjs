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
 const notes="拾光 Android 1.6.6\n\n根据1.6.4检测记录修复“页面发生滚动，本次跟随已停止”：等待收藏确认期间，滚动或收藏夹布局变化不再直接取消。确认收藏后，核对是否回到原内容窗口及本地页面锚点；未回到原内容就不自动点击分享，避免误收下一条视频。等待有固定期限，无法核对时显示手动分享引导，保留原链接和分类确认要求。锚点只短时保存在内存，不写入日志或上传。\n\n包含1.6.5的图标语义识别、B站长按/成功事件支持、“我→适配本机按钮”、分享失败时不中断的引导。小红书可记录分享/⋯，B站可记录收藏图标；分享规则验证复制链接菜单后才生效。\n\n覆盖安装，不要卸载；请在系统无障碍中将拾光收藏跟随关闭再开启。先在“我”中确认1.6.6。若B站仍无弹窗，查看新版带平台包名的检测记录，旧版0个控件记录不足以确定是哪个平台或页面。\n\n自动测试、编译和安装包验证不能替代实际手机验证。下载 shiguang-android.apk。网站：https://jiangxixu.github.io/shiguang/ 。Android 8.0及以上，固定签名。";
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
