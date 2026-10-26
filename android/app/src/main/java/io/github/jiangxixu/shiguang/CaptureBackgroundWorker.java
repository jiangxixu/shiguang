package io.github.jiangxixu.shiguang;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.*;
import androidx.annotation.NonNull;
import androidx.webkit.*;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;

/** Fresh, windowless JS engine shares the library origin/auth, not the editor lifetime. */
public final class CaptureBackgroundWorker extends Worker {
    private static final String ORIGIN="https://appassets.androidplatform.net";
    private static final String HOME=ORIGIN+"/assets/background-capture.html";
    private final Handler main=new Handler(Looper.getMainLooper());
    private final CountDownLatch completion=new CountDownLatch(1);
    private final AtomicBoolean closed=new AtomicBoolean();
    private volatile boolean retry=true;
    private WebView web;
    public CaptureBackgroundWorker(@NonNull Context context,@NonNull WorkerParameters params){super(context,params);}
    @NonNull @Override public Result doWork(){
        main.post(this::start);
        try{completion.await(180,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}
        stop();
        return retry?Result.retry():Result.success();
    }
    private void start(){
        if(closed.get()||isStopped())return;
        try{
            if(!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)){completion.countDown();return;}
            Context app=getApplicationContext();
            web=new WebView(app);
            WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);
            settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
            WebViewAssetLoader loader=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(app)).build();
            WebViewCompat.addWebMessageListener(web,"ShiguangWorkResult",Collections.singleton(ORIGIN),(v,message,origin,frame,reply)->{
                if(closed.get()||!frame||!ORIGIN.equals(origin.toString()))return;
                try{retry=new JSONObject(message.getData()).optBoolean("retry",true);}catch(Exception ignored){}
                completion.countDown();
            });
            WebViewCompat.addWebMessageListener(web,"ShiguangPreviewNative",Collections.singleton(ORIGIN),(v,message,origin,frame,reply)->{
                if(closed.get()||!frame||!ORIGIN.equals(origin.toString()))return;
                try{
                    String raw=message.getData();if(raw==null||raw.length()>3000)return;
                    JSONObject input=new JSONObject(raw);String id=input.getString("id"),url=input.getString("url");
                    boolean session=XhsMetadataLoginActivity.enabled(app);
                    new Thread(()->{
                        JSONObject result=PreviewBridge.fetch(url,session);
                        try{result.put("id",id);}catch(Exception ignored){}
                        main.post(()->{if(!closed.get())try{reply.postMessage(result.toString());}catch(Exception ignored){}});
                    },"capture-background-preview").start();
                }catch(Exception ignored){}
            });
            web.setWebViewClient(new WebViewClient(){
                @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest request){
                    WebResourceResponse response=loader.shouldInterceptRequest(request.getUrl());if(response!=null)return response;
                    if("https".equals(request.getUrl().getScheme())&&"pkgsgkrjhcckmcjqdzws.supabase.co".equals(request.getUrl().getHost()))return null;
                    return new WebResourceResponse("text/plain","UTF-8",new java.io.ByteArrayInputStream(new byte[0]));
                }
                @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest request){return true;}
                @Override public void onReceivedError(WebView v,WebResourceRequest request,WebResourceError error){if(request.isForMainFrame())completion.countDown();}
                @Override public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail detail){completion.countDown();stop();return true;}
            });
            web.loadUrl(HOME);
        }catch(RuntimeException e){completion.countDown();stop();}
    }
    private void stop(){
        closed.set(true);
        main.post(()->{if(web!=null){try{web.stopLoading();web.destroy();}catch(RuntimeException ignored){}web=null;}});
    }
    @Override public void onStopped(){stop();completion.countDown();}
}
