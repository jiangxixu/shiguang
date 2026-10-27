package io.github.jiangxixu.shiguang;
import java.util.*;

/** Only the two portrait player control groups; never feed/search/ad card menus. */
final class YoutubeLayout {
    static final int OTHER=0,SAVE=1,SHARE=2,MORE=3,COMMENT=4,LIKE=5,DISLIKE=6,SEARCH=7;
    static final class Box {
        final int left,top,right,bottom,role;
        Box(int l,int t,int r,int b,int role){left=l;top=t;right=r;bottom=b;this.role=role;}
        double x(){return (left+right)/2.0;}double y(){return (top+bottom)/2.0;}
        int width(){return right-left;}int height(){return bottom-top;}
    }
    static final class Caption {
        final Box box;final String value;
        Caption(Box box,String value){this.box=box;this.value=value==null?"":value.trim().replaceAll("\\s+"," ");}
    }
    private static boolean compact(Box b,int w,int h){return w>0&&h>w*1.4&&b.left>=0&&b.top>=0&&b.right<=w&&b.bottom<=h&&b.width()>0&&b.height()>0&&b.width()<=w*.18&&b.height()<=h*.10;}
    private static boolean row(Box a,Box b,int h){return Math.abs(a.y()-b.y())<=h*.018;}
    private static int unique(List<Box> boxes,List<Integer> matches){
        List<CaptureTargets.Box> targets=new ArrayList<>();for(int i:matches){Box b=boxes.get(i);targets.add(new CaptureTargets.Box(b.left,b.top,b.right,b.bottom));}
        int index=CaptureTargets.unique(targets);return index<0?-1:matches.get(index);
    }
    private static boolean at(List<Box> boxes,Box target,double min,double max,int w,int h){
        for(Box b:boxes)if(compact(b,w,h)&&row(b,target,h)&&b.x()/w>=min&&b.x()/w<=max)return true;return false;
    }
    static int share(List<Box> boxes,int w,int h,boolean shorts){
        List<Integer> matches=new ArrayList<>();
        for(int i=0;i<boxes.size();i++){
            Box b=boxes.get(i);if(!compact(b,w,h)||(b.role!=OTHER&&b.role!=SHARE))continue;
            double x=b.x()/w,y=b.y()/h;
            if(!shorts){
                if(x<.66||x>.79||y<.20||y>.65)continue;
                // Like, dislike, Share, Save/Thanks, More must form this player's row.
                if(at(boxes,b,.40,.55,w,h)&&at(boxes,b,.55,.66,w,h)&&at(boxes,b,.79,.89,w,h)&&at(boxes,b,.90,.99,w,h))matches.add(i);
            }else if(x>.82&&x<.99&&y>.55&&y<.87){
                for(Box save:boxes)if(save.role==SAVE&&compact(save,w,h)&&save.x()/w>.82&&Math.abs(save.x()-b.x())<w*.045&&(b.y()-save.y())/h>.045&&(b.y()-save.y())/h<.12){
                    boolean above=false;
                    for(Box comment:boxes)if(compact(comment,w,h)&&Math.abs(comment.x()-save.x())<w*.045&&(save.y()-comment.y())/h>.045&&(save.y()-comment.y())/h<.12&&(comment.role==COMMENT||comment.role==OTHER))above=true;
                    if(above){matches.add(i);break;}
                }
            }
        }
        return unique(boxes,matches);
    }
    static int more(List<Box> boxes,int w,int h,boolean shorts){
        int share=share(boxes,w,h,shorts);if(share<0)return -1;List<Integer> matches=new ArrayList<>();Box arrow=boxes.get(share);
        for(int i=0;i<boxes.size();i++){
            Box b=boxes.get(i);if(!compact(b,w,h)||(b.role!=OTHER&&b.role!=MORE))continue;
            if(b.x()/w<.90||b.x()/w>.99)continue;
            if(shorts?b.y()/h>.04&&b.y()/h<.15:row(b,arrow,h))matches.add(i);
        }
        return unique(boxes,matches);
    }
    static String caption(List<Caption> text,List<Box> controls,int w,int h,boolean shorts){
        int share=share(controls,w,h,shorts);if(share<0)return "";double rowY=controls.get(share).y()/h;
        Caption best=null;
        for(Caption c:text){
            Box b=c.box;String s=c.value;double y=(double)b.top/h;
            if(s.length()<4||s.length()>500||b.left<0||b.left>w*.2||b.width()<w*.4||b.bottom>h||s.startsWith("@"))continue;
            if(shorts?(y<.80||y>.89):(y<Math.max(.16,rowY-.15)||b.bottom/(double)h>rowY-.025))continue;
            if(s.matches("(?is)(?:Shorts|YouTube|使用此音效.*|.*(?:人赞过|次观看|小时前|天前|周前|双击|点按两次).*)")||FavoriteRules.favoriteButton(s)||FavoriteRules.savedButton(s)||FavoriteRules.share(s))continue;
            if(best==null||b.top<best.box.top)best=c;
        }
        return best==null?"":best.value;
    }
}
