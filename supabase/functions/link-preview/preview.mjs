// Public metadata only. Every outbound redirect is checked against this allowlist.
const domains=['bilibili.com','b23.tv','youtube.com','youtu.be','linux.do','zhihu.com','zhihu.link','xiaohongshu.com','xhslink.com','douyin.com','iesdouyin.com','github.com'];
export function allowed(value){try{const u=new URL(value);return u.protocol==='https:'&&!u.username&&!u.password&&(!u.port||u.port==='443')&&domains.some(d=>u.hostname===d||u.hostname.endsWith('.'+d));}catch{return false;}}
export function imageUrl(value,base){if(!value)return "";try{const u=new URL(value,base);if(!['https:','http:'].includes(u.protocol)||u.username||u.password)return '';u.protocol='https:';return u.href.slice(0,2000);}catch{return '';}}
function decode(value){return value.replace(/&amp;/gi,'&').replace(/&quot;/gi,'"').replace(/&#39;|&apos;/gi,"'").replace(/&lt;/gi,'<').replace(/&gt;/gi,'>').replace(/&#(x[\da-f]+|\d+);/gi,(_,n)=>{const code=n[0].toLowerCase()==='x'?parseInt(n.slice(1),16):Number(n);return code>0&&code<=0x10ffff?String.fromCodePoint(code):'';});}
export function parseMeta(html,url){
 const values={};for(const tag of html.match(/<meta\b[^>]*>/gi)||[]){const attrs={};for(const m of tag.matchAll(/([\w:-]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))/g))attrs[m[1].toLowerCase()]=decode(m[2]??m[3]??m[4]??'');const key=(attrs.property||attrs.name||'').toLowerCase();if(attrs.content&&!values[key])values[key]=attrs.content;}
 return {url,thumbnail:imageUrl(values['og:image:secure_url']||values['og:image']||values['twitter:image']||values['twitter:image:src']||'',url),title:(values['og:title']||values['twitter:title']||decode(html.match(/<title[^>]*>([^<]*)<\/title>/i)?.[1]||'')).trim().slice(0,500)};
}
async function limitedText(response){const reader=response.body?.getReader();if(!reader)return '';const chunks=[];let total=0;try{while(true){const {value,done}=await reader.read();if(done)break;total+=value.length;if(total>1500000)break;chunks.push(value);}}finally{await reader.cancel();}const bytes=new Uint8Array(chunks.reduce((n,c)=>n+c.length,0));let offset=0;for(const c of chunks){bytes.set(c,offset);offset+=c.length;}return new TextDecoder().decode(bytes);}
export async function preview(value,fetcher=fetch){
 if(!allowed(value))throw Error('unsupported_url');
 let url=new URL(value).href;
 const request=async target=>{if(!allowed(target))throw Error('unsupported_redirect');return fetcher(target,{redirect:'manual',signal:AbortSignal.timeout(7000),headers:{'User-Agent':'Mozilla/5.0','Accept':'text/html,application/json','Referer':'https://www.bilibili.com/'}});};
 for(let i=0;i<5;i++){
  const u=new URL(url),bv=u.hostname.endsWith('bilibili.com')&&u.pathname.match(/\/(BV[\da-zA-Z]+)(?:\/|$)/)?.[1];
  if(bv){try{const response=await request('https://api.bilibili.com/x/web-interface/view?bvid='+encodeURIComponent(bv));const data=JSON.parse(await limitedText(response));if(data.code===0&&data.data?.pic)return {url,thumbnail:imageUrl(data.data.pic,url),title:String(data.data.title||'').slice(0,500)};}catch{}}
  const response=await request(url);
  if([301,302,303,307,308].includes(response.status)){const next=response.headers.get('location');if(!next)break;url=new URL(next,url).href;if(!allowed(url))throw Error('unsupported_redirect');continue;}
  if(!response.ok)throw Error('upstream_unavailable');return parseMeta(await limitedText(response),url);
 }
 return {url,thumbnail:'',title:''};
}
