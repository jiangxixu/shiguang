package io.github.jiangxixu.shiguang;

import java.util.*;

/** Local, short-lived control states. An already-saved page is never a new favorite. */
final class FavoriteTransitions {
    private String context="";
    private long observedAt;
    private Map<String,Boolean> previous=new HashMap<>();
    void clear(){context="";previous.clear();observedAt=0;}
    Boolean previous(String scope,String key,long now){return scope.equals(context)&&now-observedAt<=2500?previous.get(key):null;}
    String update(String scope,Map<String,Boolean> current,long now){
        String changed=null;
        // Multiple independent favorite controls usually indicate a feed, not the current article.
        if(scope.equals(context)&&now-observedAt<=2500&&current.size()==1&&previous.size()==1){
            for(Map.Entry<String,Boolean> entry:current.entrySet())
                if(Boolean.TRUE.equals(entry.getValue())&&Boolean.FALSE.equals(previous.get(entry.getKey())))changed=entry.getKey();
        }
        context=scope;previous=new HashMap<>(current);observedAt=now;
        return changed;
    }
}
