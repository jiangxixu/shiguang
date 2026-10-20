package io.github.jiangxixu.shiguang;

/** Score stable selectors without tying a resource ID to a specific page-tree path. */
final class FavoriteBindingMatch {
    static int score(String expectedId,String actualId,boolean sameClass,boolean samePath,double dx,double dy){
        if(!sameClass||dx>0.30||dy>0.30)return -1;
        boolean stable=expectedId!=null&&!expectedId.isEmpty()&&expectedId.equals(actualId);
        if(!stable&&((expectedId!=null&&!expectedId.isEmpty())||!samePath||dx>0.06||dy>0.06))return -1;
        return (stable?100:40)+(samePath?30:0)+(int)Math.round(30*(1-Math.min(1,(dx+dy)/0.6)));
    }
    static boolean unique(int best,int second){return best>=0&&(second<0||best-second>=12);}
}
