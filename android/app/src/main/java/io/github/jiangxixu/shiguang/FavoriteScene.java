package io.github.jiangxixu.shiguang;

/** Keep a pending favorite through dialog scrolling, but never auto-share a different page. */
final class FavoriteScene {
    private int window=-1;
    private String anchor="";
    private long deadline;
    void clear(){window=-1;anchor="";deadline=0;}
    void start(int window,String anchor,long now){this.window=window;this.anchor=anchor==null?"":anchor;deadline=now+17000;}
    boolean tracked(){return !anchor.isEmpty();}
    void renew(long now){deadline=now+17000;}
    boolean matches(int window,String anchor){return tracked()&&this.window==window&&this.anchor.equals(anchor);}
    boolean contains(int window,java.util.Collection<String> visibleText){return tracked()&&this.window==window&&visibleText.contains(anchor);}
    boolean contains(int window,java.util.Collection<String> visibleText,String contentTitle){
        // Captions may have a trailing expand control removed by the title
        // extractor. Compare like with like, retaining exact window and title
        // identity rather than weakening the check to a text substring.
        return contains(window,visibleText)||matches(window,contentTitle);
    }
    boolean expired(long now){return now>=deadline;}
}
