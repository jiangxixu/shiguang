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
    static int articleTitleScore(String value,double top,double bottom,double width,double authorTop){
        String text=title(value);
        if(text.isEmpty()||text.length()>120||top<0.04||top>0.25||width<0.40||authorTop<=0||authorTop>0.45||bottom>authorTop)return -1;
        if(text.matches(".*(?:赞同|评论|阅读|关注|回答数|回答·|邀请回答|写回答).*"))return -1;
        return 50;
    }
    static boolean shareId(String id){return id!=null&&id.matches("com\\.zhihu\\.android:id/(?:(?:answer|article|question|toolbar|menu|action|btn|iv|ic|image|button)_)*(?:share|sharing)(?:_(?:button|btn|icon|view|layout|container))?");}
    static boolean more(String label,String id){
        String s=label==null?"":label.replaceAll("\\s+","").replaceAll("[,，]按钮","");
        return s.matches("更多|更多操作|更多选项")||(id!=null&&id.matches("com\\.zhihu\\.android:id/(?:(?:answer|article|question|toolbar|menu|action|btn|iv|ic)_)*(?:more|overflow)(?:_(?:button|btn|icon|view))?"));
    }
    static int toolbarScore(double x,double y){
        if(x<0.65)return -1;
        if(y>=0.04&&y<=0.45)return 100-(int)(y*100);
        if(y>=0.85&&y<=0.99)return 10;
        return -1;
    }
}
