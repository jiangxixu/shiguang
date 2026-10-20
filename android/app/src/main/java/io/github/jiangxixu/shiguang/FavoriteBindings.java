package io.github.jiangxixu.shiguang;

import android.content.Context;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;
import org.json.*;
import java.util.*;

/** User-taught selectors only: no text, screenshots, URLs or account data are stored. */
final class FavoriteBindings {
    static String capture(AccessibilityNodeInfo root,AccessibilityNodeInfo target){
        try{
            Rect screen=new Rect(),box=new Rect();root.getBoundsInScreen(screen);target.getBoundsInScreen(box);
            if(screen.width()<=0||screen.height()<=0||box.isEmpty()||box.width()>screen.width()*0.6||box.height()>screen.height()*0.25||target.isEditable()||target.isPassword())return null;
            ArrayList<Integer> path=new ArrayList<>();AccessibilityNodeInfo child=target;
            while(!child.equals(root)&&path.size()<30){
                AccessibilityNodeInfo parent=child.getParent();if(parent==null)return null;
                int index=-1;for(int i=0;i<parent.getChildCount();i++)if(child.equals(parent.getChild(i))){index=i;break;}
                if(index<0)return null;path.add(0,index);child=parent;
            }
            if(!child.equals(root)||path.isEmpty())return null;
            JSONObject value=new JSONObject().put("id",target.getViewIdResourceName()==null?"":target.getViewIdResourceName())
                .put("class",String.valueOf(target.getClassName())).put("root",String.valueOf(root.getClassName()))
                .put("path",new JSONArray(path)).put("landscape",screen.width()>screen.height())
                .put("x",(box.exactCenterX()-screen.left)/screen.width()).put("y",(box.exactCenterY()-screen.top)/screen.height());
            return value.toString();
        }catch(Exception e){return null;}
    }
    static void save(Context c,String pkg,String role,String rule){
        c.getSharedPreferences("favorite-bindings",Context.MODE_PRIVATE).edit().putString(pkg+":"+role,rule).apply();
    }
    static AccessibilityNodeInfo find(Context c,String pkg,String role,AccessibilityNodeInfo root,List<AccessibilityNodeInfo> nodes){
        if(root==null)return null;
        try{
            String raw=c.getSharedPreferences("favorite-bindings",Context.MODE_PRIVATE).getString(pkg+":"+role,null);if(raw==null)return null;
            JSONObject rule=new JSONObject(raw);Rect screen=new Rect();root.getBoundsInScreen(screen);
            if(screen.isEmpty()||rule.getBoolean("landscape")!=(screen.width()>screen.height())||!rule.getString("root").equals(String.valueOf(root.getClassName())))return null;
            String id=rule.getString("id");AccessibilityNodeInfo target=null;
            if(!id.isEmpty()){
                for(AccessibilityNodeInfo n:nodes)if(id.equals(n.getViewIdResourceName())){if(target!=null)return null;target=n;}
            }else{
                target=root;JSONArray path=rule.getJSONArray("path");
                for(int i=0;i<path.length()&&target!=null;i++){int index=path.getInt(i);target=index>=0&&index<target.getChildCount()?target.getChild(index):null;}
            }
            if(target==null||!target.isVisibleToUser()||!target.isEnabled()||target.isEditable()||target.isPassword()||!rule.getString("class").equals(String.valueOf(target.getClassName())))return null;
            Rect box=new Rect();target.getBoundsInScreen(box);
            if(box.isEmpty()||box.width()>screen.width()*0.6||box.height()>screen.height()*0.25)return null;
            float x=(box.exactCenterX()-screen.left)/screen.width(),y=(box.exactCenterY()-screen.top)/screen.height();
            return Math.abs(x-rule.getDouble("x"))<=0.06&&Math.abs(y-rule.getDouble("y"))<=0.06?target:null;
        }catch(Exception e){return null;}
    }
}
