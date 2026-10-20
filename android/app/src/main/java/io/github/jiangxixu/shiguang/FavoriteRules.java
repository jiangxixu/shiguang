package io.github.jiangxixu.shiguang;
import java.net.URI;

/** Strictly bounded to known apps and real content URLs, never a platform homepage. */
final class FavoriteRules {
    static final String[] PLATFORMS={"小红书","B站","知乎","抖音","YouTube"};
    static final String[] PACKAGES={"com.xingin.xhs","tv.danmaku.bili","com.zhihu.android","com.ss.android.ugc.aweme","com.google.android.youtube"};
    static int platform(String pkg){
        if(pkg==null)return -1;
        for(int i=0;i<PACKAGES.length;i++)if(PACKAGES[i].equals(pkg))return i;
        if(pkg.equals("com.bilibili.app.in")||pkg.equals("tv.danmaku.bilibilihd"))return 1;
        if(pkg.equals("com.ss.android.ugc.aweme.lite"))return 3;
        return -1;
    }
    static boolean supported(String pkg){return platform(pkg)>=0;}
    static boolean samePlatform(String a,String b){return platform(a)>=0&&platform(a)==platform(b);}
    static String platformName(String pkg){int p=platform(pkg);return p<0?"原 App":PLATFORMS[p];}
    static String clean(String s){return s==null?"":s.replaceAll("\\s+","").trim();}
    private static String control(String label){return clean(label).replaceAll("[,，](?:按钮|未选中|已选中|双击激活|点按两次即可激活)","");}
    static boolean favoriteButton(String label){String s=control(label);return s.matches("(?i)(?:收藏|收藏按钮|收藏此笔记|添加收藏|加入收藏|保存|保存到播放列表|保存至播放列表|稍后观看|加入稍后观看|儲存|儲存至播放清單|稍後觀看|Favorite|Save|Savetoplaylist|Savetowatchlater)(?:[,，:：]?\\d+(?:\\.\\d+)?[万千wWkK]?)?");}
    static boolean savedButton(String label){String s=control(label);return s.matches("(?i)(?:取消收藏|已收藏|已保存|已儲存|Saved|RemovefromWatchlater)(?:[,，:：]?\\d+(?:\\.\\d+)?[万千wWkK]?)?");}
    static boolean savedState(String label){return savedButton(label)||clean(label).matches("(?:已选中|已勾选|选中)")||clean(label).matches(".*[,，](?:已选中|已勾选)(?:[,，].*)?");}
    static boolean success(String label){String s=clean(label);return s.matches("(?:收藏成功|已收藏|已添加到收藏夹|成功添加到收藏夹|保存成功)[!！。]?")||s.matches("(?i)(?:已保存到|已保存至|已儲存至|Savedto|Addedto).{1,100}");}
    static boolean copy(String label){String s=control(label);return s.equals("复制链接")||s.equals("复制链接按钮")||s.equals("复制笔记链接")||s.equals("拷贝链接")||s.equals("複製連結")||s.equalsIgnoreCase("Copylink");}
    static boolean share(String label){String s=control(label);return s.equals("分享")||s.equals("分享按钮")||s.equals("分享笔记")||s.equals("分享视频")||s.equals("分享回答")||s.equals("分享文章")||s.equals("共享")||s.equalsIgnoreCase("Share");}
    static boolean roleId(String pkg,String id,String role){
        if(!supported(pkg)||id==null||!id.startsWith(pkg+":id/"))return false;
        String tail=id.substring((pkg+":id/").length());
        String name=role.equals("favorite")?"(?:favorite|favourite|favor|fav|collect|collection)":role.equals("share")?"(?:share|sharing|forward)":"(?:copy_link|copylink|link_copy)";
        return tail.matches("(?i)(?:(?:btn|button|iv|tv|img|image|icon|frame|action|layout|ll|rl)_)*"+name+"(?:_(?:btn|button|icon|view|layout|container))?");
    }
    static boolean confirmedSuccess(String label){return success(label)&&!clean(label).equals("已收藏");}
    static boolean contentUrl(String pkg,String value){
        try{
            URI u=new URI(value);String h=u.getHost(),path=u.getPath();
            if(!supported(pkg)||h==null||path==null||u.getRawUserInfo()!=null||!("https".equalsIgnoreCase(u.getScheme())||"http".equalsIgnoreCase(u.getScheme())))return false;
            if(u.getPort()!=-1&&u.getPort()!=443&&u.getPort()!=80)return false;
            h=h.toLowerCase(java.util.Locale.ROOT);
            if(pkg.equals("com.xingin.xhs"))return ((h.equals("xhslink.com")||h.equals("xhslink.cn"))&&path.matches("/(?:[A-Za-z0-9_-]+/)*[A-Za-z0-9_-]+/?"))
                ||((h.equals("xiaohongshu.com")||h.endsWith(".xiaohongshu.com"))&&path.matches("/(?:explore|discovery/item)/[a-fA-F0-9]{24}/?"));
            if(platform(pkg)==1)return (h.equals("b23.tv")&&path.matches("/[A-Za-z0-9]+/?"))||(host(h,"bilibili.com")&&path.matches("/video/(?:BV[A-Za-z0-9]+|av[0-9]+)/?"));
            if(platform(pkg)==2)return (h.equals("zhihu.link")&&path.matches("/[A-Za-z0-9_-]+/?"))||(host(h,"zhihu.com")&&(path.matches("/(?:question/[0-9]+(?:/answer/[0-9]+)?|answer/[0-9]+|p/[0-9]+|zvideo/[0-9]+|pin/[0-9]+)/?")));
            if(platform(pkg)==3)return (h.equals("v.douyin.com")&&path.matches("/[A-Za-z0-9_-]+/?"))||((host(h,"douyin.com")||host(h,"iesdouyin.com"))&&path.matches("/(?:share/)?(?:video|note)/[0-9]+/?"));
            if(platform(pkg)==4){
                if(h.equals("youtu.be"))return path.matches("/[A-Za-z0-9_-]{11}/?");
                if(!host(h,"youtube.com"))return false;
                if(path.matches("/(?:shorts|live)/[A-Za-z0-9_-]{11}/?"))return true;
                return path.equals("/watch")&&u.getRawQuery()!=null&&java.util.regex.Pattern.compile("(?:^|&)v=[A-Za-z0-9_-]{11}(?:&|$)").matcher(u.getRawQuery()).find();
            }
            return false;
        }catch(Exception e){return false;}
    }
    private static boolean host(String h,String domain){return h.equals(domain)||h.endsWith("."+domain);}
}
