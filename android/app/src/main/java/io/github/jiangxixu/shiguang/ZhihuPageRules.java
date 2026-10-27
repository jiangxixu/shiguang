package io.github.jiangxixu.shiguang;

/** Semantic Zhihu detail-page hints. Unknown icons are never inferred from position alone. */
final class ZhihuPageRules {
    static String title(String value){
        String s=value==null?"":value.trim().replaceAll("\\s+"," ");
        if(s.length()<8||s.length()>300||s.matches(".*(?:复制链接|收藏成功|分享给|打开知乎|已加入收藏夹).*"))return "";
        return s;
    }
    static int titleScore(String value,String id,boolean heading,double top,double width){
        String s=title(value);if(s.isEmpty()||top<0||top>0.40)return -1;
        String tail=id==null?"":id.replaceFirst("^.*:id/","").toLowerCase(java.util.Locale.ROOT);
        if(tail.matches("(?:question|answer|article)_title(?:_.*)?|(?:tv_)?question(?:_text)?"))return 100;
        if(heading&&width>=0.45)return 90;
        if(top<=0.25&&width>=0.55&&(s.endsWith("？")||s.endsWith("?")))return 60;
        return -1;
    }
    static boolean authorAction(String label){return label!=null&&label.replaceAll("[+＋\\s]","").matches("关注|已关注|互相关注");}
    static boolean volatileSceneText(String value){return value!=null&&value.matches(".*[0-9].*(?:人赞同|人收藏|人关注|次阅读|次浏览|条评论|个回答|人看过).*" );}
    static boolean authorShare(Target button,Target follow,int width,int height){
        if(width<=0||height<=width||button.area()<=0||follow.area()<=0)return false;
        double x=(button.left+button.right)/2.0/width,y=(button.top+button.bottom)/2.0/height;
        double fy=(follow.top+follow.bottom)/2.0/height,fx=(follow.left+follow.right)/2.0/width;
        int w=button.right-button.left,h=button.bottom-button.top;
        return x>=.86&&x<=.99&&y>=.08&&y<=.42&&fx>=.5&&fx<=.88&&fy>=.08&&fy<=.42
            &&button.left>=follow.right-width*.025&&Math.abs(y-fy)<=.025
            &&w>=width*.025&&w<=width*.16&&h>=height*.012&&h<=height*.08
            &&(double)w/h>=.4&&(double)w/h<=2;
    }
    static int articleTitleScore(String value,double top,double bottom,double width,double authorTop){
        String text=title(value);
        if(text.isEmpty()||text.length()>120||top<0.04||top>0.25||width<0.40||authorTop<=0||authorTop>0.45||bottom>authorTop)return -1;
        if(text.matches(".*(?:赞同|评论|阅读|关注|回答数|回答·|邀请回答|写回答).*"))return -1;
        return 50;
    }
    static boolean shareId(String id){return id!=null&&id.matches("com\\.zhihu\\.android:id/(?:(?:answer|article|question|toolbar|menu|action|btn|iv|ic|image|button)_)*(?:share|sharing)(?:_(?:button|btn|icon|view|layout|container))?");}
    static boolean more(String label,String id){
        String s=action(label);
        return s.matches("(?i)更多|更多操作|更多选项|More|Moreoptions")||(id!=null&&id.matches("com\\.zhihu\\.android:id/(?:(?:answer|article|question|toolbar|menu|action|btn|iv|ic)_)*(?:more|overflow)(?:_(?:button|btn|icon|view|layout|container))?"));
    }
    private static String action(String label){return label==null?"":label.replaceAll("\\s+","").replaceAll("(?i)[,，](?:按钮|button)","");}
    static boolean copyLink(String label){return action(label).matches("(?i)复制(?:回答|文章|问题)?链接|拷贝链接|複製連結|Copylink");}
    static boolean share(String label){return action(label).matches("(?i)分享(?:回答|文章|问题|内容)?|Share(?:answer|article|question)?");}
    static final class Target {
        final int left,top,right,bottom;
        Target(int left,int top,int right,int bottom){this.left=left;this.top=top;this.right=right;this.bottom=bottom;}
        long area(){return (long)(right-left)*(bottom-top);}
        boolean contains(Target other){return left<=other.left&&top<=other.top&&right>=other.right&&bottom>=other.bottom;}
    }
    static int nestedTarget(java.util.List<Target> targets){
        if(targets.isEmpty())return -1;int smallest=0;
        for(int i=0;i<targets.size();i++){if(targets.get(i).area()<=0)return -1;if(targets.get(i).area()<targets.get(smallest).area())smallest=i;}
        for(Target target:targets)if(!target.contains(targets.get(smallest)))return -1;
        return smallest;
    }
    static int toolbarScore(double x,double y){
        if(x<0.65)return -1;
        if(y>=0.04&&y<=0.45)return 100-(int)(y*100);
        if(y>=0.85&&y<=0.99)return 10;
        return -1;
    }
}
