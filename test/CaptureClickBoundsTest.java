package io.github.jiangxixu.shiguang;
public class CaptureClickBoundsTest {
    static void check(boolean v){if(!v)throw new AssertionError();}
    public static void main(String[] args){
        check(CaptureClickBounds.allowed(1080,144,1080,2400,3,true)); // full-width Copy-link row
        check(!CaptureClickBounds.allowed(1080,144,1080,2400,3,false)); // no generic wide click
        check(!CaptureClickBounds.allowed(1080,1600,1080,2400,3,true)); // share-sheet container
        check(CaptureClickBounds.allowed(120,120,1080,2400,3,false)); // icon
        check(!CaptureClickBounds.allowed(0,100,1080,2400,3,true));
        check(!CaptureClickBounds.allowed(1200,100,1080,2400,3,true));
        System.out.println("Verified full-width copy rows, icon clicks and whole-sheet rejection.");
    }
}
