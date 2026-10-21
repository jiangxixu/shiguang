package io.github.jiangxixu.shiguang;

final class MenuActions {
    static boolean compact(int width,int height,int screenWidth,float density){return width>0&&height>0&&width<=screenWidth&&height<=96*density;}
    static boolean youtubeMore(String text,String description,String id){
        for(String label:new String[]{text,description})if(label!=null&&label.trim().replaceAll("[,，]\\s*(?i:button|按钮)$","").matches("(?i)更多|更多操作|更多选项|More|More options|More actions"))return true;
        return id!=null&&id.matches("com\\.google\\.android\\.youtube:id/(?:overflow_button|more_button|more_options_button)");
    }
}
