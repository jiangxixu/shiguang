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
    private String pendingPackage="",pendingShareRule="",pendingTitle="";
    private long lastAttempt,copyStarted;
    private String clickedKey="";
    private final FavoriteWait wait=new FavoriteWait();
    private final FavoriteScene scene=new FavoriteScene();
    private final MenuOrigin menuOrigin=new MenuOrigin();
    private final CaptureReadiness shareReadiness=new CaptureReadiness();
    private final CaptureReadiness sampleReadiness=new CaptureReadiness();
    private String pendingFavoriteSelector="";
    private boolean samplingInterrupted;
    private int phase; // 0 idle, 1 waiting for favorite success, 2 locating copy-link action
    private final FavoriteTransitions transitions=new FavoriteTransitions();
    private FavoritePrompt prompt;
    private FavoriteCaptureOverlay captureOverlay;
    private String observedPackage="";
    private long suppressUntil;
    private long lastObserved,lastDiagnostic,lastClick,lastClickDiagnostic;
    private String lastClickPackage="",lastDiagnosticText="";
    private boolean learningArmed,scrollMessageSent,zhihuMoreOpened,zhihuShareOpened;
    private long copyTimeout=3000;
    private AccessibilityNodeInfo boundFavorite;
    private final Runnable sample=new Runnable(){public void run(){
        if(!enabled(FavoriteCaptureService.this)||phase!=0)return;
        try{
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(sampleReadiness.retry(SystemClock.elapsedRealtime(),root!=null)){handler.postDelayed(this,250);return;}
        String pkg=root==null?"":text(root.getPackageName());
        if(!FavoriteRules.supported(pkg)){transitions.clear();menuOrigin.clear();observedPackage="";handler.postDelayed(this,1000);return;}
        observe(pkg);
        if(phase==0)handler.postDelayed(this,500);
        }catch(RuntimeException error){reset();state("页面检测异常，已重新等待收藏："+error.getClass().getSimpleName());}
    }};
    private final CaptureEventBuffer<AccessibilityEvent> contentEvents=new CaptureEventBuffer<>(AccessibilityEvent::recycle);
    private long lastEventAt,eventCount;
    private String lastEventPackage="";
    private final Runnable drainContent=()->{
        AccessibilityEvent event=contentEvents.take();if(event==null)return;
        try{handleSafely(event);}finally{event.recycle();}
    };
    private static FavoriteCaptureService instance;
    static boolean active(){return instance!=null;}
    static String status(Context c){return c.getSharedPreferences("automatic",MODE_PRIVATE).getString("status","尚未检测到收藏操作");}
    static void report(Context c,String text){
        String line=new java.text.SimpleDateFormat("HH:mm:ss",java.util.Locale.ROOT).format(new Date())+" · "+text;
        String[] old=getPrefs(c).getString("history","").split("\n");
        StringBuilder history=new StringBuilder();for(int i=Math.max(0,old.length-59);i<old.length;i++)if(!old[i].isEmpty())history.append(old[i]).append('\n');
        history.append(line);getPrefs(c).edit().putString("status",line).putString("history",history.toString()).apply();
    }
    static String history(Context c){
        StringBuilder out=new StringBuilder();
        FavoriteCaptureService service=instance;
        if(service!=null){
            out.append("本次连接收到事件：").append(service.eventCount).append("\n");
            out.append("最近事件：").append(service.lastEventAt==0?"尚未收到":((SystemClock.elapsedRealtime()-service.lastEventAt)/1000)+"秒前 · "+service.lastEventPackage).append("\n");
            out.append("检测阶段：").append(service.phase==0?"等待收藏":service.phase==1?"等待原内容返回":service.phase==2?"取得原链接":service.phase==3?"校验原链接":service.phase==4?"适配按钮":"分类窗口").append(service.samplingInterrupted?" · 已中断":"").append("\n\n");
        }
        out.append(getPrefs(c).getString("history","暂无检测记录"));boolean found=false;
        for(String pkg:FavoriteRules.PACKAGES){String data=getPrefs(c).getString("pageDiagnostic:"+pkg,"");if(!data.isEmpty()){out.append("\n\n").append(data);found=true;}}
        if(!found)out.append("\n\n").append(getPrefs(c).getString("pageDiagnostic",""));return out.toString();
    }
    private static android.content.SharedPreferences getPrefs(Context c){return c.getSharedPreferences("automatic",MODE_PRIVATE);}
    static void acknowledge(Context c,long id){getPrefs(c).edit().putLong("launchAck",id).apply();report(c,"已打开链接校验窗口");}
    private void state(String text){report(this,text);}
    static boolean enabled(Context c){return c.getSharedPreferences("automatic",MODE_PRIVATE).getBoolean("enabled",false);}
    static void pause(Context c){c.getSharedPreferences("automatic",MODE_PRIVATE).edit().putBoolean("enabled",false).apply();if(instance!=null){instance.reset();instance.disableSelf();}}
    @Override protected void onServiceConnected(){instance=this;samplingInterrupted=false;eventCount=0;lastEventAt=0;lastEventPackage="";reset();state("服务已连接，等待已支持平台的收藏/保存操作");}
    private void reset(){handler.removeCallbacksAndMessages(null);contentEvents.clear();if(captureOverlay!=null){captureOverlay.dismiss();captureOverlay=null;}if(prompt!=null){prompt.dismiss();prompt=null;}phase=0;pendingFavoriteSelector="";shareReadiness.reset();sampleReadiness.reset();pendingPackage="";pendingShareRule="";pendingTitle="";zhihuMoreOpened=false;zhihuShareOpened=false;clickedKey="";transitions.clear();scene.clear();menuOrigin.clear();observedPackage="";learningArmed=false;scrollMessageSent=false;boundFavorite=null;if(instance==this&&enabled(this))handler.postDelayed(sample,200);}
    private String text(CharSequence value){return value==null?"":value.toString();}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(event==null)return;
        String pkg=text(event.getPackageName());
        if(FavoriteRules.supported(pkg)){lastEventAt=SystemClock.elapsedRealtime();lastEventPackage=pkg;eventCount++;}
        if(!enabled(this)){reset();return;}
        if(!FavoriteRules.supported(pkg))return;
        boolean receipt=FavoriteRules.confirmedSuccess(text(event.getContentDescription()));
        for(CharSequence value:event.getText())receipt|=FavoriteRules.confirmedSuccess(text(value));
        if(phase==0&&event.getEventType()==AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED&&!receipt){
            if(contentEvents.offer(AccessibilityEvent.obtain(event),pkg+":"+event.getWindowId())){
                handler.removeCallbacks(drainContent);handler.postDelayed(drainContent,80);
            }
            return;
        }
        // Clicks, success receipts and window changes run immediately. Do not replay
        // an older content event after a user action or a navigation boundary.
        handler.removeCallbacks(drainContent);contentEvents.clear();handleSafely(event);
    }
    private void handleSafely(AccessibilityEvent event){
        try{handleEvent(event);}catch(RuntimeException error){
            reset();state("检测发生异常，已重新等待收藏："+error.getClass().getSimpleName());
        }
    }
    private void handleEvent(AccessibilityEvent event){
        if(!enabled(this)){reset();return;}
        if(samplingInterrupted){samplingInterrupted=false;handler.removeCallbacks(sample);handler.post(sample);state("已重新收到系统事件，自动检测已恢复");}
        String pkg=text(event.getPackageName());if(!FavoriteRules.supported(pkg))return;
        // A visible classification form owns its lifecycle; background app
        // events must not close it or discard categories the user is entering.
        if(phase==3||phase==5)return;
        // Toasts/announcements may arrive after the menu has closed, with no
        // article root. Consume the scoped receipt before the article UI gate.
        if(WechatConfirmation.pending(phase,pendingPackage,pkg,SystemClock.elapsedRealtime()-lastAttempt)&&WechatConfirmation.transientEvent(event.getEventType())){
            boolean receipt=WechatConfirmation.receipt(text(event.getContentDescription()));
            for(CharSequence value:event.getText())receipt|=WechatConfirmation.receipt(text(value));
            if(receipt){wait.confirm(SystemClock.elapsedRealtime());state("已收到微信公众号收藏成功提示，等待原文章返回");}
            return;
        }
        if("com.tencent.mm".equals(pkg)){
            AccessibilityNodeInfo article=platformRoot(pkg);
            if(!wechatArticleNodes(article).isEmpty()){
                menuOrigin.observe(pkg,article.getWindowId(),sourceTitle(article),sceneText(article),SystemClock.elapsedRealtime());
            }
            AccessibilityNodeInfo clicked=event.getSource();
            if(event.getEventType()==AccessibilityEvent.TYPE_VIEW_CLICKED&&wechatMoreControl(clicked))menuOrigin.open(pkg,clicked.getWindowId(),SystemClock.elapsedRealtime());
            if(!wechatAllowed(article)){
                if(phase==0){transitions.clear();lastClick=0;lastClickPackage="";}return;
            }
        }
        if(phase==4){learnClick(event,pkg);return;}
        if(phase!=0&&!pkg.equals(pendingPackage)){
            AccessibilityNodeInfo foreground=getRootInActiveWindow();
            boolean transientEvent=WechatConfirmation.transientEvent(event.getEventType());
            if(CaptureReadiness.foreignForeground(pendingPackage,pkg,text(foreground==null?null:foreground.getPackageName()),transientEvent))fail("已切换平台，本次收藏跟随已取消，未保存");
            return;
        }
        long now=SystemClock.elapsedRealtime();
        if(phase==2){
            // Learn a missing share entry from a user click only after Copy link appears.
            if(copyTimeout==30000&&pkg.equals(pendingPackage)&&(event.getEventType()==AccessibilityEvent.TYPE_VIEW_CLICKED||event.getEventType()==AccessibilityEvent.TYPE_VIEW_LONG_CLICKED)){
                AccessibilityNodeInfo target=event.getSource();if(target!=null){target=control(target);AccessibilityNodeInfo page=platformRoot(pkg);if(page!=null){String rule=FavoriteBindings.capture(page,target);if(rule!=null)pendingShareRule=rule;}}
            }
            return;
        }
        if(phase>=3)return;
        boolean click=event.getEventType()==AccessibilityEvent.TYPE_VIEW_CLICKED||event.getEventType()==AccessibilityEvent.TYPE_VIEW_LONG_CLICKED;
        if(click){
            // Sharing by itself is not a collection request. Disarm any prior
            // observation so opening a share sheet cannot start a capture.
            AccessibilityNodeInfo clickedControl=event.getSource();
            if(phase==0){AccessibilityNodeInfo current=platformRoot(pkg);boundFavorite=FavoriteBindings.find(this,pkg,"favorite",current,nodes(current));}
            boolean videoMenu=phase==0&&(("com.google.android.youtube".equals(pkg)&&youtubeOverflow(clickedControl))||("com.tencent.mm".equals(pkg)&&wechatMoreControl(clickedControl)));
            if(phase==0&&!videoMenu&&!favoriteNode(favoriteSource(clickedControl))&&shareSource(clickedControl,pkg)){
                transitions.clear();menuOrigin.clear();lastClick=0;lastClickPackage="";return;
            }
            lastClick=now;lastClickPackage=pkg;
            // Opening More may precede Save; remember the video but do not capture it.
            if(videoMenu)menuOrigin.open(pkg,clickedControl.getWindowId(),now);
        }
        boolean success=FavoriteRules.confirmedSuccess(text(event.getContentDescription()));
        for(CharSequence value:event.getText())success|=FavoriteRules.confirmedSuccess(text(value));
        if(phase==0&&success&&pkg.equals(lastClickPackage)&&now-lastClick<6000){startFavorite(pkg,"",true);return;}
        if(event.getEventType()==AccessibilityEvent.TYPE_VIEW_SCROLLED){
            if(phase==1){if(!scrollMessageSent){scrollMessageSent=true;state("检测到滚动或收藏夹布局变化，继续等待收藏确认及原内容返回");}return;}
            if(menuOrigin.source(pkg,now)==null)menuOrigin.clear();
            transitions.clear();suppressUntil=now+700;return;
        }
        if(phase==0){
            if(!pkg.equals(observedPackage)){transitions.clear();if(!observedPackage.isEmpty())menuOrigin.clear();observedPackage=pkg;handler.removeCallbacks(sample);handler.post(sample);}
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
            AccessibilityNodeInfo original=source;
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
            }else if(phase==0){
                observe(pkg);
                if(phase==0&&(original==null||compact(original))&&now-lastClickDiagnostic>2000){lastClickDiagnostic=now;recordPageDiagnostic(pkg,"未识别的点击",original);}
            }
        }
        if(phase==1){
            boolean saved=FavoriteRules.pendingSuccess(pkg,text(event.getContentDescription()));
            for(CharSequence t:event.getText())saved|=FavoriteRules.pendingSuccess(pkg,text(t));
            if(saved)wait.confirm(now);
        }
    }
    private final Runnable confirm=this::confirmSaved;
    private AccessibilityNodeInfo control(AccessibilityNodeInfo n){
        if(n.isClickable())return n;
        AccessibilityNodeInfo parent=n;
        for(int i=0;i<6;i++){
            parent=parent.getParent();if(parent==null||parent.isEditable()||parent.isPassword())break;
            android.graphics.Rect bounds=new android.graphics.Rect();parent.getBoundsInScreen(bounds);
            if(parent.isClickable()&&bounds.width()<=getResources().getDisplayMetrics().widthPixels&&bounds.height()<=96*getResources().getDisplayMetrics().density)return parent;
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
        for(int i=0;i<6;i++){parent=parent.getParent();if(parent==null||!compact(parent))break;if(favoriteNode(parent))return parent;}
        // Compact action chips can nest their text below several layout wrappers.
        // Search only the clicked chip, never the whole page, and reject competing actions.
        AccessibilityNodeInfo match=compact(source)?VisibleNodes.uniqueTarget(nodes(source),n->favoriteNode(n)?control(n):null):null;
        if(match!=null)for(AccessibilityNodeInfo n:nodes(source))if(favoriteNode(n)&&control(n).equals(match))return n;
        return source;
    }
    private boolean compact(AccessibilityNodeInfo node){
        if(node==null)return false;android.graphics.Rect r=new android.graphics.Rect();node.getBoundsInScreen(r);
        android.util.DisplayMetrics m=getResources().getDisplayMetrics();return MenuActions.compact(r.width(),r.height(),m.widthPixels,m.density);
    }
    private boolean shareSource(AccessibilityNodeInfo source,String pkg){
        AccessibilityNodeInfo page=platformRoot(pkg),learned=FavoriteBindings.find(this,pkg,"share",page,nodes(page));
        for(int depth=0;source!=null&&depth<3;depth++,source=source.getParent()){
            if((learned!=null&&control(source).equals(learned))||FavoriteRules.share(text(source.getText()))||FavoriteRules.share(text(source.getContentDescription()))||FavoriteRules.roleId(pkg,source.getViewIdResourceName(),"share")||("com.zhihu.android".equals(pkg)&&(ZhihuPageRules.share(text(source.getText()))||ZhihuPageRules.share(text(source.getContentDescription()))||ZhihuPageRules.shareId(source.getViewIdResourceName()))))return true;
            if(depth==0)for(int i=0;i<source.getChildCount()&&i<12;i++){AccessibilityNodeInfo child=source.getChild(i);if(child!=null&&(FavoriteRules.share(text(child.getText()))||FavoriteRules.share(text(child.getContentDescription()))||("com.zhihu.android".equals(pkg)&&(ZhihuPageRules.share(text(child.getText()))||ZhihuPageRules.share(text(child.getContentDescription()))))))return true;}
        }
        return false;
    }
    private String scope(String pkg,int window){return pkg+":"+window;}
    private void startFavorite(String pkg,String key,boolean confirmed){
        long now=SystemClock.elapsedRealtime();if(phase!=0||now-lastAttempt<4000)return;
        if("com.tencent.mm".equals(pkg)&&!wechatAllowed(platformRoot(pkg)))return;
        MenuOrigin.Source origin=menuOrigin.source(pkg,now);
        AccessibilityNodeInfo before=platformRoot(pkg);String selector="";
        if(!key.isEmpty())for(AccessibilityNodeInfo n:nodes(before))if(nodeKey(n).equals(key)){
            String captured=FavoriteBindings.capture(before,control(n));if(captured!=null){selector=captured;break;}
        }
        reset();shareReadiness.reset();lastAttempt=now;pendingPackage=pkg;clickedKey=key;phase=1;wait.start(now);
        pendingFavoriteSelector=selector;
        AccessibilityNodeInfo page=platformRoot(pkg);pendingTitle=origin==null?sourceTitle(page):origin.title;
        scene.start(origin==null?(page==null?-1:page.getWindowId()):origin.window,origin==null?sceneText(page):origin.anchor,now);
        // The user's favorite/save action is enough to open the editor. Saving
        // into Shiguang is confirmed in that editor, independently of app toasts.
        phase=5;
        captureOverlay=new FavoriteCaptureOverlay(this,message->{reset();state(message);},()->{});
        if(!captureOverlay.showPending(pkg,pendingTitle,()->{
            phase=1;shareReadiness.reset();long at=SystemClock.elapsedRealtime();lastAttempt=at;wait.start(at);wait.confirm(at);
            // The user may spend minutes choosing categories; renew only the
            // deadline, retaining the original article identity.
            scene.renew(at);state("已选择分类，正在取得本次原内容链接");handler.postDelayed(confirm,250);
        })){reset();state("无法打开分类窗口，请检查系统 WebView 和无障碍连接");return;}
        state("已识别收藏/保存，已直接打开分类窗口");
    }
    private void observe(String pkg){
        if(phase!=0)return;
        AccessibilityNodeInfo root=platformRoot(pkg);if(root==null)return;
        if("com.tencent.mm".equals(pkg)&&!wechatAllowed(root))return;
        if("com.google.android.youtube".equals(pkg)||("com.tencent.mm".equals(pkg)&&!wechatArticleNodes(root).isEmpty()))menuOrigin.observe(pkg,root.getWindowId(),sourceTitle(root),sceneText(root),SystemClock.elapsedRealtime());
        boundFavorite=FavoriteBindings.find(this,pkg,"favorite",root,nodes(root));
        Map<String,Boolean> states=new HashMap<>();
        for(AccessibilityNodeInfo n:nodes(root))if(favoriteNode(n)){String key=nodeKey(n);states.put(key,Boolean.TRUE.equals(states.get(key))||selected(n));}
        long now=SystemClock.elapsedRealtime();
        String changed=transitions.update(scope(pkg,root.getWindowId()),states,now);
        if(changed!=null&&now>=suppressUntil)startFavorite(pkg,changed,true);
        else {
            String diagnostic=pkg+" · 可识别收藏控件 "+states.size()+" 个"+(states.isEmpty()?(FavoriteBindings.configured(this,pkg,"favorite")?"；本机适配已保留，当前页面未匹配收藏/保存按钮":"；当前页面未提供收藏/保存控件，可先适配本机按钮"):"；等待新增收藏");
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
    private AccessibilityNodeInfo root(){
        AccessibilityNodeInfo node=platformRoot(pendingPackage);
        if(phase==2){AccessibilityNodeInfo active=getRootInActiveWindow();if(active!=null&&FavoriteRules.shareSheet(text(active.getPackageName())))node=active;}
        boundFavorite=FavoriteBindings.find(this,pendingPackage,"favorite",node,nodes(node));return node;
    }
    private List<AccessibilityNodeInfo> nodes(AccessibilityNodeInfo root){
        return VisibleNodes.collect(root,new VisibleNodes.Tree<AccessibilityNodeInfo>(){
            public boolean visible(AccessibilityNodeInfo n){return n.isVisibleToUser();}
            public boolean sensitive(AccessibilityNodeInfo n){return n.isPassword()||n.isEditable();}
            public int children(AccessibilityNodeInfo n){return n.getChildCount();}
            public AccessibilityNodeInfo child(AccessibilityNodeInfo n,int index){return n.getChild(index);}
        });
    }
    private String sourceTitle(AccessibilityNodeInfo page){
        if(page==null)return "";String pkg=text(page.getPackageName());
        if(pkg.equals("com.zhihu.android"))return zhihuTitle(page);
        if(pkg.equals("com.tencent.mm")){
            String title="";
            for(AccessibilityNodeInfo n:wechatArticleNodes(page))if((Build.VERSION.SDK_INT>=28&&n.isHeading())||text(n.getViewIdResourceName()).matches("(?:.*[:/])?activity-name")){
                String value=WechatArticle.title(text(n.getText()));if(value.isEmpty())value=WechatArticle.title(text(n.getContentDescription()));
                if(!value.isEmpty()){if(!title.isEmpty()&&!title.equals(value))return "";title=value;}
            }
            return title;
        }
        String unique="";android.graphics.Rect screen=new android.graphics.Rect();page.getBoundsInScreen(screen);
        for(AccessibilityNodeInfo n:nodes(page))if(PageTitles.titleId(pkg,n.getViewIdResourceName())){
            android.graphics.Rect box=new android.graphics.Rect();n.getBoundsInScreen(box);
            if("com.google.android.youtube".equals(pkg)&&!screen.isEmpty()&&box.top>screen.top+screen.height()*.60)continue;
            String value=PageTitles.clean(text(n.getText()));if(value.isEmpty())value=PageTitles.clean(text(n.getContentDescription()));
            if(value.isEmpty())continue;if(!unique.isEmpty()&&!unique.equals(value))return "";unique=value;
        }
        if(unique.isEmpty()&&FavoriteRules.platform(pkg)==3&&!screen.isEmpty()){
            List<DouyinCaption.Text> captions=new ArrayList<>();
            for(AccessibilityNodeInfo n:nodes(page))if("android.widget.TextView".equals(text(n.getClassName()))){
                android.graphics.Rect box=new android.graphics.Rect();n.getBoundsInScreen(box);
                captions.add(new DouyinCaption.Text(text(n.getText()),(double)(box.left-screen.left)/screen.width(),(double)(box.top-screen.top)/screen.height(),(double)box.width()/screen.width()));
            }
            unique=DouyinCaption.select(captions);
        }
        return unique;
    }
    private List<AccessibilityNodeInfo> wechatArticleNodes(AccessibilityNodeInfo page){
        List<AccessibilityNodeInfo> empty=Collections.emptyList();if(page==null||!"com.tencent.mm".equals(text(page.getPackageName())))return empty;
        for(AccessibilityNodeInfo n:nodes(page))if(text(n.getClassName()).endsWith("WebView")){
            List<AccessibilityNodeInfo> content=nodes(n);
            for(AccessibilityNodeInfo item:content)if(WechatArticle.marker(item.getViewIdResourceName(),text(item.getText()))||WechatArticle.marker(item.getViewIdResourceName(),text(item.getContentDescription())))return content;
        }
        return empty;
    }
    private boolean wechatAllowed(AccessibilityNodeInfo page){
        if(!wechatArticleNodes(page).isEmpty())return true;
        // A copy-link menu is accepted only after a verified article's More action,
        // or during the capture already initiated from that article.
        boolean context=menuOrigin.source("com.tencent.mm",SystemClock.elapsedRealtime())!=null||((phase==1||phase==2)&&"com.tencent.mm".equals(pendingPackage)&&scene.tracked());
        if(!context)return false;
        for(AccessibilityNodeInfo n:nodes(page))if(FavoriteRules.copy(text(n.getText()))||FavoriteRules.copy(text(n.getContentDescription())))return true;
        return false;
    }
    private boolean wechatMoreControl(AccessibilityNodeInfo node){
        if(node==null||!compact(node))return false;
        android.graphics.Rect box=new android.graphics.Rect();node.getBoundsInScreen(box);android.util.DisplayMetrics m=getResources().getDisplayMetrics();
        if(box.centerX()<m.widthPixels*.7||box.centerY()>m.heightPixels*.2)return false;
        for(AccessibilityNodeInfo n:nodes(node))if(WechatArticle.more(text(n.getText()))||WechatArticle.more(text(n.getContentDescription())))return true;
        return false;
    }
    private AccessibilityNodeInfo wechatMore(AccessibilityNodeInfo page){
        AccessibilityNodeInfo found=null;
        for(AccessibilityNodeInfo n:nodes(page))if(wechatMoreControl(n)){
            AccessibilityNodeInfo target=control(n);if(found!=null&&!found.equals(target))return null;found=target;
        }
        return found;
    }
    private boolean youtubeOverflow(AccessibilityNodeInfo source){
        if(source==null||!compact(source))return false;
        android.graphics.Rect box=new android.graphics.Rect();source.getBoundsInScreen(box);
        android.util.DisplayMetrics screen=getResources().getDisplayMetrics();
        if(box.centerY()>screen.heightPixels*.65||box.centerX()<screen.widthPixels*.5)return false;
        if(MenuActions.youtubeMore(text(source.getText()),text(source.getContentDescription()),source.getViewIdResourceName()))return true;
        for(AccessibilityNodeInfo n:nodes(source))if(MenuActions.youtubeMore(text(n.getText()),text(n.getContentDescription()),n.getViewIdResourceName()))return true;
        return false;
    }
    private String zhihuTitle(AccessibilityNodeInfo page){
        if(page==null||!"com.zhihu.android".equals(text(page.getPackageName())))return "";
        android.graphics.Rect screen=new android.graphics.Rect();page.getBoundsInScreen(screen);if(screen.isEmpty())return "";
        List<AccessibilityNodeInfo> pageNodes=nodes(page);double authorTop=-1;
        for(AccessibilityNodeInfo n:pageNodes)if(ZhihuPageRules.authorAction(text(n.getText()))||ZhihuPageRules.authorAction(text(n.getContentDescription()))){
            android.graphics.Rect box=new android.graphics.Rect();n.getBoundsInScreen(box);double top=(double)(box.top-screen.top)/screen.height();
            if(top>0&&top<0.45&&(authorTop<0||top<authorTop))authorTop=top;
        }
        String best="";int bestScore=-1;float bestTop=Float.MAX_VALUE;
        for(AccessibilityNodeInfo n:pageNodes){
            android.graphics.Rect box=new android.graphics.Rect();n.getBoundsInScreen(box);
            String value=text(n.getText());if(value.isEmpty())value=text(n.getContentDescription());
            int score=ZhihuPageRules.titleScore(value,n.getViewIdResourceName(),Build.VERSION.SDK_INT>=28&&n.isHeading(),(double)(box.top-screen.top)/screen.height(),(double)box.width()/screen.width());
            score=Math.max(score,ZhihuPageRules.articleTitleScore(value,(double)(box.top-screen.top)/screen.height(),(double)(box.bottom-screen.top)/screen.height(),(double)box.width()/screen.width(),authorTop));
            if(score>bestScore||(score>=0&&score==bestScore&&box.top<bestTop)){bestScore=score;best=ZhihuPageRules.title(value);bestTop=box.top;}
        }
        return best;
    }
    private AccessibilityNodeInfo zhihuToolbar(Collection<AccessibilityNodeInfo> candidates,AccessibilityNodeInfo page){
        // Candidates already have a semantic share/more label or resource ID.
        // A missing extracted title must not disable an otherwise verified entry.
        if(!"com.zhihu.android".equals(pendingPackage)||page==null)return null;
        android.graphics.Rect screen=new android.graphics.Rect();page.getBoundsInScreen(screen);if(screen.isEmpty())return null;
        AccessibilityNodeInfo best=null;int score=-1;boolean tie=false;
        for(AccessibilityNodeInfo node:candidates){
            android.graphics.Rect box=new android.graphics.Rect();node.getBoundsInScreen(box);
            int value=ZhihuPageRules.toolbarScore((box.exactCenterX()-screen.left)/screen.width(),(box.exactCenterY()-screen.top)/screen.height());
            if(value>score){score=value;best=node;tie=false;}else if(value>=0&&value==score&&!node.equals(best)){
                android.graphics.Rect previous=new android.graphics.Rect();best.getBoundsInScreen(previous);
                if(previous.contains(box)){best=node;}else if(!box.contains(previous))tie=true;
            }
        }
        return tie?null:best;
    }
    private AccessibilityNodeInfo zhihuMore(AccessibilityNodeInfo page){
        Map<String,AccessibilityNodeInfo> matches=new LinkedHashMap<>();
        for(AccessibilityNodeInfo n:nodes(page))if(ZhihuPageRules.more(text(n.getText()),n.getViewIdResourceName())||ZhihuPageRules.more(text(n.getContentDescription()),n.getViewIdResourceName())){
            AccessibilityNodeInfo target=control(n);android.graphics.Rect r=new android.graphics.Rect();target.getBoundsInScreen(r);
            if(target.isVisibleToUser()&&target.isEnabled())matches.put(r.toString(),target);
        }
        return zhihuToolbar(matches.values(),page);
    }
    private String sceneText(AccessibilityNodeInfo root){
        // Prefer an exposed title/heading; otherwise keep one substantial article text
        // locally in memory. It is never persisted in diagnostics or sent to a server.
        String title=sourceTitle(root);if(!title.isEmpty())return title;
        String fallback="";
        for(AccessibilityNodeInfo n:("com.tencent.mm".equals(text(root==null?null:root.getPackageName()))?wechatArticleNodes(root):nodes(root))){
            String value=text(n.getText()).trim().replaceAll("\\s+"," ");
            if(value.isEmpty()||value.length()>300||FavoriteRules.favoriteButton(value)||FavoriteRules.savedButton(value)||FavoriteRules.success(value)||FavoriteRules.share(value)||FavoriteRules.copy(value))continue;
            if("com.zhihu.android".equals(text(root.getPackageName()))&&ZhihuPageRules.volatileSceneText(value))continue;
            String id=text(n.getViewIdResourceName());
            if(value.length()>=2&&((Build.VERSION.SDK_INT>=28&&n.isHeading())||id.matches("(?i).*:id/(?:[a-z0-9]+_)*(?:title|headline)(?:_text)?")))return value;
            if(fallback.isEmpty()&&value.length()>=8&&!value.matches("[0-9\\s:：/.,%+万千次人播放观看-]+"))fallback=value;
        }
        return fallback;
    }
    private boolean sameScene(AccessibilityNodeInfo page){
        if(page==null)return false;
        List<String> values=new ArrayList<>();
        for(AccessibilityNodeInfo node:nodes(page)){
            values.add(text(node.getText()).trim().replaceAll("\\s+"," "));
            values.add(text(node.getContentDescription()).trim().replaceAll("\\s+"," "));
        }
        return scene.contains(page.getWindowId(),values,sourceTitle(page));
    }
    private void confirmSaved(){
        if(phase!=1||!enabled(this))return;
        long now=SystemClock.elapsedRealtime();
        AccessibilityNodeInfo page=root();
        if(wait.ready(now)){
            if("com.tencent.mm".equals(pendingPackage)&&wechatAllowed(page)&&button(true)!=null){
                phase=2;copyTimeout=8000;copyStarted=now;
                state("公众号收藏已确认，正在从文章菜单取得原链接");handler.post(pollCopy);return;
            }
            if(sameScene(page)){beginShare();return;}
            if(!scene.tracked()||scene.expired(now)||wait.returnExpired(now)){state("收藏已确认，但无法核对原内容窗口，请回到原内容后点分享继续");manualShare();return;}
            handler.postDelayed(confirm,250);return;
        }
        if(wait.expired(now)){
            recordPageDiagnostic(pendingPackage,"收藏确认超时",null);
            fail("已识别点击，但15秒内未确认收藏成功；未保存。"+(FavoriteRules.platform(pendingPackage)==1?"B站请先确认收藏夹。":""));return;
        }
        boolean original=sameScene(page);
        List<AccessibilityNodeInfo> currentNodes=nodes(page);
        AccessibilityNodeInfo clickedNow=original?FavoriteBindings.findCaptured(pendingFavoriteSelector,page,currentNodes):null;
        for(AccessibilityNodeInfo n:currentNodes){
            String d=text(n.getContentDescription()),t=text(n.getText());
            if(WechatConfirmation.pending(phase,pendingPackage,text(n.getPackageName()),now-lastAttempt)&&outsideWebContent(n)&&(WechatConfirmation.receipt(t)||WechatConfirmation.receipt(d))){wait.confirm(now);break;}
            if((FavoriteRules.success(t)&&!FavoriteRules.clean(t).equals("已收藏"))||(FavoriteRules.success(d)&&!FavoriteRules.clean(d).equals("已收藏"))||(original&&(nodeKey(n).equals(clickedKey)||(clickedNow!=null&&control(n).equals(clickedNow)))&&favoriteNode(n)&&selected(n))){wait.confirm(now);break;}
        }
        handler.postDelayed(confirm,250);
    }
    private boolean outsideWebContent(AccessibilityNodeInfo node){
        // Do not mistake an article mentioning “已收藏” for a native receipt.
        for(int depth=0;node!=null&&depth<64;depth++,node=node.getParent())if(text(node.getClassName()).endsWith("WebView"))return false;
        return node==null;
    }
    private AccessibilityNodeInfo button(boolean copy){
        AccessibilityNodeInfo root=root();
        if(!copy){AccessibilityNodeInfo learned=FavoriteBindings.find(this,pendingPackage,"share",root,nodes(root));if(learned!=null)return learned;}
        Map<String,AccessibilityNodeInfo> matches=new LinkedHashMap<>();
        for(AccessibilityNodeInfo n:nodes(root)){
            String t=text(n.getText()),d=text(n.getContentDescription());
            boolean zhihu="com.zhihu.android".equals(pendingPackage);
            boolean copyText=FavoriteRules.copyForPlatform(pendingPackage,t)||FavoriteRules.copyForPlatform(pendingPackage,d)||(zhihu&&(ZhihuPageRules.copyLink(t)||ZhihuPageRules.copyLink(d)))||(FavoriteRules.shareSheet(text(n.getPackageName()))&&(FavoriteRules.systemCopy(t)||FavoriteRules.systemCopy(d)));
            boolean shareText=FavoriteRules.share(t)||FavoriteRules.share(d)||(zhihu&&(ZhihuPageRules.share(t)||ZhihuPageRules.share(d)));
            if(!(FavoriteRules.roleId(pendingPackage,n.getViewIdResourceName(),copy?"copy":"share")||(!copy&&zhihu&&ZhihuPageRules.shareId(n.getViewIdResourceName()))||(copy?copyText:shareText)))continue;
            AccessibilityNodeInfo target=n;for(int i=0;!target.isClickable()&&i<2;i++){
                AccessibilityNodeInfo parent=target.getParent();if(parent==null)break;
                android.graphics.Rect bounds=new android.graphics.Rect();parent.getBoundsInScreen(bounds);android.util.DisplayMetrics metrics=getResources().getDisplayMetrics();
                if(!CaptureClickBounds.allowed(bounds.width(),bounds.height(),metrics.widthPixels,metrics.heightPixels,metrics.density,copy))break;
                target=parent;
            }
            if(target==null||!target.isClickable())target=n;
            if(target.isEnabled()&&target.isVisibleToUser()){
                android.graphics.Rect bounds=new android.graphics.Rect();target.getBoundsInScreen(bounds);matches.put(bounds.toString(),target);
            }
        }
        if(matches.size()==1)return matches.values().iterator().next();
        if("com.zhihu.android".equals(pendingPackage)&&matches.size()>1){
            List<AccessibilityNodeInfo> targets=new ArrayList<>(matches.values());List<ZhihuPageRules.Target> boxes=new ArrayList<>();
            for(AccessibilityNodeInfo target:targets){android.graphics.Rect box=new android.graphics.Rect();target.getBoundsInScreen(box);boxes.add(new ZhihuPageRules.Target(box.left,box.top,box.right,box.bottom));}
            int index=ZhihuPageRules.nestedTarget(boxes);if(index>=0)return targets.get(index);
        }
        if(!copy){
            AccessibilityNodeInfo selected=douyinSelectedShare(root);
            if(selected!=null)return selected;
            AccessibilityNodeInfo author=zhihuAuthorShare(root);
            if(author!=null)return author;
            return zhihuToolbar(matches.values(),root);
        }
        return null;
    }
    private AccessibilityNodeInfo zhihuAuthorShare(AccessibilityNodeInfo page){
        if(phase!=1||!"com.zhihu.android".equals(pendingPackage)||page==null||!sameScene(page))return null;
        android.graphics.Rect screen=new android.graphics.Rect();page.getBoundsInScreen(screen);
        List<AccessibilityNodeInfo> visible=nodes(page),candidates=new ArrayList<>();List<ZhihuPageRules.Target> targets=new ArrayList<>();
        for(AccessibilityNodeInfo node:visible){
            if(!node.isClickable()||!node.isEnabled()||node.isEditable()||node.isPassword())continue;
            String label=text(node.getText())+" "+text(node.getContentDescription());
            if(label.matches("(?is).*(?:关注|搜索|更多|收藏|评论|点赞|举报|follow|search|more|report).*"))continue;
            android.graphics.Rect r=new android.graphics.Rect();node.getBoundsInScreen(r);
            ZhihuPageRules.Target button=new ZhihuPageRules.Target(r.left-screen.left,r.top-screen.top,r.right-screen.left,r.bottom-screen.top);
            for(AccessibilityNodeInfo follow:visible)if(ZhihuPageRules.authorAction(text(follow.getText()))||ZhihuPageRules.authorAction(text(follow.getContentDescription()))){
                android.graphics.Rect f=new android.graphics.Rect();follow.getBoundsInScreen(f);
                if(ZhihuPageRules.authorShare(button,new ZhihuPageRules.Target(f.left-screen.left,f.top-screen.top,f.right-screen.left,f.bottom-screen.top),screen.width(),screen.height())){candidates.add(node);targets.add(button);break;}
            }
        }
        int index=ZhihuPageRules.nestedTarget(targets);return index<0?null:candidates.get(index);
    }
    private AccessibilityNodeInfo douyinSelectedShare(AccessibilityNodeInfo page){
        if(phase!=1||!"com.ss.android.yumme.video".equals(pendingPackage)||page==null||!sameScene(page))return null;
        android.graphics.Rect screen=new android.graphics.Rect();page.getBoundsInScreen(screen);
        List<AccessibilityNodeInfo> candidates=new ArrayList<>();List<DouyinSelectedLayout.Box> boxes=new ArrayList<>();
        boolean favoriteRail=false;
        for(AccessibilityNodeInfo node:nodes(page)){
            android.graphics.Rect r=new android.graphics.Rect();node.getBoundsInScreen(r);
            if(screen.width()<=0||screen.height()<=0)continue;
            double x=(r.exactCenterX()-screen.left)/screen.width(),y=(r.exactCenterY()-screen.top)/screen.height();
            if(favoriteNode(node)&&x>.83&&y>.58&&y<.91)favoriteRail=true;
            if(!node.isClickable()||!node.isVisibleToUser()||!node.isEnabled()||node.isEditable()||node.isPassword())continue;
            String label=text(node.getText())+" "+text(node.getContentDescription());
            boolean excluded=label.matches("(?is).*(?:全屏|稍后|推荐|搜索|点赞|收藏|评论|fullscreen|watch.?later|search|like|comment).*" );
            candidates.add(node);boxes.add(new DouyinSelectedLayout.Box(r.left-screen.left,r.top-screen.top,r.right-screen.left,r.bottom-screen.top,excluded));
        }
        if(!favoriteRail)return null;
        int index=DouyinSelectedLayout.select(boxes,screen.width(),screen.height());
        return index<0?null:candidates.get(index);
    }
    private boolean clickNode(AccessibilityNodeInfo node){
        if(node==null||!node.isVisibleToUser()||!node.isEnabled()||node.isEditable()||node.isPassword())return false;
        android.graphics.Rect r=new android.graphics.Rect();node.getBoundsInScreen(r);
        android.util.DisplayMetrics metrics=getResources().getDisplayMetrics();
        boolean copyLabel=(phase==2)&&copyLabel(node);
        if(!CaptureClickBounds.allowed(r.width(),r.height(),metrics.widthPixels,metrics.heightPixels,metrics.density,copyLabel)){if(phase==2)state("复制控件范围无法核对（"+r.width()+"×"+r.height()+"），未执行触摸");return false;}
        if(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))return true;
        android.graphics.Path path=new android.graphics.Path();path.moveTo(r.exactCenterX(),r.exactCenterY());
        return dispatchGesture(new android.accessibilityservice.GestureDescription.Builder().addStroke(new android.accessibilityservice.GestureDescription.StrokeDescription(path,0,70)).build(),null,null);
    }
    private boolean copyLabel(AccessibilityNodeInfo n){
        boolean system=FavoriteRules.shareSheet(text(n.getPackageName()));
        if(copyLabelText(n,system))return true;
        for(int i=0;i<n.getChildCount()&&i<12;i++){AccessibilityNodeInfo child=n.getChild(i);if(child==null)continue;if(copyLabelText(child,system))return true;for(int j=0;j<child.getChildCount()&&j<12;j++){AccessibilityNodeInfo grandchild=child.getChild(j);if(grandchild!=null&&copyLabelText(grandchild,system))return true;}}
        return false;
    }
    private boolean copyLabelText(AccessibilityNodeInfo n,boolean system){String t=text(n.getText()),d=text(n.getContentDescription());return system?(FavoriteRules.systemCopy(t)||FavoriteRules.systemCopy(d)):(FavoriteRules.copyForPlatform(pendingPackage,t)||FavoriteRules.copyForPlatform(pendingPackage,d)||("com.zhihu.android".equals(pendingPackage)&&(ZhihuPageRules.copyLink(t)||ZhihuPageRules.copyLink(d))));}
    private void beginShare(){
        if(phase!=1||!enabled(this))return;
        AccessibilityNodeInfo page=root();
        if(page==null){if(shareReadiness.retry(SystemClock.elapsedRealtime(),false)){handler.postDelayed(confirm,250);return;}fail("原内容窗口持续不可用，本次未保存");return;}
        String title=sourceTitle(page);if(!title.isEmpty()){pendingTitle=title;state("已读取原页面标题，将随原链接保存");}
        AccessibilityNodeInfo share=pendingPackage.equals("com.tencent.mm")?wechatMore(page):button(false);
        if(share==null&&pendingPackage.equals("com.zhihu.android")){share=zhihuMore(page);zhihuMoreOpened=share!=null;}
        if(share==null){
            if(shareReadiness.retry(SystemClock.elapsedRealtime(),false)){handler.postDelayed(confirm,250);return;}
            state("收藏已确认，但等待菜单控件2秒后仍未找到唯一取链接入口");manualShare();return;
        }
        shareReadiness.reset();lastAttempt=SystemClock.elapsedRealtime();
        // Retain a successful semantic share selector for layouts that later omit its label.
        // It is committed only once the copy-link menu has actually appeared.
        if(!zhihuMoreOpened){String rule=FavoriteBindings.capture(page,control(share));if(rule!=null)pendingShareRule=rule;}
        phase=2;copyTimeout=8000;copyStarted=SystemClock.elapsedRealtime();
        state(zhihuMoreOpened?"正在自动打开知乎更多菜单并取得原链接":"已识别收藏状态，正在从原平台取得链接");
        if("com.ss.android.yumme.video".equals(pendingPackage))state("抖音精选 · 已定位分享控件，正在打开菜单");
        if(!clickNode(share)){fail("无法打开分享菜单，本次未保存");return;}
        handler.postDelayed(pollCopy,300);
    }
    private final Runnable pollCopy=new Runnable(){public void run(){
        if(phase!=2||!enabled(FavoriteCaptureService.this)){reset();return;}
        AccessibilityNodeInfo copy=button(true);
        if(copy!=null){
            if(!pendingShareRule.isEmpty())FavoriteBindings.save(FavoriteCaptureService.this,pendingPackage,"share",pendingShareRule);
            if(prompt!=null){prompt.dismiss();prompt=null;}
            String pkg=pendingPackage;long timestamp=System.currentTimeMillis();
            state(FavoriteRules.platformName(pkg)+" · 已找到取链接入口，正在点击并校验新链接");
            boolean clicked=clickNode(copy);
            if(!clicked){fail("未能复制原内容链接，本次未保存");return;}phase=3;
            handler.postDelayed(()->{
                if(!enabled(FavoriteCaptureService.this))return;
                AccessibilityNodeInfo current=getRootInActiveWindow();if(current==null||(!pkg.equals(text(current.getPackageName()))&&!FavoriteRules.shareSheet(text(current.getPackageName())))){fail("复制后已离开原平台，本次未保存");return;}
                try{
                    launchCapture(pkg,timestamp);
                }catch(Exception e){offerCapture(pkg,timestamp);}
            },450);return;
        }
        if(zhihuMoreOpened&&!zhihuShareOpened){
            AccessibilityNodeInfo share=button(false);
            if(share!=null){zhihuShareOpened=true;state("已找到知乎菜单中的分享入口，正在自动打开");if(!clickNode(share)){fail("无法打开知乎分享菜单，本次未保存");return;}handler.postDelayed(this,350);return;}
        }
        if(SystemClock.elapsedRealtime()-copyStarted>copyTimeout){recordPageDiagnostic(pendingPackage,"取链接菜单等待超时",null);fail("未找到「复制链接 / 分享链接」，本次未保存。请在分享菜单选择拾光。");return;}
        handler.postDelayed(this,250);
    }};
    private void recordPageDiagnostic(){recordPageDiagnostic(pendingPackage,"自动取链接未匹配",null);}
    private void recordPageDiagnostic(String pkg,String reason,AccessibilityNodeInfo clicked){
        AccessibilityNodeInfo page=platformRoot(pkg);if(page==null)return;
        android.graphics.Rect screen=new android.graphics.Rect();page.getBoundsInScreen(screen);if(screen.isEmpty())return;
        StringBuilder out=new StringBuilder("最近一次取链接诊断 · ").append(BuildConfig.VERSION_NAME).append(" · ").append(pkg).append(" · ").append(reason)
            .append("\n标题状态：").append(pendingTitle.isEmpty()?"未识别":"已识别").append("；仅记录控件结构，不含正文/标题文字\n");
        out.append("适配记录：收藏=").append(FavoriteBindings.configured(this,pkg,"favorite")).append("，分享=").append(FavoriteBindings.configured(this,pkg,"share"))
            .append("；本页分享匹配=").append(FavoriteBindings.find(this,pkg,"share",page,nodes(page))!=null).append('\n');
        if(clicked!=null)out.append("点击控件 id=").append(text(clicked.getViewIdResourceName()).replaceAll("[^a-zA-Z0-9_:./]", "")).append(" class=").append(text(clicked.getClassName())).append('\n');
        int count=0;
        for(AccessibilityNodeInfo n:nodes(page)){
            android.graphics.Rect r=new android.graphics.Rect();n.getBoundsInScreen(r);
            double x=(r.exactCenterX()-screen.left)/screen.width(),y=(r.exactCenterY()-screen.top)/screen.height();
            if(r.isEmpty()||r.width()>screen.width()*0.35||r.height()>screen.height()*0.16||y<0.03||y>0.99)continue;
            String id=text(n.getViewIdResourceName()).replaceAll("[^a-zA-Z0-9_:./]", "");
            String label=FavoriteRules.share(text(n.getText()))||FavoriteRules.share(text(n.getContentDescription()))?"分享":ZhihuPageRules.more(text(n.getText()),id)||ZhihuPageRules.more(text(n.getContentDescription()),id)?"更多":favoriteNode(n)?"收藏/保存":copyLabelText(n,false)?"取链接":"无已知动作标签";
            out.append(++count).append(" id=").append(id).append(" class=").append(text(n.getClassName()))
                .append(" click=").append(n.isClickable()).append(" label=").append(label)
                .append(" xy=").append(Math.round(x*100)).append(',').append(Math.round(y*100))
                .append(" size=").append(r.width()).append('x').append(r.height()).append('\n');
            if(count>=70||out.length()>10000)break;
        }
        out.append("匹配区域控件数：").append(count);
        getPrefs(this).edit().putString("pageDiagnostic:"+FavoriteRules.bindingProfile(pkg),out.toString()).apply();
    }
    private void manualShare(){
        recordPageDiagnostic();
        if(captureOverlay!=null){fail("未找到这篇内容的取链接入口，未保存。分类已保留，可重试。");return;}
        phase=2;copyStarted=SystemClock.elapsedRealtime();copyTimeout=30000;
        state("收藏/保存已识别，正在等待原App分享菜单；无需重新适配收藏按钮");
        String pkg=pendingPackage;prompt=new FavoritePrompt(this);
        prompt.show("拾光 · 已识别收藏","请点原 App 的分享或右上角 ⋯。出现复制链接后会继续打开分类窗口，并自动记住这次分享入口。你之前的收藏/保存适配已保留，无需重复设置。","知道了",()->{if(prompt!=null){prompt.dismiss();prompt=null;}},()->{reset();state("已取消，未保存");});
        handler.postDelayed(pollCopy,300);
    }
    static boolean calibrate(String pkg){if(instance==null||!enabled(instance))return false;instance.startLearning(pkg);return true;}
    private void startLearning(String pkg){
        reset();if(!enabled(this))return;phase=4;pendingPackage=pkg;
        String label="收藏 / 保存（YouTube：保存到播放列表）";
        state("准备记录"+label+"按钮，请先进入原App的内容详情页");
        prompt=new FavoritePrompt(this);
        if(!prompt.show("拾光 · 适配本机按钮","先打开"+FavoriteRules.platformName(pkg)+"内容"+"详情页"+"（请选择尚未收藏的内容）"+"，再点下方“记录下一次点击”，然后只点一次原App的"+label+"按钮。不会记录正文或账号。","记录下一次点击",()->{
            prompt.dismiss();prompt=null;learningArmed=true;state("请点一次原App的"+label+"按钮");Toast.makeText(this,"请点原App的"+label+"按钮",Toast.LENGTH_LONG).show();
        },()->{reset();state("按钮适配已取消");})){fail("无法显示按钮适配提示，请检查无障碍服务连接");return;}
        handler.postDelayed(()->fail("按钮适配已超时，请重新开始"),60000);
    }
    private void learnClick(AccessibilityEvent event,String pkg){
        if(!learningArmed||!FavoriteRules.bindingProfile(pendingPackage).equals(FavoriteRules.bindingProfile(pkg)))return;
        if(event.getEventType()!=AccessibilityEvent.TYPE_VIEW_CLICKED&&event.getEventType()!=AccessibilityEvent.TYPE_VIEW_LONG_CLICKED)return;
        AccessibilityNodeInfo target=event.getSource();if(target==null){state("系统未提供点击控件，请再点一次按钮");return;}
        target=control(target);AccessibilityNodeInfo page=target;
        for(int i=0;i<30;i++){AccessibilityNodeInfo parent=page.getParent();if(parent==null)break;page=parent;}
        String rule=FavoriteBindings.capture(page,target);if(rule==null){state("未取得独立按钮，请只点图标，不要点页面空白处");return;}
        pendingPackage=pkg;learningArmed=false;
        FavoriteBindings.save(this,pkg,"favorite",rule);String key=nodeKey(target);phase=0;lastAttempt=0;
        state("已永久记录本机收藏/保存按钮，后续无需重复适配");startFavorite(pkg,key,false);if(selected(target))wait.confirm(SystemClock.elapsedRealtime());
    }

    private void launchCapture(String pkg,long timestamp){
        if(captureOverlay!=null){captureOverlay.acquire(pkg,timestamp,pendingTitle);return;}
        state("已执行取链接动作，正在当前页面打开分类表单");
        captureOverlay=new FavoriteCaptureOverlay(this,message->{reset();state(message);Toast.makeText(this,message,Toast.LENGTH_SHORT).show();},()->{
            captureOverlay=null;
            try{launchCaptureActivity(pkg,timestamp);}catch(Exception e){offerCapture(pkg,timestamp);}
        });
        if(captureOverlay.show(pkg,timestamp,pendingTitle))return;
        captureOverlay=null;launchCaptureActivity(pkg,timestamp);
    }
    private void launchCaptureActivity(String pkg,long timestamp){
        state("正在打开拾光链接校验窗口");
        startActivity(new Intent(this,ClipboardCaptureActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("expectedPackage",pkg).putExtra("notBefore",timestamp).putExtra("capturedTitle",pendingTitle));
        handler.postDelayed(()->{if(getPrefs(this).getLong("launchAck",0)!=timestamp)offerCapture(pkg,timestamp);else reset();},4000);
    }
    private void offerCapture(String pkg,long timestamp){
        if(captureOverlay!=null){fail("未能读取本次原链接，未保存。分类已保留，可重试。");return;}
        if(!enabled(this)){reset();return;}
        state("已取得复制动作；系统未弹出界面，请点屏幕下方「选择分类」继续");
        if(prompt!=null)prompt.dismiss();
        prompt=new FavoritePrompt(this);
        boolean shown=prompt.show(()->{
            handler.removeCallbacksAndMessages(null);
            if(prompt!=null){prompt.dismiss();prompt=null;}
            try{launchCaptureActivity(pkg,timestamp);}catch(Exception e){fail("系统仍未允许打开拾光，请在原内容使用分享→拾光；未保存");}
        },()->{reset();state("已取消本次收藏跟随，未保存");});
        if(!shown){fail("无法显示分类提示，请使用原内容的分享→拾光；未保存");return;}
        handler.postDelayed(()->{reset();state("分类提示已超时，本次未保存");},45000);
    }
    private void fail(String message){
        if(captureOverlay!=null){handler.removeCallbacksAndMessages(null);phase=5;state(FavoriteRules.platformName(pendingPackage)+" · "+message);captureOverlay.linkFailed(message);return;}
        String platform=pendingPackage;reset();state(FavoriteRules.platformName(platform)+" · "+message);
    }
    @Override public void onInterrupt(){reset();handler.removeCallbacks(sample);samplingInterrupted=true;state("服务被系统中断，收到后续事件将恢复检测；若持续无反应请检查无障碍设置");}
    @Override public boolean onUnbind(Intent intent){
        if(instance==this)instance=null;reset();state("系统已断开收藏检测服务，请检查无障碍连接");return super.onUnbind(intent);
    }
    @Override public void onDestroy(){if(instance==this)instance=null;reset();super.onDestroy();}
}
