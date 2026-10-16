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

/** Fetch public video metadata over the phone's connection, without account cookies. */
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
    static JSONObject fetch(String value) {
        try{
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
    static void install(Activity activity,WebView web) {
        if(!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER))return;
        WebViewCompat.addWebMessageListener(web,"ShiguangPreviewNative",Collections.singleton("https://appassets.androidplatform.net"),(v,message,origin,main,reply)->{
            if(!main||!"https://appassets.androidplatform.net".equals(origin.toString()))return;
            try{String raw=message.getData();if(raw==null||raw.length()>3000)return;JSONObject input=new JSONObject(raw);String id=input.getString("id"),url=input.getString("url");
                new Thread(()->{JSONObject result=fetch(url);try{result.put("id",id);}catch(Exception ignored){}activity.runOnUiThread(()->{if(!activity.isFinishing()&&!activity.isDestroyed())reply.postMessage(result.toString());});},"shiguang-preview").start();
            }catch(Exception ignored){}
        });
    }
}
