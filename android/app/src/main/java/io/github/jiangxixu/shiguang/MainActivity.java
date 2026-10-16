package io.github.jiangxixu.shiguang;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.*;
import android.widget.*;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import org.json.JSONObject;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final String ORIGIN = "https://appassets.androidplatform.net";
    private static final String HOME = ORIGIN + "/assets/index.html";
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private byte[] pendingExport;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.rgb(246,245,240));
        web = new WebView(this);
        layout.addView(web, new LinearLayout.LayoutParams(-1,0,1));
        setContentView(layout);
        ShareTargets.publish(this);
        WebSettings settings=web.getSettings();
        settings.setJavaScriptEnabled(true); settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false); settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        WebViewAssetLoader loader=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
        web.setWebViewClient(new WebViewClient(){
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){return loader.shouldInterceptRequest(r.getUrl());}
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){Uri u=r.getUrl();if(isLocal(u)){if("/assets/".equals(u.getPath())||"/".equals(u.getPath())){v.loadUrl(HOME);return true;}return false;}external(u);return true;}
            @Override public void onReceivedSslError(WebView v,android.webkit.SslErrorHandler h,android.net.http.SslError error){h.cancel();}
        });
        web.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> cb,FileChooserParams params){
                if(fileCallback!=null)fileCallback.onReceiveValue(null);fileCallback=cb;
                Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
                try{startActivityForResult(pick,10);return true;}catch(Exception e){fileCallback=null;return false;}
            }
        });
        if(WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)){
            WebViewCompat.addWebMessageListener(web,"ShiguangNative",Collections.singleton(ORIGIN),(v,message,sourceOrigin,isMainFrame,reply)->{
                if(!isMainFrame||!ORIGIN.equals(sourceOrigin.toString()))return;
                try{
                    String raw=message.getData();if(raw==null||raw.length()>16*1024*1024)throw new Exception();
                    JSONObject data=new JSONObject(raw);
                    if("paste".equals(data.optString("action"))){
                        android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                        String text="";if(clipboard.hasPrimaryClip()&&clipboard.getPrimaryClip().getItemCount()>0){CharSequence value=clipboard.getPrimaryClip().getItemAt(0).coerceToText(this);if(value!=null)text=value.toString();}
                        JSONObject response=new JSONObject();response.put("clipboard",text.substring(0,Math.min(10000,text.length())));reply.postMessage(response.toString());return;
                    }
                    String name=data.getString("name");
                    if(!name.matches("[^/\\\\]{1,120}\\.(json|csv)"))throw new Exception();
                    if(pendingExport!=null){Toast.makeText(this,"请先完成上一次文件保存",Toast.LENGTH_SHORT).show();return;}
                    pendingExport=data.getString("content").getBytes(StandardCharsets.UTF_8);
                    Intent create=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(name.endsWith(".csv")?"text/csv":"application/json").putExtra(Intent.EXTRA_TITLE,name);
                    startActivityForResult(create,11);
                }catch(Exception e){pendingExport=null;Toast.makeText(this,"无法导出，请更新 Android System WebView 后重试",Toast.LENGTH_LONG).show();}
            });
        }
        web.setDownloadListener((url,ua,disposition,mime,length)->{if(url.startsWith("https://"))external(Uri.parse(url));else Toast.makeText(this,"请更新 Android System WebView 后重试导出",Toast.LENGTH_LONG).show();});
        PreviewBridge.install(this,web);
        loadIntent(getIntent());
    }
    private boolean isLocal(Uri u){return "https".equals(u.getScheme())&&"appassets.androidplatform.net".equals(u.getHost());}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void showAbout(){new android.app.AlertDialog.Builder(this).setTitle("拾光 "+BuildConfig.VERSION_NAME).setMessage("在 App 和网站使用同一个邮箱登录，即可同步收藏。离线时先保存在本机，打开 App 或再次分享时补传。\n\n在其他 App 中选择分享链接到「拾光」，即可保存。\n\n更新：github.com/jiangxixu/shiguang\n卸载或清除数据前，请确认同步完成或导出备份。").setPositiveButton("知道了",null).show();}
    private void external(Uri u){if(!"https".equals(u.getScheme())&&!"http".equals(u.getScheme()))return;try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception ignored){Toast.makeText(this,"没有可打开链接的浏览器",Toast.LENGTH_SHORT).show();}}
    private void loadIntent(Intent intent){
        if(Intent.ACTION_SEND.equals(intent.getAction())){
            String text=intent.getStringExtra(Intent.EXTRA_TEXT);if(text!=null){
                Matcher m=Pattern.compile("https?://[^\\s<>\\\"，。；）】]+").matcher(text);
                if(m.find()){String title=intent.getStringExtra(Intent.EXTRA_SUBJECT);if(title==null)title="";
                    web.loadUrl(HOME+"?url="+Uri.encode(m.group())+"&title="+Uri.encode(title)+"&note="+Uri.encode(text.substring(0,Math.min(text.length(),5000))));return;}
            }Toast.makeText(this,"分享内容中未找到网页链接，请复制链接后添加",Toast.LENGTH_LONG).show();
        }web.loadUrl(HOME);
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);loadIntent(intent);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
        if(request==10&&fileCallback!=null){fileCallback.onReceiveValue(result==RESULT_OK&&data!=null&&data.getData()!=null?new Uri[]{data.getData()}:null);fileCallback=null;}
        if(request==11){byte[] bytes=pendingExport;pendingExport=null;if(result==RESULT_OK&&data!=null&&data.getData()!=null&&bytes!=null){try(OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out==null)throw new Exception();out.write(bytes);Toast.makeText(this,"备份已保存",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"保存失败，请重试导出备份",Toast.LENGTH_LONG).show();}}}
    }
    @Override protected void onResume(){super.onResume();if(web!=null)web.evaluateJavascript("window.dispatchEvent(new Event('shiguang-refresh'));window.ShiguangCloud?.schedule()",null);}
    @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
    @Override protected void onDestroy(){if(fileCallback!=null)fileCallback.onReceiveValue(null);web.destroy();super.onDestroy();}
}
