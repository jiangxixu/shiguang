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
    private boolean awaitingOverlayPermission;
    private ValueCallback<Uri[]> fileCallback;
    private byte[] pendingExport;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        awaitingOverlayPermission=state!=null&&state.getBoolean("awaitingOverlayPermission",false);
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
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){Uri u=r.getUrl();if(isLocal(u)&&("/assets/".equals(u.getPath())||"/".equals(u.getPath())))u=Uri.parse(HOME);return loader.shouldInterceptRequest(u);}
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){Uri u=r.getUrl();if(isLocal(u)){if("/assets/".equals(u.getPath())||"/".equals(u.getPath())){v.loadUrl(HOME);return true;}return false;}external(u);return true;}
            @Override public void onPageFinished(WebView v,String url){if(isLocal(Uri.parse(url)))v.evaluateJavascript("document.documentElement.dataset.appVersion="+JSONObject.quote(BuildConfig.VERSION_NAME),null);}
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
                    String action=data.optString("action");
                    if("enableFloating".equals(action)){enableFloating();return;}
                    if("disableFloating".equals(action)){stopService(new Intent(this,FloatingCaptureService.class));Toast.makeText(this,"悬浮收藏已关闭",Toast.LENGTH_SHORT).show();return;}
                    if("captureClipboard".equals(action)){startActivity(new Intent(this,ClipboardCaptureActivity.class));return;}
                    if("pinCapture".equals(action)){ShareTargets.pin(this);return;}
                    if("addCaptureTile".equals(action)){addCaptureTile();return;}
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
    private void enableFloating(){
        if(android.provider.Settings.canDrawOverlays(this)){startFloating();return;}
        new android.app.AlertDialog.Builder(this).setTitle("开启悬浮收藏")
            .setMessage("请在下一页为拾光允许「显示在其他应用上层」。开启后：复制链接 → 点悬浮球 → 分类保存。拖到左右边缘可收起，长按可关闭。不会后台读取剪贴板。")
            .setNegativeButton("暂不开启",null).setPositiveButton("前往允许",(dialog,which)->{
                awaitingOverlayPermission=true;
                try{startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));}
                catch(Exception e){awaitingOverlayPermission=false;Toast.makeText(this,"请到系统设置 → 应用 → 拾光，允许显示在其他应用上层",Toast.LENGTH_LONG).show();}
            }).show();
    }
    private void startFloating(){
        try{
            if(android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},12);
            startForegroundService(new Intent(this,FloatingCaptureService.class));
            Toast.makeText(this,"悬浮球已开启：拖到边缘收起，点把手展开，长按关闭",Toast.LENGTH_LONG).show();
        }catch(Exception e){Toast.makeText(this,"开启失败，请检查悬浮窗权限后重试",Toast.LENGTH_LONG).show();}
    }
    @Override protected void onSaveInstanceState(Bundle state){state.putBoolean("awaitingOverlayPermission",awaitingOverlayPermission);super.onSaveInstanceState(state);}
    private void addCaptureTile(){
        if(android.os.Build.VERSION.SDK_INT>=33){
            try{
                getSystemService(android.app.StatusBarManager.class).requestAddTileService(new android.content.ComponentName(this,CaptureTileService.class),"拾光收藏",android.graphics.drawable.Icon.createWithResource(this,R.drawable.ic_capture_tile),getMainExecutor(),result->{
                    boolean added=result==android.app.StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED||result==android.app.StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED;
                    Toast.makeText(this,added?"已添加：复制链接后，下拉点「拾光收藏」":"未添加。可下拉两次 → 编辑按钮 → 拖入「拾光收藏」",Toast.LENGTH_LONG).show();
                });return;
            }catch(Exception ignored){}
        }
        new android.app.AlertDialog.Builder(this).setTitle("添加拾光收藏按钮").setMessage("从屏幕顶部下拉两次，点击编辑按钮（铅笔或更多），找到「拾光收藏」并拖到常用区域。以后复制链接，下拉点它即可选择分类并收藏。").setPositiveButton("知道了",null).show();
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
    @Override protected void onResume(){super.onResume();if(awaitingOverlayPermission){awaitingOverlayPermission=false;if(android.provider.Settings.canDrawOverlays(this))startFloating();else Toast.makeText(this,"尚未允许悬浮窗，可在「我」中重新开启",Toast.LENGTH_LONG).show();}if(web!=null)web.evaluateJavascript("window.dispatchEvent(new Event('shiguang-refresh'));window.ShiguangCloud?.schedule()",null);}
    @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
    @Override protected void onDestroy(){if(fileCallback!=null)fileCallback.onReceiveValue(null);web.destroy();super.onDestroy();}
}
