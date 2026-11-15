// Self-contained: Chrome serializes this function into the active page.
function ShiguangReadPage(targetUrl){
 const image=value=>{try{if(!value)return '';const u=new URL(String(value).trim(),location.href);if(u.protocol==='http:')u.protocol='https:';if(u.protocol!=='https:'||u.username||u.password)return '';return u.href;}catch{return '';}};
 const from=node=>{if(!node)return '';for(const value of [node.currentSrc,node.getAttribute('data-original'),node.getAttribute('data-src'),node.getAttribute('data-actualsrc'),node.getAttribute('poster'),node.getAttribute('src')]){const url=image(value);if(url)return url;}return '';};
 const first=selectors=>{for(const selector of selectors){for(const node of document.querySelectorAll(selector)){const url=from(node);if(url)return url;}}return '';};
 if(targetUrl&&new URL(targetUrl,location.href).href!==location.href){
  const anchor=[...document.querySelectorAll('a[href]')].find(a=>a.href===targetUrl),scope=anchor?.closest('section.note-item,.video-card,.bili-video-card,.ContentItem,.List-item,ytd-playlist-video-renderer,ytd-rich-item-renderer')||anchor;
  return {url:targetUrl,title:anchor?.getAttribute('title')||anchor?.innerText?.trim()||targetUrl,thumbnail:from(scope?.querySelector('img:not([class*="avatar"])')),selection:'',createdAt:new Date().toISOString()};
 }
 const host=location.hostname,at=domain=>host===domain||host.endsWith('.'+domain);
 let thumbnail='';
 if(at('xiaohongshu.com'))thumbnail=first(['.note-detail-mask .swiper-slide-active img','.note-container .swiper-slide-active img','.note-detail-mask video[poster]','.note-container video[poster]','.note-detail-mask .slide-container img','.note-container .slide-container img']);
 if(at('zhihu.com'))thumbnail=first(['.Post-RichTextContainer .RichText img','.Post-RichText img','article .RichContent-cover img','.QuestionAnswer-content .RichText img','.AnswerItem .RichText img','article .RichText img']);
 if(at('mp.weixin.qq.com'))thumbnail=first(['#js_content img[data-src]','#js_content img']);
 if(!thumbnail)for(const selector of ['meta[property="og:image"]','meta[property="og:image:url"]','meta[property="og:image:secure_url"]','meta[name="twitter:image"]','meta[property="twitter:image"]','meta[name="twitter:image:src"]','meta[itemprop="image"]','link[rel="image_src"]']){
  for(const node of document.querySelectorAll(selector)){const url=image(node.getAttribute('content')||node.getAttribute('href'));if(url){thumbnail=url;break;}}if(thumbnail)break;
 }
 if(!thumbnail)thumbnail=first(['main video[poster]','article video[poster]','.bpx-player-video-wrap video[poster]','video[poster]']);
 if(!thumbnail)for(const node of document.querySelectorAll('script[type="application/ld+json"]')){try{const queue=[JSON.parse(node.textContent)];let count=0;while(queue.length&&count++<150){const value=queue.shift();if(Array.isArray(value)){queue.push(...value);continue;}if(!value||typeof value!=='object')continue;const type=String(value['@type']||'');if(/Article|VideoObject|BlogPosting|NewsArticle|CreativeWork/.test(type)){const candidate=value.thumbnailUrl||value.image;for(const item of [].concat(candidate||[])){thumbnail=image(typeof item==='string'?item:item?.url||item?.contentUrl);if(thumbnail)break;}}if(thumbnail)break;if(value['@graph'])queue.push(value['@graph']);if(value.mainEntity)queue.push(value.mainEntity);}if(thumbnail)break;}catch{}}
 if(!thumbnail)thumbnail=first(['article img[itemprop="image"]','main img[itemprop="image"]']);
 return {url:location.href,title:document.title,thumbnail,selection:getSelection().toString().slice(0,5000),createdAt:new Date().toISOString()};
}
if(typeof module!=='undefined')module.exports=ShiguangReadPage;
