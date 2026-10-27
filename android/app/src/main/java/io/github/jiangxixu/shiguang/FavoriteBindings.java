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
            if(!MenuActions.bindingShape(box.width(),box.height(),screen.width(),screen.height())||target.isEditable()||target.isPassword())return null;
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
    private static String key(String pkg,String role){int index=FavoriteRules.platform(pkg);return (index>=0?FavoriteRules.PACKAGES[index]:pkg)+":"+role;}
    private static JSONArray rules(Context c,String pkg,String role){
        String raw=c.getSharedPreferences("favorite-bindings",Context.MODE_PRIVATE).getString(key(pkg,role),null);
        if(raw==null)raw=c.getSharedPreferences("favorite-bindings",Context.MODE_PRIVATE).getString(pkg+":"+role,null);
        try{if(raw!=null)return raw.trim().startsWith("[")?new JSONArray(raw):new JSONArray().put(new JSONObject(raw));}catch(Exception ignored){}
        return new JSONArray();
    }
    static boolean configured(Context c,String pkg,String role){return rules(c,pkg,role).length()>0;}
    static void save(Context c,String pkg,String role,String rule){
        try{
            JSONArray previous=rules(c,pkg,role),all=new JSONArray().put(new JSONObject(rule));
            for(int i=0;i<previous.length()&&all.length()<8;i++)if(!previous.getJSONObject(i).toString().equals(rule))all.put(previous.getJSONObject(i));
            c.getSharedPreferences("favorite-bindings",Context.MODE_PRIVATE).edit().putString(key(pkg,role),all.toString()).commit();
        }catch(Exception ignored){}
    }
    static AccessibilityNodeInfo find(Context c,String pkg,String role,AccessibilityNodeInfo root,List<AccessibilityNodeInfo> nodes){
        return find(rules(c,pkg,role),root,nodes);
    }
    static AccessibilityNodeInfo findCaptured(String rule,AccessibilityNodeInfo root,List<AccessibilityNodeInfo> nodes){
        try{return find(new JSONArray().put(new JSONObject(rule)),root,nodes);}catch(Exception ignored){return null;}
    }
    private static AccessibilityNodeInfo find(JSONArray profiles,AccessibilityNodeInfo root,List<AccessibilityNodeInfo> nodes){
        if(root==null)return null;
        Rect screen=new Rect();root.getBoundsInScreen(screen);if(screen.isEmpty())return null;
        AccessibilityNodeInfo best=null;int bestScore=-1,second=-1;
        Map<AccessibilityNodeInfo,Integer> scores=new HashMap<>();
        for(int i=0;i<profiles.length();i++)try{
            JSONObject rule=profiles.getJSONObject(i);
            if(rule.getBoolean("landscape")!=(screen.width()>screen.height()))continue;
            AccessibilityNodeInfo pathTarget=root;JSONArray path=rule.getJSONArray("path");
            for(int j=0;j<path.length()&&pathTarget!=null;j++){int index=path.getInt(j);pathTarget=index>=0&&index<pathTarget.getChildCount()?pathTarget.getChild(index):null;}
            for(AccessibilityNodeInfo n:nodes){
                if(!n.isVisibleToUser()||!n.isEnabled()||n.isEditable()||n.isPassword())continue;
                Rect box=new Rect();n.getBoundsInScreen(box);
                if(!MenuActions.bindingShape(box.width(),box.height(),screen.width(),screen.height()))continue;
                double x=(box.exactCenterX()-screen.left)/screen.width(),y=(box.exactCenterY()-screen.top)/screen.height();
                int score=FavoriteBindingMatch.score(rule.optString("id"),n.getViewIdResourceName(),rule.optString("class").equals(String.valueOf(n.getClassName())),n.equals(pathTarget),Math.abs(x-rule.getDouble("x")),Math.abs(y-rule.getDouble("y")));
                if(score>=0)scores.put(n,Math.max(scores.getOrDefault(n,-1),score));
            }
        }catch(Exception ignored){}
        for(Map.Entry<AccessibilityNodeInfo,Integer> entry:scores.entrySet()){
            int score=entry.getValue();if(score>bestScore){second=bestScore;bestScore=score;best=entry.getKey();}else second=Math.max(second,score);
        }
        return FavoriteBindingMatch.unique(bestScore,second)?best:null;
    }
}
