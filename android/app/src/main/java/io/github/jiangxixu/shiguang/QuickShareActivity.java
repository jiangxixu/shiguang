package io.github.jiangxixu.shiguang;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.*;
import android.widget.Toast;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import java.util.Collections;
import org.json.JSONObject;
import org.json.JSONArray;

/** Shows optional categories and reason before saving a shared link. */
public class QuickShareActivity extends Activity {
    private static final String HOME="https://appassets.androidplatform.net/assets/quick-share.html";
    private WebView web;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean done=false,started=false,locallySaved=false;
    private final Runnable timeout=()->complete(locallySaved?"已保存在手机，打开拾光后会重试云端同步":"保存超时，请重新分享",locallySaved);

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        Intent intent=getIntent();
        if(!Intent.ACTION_SEND.equals(intent.getAction())){finish();return;}
        ShareTargets.used(this);
        String text=intent.getStringExtra(Intent.EXTRA_TEXT);
        if(text==null||text.length()>100000){complete("分享内容中未找到链接",false);return;}
        java.util.List<String> links=ShareLinks.candidates(text);
        if(links.isEmpty()){complete("分享内容中未找到有效链接，请选择复制链接或分享链接",false);return;}
        if(links.size()>1){
            String[] choices=new String[links.size()];for(int i=0;i<links.size();i++)choices[i]=ShareLinks.platformName(links.get(i))+"\n"+links.get(i);
            new android.app.AlertDialog.Builder(this).setTitle("复制内容里有多个链接，请选择要收藏的内容")
                .setItems(choices,(dialog,index)->showCapture(links.get(index),text,sharedSubject(intent)))
                .setNegativeButton("取消",(dialog,index)->finish()).setOnCancelListener(dialog->finish()).show();return;
        }
        showCapture(links.get(0),text,sharedSubject(intent));
    }
    private String sharedSubject(Intent intent){CharSequence subject=intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT);if(subject==null||subject.toString().trim().isEmpty())subject=intent.getCharSequenceExtra(Intent.EXTRA_TITLE);return subject==null?"":subject.toString();}
    private void showCapture(String link,String text,String subject){
        String title=subject==null?"":subject.trim();
        if(title.isEmpty())title=ShareLinks.sharedTitle(text);
        if(title.isEmpty())title=text.replace(link,"").replaceAll("^[\\s\\-—:：]+|[\\s\\-—:：]+$","");
        if(title.isEmpty())title=Uri.parse(link).getHost();
        JSONObject item=new JSONObject();
        try{item.put("url",link);item.put("title",title==null?"收藏链接":title.substring(0,Math.min(500,title.length())));}catch(Exception e){complete("无法读取分享内容",false);return;}
        String payload=item.toString();
        web=new WebView(this);web.setBackgroundColor(android.graphics.Color.rgb(246,245,240));setContentView(web);
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);getWindow().setDimAmount(0.3f);
        getWindow().setGravity(android.view.Gravity.BOTTOM);
        getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT,Math.min(Math.round(650*getResources().getDisplayMetrics().density),(int)(getResources().getDisplayMetrics().heightPixels*0.9)));
        WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        WebViewAssetLoader loader=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
        final boolean canReport=WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER);
        if(canReport)WebViewCompat.addWebMessageListener(web,"ShiguangQuickResult",Collections.singleton("https://appassets.androidplatform.net"),(v,message,origin,mainFrame,reply)->{
            if(!mainFrame||!"https://appassets.androidplatform.net".equals(origin.toString())||done)return;
            try{
                JSONObject report=new JSONObject(message.getData());
                if(report.optBoolean("cancel")){setResult(RESULT_CANCELED);finish();return;}
                if(report.optBoolean("saved")){locallySaved=true;handler.postDelayed(timeout,10000);}
                if(report.optBoolean("done"))complete(report.optBoolean("duplicate")?"拾光中已收藏过这条内容":report.optBoolean("cloud")?"已收进拾光并同步到云端":"已保存在手机，联网登录后自动同步",locallySaved);
                if(report.optBoolean("error"))complete("未保存：存储异常，请打开拾光检查或备份",false);
            }catch(Exception ignored){}
        });
        web.setWebViewClient(new WebViewClient(){
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest request){WebResourceResponse response=loader.shouldInterceptRequest(request.getUrl());if(response!=null)return response;if("https".equals(request.getUrl().getScheme())&&"pkgsgkrjhcckmcjqdzws.supabase.co".equals(request.getUrl().getHost()))return null;return new WebResourceResponse("text/plain","UTF-8",new java.io.ByteArrayInputStream(new byte[0]));}
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest request){return true;}
            @Override public void onPageFinished(WebView v,String url){
                if(!HOME.equals(url)||done||started)return;started=true;
                if(!canReport){complete("请更新 Android System WebView 后使用分类收藏",false);return;}
                v.evaluateJavascript("window.shiguangReceiveShare("+payload+")",null);
            }
        });
        PreviewBridge.install(this,web);
        web.loadUrl(HOME);
    }
    private void complete(String message,boolean success){
        if(done)return;done=true;handler.removeCallbacks(timeout);
        Toast.makeText(this,message,success?Toast.LENGTH_SHORT:Toast.LENGTH_LONG).show();
        setResult(success?RESULT_OK:RESULT_CANCELED);finish();
    }
    @Override protected void onDestroy(){handler.removeCallbacks(timeout);if(web!=null){web.stopLoading();web.destroy();}super.onDestroy();}
}
