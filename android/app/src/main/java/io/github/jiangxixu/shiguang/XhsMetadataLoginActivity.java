package io.github.jiangxixu.shiguang;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.*;
import android.widget.*;

/** User-operated platform login. No native JS bridge is exposed to external pages. */
public class XhsMetadataLoginActivity extends Activity {
    private WebView browser;
    static boolean enabled(android.content.Context c){return c.getSharedPreferences("preview",MODE_PRIVATE).getBoolean("xhsWebSession",false);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setBackgroundColor(0xfff6f5f0);
        TextView info=new TextView(this);info.setText("小红书网页登录 · 登录后点下方返回补全。网页凭据仅保留在本机，用于从小红书读取收藏的标题和封面。");info.setPadding(20,16,20,16);layout.addView(info);
        browser=new WebView(this);layout.addView(browser,new LinearLayout.LayoutParams(-1,0,1));
        Button back=new Button(this);back.setText("完成，返回拾光补全");back.setOnClickListener(v->{CookieManager.getInstance().flush();finish();});layout.addView(back);setContentView(layout);
        WebSettings settings=browser.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36");
        browser.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
                String url=request.getUrl().toString();if(XhsLinks.allowed(url))return false;
                // Other authentication apps require an explicit user tap. Do not auto-launch redirects.
                if(request.hasGesture()&&!"http".equals(request.getUrl().getScheme())&&!"https".equals(request.getUrl().getScheme())){
                    try{startActivity(new Intent(Intent.ACTION_VIEW,request.getUrl()));}catch(Exception ignored){Toast.makeText(XhsMetadataLoginActivity.this,"请使用网页提供的手机号或扫码登录",Toast.LENGTH_LONG).show();}
                }
                return true;
            }
            @Override public void onReceivedSslError(WebView v,android.webkit.SslErrorHandler h,android.net.http.SslError error){h.cancel();}
        });
        browser.loadUrl("https://www.xiaohongshu.com/login");
    }
    @Override protected void onDestroy(){if(browser!=null){browser.stopLoading();browser.destroy();}super.onDestroy();}
}
