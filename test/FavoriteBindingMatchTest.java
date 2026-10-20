package io.github.jiangxixu.shiguang;
public class FavoriteBindingMatchTest {
    static void check(boolean v){if(!v)throw new AssertionError();}
    public static void main(String[] args){
        // A stable opaque ID survives a changed tree path and moderate toolbar movement.
        check(FavoriteBindingMatch.score("app:id/a1","app:id/a1",true,false,0.12,0.15)>0);
        check(FavoriteBindingMatch.score("app:id/a1","app:id/other",true,true,0,0)<0);
        check(FavoriteBindingMatch.score("app:id/a1","app:id/a1",false,true,0,0)<0);
        check(FavoriteBindingMatch.score("app:id/a1","app:id/a1",true,false,0.31,0)<0);
        // Anonymous controls require both the original path and close geometry.
        check(FavoriteBindingMatch.score("",null,true,true,0.02,0.03)>0);
        check(FavoriteBindingMatch.score("",null,true,false,0,0)<0);
        check(FavoriteBindingMatch.score("",null,true,true,0.07,0)<0);
        check(FavoriteBindingMatch.unique(120,-1));check(FavoriteBindingMatch.unique(130,110));
        check(!FavoriteBindingMatch.unique(130,125));check(!FavoriteBindingMatch.unique(-1,-1));
        System.out.println("Stable binding, layout drift and ambiguous control rejection passed.");
    }
}
