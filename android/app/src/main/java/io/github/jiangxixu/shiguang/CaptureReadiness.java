package io.github.jiangxixu.shiguang;

/** Bounded waits for transient UI gaps, reset for each collection attempt. */
final class CaptureReadiness {
    private long missingSince=-1;
    void reset(){missingSince=-1;}
    boolean retry(long now,boolean ready){
        if(ready){reset();return false;}
        if(missingSince<0)missingSince=now;
        return now-missingSince<2000;
    }
    static boolean foreignForeground(String pending,String eventPackage,String foreground,boolean transientEvent){
        return !transientEvent&&!pending.isEmpty()&&!pending.equals(eventPackage)
            &&eventPackage.equals(foreground);
    }
}
