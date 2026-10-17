package io.github.jiangxixu.shiguang;
import java.net.URI;

/** Strictly bounded to known apps and real content URLs, never a platform homepage. */
final class FavoriteRules {
    static boolean supported(String pkg){return "com.xingin.xhs".equals(pkg)||"tv.danmaku.bili".equals(pkg)||"com.bilibili.app.in".equals(pkg);}
    static String clean(String s){return s==null?"":s.replaceAll("\\s+","").trim();}
    private static String control(String label){return clean(label).replaceAll("[,，](?:按钮|未选中|已选中|双击激活|点按两次即可激活)","");}
    static boolean favoriteButton(String label){String s=control(label);return s.matches("(?:收藏|收藏按钮|收藏此笔记|添加收藏|加入收藏|Favorite|Save)(?:[,，:：]?\\d+(?:\\.\\d+)?[万千wWkK]?)?");}
    static boolean savedButton(String label){String s=control(label);return s.matches("(?:取消收藏|已收藏)(?:[,，:：]?\\d+(?:\\.\\d+)?[万千wWkK]?)?");}
    static boolean success(String label){String s=clean(label);return s.matches("(?:收藏成功|已收藏|已添加到收藏夹|成功添加到收藏夹)[!！。]?");}
    static boolean copy(String label){String s=control(label);return s.equals("复制链接")||s.equals("复制链接按钮")||s.equals("复制笔记链接")||s.equals("拷贝链接")||s.equalsIgnoreCase("Copylink");}
    static boolean share(String label){String s=control(label);return s.equals("分享")||s.equals("分享按钮")||s.equals("分享笔记")||s.equals("分享视频")||s.equalsIgnoreCase("Share");}
    static boolean contentUrl(String pkg,String value){
        try{
            URI u=new URI(value);String h=u.getHost(),path=u.getPath();
            if(!supported(pkg)||h==null||path==null||u.getRawUserInfo()!=null||!("https".equalsIgnoreCase(u.getScheme())||"http".equalsIgnoreCase(u.getScheme())))return false;
            if(u.getPort()!=-1&&u.getPort()!=443&&u.getPort()!=80)return false;
            h=h.toLowerCase(java.util.Locale.ROOT);
            if(pkg.equals("com.xingin.xhs"))return ((h.equals("xhslink.com")||h.equals("xhslink.cn"))&&path.matches("/(?:[A-Za-z0-9_-]+/)*[A-Za-z0-9_-]+/?"))
                ||((h.equals("xiaohongshu.com")||h.endsWith(".xiaohongshu.com"))&&path.matches("/(?:explore|discovery/item)/[a-fA-F0-9]{24}/?"));
            return (h.equals("b23.tv")&&path.matches("/[A-Za-z0-9]+/?"))||((h.equals("bilibili.com")||h.endsWith(".bilibili.com"))&&path.matches("/video/(?:BV[A-Za-z0-9]+|av[0-9]+)/?"));
        }catch(Exception e){return false;}
    }
}
