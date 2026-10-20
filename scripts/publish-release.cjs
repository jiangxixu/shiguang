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
 const notes="拾光 Android 1.6.5\n\n针对截图中“未找到可识别的分享按钮”改进：除了文字，还识别分享/收藏图标的语义资源ID；只有唯一候选才操作，不会把任意“更多”按钮当分享。已确认收藏但无法定位分享时，保留30秒本次流程并提示手动点原App分享/⋯，检测到复制链接菜单后自动继续校验原链接、打开分类。\n\n新增“我→适配本机按钮”：选择小红书或B站，再选择收藏图标或分享/⋯。先进入内容详情页，点“记录下一次点击”，再点原App对应图标。分享规则必须验证出现复制链接菜单才保存；收藏适配请用尚未收藏的内容，并在原平台完成收藏。规则仅在本机保存控件标识/结构/相对位置，不保存正文、链接、账号、截图。只有规则唯一、控件类型与位置匹配才复用。页面改版后可能需要重新适配。\n\nB站增加长按收藏事件、明确收藏成功事件的跟随，并支持HD版包名。图标不响应无障碍点击时，只对实际识别/记录的控件中心尝试触摸，不盲点固定坐标。没有取得本次原内容链接仍不保存。\n\n覆盖安装，不要卸载；请在系统无障碍中将“拾光·收藏跟随”关闭再开启，以加载新增事件和控件触摸能力。测试和编译通过不等于已在你的手机验证，B站无弹窗仍需“我→查看检测记录”的实际记录定位。\n\n下载 shiguang-android.apk。网站：https://jiangxixu.github.io/shiguang/ 。Android 8.0及以上，固定签名。";
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
