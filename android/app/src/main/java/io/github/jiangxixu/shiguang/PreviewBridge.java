package io.github.jiangxixu.shiguang;

import android.app.Activity;
import android.webkit.WebView;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import org.json.JSONObject;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Phone metadata fetch; XHS web cookies are used only after explicit local authorization. */
final class PreviewBridge {
    private static boolean allowed(URL u) {
        String host=u.getHost().toLowerCase(java.util.Locale.ROOT);
        return "https".equals(u.getProtocol())&&u.getUserInfo()==null&&(u.getPort()==-1||u.getPort()==443)&&
            (host.equals("b23.tv")||host.endsWith(".b23.tv")||host.equals("bilibili.com")||host.endsWith(".bilibili.com"));
    }
    private static String read(HttpsURLConnection c) throws Exception {
        try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>1500000)throw new Exception("size");out.write(buffer,0,n);}return new String(out.toByteArray(),StandardCharsets.UTF_8);
        }
    }
    private static HttpsURLConnection connect(URL url) throws Exception {
        if(!allowed(url))throw new Exception("host");
        HttpsURLConnection c=(HttpsURLConnection)url.openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(4000);c.setReadTimeout(4000);
        c.setRequestProperty("User-Agent","Mozilla/5.0");c.setRequestProperty("Referer","https://www.bilibili.com/");return c;
    }
    static JSONObject fetch(String value,boolean xhsSession) {
        try{
            if(XhsLinks.allowed(value))return xhs(value,xhsSession);
            if(!ContentPreviewUrls.family(value).isEmpty())return content(value);
            if(value.length()>2000)return new JSONObject();URL url=new URL(value);
            for(int i=0;i<5;i++){
                if(!allowed(url))return new JSONObject();
                Matcher bv=Pattern.compile("/(BV[0-9A-Za-z]+)(?:/|$)").matcher(url.getPath());
                if(url.getHost().endsWith("bilibili.com")&&bv.find()){
                    HttpsURLConnection c=connect(new URL("https://api.bilibili.com/x/web-interface/view?bvid="+bv.group(1)));
                    try{JSONObject body=new JSONObject(read(c));if(body.optInt("code",-1)==0){JSONObject data=body.getJSONObject("data");return new JSONObject().put("thumbnail",data.optString("pic").replaceFirst("^http:","https:")).put("title",data.optString("title"));}}finally{c.disconnect();}
                    return new JSONObject();
                }
                HttpsURLConnection c=connect(url);
                try{int status=c.getResponseCode();if(status>=300&&status<400){String next=c.getHeaderField("Location");if(next==null)return new JSONObject();url=new URL(url,next);if("http".equals(url.getProtocol()))url=new URL(url.toString().replaceFirst("^http:","https:"));continue;}return new JSONObject();}finally{c.disconnect();}
            }
        }catch(Exception ignored){}
        return new JSONObject();
    }
    private static JSONObject content(String value) throws Exception {
        String family=ContentPreviewUrls.family(value),current=value;
        if(family.equals("youtube"))current="https://www.youtube.com/oembed?url="+java.net.URLEncoder.encode(value,"UTF-8")+"&format=json";
        long deadline=android.os.SystemClock.elapsedRealtime()+10000;
        for(int hop=0;hop<5;hop++){
            if(family.equals("wechat")&&!FavoriteRules.contentUrl("com.tencent.mm",current))return new JSONObject();
            if(!ContentPreviewUrls.redirect(value,current))return new JSONObject();
            long remaining=deadline-android.os.SystemClock.elapsedRealtime();if(remaining<=0)return new JSONObject();
            HttpsURLConnection c=(HttpsURLConnection)new URL(current).openConnection();c.setInstanceFollowRedirects(false);
            c.setConnectTimeout((int)Math.min(3000,remaining));c.setReadTimeout((int)Math.min(3000,remaining));c.setRequestProperty("User-Agent",(family.equals("douyin")||family.equals("wechat"))?"Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/130.0.0.0 Mobile Safari/537.36":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/130.0.0.0 Safari/537.36");
            try{
                int status=c.getResponseCode();
                if(status>=300&&status<400){String location=c.getHeaderField("Location");if(location==null)return new JSONObject();current=new URL(new URL(current),location).toString();continue;}
                if(status!=200)return new JSONObject();
                String body;
                try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                    byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>2000000||android.os.SystemClock.elapsedRealtime()>=deadline)return new JSONObject();out.write(buffer,0,n);}body=new String(out.toByteArray(),StandardCharsets.UTF_8);
                }
                String title,image;
                if(family.equals("youtube")){JSONObject data=new JSONObject(body);title=data.optString("title","");image=data.optString("thumbnail_url","");}
                else{java.util.Map<String,String> meta=PublicMetadata.parse(body);title=meta.get("title");image=meta.get("thumbnail");}
                if(family.equals("wechat")){java.util.Map<String,String> meta=WechatArticle.metadata(body);title=meta.get("title");image=meta.get("thumbnail");}
                boolean matched=false;
                if(family.equals("douyin")){
                    JSONObject exact=DouyinMetadata.parse(body,current);matched=exact.optBoolean("matched");
                    if(!exact.optString("title").isEmpty())title=exact.optString("title");
                    if(!exact.optString("thumbnail").isEmpty())image=exact.optString("thumbnail");
                }
                title=ContentPreviewUrls.title(title);if(title.isEmpty()&&!matched)return new JSONObject();
                JSONObject result=new JSONObject();if(!title.isEmpty())result.put("title",title);
                if(image!=null&&!image.isEmpty()){image=new URL(new URL(current),image).toString().replaceFirst("(?i)^http:","https:");URL u=new URL(image);if("https".equals(u.getProtocol())&&u.getUserInfo()==null&&(u.getPort()==-1||u.getPort()==443))result.put("thumbnail",image);}
                return result;
            }finally{c.disconnect();}
        }
        return new JSONObject();
    }
    private static JSONObject xhs(String value,boolean session) throws Exception {
        long deadline=android.os.SystemClock.elapsedRealtime()+10000;String current=value;
        for(int hop=0;hop<5;hop++){
            if(!XhsLinks.allowed(current))return new JSONObject();
            if(!XhsLinks.shortLink(current)&&XhsLinks.noteId(current)==null)return new JSONObject().put("requiresLogin",true);
            long remaining=deadline-android.os.SystemClock.elapsedRealtime();if(remaining<=0)return new JSONObject();
            HttpsURLConnection c=(HttpsURLConnection)new URL(current).openConnection();
            c.setInstanceFollowRedirects(false);c.setConnectTimeout((int)Math.min(3000,remaining));c.setReadTimeout((int)Math.min(3000,remaining));
            c.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36");
            c.setRequestProperty("Accept","text/html,application/xhtml+xml");
            c.setRequestProperty("Referer","https://www.xiaohongshu.com/");
            if(session){String cookies=android.webkit.CookieManager.getInstance().getCookie(current);if(cookies!=null&&!cookies.isEmpty())c.setRequestProperty("Cookie",cookies);}
            try{
                int status=c.getResponseCode();
                if(status>=300&&status<400){String next=c.getHeaderField("Location");if(next==null)return new JSONObject();current=new URL(new URL(current),next).toString().replaceFirst("(?i)^http:","https:");continue;}
                if(status!=200||XhsLinks.noteId(current)==null)return new JSONObject();
                String html;
                try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                    byte[] buffer=new byte[8192];int n;
                    while((n=in.read(buffer))!=-1){if(out.size()+n>3000000||android.os.SystemClock.elapsedRealtime()>=deadline)return new JSONObject();out.write(buffer,0,n);}
                    html=new String(out.toByteArray(),StandardCharsets.UTF_8);
                }
                java.util.Map<String,String> meta=PublicMetadata.parse(html);String title=meta.get("title"),image=meta.get("thumbnail");boolean matchedNote=false;
                JSONObject exact=XhsNoteMetadata.parse(html,XhsLinks.noteId(current));
                matchedNote=exact.optBoolean("matched");
                if(!exact.optString("title").isEmpty())title=exact.optString("title");
                if(!exact.optString("thumbnail").isEmpty())image=exact.optString("thumbnail");
                // Login/error pages must not replace a saved note title or masquerade as its cover.
                if(title==null||title.trim().isEmpty())return new JSONObject();
                if(!matchedNote&&title.matches("(?i)(?:小红书登录|登录小红书|安全验证|访问频繁|页面不存在|内容不存在|Sign.?in).*"))return new JSONObject().put("requiresLogin",true);
                if(title.matches("(?:小红书|小红书\\s*[-–—]\\s*你的生活指南|小红书\\s*[-–—]\\s*你的生活兴趣社区)"))return new JSONObject().put("requiresLogin",true);
                JSONObject result=new JSONObject().put("title",title.substring(0,Math.min(title.length(),500)));
                if(image!=null&&!image.isEmpty()){
                    image=image.replaceFirst("^//","https://").replaceFirst("(?i)^http:","https:");
                    URL u=new URL(image);if("https".equals(u.getProtocol())&&u.getUserInfo()==null&&(u.getPort()==-1||u.getPort()==443))result.put("thumbnail",image);
                }
                return result;
            }finally{c.disconnect();}
        }
        return new JSONObject();
    }
    static void install(android.content.Context activity,WebView web) {
        if(!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER))return;
        WebViewCompat.addWebMessageListener(web,"ShiguangPreviewNative",Collections.singleton("https://appassets.androidplatform.net"),(v,message,origin,main,reply)->{
            if(!main||!"https://appassets.androidplatform.net".equals(origin.toString()))return;
            try{String raw=message.getData();if(raw==null||raw.length()>3000)return;JSONObject input=new JSONObject(raw);String id=input.getString("id"),url=input.getString("url");
                boolean xhsSession=XhsMetadataLoginActivity.enabled(activity);
                new Thread(()->{JSONObject result=fetch(url,xhsSession);try{result.put("id",id);}catch(Exception ignored){}new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{if(web.isAttachedToWindow()&&(!(activity instanceof Activity)||(!((Activity)activity).isFinishing()&&!((Activity)activity).isDestroyed())))try{reply.postMessage(result.toString());}catch(Exception ignored){}});},"shiguang-preview").start();
            }catch(Exception ignored){}
        });
    }
}
