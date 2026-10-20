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
    private int phase; // 0 idle, 1 waiting for favorite success, 2 locating copy-link action
    private final FavoriteTransitions transitions=new FavoriteTransitions();
    private FavoritePrompt prompt;
    private FavoriteCaptureOverlay captureOverlay;
    private String observedPackage="";
    private long suppressUntil;
    private long lastObserved,lastDiagnostic,lastClick;
    private String lastClickPackage="",lastDiagnosticText="",learningRole="",learningRule="";
    private boolean learningArmed,scrollMessageSent,zhihuMoreOpened,zhihuShareOpened;
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
    static String history(Context c){return getPrefs(c).getString("history","暂无检测记录")+"\n\n"+getPrefs(c).getString("pageDiagnostic","");}
    private static android.content.SharedPreferences getPrefs(Context c){return c.getSharedPreferences("automatic",MODE_PRIVATE);}
    static void acknowledge(Context c,long id){getPrefs(c).edit().putLong("launchAck",id).apply();report(c,"已打开链接校验窗口");}
    private void state(String text){report(this,text);}
    static boolean enabled(Context c){return c.getSharedPreferences("automatic",MODE_PRIVATE).getBoolean("enabled",false);}
    static void pause(Context c){c.getSharedPreferences("automatic",MODE_PRIVATE).edit().putBoolean("enabled",false).apply();if(instance!=null){instance.reset();instance.disableSelf();}}
    @Override protected void onServiceConnected(){instance=this;state("服务已连接，等待已支持平台的收藏/保存操作");handler.post(sample);}
    private void reset(){handler.removeCallbacksAndMessages(null);if(captureOverlay!=null){captureOverlay.dismiss();captureOverlay=null;}if(prompt!=null){prompt.dismiss();prompt=null;}phase=0;pendingPackage="";pendingShareRule="";pendingTitle="";zhihuMoreOpened=false;zhihuShareOpened=false;clickedKey="";transitions.clear();scene.clear();observedPackage="";learningArmed=false;scrollMessageSent=false;learningRole="";learningRule="";boundFavorite=null;}
    private String text(CharSequence value){return value==null?"":value.toString();}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(!enabled(this)){reset();return;}
        String pkg=text(event.getPackageName());if(!FavoriteRules.supported(pkg))return;
        if(phase==4){learnClick(event,pkg);return;}
        if(phase!=0&&!pkg.equals(pendingPackage)){fail("已切换平台，本次收藏跟随已取消，未保存");return;}
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
        if(click){lastClick=now;lastClickPackage=pkg;}
        boolean success=FavoriteRules.confirmedSuccess(text(event.getContentDescription()));
        for(CharSequence value:event.getText())success|=FavoriteRules.confirmedSuccess(text(value));
        if(phase==0&&success&&pkg.equals(lastClickPackage)&&now-lastClick<6000){startFavorite(pkg,"",true);return;}
        if(event.getEventType()==AccessibilityEvent.TYPE_VIEW_SCROLLED){
            if(phase==1){if(!scrollMessageSent){scrollMessageSent=true;state("检测到滚动或收藏夹布局变化，继续等待收藏确认及原内容返回");}return;}
            transitions.clear();suppressUntil=now+700;return;
        }
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
            // A user may use either entry; favorite/save always takes priority.
            AccessibilityNodeInfo original=source;
            source=favoriteSource(source);
            if(!favoriteNode(source)&&shareSource(original,pkg)&&now-lastAttempt>4000){
                reset();lastAttempt=now;pendingPackage=pkg;phase=2;copyTimeout=8000;copyStarted=now;
                state("已识别分享入口，等待原平台链接后打开分类窗口");handler.postDelayed(pollCopy,300);return;
            }
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
            boolean saved=FavoriteRules.pendingSuccess(pkg,text(event.getContentDescription()));
            for(CharSequence t:event.getText())saved|=FavoriteRules.pendingSuccess(pkg,text(t));
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
    private boolean shareSource(AccessibilityNodeInfo source,String pkg){
        AccessibilityNodeInfo page=platformRoot(pkg),learned=FavoriteBindings.find(this,pkg,"share",page,nodes(page));
        for(int depth=0;source!=null&&depth<3;depth++,source=source.getParent()){
            if((learned!=null&&control(source).equals(learned))||FavoriteRules.share(text(source.getText()))||FavoriteRules.share(text(source.getContentDescription()))||FavoriteRules.roleId(pkg,source.getViewIdResourceName(),"share"))return true;
            if(depth==0)for(int i=0;i<source.getChildCount()&&i<12;i++){AccessibilityNodeInfo child=source.getChild(i);if(child!=null&&(FavoriteRules.share(text(child.getText()))||FavoriteRules.share(text(child.getContentDescription()))))return true;}
        }
        return false;
    }
    private String scope(String pkg,int window){return pkg+":"+window;}
    private void startFavorite(String pkg,String key,boolean confirmed){
        long now=SystemClock.elapsedRealtime();if(phase!=0||now-lastAttempt<4000)return;
        reset();lastAttempt=now;pendingPackage=pkg;clickedKey=key;phase=1;wait.start(now);
        AccessibilityNodeInfo page=platformRoot(pkg);pendingTitle=sourceTitle(page);scene.start(page==null?-1:page.getWindowId(),sceneText(page),now);
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
            String diagnostic=pkg+" · 可识别收藏控件 "+states.size()+" 个"+(states.isEmpty()?(FavoriteBindings.configured(this,pkg,"favorite")?"；本机适配已保留，当前页面未匹配，可使用分享入口":"；当前页面未提供收藏/保存控件，可使用分享入口或首次适配"):"；等待新增收藏");
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
        if(phase==2||(phase==4&&learningRole.equals("share"))){AccessibilityNodeInfo active=getRootInActiveWindow();if(active!=null&&FavoriteRules.shareSheet(text(active.getPackageName())))node=active;}
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
        String unique="";
        for(AccessibilityNodeInfo n:nodes(page))if(PageTitles.titleId(pkg,n.getViewIdResourceName())){
            String value=PageTitles.clean(text(n.getText()));if(value.isEmpty())value=PageTitles.clean(text(n.getContentDescription()));
            if(value.isEmpty())continue;if(!unique.isEmpty()&&!unique.equals(value))return "";unique=value;
        }
        return unique;
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
        if(!"com.zhihu.android".equals(pendingPackage)||page==null||pendingTitle.isEmpty())return null;
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
        String fallback="";
        for(AccessibilityNodeInfo n:nodes(root)){
            String value=text(n.getText()).trim().replaceAll("\\s+"," ");
            if(value.isEmpty()||value.length()>300||FavoriteRules.favoriteButton(value)||FavoriteRules.savedButton(value)||FavoriteRules.success(value)||FavoriteRules.share(value)||FavoriteRules.copy(value))continue;
            String id=text(n.getViewIdResourceName());
            if(value.length()>=2&&((Build.VERSION.SDK_INT>=28&&n.isHeading())||id.matches("(?i).*:id/(?:[a-z0-9]+_)*(?:title|headline)(?:_text)?")))return value;
            if(fallback.isEmpty()&&value.length()>=8&&!value.matches("[0-9\\s:：/.,%+万千次人播放观看-]+"))fallback=value;
        }
        return fallback;
    }
    private void confirmSaved(){
        if(phase!=1||!enabled(this))return;
        long now=SystemClock.elapsedRealtime();
        AccessibilityNodeInfo page=root();
        if(wait.ready(now)){
            if(page!=null&&scene.matches(page.getWindowId(),sceneText(page))){beginShare();return;}
            if(!scene.tracked()||scene.expired(now)){state("收藏已确认，但无法核对原内容窗口，请回到原内容后点分享继续");manualShare();return;}
            handler.postDelayed(confirm,250);return;
        }
        if(wait.expired(now)){fail("已识别点击，但15秒内未确认收藏成功；未保存。B站请先确认收藏夹。");return;}
        boolean original=page!=null&&scene.matches(page.getWindowId(),sceneText(page));
        for(AccessibilityNodeInfo n:nodes(page)){
            String d=text(n.getContentDescription()),t=text(n.getText());
            if((FavoriteRules.success(t)&&!FavoriteRules.clean(t).equals("已收藏"))||(FavoriteRules.success(d)&&!FavoriteRules.clean(d).equals("已收藏"))||(original&&nodeKey(n).equals(clickedKey)&&favoriteNode(n)&&selected(n))){wait.confirm(now);break;}
        }
        handler.postDelayed(confirm,250);
    }
    private AccessibilityNodeInfo button(boolean copy){
        AccessibilityNodeInfo root=root();
        if(!copy){AccessibilityNodeInfo learned=FavoriteBindings.find(this,pendingPackage,"share",root,nodes(root));if(learned!=null)return learned;}
        Map<String,AccessibilityNodeInfo> matches=new LinkedHashMap<>();
        for(AccessibilityNodeInfo n:nodes(root)){
            String t=text(n.getText()),d=text(n.getContentDescription());
            boolean copyText=FavoriteRules.copyForPlatform(pendingPackage,t)||FavoriteRules.copyForPlatform(pendingPackage,d)||(FavoriteRules.shareSheet(text(n.getPackageName()))&&(FavoriteRules.systemCopy(t)||FavoriteRules.systemCopy(d)));
            if(!(FavoriteRules.roleId(pendingPackage,n.getViewIdResourceName(),copy?"copy":"share")||(!copy&&pendingPackage.equals("com.zhihu.android")&&ZhihuPageRules.shareId(n.getViewIdResourceName()))||(copy?copyText:(FavoriteRules.share(t)||FavoriteRules.share(d)))))continue;
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
        // Ambiguous controls are deliberately not clicked.
        return matches.size()==1?matches.values().iterator().next():(!copy?zhihuToolbar(matches.values(),root):null);
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
    private boolean copyLabelText(AccessibilityNodeInfo n,boolean system){String t=text(n.getText()),d=text(n.getContentDescription());return system?(FavoriteRules.systemCopy(t)||FavoriteRules.systemCopy(d)):(FavoriteRules.copyForPlatform(pendingPackage,t)||FavoriteRules.copyForPlatform(pendingPackage,d));}
    private void beginShare(){
        if(phase!=1||!enabled(this))return;
        AccessibilityNodeInfo page=root();if(page==null){fail("无法读取原平台当前窗口，本次未保存");return;}
        String title=sourceTitle(page);if(!title.isEmpty()){pendingTitle=title;state("已读取原页面标题，将随原链接保存");}
        AccessibilityNodeInfo share=button(false);
        if(share==null&&pendingPackage.equals("com.zhihu.android")){share=zhihuMore(page);zhihuMoreOpened=share!=null;}
        lastAttempt=SystemClock.elapsedRealtime();
        if(share==null){if(pendingPackage.equals("com.zhihu.android"))state("知乎自动取链接："+(pendingTitle.isEmpty()?"未读到明确标题":"已读到原页面标题")+"；未找到可唯一定位的分享/更多控件");manualShare();return;}
        phase=2;copyTimeout=8000;copyStarted=SystemClock.elapsedRealtime();
        state(zhihuMoreOpened?"正在自动打开知乎更多菜单并取得原链接":"已识别收藏状态，正在从原平台取得链接");
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
        if(SystemClock.elapsedRealtime()-copyStarted>copyTimeout){fail("未找到「复制链接 / 分享链接」，本次未保存。请在分享菜单选择拾光。");return;}
        handler.postDelayed(this,250);
    }};
    private void recordPageDiagnostic(){
        AccessibilityNodeInfo page=platformRoot(pendingPackage);if(page==null)return;
        android.graphics.Rect screen=new android.graphics.Rect();page.getBoundsInScreen(screen);if(screen.isEmpty())return;
        StringBuilder out=new StringBuilder("最近一次取链接诊断 · ").append(BuildConfig.VERSION_NAME).append(" · ").append(pendingPackage)
            .append("\n标题状态：").append(pendingTitle.isEmpty()?"未识别":"已识别").append("；仅记录控件结构，不含正文/标题文字\n");
        int count=0;
        for(AccessibilityNodeInfo n:nodes(page)){
            android.graphics.Rect r=new android.graphics.Rect();n.getBoundsInScreen(r);
            double x=(r.exactCenterX()-screen.left)/screen.width(),y=(r.exactCenterY()-screen.top)/screen.height();
            if(r.isEmpty()||r.width()>screen.width()*0.35||r.height()>screen.height()*0.16||x<0.60||!(y<0.5||y>0.80))continue;
            String id=text(n.getViewIdResourceName()).replaceAll("[^a-zA-Z0-9_:./]", "");
            String label=FavoriteRules.share(text(n.getText()))||FavoriteRules.share(text(n.getContentDescription()))?"分享":ZhihuPageRules.more(text(n.getText()),id)||ZhihuPageRules.more(text(n.getContentDescription()),id)?"更多":"无已知动作标签";
            out.append(++count).append(" id=").append(id).append(" class=").append(text(n.getClassName()))
                .append(" click=").append(n.isClickable()).append(" label=").append(label)
                .append(" xy=").append(Math.round(x*100)).append(',').append(Math.round(y*100))
                .append(" size=").append(r.width()).append('x').append(r.height()).append('\n');
            if(count>=35||out.length()>6000)break;
        }
        out.append("匹配区域控件数：").append(count);
        getPrefs(this).edit().putString("pageDiagnostic",out.toString()).apply();
    }
    private void manualShare(){
        recordPageDiagnostic();
        phase=2;copyStarted=SystemClock.elapsedRealtime();copyTimeout=30000;
        state("收藏/保存已识别，正在等待原App分享菜单；无需重新适配收藏按钮");
        String pkg=pendingPackage;prompt=new FavoritePrompt(this);
        prompt.show("拾光 · 已识别收藏","请点原 App 的分享或右上角 ⋯。出现复制链接后会继续打开分类窗口，并自动记住这次分享入口。你之前的收藏/保存适配已保留，无需重复设置。","知道了",()->{if(prompt!=null){prompt.dismiss();prompt=null;}},()->{reset();state("已取消，未保存");});
        handler.postDelayed(pollCopy,300);
    }
    static boolean calibrate(String pkg,String role){if(instance==null||!enabled(instance))return false;instance.startLearning(pkg,role);return true;}
    private void startLearning(String pkg,String role){
        reset();if(!enabled(this))return;phase=4;pendingPackage=pkg;learningRole=role;
        String label=role.equals("share")?"分享（或 ⋯）":"收藏 / 保存（YouTube：保存到播放列表）";
        state("准备记录"+label+"按钮，请先进入原App的内容详情页");
        prompt=new FavoritePrompt(this);
        if(!prompt.show("拾光 · 适配本机按钮","先打开"+FavoriteRules.platformName(pkg)+"内容"+"详情页"+(role.equals("favorite")?"（请选择尚未收藏的内容）":"")+"，再点下方“记录下一次点击”，然后只点一次原App的"+label+"按钮。不会记录正文或账号。","记录下一次点击",()->{
            prompt.dismiss();prompt=null;learningArmed=true;state("请点一次原App的"+label+"按钮");Toast.makeText(this,"请点原App的"+label+"按钮",Toast.LENGTH_LONG).show();
        },()->{reset();state("按钮适配已取消");})){fail("无法显示按钮适配提示，请检查无障碍服务连接");return;}
        handler.postDelayed(()->fail("按钮适配已超时，请重新开始"),60000);
    }
    private void learnClick(AccessibilityEvent event,String pkg){
        if(!learningArmed||!FavoriteRules.samePlatform(pendingPackage,pkg))return;
        if(event.getEventType()!=AccessibilityEvent.TYPE_VIEW_CLICKED&&event.getEventType()!=AccessibilityEvent.TYPE_VIEW_LONG_CLICKED)return;
        AccessibilityNodeInfo target=event.getSource();if(target==null){state("系统未提供点击控件，请再点一次按钮");return;}
        target=control(target);AccessibilityNodeInfo page=target;
        for(int i=0;i<30;i++){AccessibilityNodeInfo parent=page.getParent();if(parent==null)break;page=parent;}
        String rule=FavoriteBindings.capture(page,target);if(rule==null){state("未取得独立按钮，请只点图标，不要点页面空白处");return;}
        pendingPackage=pkg;learningArmed=false;learningRule=rule;
        if(learningRole.equals("favorite")){
            FavoriteBindings.save(this,pkg,"favorite",rule);String key=nodeKey(target);reset();
            state("已永久记录本机收藏/保存按钮，后续无需重复适配");startFavorite(pkg,key,false);if(selected(target))wait.confirm(SystemClock.elapsedRealtime());return;
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
        if(captureOverlay!=null)return;
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
    private void fail(String message){String platform=pendingPackage;reset();state(FavoriteRules.platformName(platform)+" · "+message);Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    @Override public void onInterrupt(){reset();state("服务被系统中断，请返回无障碍设置检查");}
    @Override public void onDestroy(){reset();if(instance==this)instance=null;super.onDestroy();}
}
