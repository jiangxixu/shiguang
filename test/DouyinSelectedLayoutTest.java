package io.github.jiangxixu.shiguang;
import java.util.*;
public class DouyinSelectedLayoutTest {
    static void check(boolean value){if(!value)throw new AssertionError();}
    static DouyinSelectedLayout.Box box(int l,int t,int r,int b){return new DouyinSelectedLayout.Box(l,t,r,b,false);}
    public static void main(String[] args){
        // Coordinates measured from the supplied 922 x 2048 portrait screenshot.
        DouyinSelectedLayout.Box share=box(676,1926,775,2023),fullscreen=box(794,1926,893,2023);
        List<DouyinSelectedLayout.Box> page=Arrays.asList(box(31,1926,655,2023),share,fullscreen,box(800,1740,900,1850),box(800,1570,890,1650));
        check(DouyinSelectedLayout.select(page,922,2048)==1);
        check(DouyinSelectedLayout.select(Arrays.asList(share),922,2048)==-1);
        check(DouyinSelectedLayout.select(Arrays.asList(fullscreen),922,2048)==-1);
        check(DouyinSelectedLayout.select(page,2048,922)==-1);
        check(DouyinSelectedLayout.select(Arrays.asList(box(675,1925,776,2024),share,fullscreen),922,2048)==1);
        check(DouyinSelectedLayout.select(Arrays.asList(share,box(700,1930,790,2020),fullscreen),922,2048)==-1);
        check(DouyinSelectedLayout.select(Arrays.asList(new DouyinSelectedLayout.Box(676,1926,775,2023,true),fullscreen),922,2048)==-1);
        List<DouyinSelectedLayout.Box> scaled=new ArrayList<>();for(DouyinSelectedLayout.Box b:page)scaled.add(box(b.left*2,b.top*2,b.right*2,b.bottom*2));
        check(DouyinSelectedLayout.select(scaled,1844,4096)==1);
        check(DouyinSelectedLayout.select(Arrays.asList(box(676,1500,775,1597),fullscreen),922,2048)==-1);
        List<DouyinSelectedLayout.Box> rail=Arrays.asList(box(800,1570,890,1650),box(800,1390,890,1480),box(800,1740,900,1850),box(800,1220,890,1300));
        check(DouyinSelectedLayout.favorite(rail,922,2048,true)==0);
        check(DouyinSelectedLayout.favorite(rail,922,2048,false)==-1);
        check(DouyinSelectedLayout.favorite(Arrays.asList(rail.get(0)),922,2048,true)==-1);
        System.out.println("Selected Share circle, nested wrappers, fullscreen/watch-later exclusion and scale checks passed.");
    }
}
