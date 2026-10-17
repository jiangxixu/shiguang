package io.github.jiangxixu.shiguang;
import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import java.util.regex.Pattern;

/** Reads clipboard only after an explicit tap and foreground window focus. */
public class ClipboardCaptureActivity extends Activity {
    private boolean attempted;
    private TextView status;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(1);
        int padding=Math.round(24*getResources().getDisplayMetrics().density);layout.setPadding(padding,padding,padding,padding);
        status=new TextView(this);status.setTextSize(18);status.setText("正在读取复制的链接…");layout.addView(status);
        Button retry=new Button(this);retry.setText("重新读取链接");retry.setOnClickListener(v->readClipboard());layout.addView(retry);
        Button cancel=new Button(this);cancel.setText("返回");cancel.setOnClickListener(v->finish());layout.addView(cancel);
        setContentView(layout);
    }
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused&&!attempted){attempted=true;status.post(this::readClipboard);}}
    private void readClipboard(){
        if(!hasWindowFocus())return;
        try{
            ClipboardManager clipboard=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            if(clipboard.hasPrimaryClip()&&clipboard.getPrimaryClip().getItemCount()>0){
                CharSequence value=clipboard.getPrimaryClip().getItemAt(0).coerceToText(this);
                String text=value==null?"":value.toString();
                if(text.length()<=100000&&Pattern.compile("https?://[^\\s<>]+").matcher(text).find()){
                    startActivity(new Intent(this,QuickShareActivity.class).setAction(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,text));finish();return;
                }
            }
            status.setText("没有读到网页链接。请先在原 App 选择「复制链接」，再点拾光收藏。下一步可选择分类、填写原因。");
        }catch(Exception e){status.setText("暂时无法读取剪贴板，请复制链接后重试，或使用原 App 的分享 → 拾光。");}
    }
}
