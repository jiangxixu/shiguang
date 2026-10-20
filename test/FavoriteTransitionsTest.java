package io.github.jiangxixu.shiguang;

import java.util.*;

public class FavoriteTransitionsTest {
    static Map<String,Boolean> state(boolean saved){return Collections.singletonMap("footer:favorite",saved);}
    static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        FavoriteTransitions t=new FavoriteTransitions();
        // Content changes can arrive before a click, or the click source may have no label.
        check(t.update("xhs:window1",state(false),1000)==null);
        t.interact("xhs:window1","footer:favorite",1200); // unlabeled click still provides user interaction
        check(Boolean.FALSE.equals(t.previous("xhs:window1","footer:favorite",1200)));
        check("footer:favorite".equals(t.update("xhs:window1",state(true),1250)));
        check(t.update("xhs:window1",state(true),1300)==null);
        check(t.update("xhs:window1",state(false),1400)==null); // cancellation
        t.clear();check(t.update("xhs:window1",state(true),1500)==null); // existing favorite
        t.clear();t.update("xhs:window1",state(false),1600);
        check(t.update("xhs:window1",state(true),1700)==null); // async initial loading without interaction
        t.clear();t.update("xhs:window1",state(false),1800);t.interact("xhs:window1","footer:favorite",1801);
        check(t.update("xhs:window1",state(true),3900)==null); // old interaction
        t.clear();t.update("bili:window1",state(false),2000);t.interact("bili:window2","footer:favorite",2050);
        check(t.update("bili:window2",state(true),2100)==null); // new dialog / different item
        t.clear();t.update("xhs:window1",state(false),3000);t.interact("bili:window1","footer:favorite",3050);
        check(t.update("bili:window1",state(true),3100)==null); // switched platform
        t.clear();t.update("xhs:window1",state(false),4000);t.interact("xhs:window1","footer:favorite",6950);
        check(t.previous("xhs:window1","footer:favorite",7000)==null);
        check(t.update("xhs:window1",state(true),7000)==null); // stale state
        t.clear();t.update("xhs:window1",state(false),8000);t.interact("xhs:window1","footer:favorite",8050);
        Map<String,Boolean> feed=new HashMap<>(state(true));feed.put("another-item",false);
        check(t.update("xhs:window1",feed,8100)==null); // ambiguous feed
        t.clear();t.update("xhs:window1",state(false),9000);t.clear();
        check(t.update("xhs:window1",state(true),9100)==null); // scroll invalidates state
        t.clear();t.update("xhs:window1",state(false),10000);
        check(t.update("xhs:window1",state(true),10100)==null); // content arrives before click
        t.interact("xhs:window1","footer:favorite",10150);
        check(Boolean.FALSE.equals(t.previous("xhs:window1","footer:favorite",10150)));
        check("footer:favorite".equals(t.update("xhs:window1",state(true),10160)));
        check(t.update("xhs:window1",state(true),10200)==null); // delivered once
        t.clear();t.update("xhs:window1",state(false),11000);
        t.interact("xhs:window1","unrelated-like-button",11050);
        check(t.update("xhs:window1",state(true),11100)==null); // unrelated click is not proof
        check(t.update("xhs:window1",state(true),12000)==null); // pending change expires
        System.out.println("Favorite transitions: missing click, duplicate, cancel, stale page, platform/window switch and feed isolation passed.");
    }
}
