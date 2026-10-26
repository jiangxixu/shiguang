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
        calls.clear();
        check(PlatformLinks.open("https://v.douyin.com/AbC123/",(url,pkg)->{calls.add(String.valueOf(pkg));return "com.ss.android.yumme.video".equals(pkg);}));
        check(calls.equals(Arrays.asList("com.ss.android.ugc.aweme","com.ss.android.ugc.aweme.lite","com.ss.android.yumme.video")));
        check(PlatformLinks.packages("youtu.be")[0].equals("com.google.android.youtube"));
        check(PlatformLinks.packages("xhslink.cn")[0].equals("com.xingin.xhs"));
        String note="https://www.xiaohongshu.com/discovery/item/0123456789abcdef01234567?xsec_token=a%2Bb";
        check(PlatformLinks.open(note,(url,pkg)->{check(url.equals("xhsdiscover://item/0123456789abcdef01234567?xsec_token=a%2Bb"));check(pkg.equals("com.xingin.xhs"));return true;}));
        calls.clear();
        check(PlatformLinks.open("https://xhslink.cn/o/abc",(url,pkg)->{calls.add(url);return pkg==null;}));
        check(calls.get(0).equals("https://xhslink.cn/o/abc"));
        check(calls.size()==2);check(calls.get(1).equals("https://xhslink.cn/o/abc"));
        // A resolved short link must launch the actual native note, never an in-app browser.
        String shortUrl="http://xhslink.com/a/Abc123";
        String resolved=XhsLinks.resolve(shortUrl,url->{if(url.contains("xhslink.com"))return "https://xhslink.cn/o/Abc123";return note;});
        check(resolved.equals(note));
        check(PlatformLinks.open(resolved,(url,pkg)->{check(url.equals("xhsdiscover://item/0123456789abcdef01234567?xsec_token=a%2Bb"));check(pkg.equals("com.xingin.xhs"));return true;}));
        check(XhsLinks.resolve(shortUrl,url->"https://127.0.0.1/private").equals(shortUrl));
        check(XhsLinks.resolve(shortUrl,url->"https://xiaohongshu.com.evil.test/explore/0123456789abcdef01234567").equals(shortUrl));
        check(XhsLinks.resolve(shortUrl,url->"https://www.xiaohongshu.com/user/profile/0123456789abcdef01234567").equals(shortUrl));
        int[] hops={0};check(XhsLinks.resolve(shortUrl,url->{hops[0]++;return url;}).equals(shortUrl));check(hops[0]==5);
        check(XhsLinks.resolve(note,url->{throw new AssertionError("Resolved note should not be fetched");}).equals(note));
        check(XhsLinks.resolve(shortUrl,url->{throw new java.io.IOException("offline");}).equals(shortUrl));
        for(String host:new String[]{"bilibili.com.evil.test","evilbilibili.com","xiaohongshu.com.evil.test","linux.do"})check(PlatformLinks.packages(host).length==0);
        for(String url:new String[]{"javascript:alert(1)","intent://x","file:///a","https://user@bilibili.com/video/a","not a URL"})
            check(!PlatformLinks.open(url,(u,pkg)->{throw new AssertionError("unsafe link was launched");}));
        check(!PlatformLinks.open(xhs,(u,pkg)->false));
        System.out.println("App-first routing, short links, token preservation, fallback and unsafe-link checks passed.");
    }
}
