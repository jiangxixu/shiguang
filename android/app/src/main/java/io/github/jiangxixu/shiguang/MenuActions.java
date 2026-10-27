package io.github.jiangxixu.shiguang;

final class MenuActions {
    static boolean bindingShape(int width,int height,int screenWidth,int screenHeight){
        if(width<=0||height<=0||screenWidth<=0||screenHeight<=0||width>screenWidth||height>screenHeight*.25)return false;
        return width<=screenWidth*.6||(height<=screenHeight*.10&&width>=height*3L);
    }
    static boolean articleFooter(String text){return text!=null&&text.trim().matches("阅读原文|在看|写留言|精选留言|赞赏作者|喜欢作者|留言");}
    static boolean articleContext(java.util.Collection<String> labels,boolean heading){
        java.util.Set<String> footers=new java.util.HashSet<>();
        for(String label:labels)if(articleFooter(label))footers.add(label.trim());
        return footers.size()>=2||(heading&&!footers.isEmpty());
    }
    static boolean compact(int width,int height,int screenWidth,float density){return width>0&&height>0&&width<=screenWidth&&height<=96*density;}
    static boolean youtubeMore(String text,String description,String id){
        for(String label:new String[]{text,description})if(label!=null&&label.trim().replaceAll("[,，]\\s*(?i:button|按钮)$","").matches("(?i)更多|更多操作|更多选项|More|More options|More actions"))return true;
        return id!=null&&id.matches("com\\.google\\.android\\.youtube:id/(?:overflow_button|more_button|more_options_button)");
    }
}
