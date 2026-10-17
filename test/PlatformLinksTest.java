package io.github.jiangxixu.shiguang;
import java.util.*;

public class PlatformLinksTest {
    static void check(boolean condition){if(!condition)throw new AssertionError();}
    public static void main(String[] args){
        String xhs="https://www.xiaohongshu.com/explore/abc?xsec_token=abc%2B123&xsec_source=pc#part";
        List<String> calls=new ArrayList<>();
        check(PlatformLinks.open(xhs,(url,pkg)->{check(url.equals(xhs));calls.add(String.valueOf(pkg));return true;}));
        check(calls.equals(Arrays.asList("com.xingin.xhs")));
        calls.clear();
        check(PlatformLinks.open("https://b23.tv/abc",(url,pkg)->{calls.add(String.valueOf(pkg));return pkg==null;}));
        check(calls.equals(Arrays.asList("tv.danmaku.bili","com.bilibili.app.in","tv.danmaku.bilibilihd","null")));
        calls.clear();
        check(PlatformLinks.open("https://www.bilibili.com/video/BV123",(url,pkg)->{calls.add(pkg);return "com.bilibili.app.in".equals(pkg);}));
        check(calls.size()==2);
        check(PlatformLinks.packages("WWW.ZHIHU.COM")[0].equals("com.zhihu.android"));
        check(PlatformLinks.packages("v.douyin.com")[0].equals("com.ss.android.ugc.aweme"));
        check(PlatformLinks.packages("youtu.be")[0].equals("com.google.android.youtube"));
        for(String host:new String[]{"bilibili.com.evil.test","evilbilibili.com","xiaohongshu.com.evil.test","linux.do"})check(PlatformLinks.packages(host).length==0);
        for(String url:new String[]{"javascript:alert(1)","intent://x","file:///a","https://user@bilibili.com/video/a","not a URL"})
            check(!PlatformLinks.open(url,(u,pkg)->{throw new AssertionError("unsafe link was launched");}));
        check(!PlatformLinks.open(xhs,(u,pkg)->false));
        System.out.println("App-first routing, short links, token preservation, fallback and unsafe-link checks passed.");
    }
}
