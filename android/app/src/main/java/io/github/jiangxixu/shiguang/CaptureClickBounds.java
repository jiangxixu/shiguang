package io.github.jiangxixu.shiguang;

/** Full-width Copy-link rows are valid controls; whole-screen containers are not. */
final class CaptureClickBounds {
    static boolean allowed(int width,int height,int screenWidth,int screenHeight,float density,boolean copyLabel){
        if(width<=0||height<=0||screenWidth<=0||screenHeight<=0)return false;
        if(width<=screenWidth*0.6&&height<=screenHeight*0.25)return true;
        return copyLabel&&width<=screenWidth&&height<=Math.min(96*density,screenHeight*0.15);
    }
}
