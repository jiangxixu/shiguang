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
    private final FavoriteTransitions transitions=new FavoriteTransitions();
    private FavoritePrompt prompt;
    private String observedPackage="";
    private long suppressUntil;
    private long lastObserved,lastDiagnostic,lastClick;
    private String lastClickPackage="",lastDiagnosticText="",learningRole="",learningRule="";
    private boolean learningArmed;
    private long copyTimeout=3000;
    private AccessibilityNodeInfo boundFavorite;
    private final Runnable sample=new Runnable(){public void run(){
        if(!enabled(FavoriteCaptureService.this)||phase!=0)return;
        AccessibilityNodeInfo root=getRootInActiveWindow();
        String pkg=root==null?"":text(root.getPackageName());
        if(!FavoriteRules.supported(pkg)){transitions.clear();observedPackage="";return;}
        observe(pkg);
        if(phase==0)handler.postDelayed(this,500);
    }};
    private static FavoriteCaptureService instance;
    static boolean active(){return instance!=null;}
    static String status(Context c){return c.getSharedPreferences("automatic",MODE_PRIVATE).getString("status","尚未检测到收藏操作");}
    static void report(Context c,String text){
        String line=new java.text.SimpleDateFormat("HH:mm:ss",java.util.Locale.ROOT).format(new Date())+" · "+text;
        String[] old=getPrefs(c).getString("history","").split("\n");
        StringBuilder history=new StringBuilder();for(int i=Math.max(0,old.length-11);i<old.length;i++)if(!old[i].isEmpty())history.append(old[i]).append('\n');
        history.append(line);getPrefs(c).edit().putString("status",line).putString("history",history.toString()).apply();
    }
    static String history(Context c){return getPrefs(c).getString("history","暂无检测记录");}
    private static android.content.SharedPreferences getPrefs(Context c){return c.getSharedPreferences("automatic",MODE_PRIVATE);}
    static void acknowledge(Context c,long id){getPrefs(c).edit().putLong("launchAck",id).apply();report(c,"已打开链接校验窗口");}
    private void state(String text){report(this,text);}
    static boolean enabled(Context c){return c.getSharedPreferences("automatic",MODE_PRIVATE).getBoolean("enabled",false);}
    static void pause(Context c){c.getSharedPreferences("automatic",MODE_PRIVATE).edit().putBoolean("enabled",false).apply();if(instance!=null){instance.reset();instance.disableSelf();}}
    @Override protected void onServiceConnected(){instance=this;state("服务已连接，等待小红书或B站的收藏操作");handler.post(sample);}
    private void reset(){handler.removeCallbacksAndMessages(null);if(prompt!=null){prompt.dismiss();prompt=null;}phase=0;pendingPackage="";clickedKey="";transitions.clear();observedPackage="";learningArmed=false;learningRole="";learningRule="";boundFavorite=null;}
    private String text(CharSequence value){return value==null?"":value.toString();}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(!enabled(this)){reset();return;}
        String pkg=text(event.getPackageName());if(!FavoriteRules.supported(pkg))return;
        if(phase==4){learnClick(event,pkg);return;}
        if(phase!=0&&!pkg.equals(pendingPackage)){fail("已切换平台，本次收藏跟随已取消，未保存");return;}
        long now=SystemClock.elapsedRealtime();
        if(phase>=2)return;
        boolean click=event.getEventType()==AccessibilityEvent.TYPE_VIEW_CLICKED||event.getEventType()==AccessibilityEvent.TYPE_VIEW_LONG_CLICKED;
        if(click){lastClick=now;lastClickPackage=pkg;}
        boolean success=FavoriteRules.confirmedSuccess(text(event.getContentDescription()));
        for(CharSequence value:event.getText())success|=FavoriteRules.confirmedSuccess(text(value));
        if(phase==0&&success&&pkg.equals(lastClickPackage)&&now-lastClick<6000){startFavorite(pkg,"",true);return;}
        if(event.getEventType()==AccessibilityEvent.TYPE_VIEW_SCROLLED){if(phase!=0)fail("页面发生滚动，本次跟随已停止，未保存");transitions.clear();suppressUntil=now+700;return;}
        if(phase==0){
            if(!pkg.equals(observedPackage)){transitions.clear();observedPackage=pkg;handler.removeCallbacks(sample);handler.post(sample);}
            int type=event.getEventType();
            AccessibilityNodeInfo current=platformRoot(pkg);
            if(current!=null){
                boundFavorite=FavoriteBindings.find(this,pkg,"favorite",current,nodes(current));
                // A state refresh during initial loading is not a user favorite. A generic
                // click (even with no label), or an explicit success event, must precede it.
                if(click){AccessibilityNodeInfo clicked=favoriteSource(event.getSource());transitions.interact(scope(pkg,current.getWindowId()),clicked==null?"":nodeKey(clicked),now);}
                else if(success)transitions.interact(scope(pkg,current.getWindowId()),"",now);
            }
            if(!click&&now-lastObserved>150){lastObserved=now;observe(pkg);}
        }
        if(click){
            AccessibilityNodeInfo source=event.getSource();
            if(source!=null&&(source.isPassword()||source.isEditable()))return;
            source=favoriteSource(source);
            String label=text(event.getContentDescription());
            Boolean previous=source==null?null:transitions.previous(scope(pkg,source.getWindowId()),nodeKey(source),now);
            boolean newlySelected=source!=null&&Boolean.FALSE.equals(previous)&&selected(source);
            if(FavoriteRules.savedButton(label)&&!newlySelected){state("收到已收藏/取消收藏按钮，未确认新增收藏，未保存");return;}
            for(CharSequence s:event.getText())if(FavoriteRules.savedButton(text(s))&&!newlySelected){state("收到已收藏/取消收藏按钮，未确认新增收藏，未保存");return;}
            if(Boolean.TRUE.equals(previous)&&source!=null){reset();state("检测到取消收藏，未保存");return;}
            if(newlySelected)label="收藏";
            if(!FavoriteRules.favoriteButton(label))for(CharSequence s:event.getText())if(FavoriteRules.favoriteButton(text(s))){label=text(s);break;}
            if(!FavoriteRules.favoriteButton(label)&&source!=null&&favoriteNode(source)&&!FavoriteRules.savedButton(text(source.getText()))&&!FavoriteRules.savedButton(text(source.getContentDescription())))label="收藏";
            if(!FavoriteRules.favoriteButton(label)&&source!=null){String t=text(source.getText()),d=text(source.getContentDescription());if(FavoriteRules.favoriteButton(t))label=t;else if(FavoriteRules.favoriteButton(d))label=d;}
            if(!FavoriteRules.favoriteButton(label)&&source!=null&&(FavoriteRules.savedButton(text(source.getText()))||FavoriteRules.savedButton(text(source.getContentDescription())))){state("收到已收藏按钮，但缺少收藏前状态；未执行自动保存");return;}
            if(FavoriteRules.favoriteButton(label)&&now-lastAttempt>4000){
                startFavorite(pkg,source==null?"":nodeKey(source),false);
                // The event can already contain the post-click state.
                // Checked/selected state must confirm a save; opening a Bilibili folder picker alone is not enough.
                if(source!=null&&selected(source))wait.confirm(now);
            }else if(phase==0){observe(pkg);}
        }
        if(phase==1){
            boolean saved=FavoriteRules.success(text(event.getContentDescription()));
            for(CharSequence t:event.getText())saved|=FavoriteRules.success(text(t));
            if(saved)wait.confirm(now);
        }
    }
    private final Runnable confirm=this::confirmSaved;
    private AccessibilityNodeInfo control(AccessibilityNodeInfo n){
        if(n.isClickable())return n;
        AccessibilityNodeInfo parent=n;
        for(int i=0;i<2;i++){
            parent=parent.getParent();if(parent==null||parent.isEditable()||parent.isPassword())break;
            android.graphics.Rect bounds=new android.graphics.Rect();parent.getBoundsInScreen(bounds);
            if(parent.isClickable()&&bounds.width()<=getResources().getDisplayMetrics().widthPixels/2&&bounds.height()<=100*getResources().getDisplayMetrics().density)return parent;
        }
        return n;
    }
    private String nodeKey(AccessibilityNodeInfo n){n=control(n);android.graphics.Rect r=new android.graphics.Rect();n.getBoundsInScreen(r);return n.getWindowId()+":"+n.getViewIdResourceName()+":"+r;}
    private boolean selected(AccessibilityNodeInfo n){
        AccessibilityNodeInfo target=control(n);
        return n.isSelected()||n.isChecked()||target.isSelected()||target.isChecked()||FavoriteRules.savedState(text(n.getContentDescription()))||FavoriteRules.savedState(text(n.getText()))
            ||(Build.VERSION.SDK_INT>=30&&(FavoriteRules.savedState(text(n.getStateDescription()))||FavoriteRules.savedState(text(target.getStateDescription()))));
    }
    private boolean favoriteNode(AccessibilityNodeInfo n){return n!=null&&((boundFavorite!=null&&control(n).equals(boundFavorite))||FavoriteRules.roleId(text(n.getPackageName()),n.getViewIdResourceName(),"favorite")||FavoriteRules.favoriteButton(text(n.getText()))||FavoriteRules.favoriteButton(text(n.getContentDescription()))||FavoriteRules.savedButton(text(n.getText()))||FavoriteRules.savedButton(text(n.getContentDescription())));}
    private AccessibilityNodeInfo favoriteSource(AccessibilityNodeInfo source){
        if(source==null||favoriteNode(source))return source;
        AccessibilityNodeInfo parent=source;
        for(int i=0;i<2;i++){parent=parent.getParent();if(parent==null)break;if(favoriteNode(parent))return parent;}
        // A clickable container may expose its label only on a direct child. Never search the entire page for a click match.
        AccessibilityNodeInfo match=null;
        for(int i=0;i<source.getChildCount()&&i<12;i++){AccessibilityNodeInfo child=source.getChild(i);if(child!=null&&!child.isPassword()&&!child.isEditable()&&child.isVisibleToUser()&&favoriteNode(child)){if(match!=null)return source;match=child;}}
        return match==null?source:match;
    }
    private String scope(String pkg,int window){return pkg+":"+window;}
    private void startFavorite(String pkg,String key,boolean confirmed){
        long now=SystemClock.elapsedRealtime();if(phase!=0||now-lastAttempt<4000)return;
        reset();lastAttempt=now;pendingPackage=pkg;clickedKey=key;phase=1;wait.start(now);
        if(confirmed)wait.confirm(now);
        state(confirmed?"已识别未收藏→已收藏，准备取得原链接":"已识别收藏按钮，等待平台确认收藏成功");
        handler.postDelayed(confirm,250);
    }
    private void observe(String pkg){
        if(phase!=0)return;
        AccessibilityNodeInfo root=platformRoot(pkg);if(root==null)return;
        boundFavorite=FavoriteBindings.find(this,pkg,"favorite",root,nodes(root));
        Map<String,Boolean> states=new HashMap<>();
        for(AccessibilityNodeInfo n:nodes(root))if(favoriteNode(n)){String key=nodeKey(n);states.put(key,Boolean.TRUE.equals(states.get(key))||selected(n));}
        long now=SystemClock.elapsedRealtime();
        String changed=transitions.update(scope(pkg,root.getWindowId()),states,now);
        if(changed!=null&&now>=suppressUntil)startFavorite(pkg,changed,true);
        else {
            String diagnostic=pkg+" · 可识别收藏控件 "+states.size()+" 个"+(states.isEmpty()?"；可到我→适配本机按钮记录收藏图标":"；等待新增收藏");
            if(!diagnostic.equals(lastDiagnosticText)&&now-lastDiagnostic>2000){lastDiagnostic=now;lastDiagnosticText=diagnostic;state(diagnostic);}
        }
    }
    private AccessibilityNodeInfo platformRoot(String pkg){
        AccessibilityNodeInfo node=getRootInActiveWindow();
        if(node!=null&&pkg.equals(text(node.getPackageName())))return node;
        // App dialogs and share panels may be in a separate accessibility window.
        for(AccessibilityWindowInfo window:getWindows())if(window.isActive()||window.isFocused()){
            node=window.getRoot();if(node!=null&&pkg.equals(text(node.getPackageName())))return node;
        }
        return null;
    }
    private AccessibilityNodeInfo root(){AccessibilityNodeInfo node=platformRoot(pendingPackage);boundFavorite=FavoriteBindings.find(this,pendingPackage,"favorite",node,nodes(node));return node;}
    private List<AccessibilityNodeInfo> nodes(AccessibilityNodeInfo root){
        List<AccessibilityNodeInfo> result=new ArrayList<>();if(root==null)return result;
        ArrayDeque<AccessibilityNodeInfo> queue=new ArrayDeque<>();queue.add(root);
        while(!queue.isEmpty()&&result.size()<800){AccessibilityNodeInfo n=queue.remove();result.add(n);for(int i=0;i<n.getChildCount()&&queue.size()<800;i++){AccessibilityNodeInfo child=n.getChild(i);if(child!=null&&child.isVisibleToUser()&&!child.isPassword()&&!child.isEditable())queue.add(child);}}
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
        AccessibilityNodeInfo root=root();
        if(!copy){AccessibilityNodeInfo learned=FavoriteBindings.find(this,pendingPackage,"share",root,nodes(root));if(learned!=null)return learned;}
        Map<String,AccessibilityNodeInfo> matches=new LinkedHashMap<>();
        for(AccessibilityNodeInfo n:nodes(root)){
            String t=text(n.getText()),d=text(n.getContentDescription());
            if(!(FavoriteRules.roleId(pendingPackage,n.getViewIdResourceName(),copy?"copy":"share")||(copy?(FavoriteRules.copy(t)||FavoriteRules.copy(d)):(FavoriteRules.share(t)||FavoriteRules.share(d)))))continue;
            AccessibilityNodeInfo target=n;for(int i=0;!target.isClickable()&&i<2;i++){target=target.getParent();if(target==null)break;}
            if(target==null||!target.isClickable())target=n;
            if(target.isEnabled()&&target.isVisibleToUser()){
                android.graphics.Rect bounds=new android.graphics.Rect();target.getBoundsInScreen(bounds);matches.put(bounds.toString(),target);
            }
        }
        // Ambiguous controls are deliberately not clicked.
        return matches.size()==1?matches.values().iterator().next():null;
    }
    private boolean clickNode(AccessibilityNodeInfo node){
        if(node==null||!node.isVisibleToUser()||!node.isEnabled()||node.isEditable()||node.isPassword())return false;
        android.graphics.Rect r=new android.graphics.Rect();node.getBoundsInScreen(r);
        if(r.isEmpty()||r.width()>getResources().getDisplayMetrics().widthPixels*0.6||r.height()>getResources().getDisplayMetrics().heightPixels*0.25)return false;
        if(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))return true;
        android.graphics.Path path=new android.graphics.Path();path.moveTo(r.exactCenterX(),r.exactCenterY());
        return dispatchGesture(new android.accessibilityservice.GestureDescription.Builder().addStroke(new android.accessibilityservice.GestureDescription.StrokeDescription(path,0,70)).build(),null,null);
    }
    private void beginShare(){
        if(phase!=1||!enabled(this))return;
        if(root()==null){fail("无法读取原平台当前窗口，本次未保存");return;}
        AccessibilityNodeInfo share=button(false);lastAttempt=SystemClock.elapsedRealtime();
        if(share==null){manualShare();return;}
        phase=2;copyTimeout=5000;copyStarted=SystemClock.elapsedRealtime();
        state("已识别收藏状态，正在从原平台取得链接");
        if(!clickNode(share)){fail("无法打开分享菜单，本次未保存");return;}
        handler.postDelayed(pollCopy,300);
    }
    private final Runnable pollCopy=new Runnable(){public void run(){
        if(phase!=2||!enabled(FavoriteCaptureService.this)){reset();return;}
        AccessibilityNodeInfo copy=button(true);
        if(copy!=null){
            if(prompt!=null){prompt.dismiss();prompt=null;}
            String pkg=pendingPackage;long timestamp=System.currentTimeMillis();
            boolean clicked=clickNode(copy);
            if(!clicked){fail("未能复制原内容链接，本次未保存");return;}phase=3;
            handler.postDelayed(()->{
                if(!enabled(FavoriteCaptureService.this))return;
                AccessibilityNodeInfo current=getRootInActiveWindow();if(current==null||!pkg.equals(text(current.getPackageName()))){fail("复制后已离开原平台，本次未保存");return;}
                try{
                    launchCapture(pkg,timestamp);
                }catch(Exception e){offerCapture(pkg,timestamp);}
            },450);return;
        }
        if(SystemClock.elapsedRealtime()-copyStarted>copyTimeout){fail("未找到「复制链接」，本次未保存。请在分享菜单选择拾光。");return;}
        handler.postDelayed(this,250);
    }};
    private void manualShare(){
        phase=2;copyStarted=SystemClock.elapsedRealtime();copyTimeout=30000;
        state("已识别收藏，当前分享图标未适配；请点原App分享/更多，随后自动继续");
        String pkg=pendingPackage;prompt=new FavoritePrompt(this);
        prompt.show("拾光 · 已识别收藏","请点原 App 的分享或右上角 ⋯。出现复制链接后，拾光会继续打开分类窗口。也可记录这个分享图标，供下次自动使用。","记录分享按钮",()->startLearning(pkg,"share"),()->{reset();state("已取消，未保存");});
        handler.postDelayed(pollCopy,300);
    }
    static boolean calibrate(String pkg,String role){if(instance==null||!enabled(instance))return false;instance.startLearning(pkg,role);return true;}
    private void startLearning(String pkg,String role){
        reset();if(!enabled(this))return;phase=4;pendingPackage=pkg;learningRole=role;
        String label=role.equals("share")?"分享（或 ⋯）":"收藏";
        state("准备记录"+label+"按钮，请先进入原App的内容详情页");
        prompt=new FavoritePrompt(this);
        if(!prompt.show("拾光 · 适配本机按钮","先打开"+(pkg.equals("com.xingin.xhs")?"小红书笔记":"B站视频")+"详情页"+(role.equals("favorite")?"（请选择尚未收藏的内容）":"")+"，再点下方“记录下一次点击”，然后只点一次原App的"+label+"按钮。不会记录正文或账号。","记录下一次点击",()->{
            prompt.dismiss();prompt=null;learningArmed=true;state("请点一次原App的"+label+"按钮");Toast.makeText(this,"请点原App的"+label+"按钮",Toast.LENGTH_LONG).show();
        },()->{reset();state("按钮适配已取消");})){fail("无法显示按钮适配提示，请检查无障碍服务连接");return;}
        handler.postDelayed(()->fail("按钮适配已超时，请重新开始"),60000);
    }
    private void learnClick(AccessibilityEvent event,String pkg){
        if(!learningArmed||(!pkg.equals(pendingPackage)&&!(pendingPackage.equals("tv.danmaku.bili")&&FavoriteRules.supported(pkg)&&!pkg.equals("com.xingin.xhs"))))return;
        if(event.getEventType()!=AccessibilityEvent.TYPE_VIEW_CLICKED&&event.getEventType()!=AccessibilityEvent.TYPE_VIEW_LONG_CLICKED)return;
        AccessibilityNodeInfo target=event.getSource();if(target==null){state("系统未提供点击控件，请再点一次按钮");return;}
        target=control(target);AccessibilityNodeInfo page=target;
        for(int i=0;i<30;i++){AccessibilityNodeInfo parent=page.getParent();if(parent==null)break;page=parent;}
        String rule=FavoriteBindings.capture(page,target);if(rule==null){state("未取得独立按钮，请只点图标，不要点页面空白处");return;}
        pendingPackage=pkg;learningArmed=false;learningRule=rule;
        if(learningRole.equals("favorite")){
            FavoriteBindings.save(this,pkg,"favorite",rule);String key=nodeKey(target);reset();
            state("已记录本机收藏按钮，等待平台确认本次收藏");startFavorite(pkg,key,false);return;
        }
        // A share binding is activated only after the menu exposes Copy link.
        handler.postDelayed(new Runnable(){final long until=SystemClock.elapsedRealtime()+5000;public void run(){
            if(phase!=4)return;
            if(button(true)!=null){FavoriteBindings.save(FavoriteCaptureService.this,pkg,"share",learningRule);state("已验证并记录分享按钮，继续取得原链接");phase=2;copyTimeout=5000;copyStarted=SystemClock.elapsedRealtime();handler.removeCallbacksAndMessages(null);handler.post(pollCopy);return;}
            if(SystemClock.elapsedRealtime()>=until){fail("未出现复制链接菜单，分享按钮未记录；请重新适配");return;}
            handler.postDelayed(this,250);
        }},250);
    }
    private void launchCapture(String pkg,long timestamp){
        state("已复制原链接，正在打开分类窗口");
        startActivity(new Intent(this,ClipboardCaptureActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("expectedPackage",pkg).putExtra("notBefore",timestamp));
        handler.postDelayed(()->{if(getPrefs(this).getLong("launchAck",0)!=timestamp)offerCapture(pkg,timestamp);else reset();},2000);
    }
    private void offerCapture(String pkg,long timestamp){
        if(!enabled(this)){reset();return;}
        state("已取得复制动作；系统未弹出界面，请点屏幕下方「选择分类」继续");
        if(prompt!=null)prompt.dismiss();
        prompt=new FavoritePrompt(this);
        boolean shown=prompt.show(()->{
            handler.removeCallbacksAndMessages(null);
            if(prompt!=null){prompt.dismiss();prompt=null;}
            try{launchCapture(pkg,timestamp);}catch(Exception e){fail("系统仍未允许打开拾光，请在原内容使用分享→拾光；未保存");}
        },()->{reset();state("已取消本次收藏跟随，未保存");});
        if(!shown){fail("无法显示分类提示，请使用原内容的分享→拾光；未保存");return;}
        handler.postDelayed(()->{reset();state("分类提示已超时，本次未保存");},45000);
    }
    private void fail(String message){reset();state(message);Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    @Override public void onInterrupt(){reset();state("服务被系统中断，请返回无障碍设置检查");}
    @Override public void onDestroy(){reset();if(instance==this)instance=null;super.onDestroy();}
}
