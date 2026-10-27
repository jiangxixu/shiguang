package io.github.jiangxixu.shiguang;
/** Only explicit content-title fields are accepted outside Zhihu's structured header. */
final class PageTitles {
    static String clean(String text){
        String value=text==null?"":text.trim().replaceAll("\\s+"," ");
        if(value.length()<2||value.length()>300||value.endsWith("…")||value.endsWith("...")||value.matches("(?i)(?:收藏.*|添加到.*|保存到.*|分享.*|复制.*|登录.*|安全验证.*|Save(?:d)?(?:to)?.*|YouTube|抖音|小红书|哔哩哔哩|知乎)"))return "";
        return value;
    }
    static boolean titleId(String pkg,String id){
        if(!FavoriteRules.supported(pkg)||id==null||!id.startsWith(pkg+":id/"))return false;
        String name=id.substring((pkg+":id/").length());
        return name.matches("(?i)(?:(?:tv|video|note|aweme|player|item|content)_)*(?:title|desc|description)(?:_text)?");
    }
}
