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
    private long lastAttempt,copyStarted;
    private String clickedKey="";
    private final FavoriteWait wait=new FavoriteWait();
    private int phase; // 0 idle, 1 waiting for favorite success, 2 locating copy-link action
    private final Map<String,Boolean> previousStates=new HashMap<>();
    private long lastObserved;
    private static FavoriteCaptureService instance;
    static boolean active(){return instance!=null;}
    static String status(Context c){return c.getSharedPreferences("automatic",MODE_PRIVATE).getString("status","尚未检测到收藏操作");}
    static void report(Context c,String text){getPrefs(c).edit().putString("status",new java.text.SimpleDateFormat("HH:mm:ss",java.util.Locale.ROOT).format(new Date())+" · "+text).apply();}
    private static android.content.SharedPreferences getPrefs(Context c){return c.getSharedPreferences("automatic",MODE_PRIVATE);}
    static void acknowledge(Context c,long id){getPrefs(c).edit().putLong("launchAck",id).apply();report(c,"已打开链接校验窗口");}
    private void state(String text){report(this,text);}
    static boolean enabled(Context c){return c.getSharedPreferences("automatic",MODE_PRIVATE).getBoolean("enabled",false);}
    static void pause(Context c){c.getSharedPreferences("automatic",MODE_PRIVATE).edit().putBoolean("enabled",false).apply();if(instance!=null){instance.reset();instance.disableSelf();}}
    @Override protected void onServiceConnected(){instance=this;state("服务已连接，等待小红书或B站的收藏操作");}
    private void reset(){handler.removeCallbacksAndMessages(null);phase=0;pendingPackage="";clickedKey="";}
    private String text(CharSequence value){return value==null?"":value.toString();}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(!enabled(this)){reset();return;}
        String pkg=text(event.getPackageName());if(!FavoriteRules.supported(pkg))return;
        if(phase!=0&&!pkg.equals(pendingPackage)){fail("已切换平台，本次收藏跟随已取消，未保存");return;}
        long now=SystemClock.elapsedRealtime();
        if(phase>=2)return;
        if(event.getEventType()==AccessibilityEvent.TYPE_VIEW_SCROLLED){if(phase!=0)fail("页面发生滚动，本次跟随已停止，未保存");previousStates.clear();return;}
        if(phase==0&&event.getEventType()==AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED&&now-lastObserved>400){
            lastObserved=now;observe(pkg);return;
        }
        if(event.getEventType()==AccessibilityEvent.TYPE_VIEW_CLICKED){
            AccessibilityNodeInfo source=event.getSource();
            if(source!=null&&(source.isPassword()||source.isEditable()))return;
            source=favoriteSource(source);
            String label=text(event.getContentDescription());
            Boolean previous=source==null?null:previousStates.get(nodeKey(source));
            boolean newlySelected=source!=null&&Boolean.FALSE.equals(previous)&&selected(source);
            if(FavoriteRules.savedButton(label)&&!newlySelected){state("收到已收藏/取消收藏按钮，未确认新增收藏，未保存");return;}
            for(CharSequence s:event.getText())if(FavoriteRules.savedButton(text(s))&&!newlySelected){state("收到已收藏/取消收藏按钮，未确认新增收藏，未保存");return;}
            if(Boolean.TRUE.equals(previous)&&source!=null&&!selected(source)){reset();state("检测到取消收藏，未保存");return;}
            if(newlySelected)label="收藏";
            if(!FavoriteRules.favoriteButton(label))for(CharSequence s:event.getText())if(FavoriteRules.favoriteButton(text(s))){label=text(s);break;}
            if(!FavoriteRules.favoriteButton(label)&&source!=null){String t=text(source.getText()),d=text(source.getContentDescription());if(FavoriteRules.favoriteButton(t))label=t;else if(FavoriteRules.favoriteButton(d))label=d;}
            if(!FavoriteRules.favoriteButton(label)&&source!=null&&(FavoriteRules.savedButton(text(source.getText()))||FavoriteRules.savedButton(text(source.getContentDescription())))){state("收到已收藏按钮，但缺少收藏前状态；未执行自动保存");return;}
            if(FavoriteRules.favoriteButton(label)&&now-lastAttempt>4000){
                reset();pendingPackage=pkg;phase=1;wait.start(now);clickedKey=source==null?"":nodeKey(source);
                state("已识别收藏按钮，等待平台确认收藏成功");
                Toast.makeText(this,"拾光已识别收藏，正在确认原平台状态…",Toast.LENGTH_SHORT).show();
                handler.postDelayed(confirm,250);
                // Checked/selected state must confirm a save; opening a Bilibili folder picker alone is not enough.
                if(source!=null&&selected(source))wait.confirm(now);
            }else if(phase==0){state("已收到"+(pkg.equals("com.xingin.xhs")?"小红书":"B站")+"点击，但未匹配到收藏控件；未保存");}
        }
        if(phase==1){
            boolean saved=FavoriteRules.success(text(event.getContentDescription()));
            for(CharSequence t:event.getText())saved|=FavoriteRules.success(text(t));
            if(saved)wait.confirm(now);
        }
    }
    private final Runnable confirm=this::confirmSaved;
    private String nodeKey(AccessibilityNodeInfo n){android.graphics.Rect r=new android.graphics.Rect();n.getBoundsInScreen(r);return n.getWindowId()+":"+n.getViewIdResourceName()+":"+r;}
    private boolean selected(AccessibilityNodeInfo n){return n.isSelected()||n.isChecked()||FavoriteRules.savedButton(text(n.getContentDescription()))||FavoriteRules.savedButton(text(n.getText()));}
    private boolean favoriteNode(AccessibilityNodeInfo n){return n!=null&&(FavoriteRules.favoriteButton(text(n.getText()))||FavoriteRules.favoriteButton(text(n.getContentDescription()))||FavoriteRules.savedButton(text(n.getText()))||FavoriteRules.savedButton(text(n.getContentDescription())));}
    private AccessibilityNodeInfo favoriteSource(AccessibilityNodeInfo source){
        if(source==null||favoriteNode(source))return source;
        // A clickable container may expose its label only on a direct child. Never search the entire page for a click match.
        AccessibilityNodeInfo match=null;
        for(int i=0;i<source.getChildCount()&&i<12;i++){AccessibilityNodeInfo child=source.getChild(i);if(child!=null&&!child.isPassword()&&!child.isEditable()&&child.isVisibleToUser()&&favoriteNode(child)){if(match!=null)return source;match=child;}}
        return match==null?source:match;
    }
    private void observe(String pkg){
        AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null||!pkg.equals(text(root.getPackageName())))return;
        previousStates.clear();
        for(AccessibilityNodeInfo n:nodes(root)){
            String t=text(n.getText()),d=text(n.getContentDescription());
            if(favoriteNode(n))previousStates.put(nodeKey(n),selected(n));
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
        long now=SystemClock.elapsedRealtime();
        if(wait.ready(now)){beginShare();return;}
        if(wait.expired(now)){fail("已识别点击，但15秒内未确认收藏成功；未保存。B站请先确认收藏夹。");return;}
        for(AccessibilityNodeInfo n:nodes(root())){
            String d=text(n.getContentDescription()),t=text(n.getText());
            if((FavoriteRules.success(t)&&!FavoriteRules.clean(t).equals("已收藏"))||(FavoriteRules.success(d)&&!FavoriteRules.clean(d).equals("已收藏"))||(nodeKey(n).equals(clickedKey)&&favoriteNode(n)&&selected(n))){wait.confirm(now);break;}
        }
        handler.postDelayed(confirm,250);
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
        if(root()==null){fail("无法读取原平台当前窗口，本次未保存");return;}
        AccessibilityNodeInfo share=button(false);lastAttempt=SystemClock.elapsedRealtime();
        if(share==null){fail("未找到可识别的分享按钮，本次未保存。可使用分享 → 拾光。");return;}
        phase=2;copyStarted=SystemClock.elapsedRealtime();
        state("已识别收藏状态，正在从原平台取得链接");
        if(!share.performAction(AccessibilityNodeInfo.ACTION_CLICK)){fail("无法打开分享菜单，本次未保存");return;}
        handler.postDelayed(pollCopy,300);
    }
    private final Runnable pollCopy=new Runnable(){public void run(){
        if(phase!=2||!enabled(FavoriteCaptureService.this)){reset();return;}
        if(root()==null){fail("分享菜单无法读取，本次未保存");return;}
        AccessibilityNodeInfo copy=button(true);
        if(copy!=null){
            String pkg=pendingPackage;long timestamp=System.currentTimeMillis();
            boolean clicked=copy.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            if(!clicked){fail("未能复制原内容链接，本次未保存");return;}phase=3;
            handler.postDelayed(()->{
                if(!enabled(FavoriteCaptureService.this))return;
                AccessibilityNodeInfo current=getRootInActiveWindow();if(current==null||!pkg.equals(text(current.getPackageName()))){fail("复制后已离开原平台，本次未保存");return;}
                try{
                    state("已点击复制链接，正在校验并打开分类窗口");startActivity(new Intent(FavoriteCaptureService.this,ClipboardCaptureActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("expectedPackage",pkg).putExtra("notBefore",timestamp));
                    handler.postDelayed(()->{if(getPrefs(FavoriteCaptureService.this).getLong("launchAck",0)!=timestamp)fail("已取得复制动作，但系统未显示拾光窗口。请检查拾光的后台弹出界面权限；未保存。");else reset();},2500);
                }catch(Exception e){fail("系统拒绝弹出拾光窗口，请检查后台弹出界面权限；未保存");}
            },450);return;
        }
        if(SystemClock.elapsedRealtime()-copyStarted>3000){fail("未找到「复制链接」，本次未保存。请在分享菜单选择拾光。");return;}
        handler.postDelayed(this,250);
    }};
    private void fail(String message){reset();state(message);Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    @Override public void onInterrupt(){reset();state("服务被系统中断，请返回无障碍设置检查");}
    @Override public void onDestroy(){reset();if(instance==this)instance=null;super.onDestroy();}
}
