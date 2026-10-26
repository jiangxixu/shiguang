package io.github.jiangxixu.shiguang;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import androidx.work.*;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

/** Schedules durable work; never owns or retains an editor WebView. */
final class CaptureBackgroundWork {
    private static final ExecutorService QUEUE=Executors.newSingleThreadExecutor();
    private static Constraints network(){return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();}
    static void install(Context context,WebView web){
        if(!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER))return;
        Context app=context.getApplicationContext();
        WebViewCompat.addWebMessageListener(web,"ShiguangBackground",Collections.singleton("https://appassets.androidplatform.net"),(v,message,origin,main,reply)->{
            if(!main||!"https://appassets.androidplatform.net".equals(origin.toString()))return;
            try{
                String raw=message.getData();if(raw==null||raw.length()>200)return;
                String id=new JSONObject(raw).getString("id");if(id.length()>32)return;
                QUEUE.execute(()->{
                    boolean ok=false;
                    try{
                        WorkManager manager=WorkManager.getInstance(app);
                        manager.enqueueUniquePeriodicWork("capture-recovery",ExistingPeriodicWorkPolicy.KEEP,
                            new PeriodicWorkRequest.Builder(CaptureBackgroundWorker.class,1,TimeUnit.HOURS).setConstraints(network()).build()).getResult().get(6,TimeUnit.SECONDS);
                        // Independent wakeups do not wait behind an older network retry.
                        // The runner serializes drains with a same-origin Web Lock.
                        manager.enqueue(
                            new OneTimeWorkRequest.Builder(CaptureBackgroundWorker.class).setConstraints(network())
                                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build()).getResult().get(6,TimeUnit.SECONDS);
                        ok=true;
                    }catch(Exception ignored){}
                    final boolean success=ok;
                    new Handler(Looper.getMainLooper()).post(()->{try{reply.postMessage(new JSONObject().put("id",id).put("ok",success).toString());}catch(Exception ignored){}});
                });
            }catch(Exception ignored){}
        });
    }
}
