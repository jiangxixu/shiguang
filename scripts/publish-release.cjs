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
 const notes="拾光 Android 1.6.3\n\n改进收藏跟随（试用）：参考小星记账官方帮助描述的“无障碍读取特定界面→本地识别→弹窗”机制，在原有拾光代码上加入未收藏→已收藏状态比较，不再只依赖单次点击事件。读取已勾选/已选中状态描述，合并按钮及子标签，支持活动分享窗口。状态仅短时保存在内存，不上传页面树。\n\n确认新增收藏后，自动尝试原 App 分享→复制链接→校验本次平台原链接→拾光分类窗口。B站先确认原收藏夹。若系统拦住后台窗口，出现“选择分类”提示卡，点击继续。取消、已有收藏、滚动换页、旧状态和多个收藏控件不作为新增状态触发。必须取得真实链接；不以标题摘录或主页代替。\n\n在“我”中新增“查看检测记录”，显示最近12条处理阶段，可自行复制；不含页面文字、链接或账号。用于核对服务是否连接、按钮是否可识别、在哪一步停止。\n\n下载 shiguang-android.apk 覆盖安装，不要卸载。打开拾光→我→开启收藏跟随，在系统无障碍中启用对应服务。已开启的用户建议关闭该服务再开启一次，以加载新的多窗口事件配置。识别后分类和原因仍可留空，只有确认保存才会同步。\n\n这是无 Root、无截图的可识别界面适配，不读取其他 App 内部数据；不能保证所有平台版本均暴露收藏和分享按钮。自动测试和构建通过并不代表已在你的手机实测。官网参考：https://cxincx.com/support/auto/2.html 。网站：https://jiangxixu.github.io/shiguang/ 。Android 8.0及以上，固定签名。";
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
