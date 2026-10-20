package io.github.jiangxixu.shiguang;

import java.util.*;

/** Local, short-lived control states. An already-saved page is never a new favorite. */
final class FavoriteTransitions {
    private String context="";
    private long observedAt;
    private long interactionAt=Long.MIN_VALUE;
    private String interactionScope="";
    private String interactionKey="",pendingKey="",pendingScope="";
    private long pendingAt;
    private Map<String,Boolean> previous=new HashMap<>();
    void clear(){context="";previous.clear();observedAt=0;interactionAt=Long.MIN_VALUE;interactionScope="";pendingKey="";pendingScope="";}
    void interact(String scope,String key,long now){interactionScope=scope;interactionKey=key;interactionAt=now;}
    Boolean previous(String scope,String key,long now){
        if(scope.equals(pendingScope)&&key.equals(pendingKey)&&now-pendingAt<=700)return false;
        return scope.equals(context)&&now-observedAt<=2500?previous.get(key):null;
    }
    String update(String scope,Map<String,Boolean> current,long now){
        String changed=null;
        // Multiple independent favorite controls usually indicate a feed, not the current article.
        if(scope.equals(context)&&now-observedAt<=2500&&current.size()==1&&previous.size()==1){
            for(Map.Entry<String,Boolean> entry:current.entrySet())
                if(Boolean.TRUE.equals(entry.getValue())&&Boolean.FALSE.equals(previous.get(entry.getKey()))){pendingKey=entry.getKey();pendingAt=now;pendingScope=scope;}
        }
        // Some apps send the content update before the click callback. Retain only
        // this transition briefly, so the later callback can confirm it.
        if(!scope.equals(pendingScope)||now-pendingAt>700||current.size()!=1||!Boolean.TRUE.equals(current.get(pendingKey))){pendingKey="";pendingScope="";}
        if(!pendingKey.isEmpty()&&scope.equals(interactionScope)&&(interactionKey.isEmpty()||interactionKey.equals(pendingKey))&&now>=interactionAt&&now-interactionAt<=2000){changed=pendingKey;pendingKey="";pendingScope="";}
        context=scope;previous=new HashMap<>(current);observedAt=now;
        return changed;
    }
}
