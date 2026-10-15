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
        LinearLayout bar = new LinearLayout(this);
        bar.setPadding(12,2,12,2);
        addButton(bar,"↻ 刷新",()->web.reload());
        addButton(bar,"↗ 网页版",()->external(Uri.parse(BuildConfig.WEB_URL)));
        addButton(bar,"关于",()->new android.app.AlertDialog.Builder(this).setTitle("拾光 "+BuildConfig.VERSION_NAME).setMessage("独立 Android 应用，知识库可离线使用。\n\n网页与 App 的收藏各自保存在设备上，使用导出/导入备份迁移，当前没有账号登录与云端同步。卸载或清除 App 数据前务必备份。\n\n其他 App 中可选择分享链接到「拾光」，检查后保存。源码与更新：github.com/jiangxixu/shiguang").setPositiveButton("知道了",null).show());
        layout.addView(bar); setContentView(layout);
        WebSettings settings=web.getSettings();
        settings.setJavaScriptEnabled(true); settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false); settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        WebViewAssetLoader loader=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
        web.setWebViewClient(new WebViewClient(){
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){return loader.shouldInterceptRequest(r.getUrl());}
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){Uri u=r.getUrl();if(isLocal(u))return false;external(u);return true;}
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
                    JSONObject data=new JSONObject(raw);String name=data.getString("name");
                    if(!name.matches("[^/\\\\]{1,120}\\.(json|csv)"))throw new Exception();
                    if(pendingExport!=null){Toast.makeText(this,"请先完成上一次文件保存",Toast.LENGTH_SHORT).show();return;}
                    pendingExport=data.getString("content").getBytes(StandardCharsets.UTF_8);
                    Intent create=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(name.endsWith(".csv")?"text/csv":"application/json").putExtra(Intent.EXTRA_TITLE,name);
                    startActivityForResult(create,11);
                }catch(Exception e){pendingExport=null;Toast.makeText(this,"无法导出，请更新 Android System WebView 后重试",Toast.LENGTH_LONG).show();}
            });
        }
        web.setDownloadListener((url,ua,disposition,mime,length)->{if(url.startsWith("https://"))external(Uri.parse(url));else Toast.makeText(this,"请更新 Android System WebView 后重试导出",Toast.LENGTH_LONG).show();});
        loadIntent(getIntent());
    }
    private boolean isLocal(Uri u){return "https".equals(u.getScheme())&&"appassets.androidplatform.net".equals(u.getHost());}
    private void addButton(LinearLayout bar,String title,Runnable action){Button b=new Button(this);b.setText(title);b.setTextSize(11);b.setAllCaps(false);b.setOnClickListener(v->action.run());bar.addView(b,new LinearLayout.LayoutParams(0,-2,1));}
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
    @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
    @Override protected void onDestroy(){if(fileCallback!=null)fileCallback.onReceiveValue(null);web.destroy();super.onDestroy();}
}
