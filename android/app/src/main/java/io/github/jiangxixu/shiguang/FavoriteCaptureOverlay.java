package io.github.jiangxixu.shiguang;

import android.accessibilityservice.AccessibilityService;
import android.content.*;
import android.graphics.PixelFormat;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import androidx.webkit.*;
import org.json.JSONObject;
import java.util.Collections;

/** Visible, focusable classification window. Clipboard reads only occur while it has focus. */
final class FavoriteCaptureOverlay {
    interface Result { void finish(String message); }
    private static final String HOME="https://appassets.androidplatform.net/assets/quick-share.html";
    private final AccessibilityService service;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Result result;
    private final Runnable openActivity;
    private LinearLayout container;
    private WebView web;
    private TextView status;
    private Button fallback;
    private boolean closed,reading,loaded,started,saved;
    private String payload;
    private int attempts;
    private boolean pendingMode,resolving;
    private Runnable requestLink;
    private String readPackage,readTitle;
    private long readAfter;
    private WindowManager.LayoutParams windowParams;
    private boolean attached;
    FavoriteCaptureOverlay(AccessibilityService service,Result result,Runnable openActivity){this.service=service;this.result=result;this.openActivity=openActivity;}
    boolean showPending(String pkg,String title,Runnable request){pendingMode=true;requestLink=request;return show(pkg,-1,title);}
    boolean show(String pkg,long notBefore,String title){
        readPackage=pkg;readAfter=notBefore;readTitle=title;
        if(pendingMode)try{payload=new JSONObject().put("pending",true).put("title",title).put("platform",FavoriteRules.platformName(pkg)).toString();}catch(Exception ignored){}
        if(!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER))return false;
        container=new LinearLayout(service);container.setOrientation(LinearLayout.VERTICAL);container.setBackgroundColor(0xfff6f5f0);
        LinearLayout header=new LinearLayout(service);
        status=new TextView(service);status.setText(pendingMode?"拾光 · 选择分类":"拾光 · 正在校验原链接…");status.setTextColor(0xff354936);status.setPadding(16,16,8,16);header.addView(status,new LinearLayout.LayoutParams(0,-2,1));
        Button cancel=new Button(service);cancel.setText("关闭");cancel.setOnClickListener(v->finish(saved?"已保存在手机":"已取消，本次未保存"));header.addView(cancel);container.addView(header);
        if(pendingMode)header.setVisibility(View.GONE);
        fallback=new Button(service);fallback.setText("打开拾光继续");fallback.setVisibility(View.GONE);fallback.setOnClickListener(v->{dismiss();openActivity.run();});container.addView(fallback);
        web=new WebView(service){@Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus&&!closed&&!reading&&readAfter>=0){reading=true;handler.post(()->read(readPackage,readAfter,readTitle));}}};
        web.setFocusableInTouchMode(true);container.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        web.setOnKeyListener((v,key,event)->{if(key==KeyEvent.KEYCODE_BACK&&event.getAction()==KeyEvent.ACTION_UP){finish(saved?"已保存在手机":"已取消，本次未保存");return true;}return false;});
        WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        WebViewAssetLoader loader=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(service)).build();
        WebViewCompat.addWebMessageListener(web,"ShiguangQuickResult",Collections.singleton("https://appassets.androidplatform.net"),(v,message,origin,main,reply)->{
            if(closed||!main||!"https://appassets.androidplatform.net".equals(origin.toString()))return;
            try{JSONObject data=new JSONObject(message.getData());
                if(data.optBoolean("resolve")&&pendingMode&&!resolving){resolving=true;if(attached){service.getSystemService(WindowManager.class).removeView(container);attached=false;}requestLink.run();return;}
                if(data.optBoolean("cancel")){finish("已取消，本次未保存");return;}
                if(data.optBoolean("saved")){saved=true;status.setText("已保存，正在同步…");handler.postDelayed(()->finish("已保存在手机，打开拾光后可继续同步"),10000);}
                if(data.optBoolean("done"))finish(data.optBoolean("duplicate")?"已补全已有收藏":data.optBoolean("cloud")?"已收进拾光并同步":"已保存在手机，联网登录后自动同步");
                if(data.optBoolean("error"))finish("保存失败，请打开拾光检查存储");
            }catch(Exception ignored){}
        });
        web.setWebViewClient(new WebViewClient(){
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest request){WebResourceResponse response=loader.shouldInterceptRequest(request.getUrl());if(response!=null)return response;if("https".equals(request.getUrl().getScheme())&&"pkgsgkrjhcckmcjqdzws.supabase.co".equals(request.getUrl().getHost()))return null;return new WebResourceResponse("text/plain","UTF-8",new java.io.ByteArrayInputStream(new byte[0]));}
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest request){return true;}
            @Override public void onPageFinished(WebView v,String url){if(HOME.equals(url)&&!closed){loaded=true;deliver();}}
        });
        PreviewBridge.install(service,web);
        android.util.DisplayMetrics m=service.getResources().getDisplayMetrics();
        WindowManager.LayoutParams params=new WindowManager.LayoutParams(m.widthPixels,Math.min((int)(680*m.density),(int)(m.heightPixels*.88)),WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_DIM_BEHIND,PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.BOTTOM;params.dimAmount=.2f;params.softInputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        windowParams=params;
        try{service.getSystemService(WindowManager.class).addView(container,params);web.requestFocus();web.loadUrl(HOME);
            attached=true;
            if(!pendingMode)handler.postDelayed(()->{if(!closed&&payload==null)unavailable("未取得新链接，请点下方按钮继续校验");},5000);
            return true;
        }catch(Exception e){dismiss();return false;}
    }
    private void restore(){if(!attached){service.getSystemService(WindowManager.class).addView(container,windowParams);attached=true;}web.requestFocus();}
    void acquire(String pkg,long timestamp,String title){
        if(closed)return;readPackage=pkg;readAfter=timestamp;readTitle=title;payload=null;attempts=0;reading=false;
        restore();handler.postDelayed(()->read(pkg,timestamp,title),150);
        handler.postDelayed(()->{if(!closed&&resolving&&payload==null)unavailable("未读到新链接");},5000);
    }
    void linkFailed(String message){
        if(closed)return;resolving=false;readAfter=-1;reading=false;handler.removeCallbacksAndMessages(null);
        restore();status.setText("拾光 · 选择分类");
        web.evaluateJavascript("window.shiguangResolveFailed("+JSONObject.quote(message)+")",null);
    }
    private void read(String pkg,long notBefore,String title){
        if(closed||payload!=null)return;
        if(!container.hasWindowFocus()){reading=false;return;}
        attempts++;
        try{
            ClipboardManager manager=(ClipboardManager)service.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip=manager.getPrimaryClip();
            if(clip!=null&&clip.getItemCount()>0){
                CharSequence raw=clip.getItemAt(0).coerceToText(service);
                CapturedLink link=CapturedLink.parse(pkg,raw==null?"":raw.toString(),clip.getDescription().getTimestamp(),notBefore,title);
                if(link!=null){payload=new JSONObject().put("url",link.url).put("title",link.title).toString();status.setText("拾光 · 选择分类");FavoriteCaptureService.report(service,"新链接校验通过，继续保存已选分类");if(pendingMode){web.evaluateJavascript("window.shiguangReceiveResolvedShare("+payload+")",null);}else deliver();return;}
            }
        }catch(Exception ignored){}
        if(attempts<8)handler.postDelayed(()->read(pkg,notBefore,title),250);else unavailable("未读到对应内容的新链接，可打开拾光继续校验");
    }
    private void deliver(){if(!closed&&loaded&&payload!=null&&!started){started=true;web.evaluateJavascript("window.shiguangReceiveShare("+payload+")",null);}}
    private void unavailable(String text){if(closed)return;if(pendingMode){linkFailed("未取得这篇内容的原链接，未保存。分类已保留，可重试。");return;}status.setText(text);fallback.setVisibility(View.VISIBLE);FavoriteCaptureService.report(service,text);}
    private void finish(String message){if(closed)return;dismiss();result.finish(message);}
    void dismiss(){if(closed)return;closed=true;handler.removeCallbacksAndMessages(null);if(container!=null&&attached)try{service.getSystemService(WindowManager.class).removeView(container);attached=false;}catch(Exception ignored){}if(web!=null){web.stopLoading();web.destroy();web=null;}}
}
