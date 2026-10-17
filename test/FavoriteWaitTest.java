package io.github.jiangxixu.shiguang;
public class FavoriteWaitTest {
    static void check(boolean v){if(!v)throw new AssertionError();}
    public static void main(String[] args){
        FavoriteWait wait=new FavoriteWait();wait.start(0);
        // Continuous content changes and duplicate success events must not starve launch.
        for(int now=100;now<900;now+=100){wait.confirm(now);check(!wait.ready(now));}
        check(wait.ready(900));check(!wait.expired(16000));
        wait.start(20000);check(!wait.ready(35000));check(wait.expired(35000));
        wait.start(40000);check(!wait.ready(40000));check(!wait.expired(40000));
        wait.confirm(40500);check(!wait.ready(41299));check(wait.ready(41300));
        System.out.println("Busy-page confirmation, repeated success, timeout and next-attempt regression tests passed.");
    }
}
