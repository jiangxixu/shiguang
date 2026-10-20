package io.github.jiangxixu.shiguang;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

/** Visible recovery when the phone blocks background Activity launches; no clipboard access here. */
final class FavoritePrompt {
    private final AccessibilityService service;
    private View view;
    FavoritePrompt(AccessibilityService service){this.service=service;}
    boolean show(Runnable open,Runnable cancel){
        return show("拾光 · 已检测到收藏","系统未自动打开窗口。点击选择分类后，校验原链接；确认保存才会加入拾光。","选择分类",open,cancel);
    }
    boolean show(String heading,String description,String action,Runnable open,Runnable cancel){
        LinearLayout card=new LinearLayout(service);card.setOrientation(LinearLayout.VERTICAL);
        int pad=dp(16);card.setPadding(pad,pad,pad,pad);
        GradientDrawable bg=new GradientDrawable();bg.setColor(Color.rgb(246,245,240));bg.setCornerRadius(dp(18));card.setBackground(bg);card.setElevation(dp(8));
        TextView title=new TextView(service);title.setText(heading);title.setTextColor(Color.rgb(45,65,50));title.setTextSize(18);card.addView(title);
        TextView message=new TextView(service);message.setText(description);message.setTextColor(Color.DKGRAY);message.setPadding(0,dp(8),0,dp(8));card.addView(message);
        LinearLayout actions=new LinearLayout(service);
        Button dismiss=new Button(service);dismiss.setText("这次跳过");dismiss.setOnClickListener(v->cancel.run());actions.addView(dismiss,new LinearLayout.LayoutParams(0,-2,1));
        Button capture=new Button(service);capture.setText(action);capture.setOnClickListener(v->open.run());actions.addView(capture,new LinearLayout.LayoutParams(0,-2,1));card.addView(actions);
        WindowManager.LayoutParams params=new WindowManager.LayoutParams(service.getResources().getDisplayMetrics().widthPixels-dp(32),WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;params.y=dp(40);
        try{service.getSystemService(WindowManager.class).addView(card,params);view=card;return true;}catch(Exception e){return false;}
    }
    private int dp(int value){return Math.round(value*service.getResources().getDisplayMetrics().density);}
    void dismiss(){if(view!=null){try{service.getSystemService(WindowManager.class).removeView(view);}catch(Exception ignored){}view=null;}}
}
