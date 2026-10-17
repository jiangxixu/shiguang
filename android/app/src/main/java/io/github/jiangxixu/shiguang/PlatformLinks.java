package io.github.jiangxixu.shiguang;

import java.net.URI;
import java.util.Locale;

/** Launch the original URL in a matching app, then fall back to Android's default handler. */
final class PlatformLinks {
    interface Opener {boolean open(String url,String packageName);}
    private static boolean host(String actual,String domain){return actual.equals(domain)||actual.endsWith("."+domain);}
    static String[] packages(String actual){
        String h=actual.toLowerCase(Locale.ROOT);
        if(host(h,"xiaohongshu.com")||host(h,"xhslink.com")||host(h,"xhslink.cn"))return new String[]{"com.xingin.xhs"};
        if(host(h,"bilibili.com")||host(h,"b23.tv"))return new String[]{"tv.danmaku.bili","com.bilibili.app.in","tv.danmaku.bilibilihd"};
        if(host(h,"zhihu.com")||host(h,"zhihu.link"))return new String[]{"com.zhihu.android"};
        if(host(h,"douyin.com")||host(h,"iesdouyin.com"))return new String[]{"com.ss.android.ugc.aweme","com.ss.android.ugc.aweme.lite"};
        if(host(h,"youtube.com")||host(h,"youtu.be"))return new String[]{"com.google.android.youtube"};
        if(host(h,"github.com"))return new String[]{"com.github.android"};
        return new String[0];
    }
    static boolean open(String url,Opener opener){
        if(url==null||url.length()>8192)return false;
        try{
            URI uri=new URI(url);String scheme=uri.getScheme(),h=uri.getHost();
            if(h==null||uri.getRawUserInfo()!=null||!("https".equalsIgnoreCase(scheme)||"http".equalsIgnoreCase(scheme)))return false;
            if(uri.getPort()==-1||uri.getPort()==443||uri.getPort()==80){
                if(host(h.toLowerCase(Locale.ROOT),"xiaohongshu.com")){
                    java.util.regex.Matcher note=java.util.regex.Pattern.compile("^/(?:explore|discovery/item)/([a-fA-F0-9]{24})/?$").matcher(uri.getPath());
                    if(note.find()){
                        String deep="xhsdiscover://item/"+note.group(1)+(uri.getRawQuery()==null?"":"?"+uri.getRawQuery());
                        if(opener.open(deep,"com.xingin.xhs"))return true;
                    }
                }
                for(String pkg:packages(h))if(opener.open(url,pkg))return true;
                if(host(h.toLowerCase(Locale.ROOT),"xhslink.com")||host(h.toLowerCase(Locale.ROOT),"xhslink.cn")){
                    try{if(opener.open("xhsdiscover://webview?link="+java.net.URLEncoder.encode(url,"UTF-8"),"com.xingin.xhs"))return true;}catch(java.io.UnsupportedEncodingException ignored){}
                }
            }
            return opener.open(url,null);
        }catch(java.net.URISyntaxException e){return false;}
    }
}
