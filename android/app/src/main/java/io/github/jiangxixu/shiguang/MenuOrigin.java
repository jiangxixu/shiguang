package io.github.jiangxixu.shiguang;

/** Short-lived source context for an explicit current-video overflow action. */
final class MenuOrigin {
    static final class Source {
        final String pkg,title,anchor;final int window;final long at;
        Source(String pkg,int window,String title,String anchor,long at){this.pkg=pkg;this.window=window;this.title=title;this.anchor=anchor;this.at=at;}
    }
    private Source observed,opened;private long until;
    void observe(String pkg,int window,String title,String anchor,long now){
        if(opened!=null&&now>until)opened=null;
        if(opened==null&&anchor!=null&&!anchor.isEmpty())observed=new Source(pkg,window,title,anchor,now);
    }
    void open(String pkg,int window,long now){
        if(observed!=null&&observed.pkg.equals(pkg)&&observed.window==window&&now-observed.at<=2000){opened=observed;until=now+30000;}
    }
    void openMenu(String pkg,long now){
        // Called only for a semantically verified action menu, which can have a new window ID.
        if(opened==null&&observed!=null&&observed.pkg.equals(pkg)&&now>=observed.at&&now-observed.at<=2000){opened=observed;until=now+30000;}
    }
    Source source(String pkg,long now){return opened!=null&&opened.pkg.equals(pkg)&&now<=until?opened:null;}
    void clear(){observed=null;opened=null;until=0;}
}
