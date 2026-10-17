package io.github.jiangxixu.shiguang;

/** Pixel geometry shared by drag, docking and screen-size changes. */
final class OverlayGeometry {
    static int clamp(int value,int max){return Math.max(0,Math.min(value,Math.max(0,max)));}
    static int edge(int x,int screenWidth,int size,int threshold){
        int max=Math.max(0,screenWidth-size);
        if(x<=threshold)return -1;
        if(x>=max-threshold)return 1;
        return 0;
    }
    static int expandedX(boolean right,int screenWidth,int size,int inset){
        return clamp(right?screenWidth-size-inset:inset,screenWidth-size);
    }
}
