package io.github.jiangxixu.shiguang;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import java.util.Collections;
import java.util.Arrays;
import android.widget.Toast;

/** Offers a real share destination; Android owns the final Sharesheet order. */
final class ShareTargets {
    static void publish(Context context) {
        try {
            ShortcutInfo.Builder builder=new ShortcutInfo.Builder(context,"collect")
                .setShortLabel("收藏到拾光").setLongLabel("收藏到拾光 · 选择分类")
                .setIcon(Icon.createWithResource(context,R.drawable.ic_shiguang))
                .setCategories(Collections.singleton("io.github.jiangxixu.shiguang.COLLECT"))
                .setIntent(new Intent(context,MainActivity.class).setAction(Intent.ACTION_VIEW))
                .setRank(0);
            if(Build.VERSION.SDK_INT>=29)builder.setLongLived(true);
            context.getSystemService(ShortcutManager.class).setDynamicShortcuts(Arrays.asList(builder.build(),clipboard(context)));
        } catch(Exception ignored) {}
    }
    private static ShortcutInfo clipboard(Context context){
        return new ShortcutInfo.Builder(context,"clipboard-collect").setShortLabel("复制后收藏").setLongLabel("复制链接后收藏到拾光")
            .setIcon(Icon.createWithResource(context,R.drawable.ic_shiguang)).setRank(1)
            .setIntent(new Intent(context,ClipboardCaptureActivity.class).setAction(Intent.ACTION_VIEW)).build();
    }
    static void pin(Context context){
        try{
            ShortcutManager manager=context.getSystemService(ShortcutManager.class);
            if(manager.isRequestPinShortcutSupported()&&manager.requestPinShortcut(clipboard(context),null)){
                Toast.makeText(context,"请在系统提示中确认添加桌面快捷方式",Toast.LENGTH_LONG).show();return;
            }
        }catch(Exception ignored){}
        Toast.makeText(context,"请长按桌面拾光图标，再将「复制后收藏」拖到桌面",Toast.LENGTH_LONG).show();
    }
    static void used(Context context) {
        try {context.getSystemService(ShortcutManager.class).reportShortcutUsed("collect");}catch(Exception ignored){}
    }
}
