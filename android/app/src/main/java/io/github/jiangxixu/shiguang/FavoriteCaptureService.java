package io.github.jiangxixu.shiguang;

import android.accessibilityservice.AccessibilityService;
import android.content.*;
import android.os.*;
import android.view.accessibility.*;
import android.widget.Toast;
import java.util.*;

/** Optional experimental workflow: favorite -> share -> copy link -> classification. */
public class FavoriteCaptureService extends AccessibilityService {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private String pendingPackage="";
    private long armedAt,lastAttempt,copyStarted;
    private int phase; // 0 idle, 1 waiting for favorite success, 2 locating copy-link action
    private final Map<String,Boolean> previousStates=new HashMap<>();
    private long lastObserved;
    private static FavoriteCaptureService instance;
    static boolean active(){return instance!=null;}
    static String status(Context c){return c.getSharedPreferences("automatic",MODE_PRIVATE).getString("status","尚未检测到收藏操作");}
    private void state(String text){getSharedPreferences("automatic",MODE_PRIVATE).edit().putString("status",text).apply();}
    static boolean enabled(Context c){return c.getSharedPreferences("automatic",MODE_PRIVATE).getBoolean("enabled",false);}
    static void pause(Context c){c.getSharedPreferences("automatic",MODE_PRIVATE).edit().putBoolean("enabled",false).apply();if(instance!=null){instance.reset();instance.disableSelf();}}
    @Override protected void onServiceConnected(){instance=this;state("服务已连接，等待小红书或B站的收藏操作");}
    private void reset(){handler.removeCallbacksAndMessages(null);phase=0;pendingPackage="";}
    private String text(CharSequence value){return value==null?"":value.toString();}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(!enabled(this)){reset();return;}
        String pkg=text(event.getPackageName());if(!FavoriteRules.supported(pkg))return;
        if(phase!=0&&!pkg.equals(pendingPackage)){reset();return;}
        long now=SystemClock.elapsedRealtime();
        if(phase==1&&now-armedAt>15000){reset();}
        if(phase==2)return;
        if(event.getEventType()==AccessibilityEvent.TYPE_VIEW_SCROLLED){reset();previousStates.clear();return;}
        if(phase==0&&event.getEventType()==AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED&&now-lastObserved>400){
            lastObserved=now;observe(pkg);return;
        }
        if(event.getEventType()==AccessibilityEvent.TYPE_VIEW_CLICKED){
            AccessibilityNodeInfo source=event.getSource();
            if(source!=null&&(source.isPassword()||source.isEditable()))return;
            String label=text(event.getContentDescription());
            Boolean previous=source==null?null:previousStates.get(nodeKey(source));
            boolean newlySelected=source!=null&&Boolean.FALSE.equals(previous)&&selected(source);
            if((label.contains("取消收藏")||label.equals("已收藏"))&&!newlySelected){reset();return;}
            for(CharSequence s:event.getText())if(text(s).contains("取消收藏")&&!newlySelected){reset();return;}
            if(Boolean.TRUE.equals(previous)&&source!=null&&!selected(source)){reset();return;}
            if(newlySelected)label="收藏";
            if(!FavoriteRules.favoriteButton(label))for(CharSequence s:event.getText())if(FavoriteRules.favoriteButton(text(s))){label=text(s);break;}
            if(!FavoriteRules.favoriteButton(label)&&source!=null){String t=text(source.getText()),d=text(source.getContentDescription());if(FavoriteRules.favoriteButton(t))label=t;else if(FavoriteRules.favoriteButton(d))label=d;}
            if(FavoriteRules.favoriteButton(label)&&now-lastAttempt>4000){
                reset();pendingPackage=pkg;phase=1;armedAt=now;
                state("已识别收藏按钮，等待平台确认收藏成功");
                // Checked/selected state must confirm a save; opening a Bilibili folder picker alone is not enough.
                if(source!=null&&(source.isSelected()||source.isChecked()||text(source.getContentDescription()).contains("取消收藏")))handler.postDelayed(this::beginShare,900);
            }else if(phase==1&&source!=null&&(text(source.getText()).equals("完成")||text(source.getText()).equals("确定"))){handler.postDelayed(this::confirmSaved,500);}
        }
        if(phase==1){
            boolean saved=FavoriteRules.success(text(event.getContentDescription()));
            for(CharSequence t:event.getText())saved|=FavoriteRules.success(text(t));
            if(saved){handler.removeCallbacksAndMessages(null);handler.postDelayed(this::beginShare,800);}
            else if(event.getEventType()==AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED){handler.removeCallbacks(confirm);handler.postDelayed(confirm,350);}
        }
    }
    private final Runnable confirm=this::confirmSaved;
    private String nodeKey(AccessibilityNodeInfo n){android.graphics.Rect r=new android.graphics.Rect();n.getBoundsInScreen(r);return n.getWindowId()+":"+n.getViewIdResourceName()+":"+r;}
    private boolean selected(AccessibilityNodeInfo n){return n.isSelected()||n.isChecked()||text(n.getContentDescription()).contains("取消收藏")||text(n.getContentDescription()).equals("已收藏")||text(n.getText()).equals("已收藏");}
    private void observe(String pkg){
        AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null||!pkg.equals(text(root.getPackageName())))return;
        previousStates.clear();
        for(AccessibilityNodeInfo n:nodes(root)){
            String t=text(n.getText()),d=text(n.getContentDescription());
            if(FavoriteRules.favoriteButton(t)||FavoriteRules.favoriteButton(d)||d.contains("取消收藏")||d.equals("已收藏"))previousStates.put(nodeKey(n),selected(n));
        }
    }
    private AccessibilityNodeInfo root(){AccessibilityNodeInfo node=getRootInActiveWindow();return node!=null&&pendingPackage.equals(text(node.getPackageName()))?node:null;}
    private List<AccessibilityNodeInfo> nodes(AccessibilityNodeInfo root){
        List<AccessibilityNodeInfo> result=new ArrayList<>();if(root==null)return result;
        ArrayDeque<AccessibilityNodeInfo> queue=new ArrayDeque<>();queue.add(root);
        while(!queue.isEmpty()&&result.size()<400){AccessibilityNodeInfo n=queue.remove();result.add(n);for(int i=0;i<n.getChildCount()&&queue.size()<400;i++){AccessibilityNodeInfo child=n.getChild(i);if(child!=null&&child.isVisibleToUser()&&!child.isPassword()&&!child.isEditable())queue.add(child);}}
        return result;
    }
    private void confirmSaved(){
        if(phase!=1||!enabled(this))return;
        for(AccessibilityNodeInfo n:nodes(root())){
            String d=text(n.getContentDescription()),t=text(n.getText());
            if(FavoriteRules.success(t)||FavoriteRules.success(d)||d.equals("取消收藏")||((FavoriteRules.favoriteButton(d)||FavoriteRules.favoriteButton(t))&&(n.isSelected()||n.isChecked()))){beginShare();return;}
        }
    }
    private AccessibilityNodeInfo button(boolean copy){
        Map<String,AccessibilityNodeInfo> matches=new LinkedHashMap<>();
        for(AccessibilityNodeInfo n:nodes(root())){
            String t=text(n.getText()),d=text(n.getContentDescription());
            if(copy?!(FavoriteRules.copy(t)||FavoriteRules.copy(d)):!(FavoriteRules.share(t)||FavoriteRules.share(d)))continue;
            AccessibilityNodeInfo target=n;for(int i=0;!target.isClickable()&&i<2;i++){target=target.getParent();if(target==null)break;}
            if(target!=null&&target.isClickable()&&target.isEnabled()&&target.isVisibleToUser()){
                android.graphics.Rect bounds=new android.graphics.Rect();target.getBoundsInScreen(bounds);matches.put(bounds.toString(),target);
            }
        }
        // Ambiguous controls are deliberately not clicked.
        return matches.size()==1?matches.values().iterator().next():null;
    }
    private void beginShare(){
        if(phase!=1||!enabled(this))return;
        if(root()==null){reset();return;}
        AccessibilityNodeInfo share=button(false);lastAttempt=SystemClock.elapsedRealtime();
        if(share==null){fail("未找到可识别的分享按钮，本次未保存。可使用分享 → 拾光。");return;}
        phase=2;copyStarted=SystemClock.elapsedRealtime();
        state("已识别收藏状态，正在从原平台取得链接");
        if(!share.performAction(AccessibilityNodeInfo.ACTION_CLICK)){fail("无法打开分享菜单，本次未保存");return;}
        handler.postDelayed(pollCopy,300);
    }
    private final Runnable pollCopy=new Runnable(){public void run(){
        if(phase!=2||!enabled(FavoriteCaptureService.this)){reset();return;}
        if(root()==null){reset();return;}
        AccessibilityNodeInfo copy=button(true);
        if(copy!=null){
            String pkg=pendingPackage;long timestamp=System.currentTimeMillis();
            boolean clicked=copy.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            reset();if(!clicked){fail("未能复制原内容链接，本次未保存");return;}
            handler.postDelayed(()->{
                if(!enabled(FavoriteCaptureService.this))return;
                AccessibilityNodeInfo current=getRootInActiveWindow();if(current==null||!pkg.equals(text(current.getPackageName())))return;
                try{state("已点击复制链接，正在校验并打开分类窗口");startActivity(new Intent(FavoriteCaptureService.this,ClipboardCaptureActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("expectedPackage",pkg).putExtra("notBefore",timestamp));}
                catch(Exception e){Toast.makeText(FavoriteCaptureService.this,"系统未允许弹出收藏窗口，请打开拾光检查无障碍权限",Toast.LENGTH_LONG).show();}
            },450);return;
        }
        if(SystemClock.elapsedRealtime()-copyStarted>3000){fail("未找到「复制链接」，本次未保存。请在分享菜单选择拾光。");return;}
        handler.postDelayed(this,250);
    }};
    private void fail(String message){reset();state(message);Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    @Override public void onInterrupt(){reset();}
    @Override public void onDestroy(){reset();if(instance==this)instance=null;super.onDestroy();}
}
