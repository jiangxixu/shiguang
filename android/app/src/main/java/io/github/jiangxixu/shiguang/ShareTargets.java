package io.github.jiangxixu.shiguang;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import java.util.Collections;

/** Offers a real share destination; Android owns the final Sharesheet order. */
final class ShareTargets {
    static void publish(Context context) {
        // Retire only the obsolete capture shortcut/channel; collection data and
        // accessibility bindings live in separate stores and are not touched.
        try {
            ShortcutManager manager=context.getSystemService(ShortcutManager.class);
            manager.removeDynamicShortcuts(Collections.singletonList("clipboard-collect"));
            manager.disableShortcuts(Collections.singletonList("clipboard-collect"),"请打开拾光使用自动收藏");
        } catch(Exception ignored) {}
        try {
            android.app.NotificationManager notifications=context.getSystemService(android.app.NotificationManager.class);
            notifications.cancel(154);
            notifications.deleteNotificationChannel("floating-capture");
        } catch(Exception ignored) {}
        try {
            ShortcutInfo.Builder builder=new ShortcutInfo.Builder(context,"collect")
                .setShortLabel("收藏到拾光").setLongLabel("收藏到拾光 · 选择分类")
                .setIcon(Icon.createWithResource(context,R.drawable.ic_shiguang))
                .setCategories(Collections.singleton("io.github.jiangxixu.shiguang.COLLECT"))
                .setIntent(new Intent(context,MainActivity.class).setAction(Intent.ACTION_VIEW))
                .setRank(0);
            if(Build.VERSION.SDK_INT>=29)builder.setLongLived(true);
            context.getSystemService(ShortcutManager.class).setDynamicShortcuts(Collections.singletonList(builder.build()));
        } catch(Exception ignored) {}
    }
    static void used(Context context) {
        try {context.getSystemService(ShortcutManager.class).reportShortcutUsed("collect");}catch(Exception ignored){}
    }
}
