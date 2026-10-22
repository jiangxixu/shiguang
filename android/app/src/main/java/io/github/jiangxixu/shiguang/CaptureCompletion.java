package io.github.jiangxixu.shiguang;

import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.webkit.WebView;
import java.util.IdentityHashMap;
import java.util.Map;

/** Bounded ownership of post-save work. The collection is already durable before handoff. */
final class CaptureCompletion {
    private static final Handler handler=new Handler(Looper.getMainLooper());
    private static final Map<WebView,Runnable> running=new IdentityHashMap<>();
    static void retain(WebView web){
        if(web.getParent() instanceof ViewGroup)((ViewGroup)web.getParent()).removeView(web);
        Runnable timeout=()->finish(web);
        running.put(web,timeout);
        handler.postDelayed(timeout,30000);
    }
    static boolean owns(WebView web){return running.containsKey(web);}
    static void finish(WebView web){
        Runnable timeout=running.remove(web);if(timeout==null)return;
        handler.removeCallbacks(timeout);web.stopLoading();web.destroy();
    }
}
