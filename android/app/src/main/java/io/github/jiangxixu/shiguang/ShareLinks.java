package io.github.jiangxixu.shiguang;

import java.net.URI;
import java.util.*;
import java.util.regex.*;

/** Parse copied share prose, never infer the source from the foreground application. */
final class ShareLinks {
    private static final Pattern LINK=Pattern.compile("(?i)(?:https?://|(?<![\\w@/])(?:www\\.)?(?:xhslink\\.(?:com|cn)|b23\\.tv|v\\.douyin\\.com)/)[^\\s<>\\\"'，。；：！？（）【】《》“”‘’\\u200B-\\u200D\\uFEFF]+");
    static List<String> candidates(String text){
        LinkedHashSet<String> all=new LinkedHashSet<>(),known=new LinkedHashSet<>();
        if(text==null||text.length()>100000)return new ArrayList<>();
        Matcher m=LINK.matcher(text);
        while(m.find()&&all.size()<20){
            String value=m.group();if(!value.matches("(?i)^https?://.*"))value="https://"+value;
            value=value.replaceAll("[)\\]}.!,;]+$","");
            try{
                URI u=new URI(value);String h=u.getHost();
                if(h==null||u.getRawUserInfo()!=null||(u.getPort()!=-1&&u.getPort()!=80&&u.getPort()!=443))continue;
                if(knownHost(h)){
                    // Mobile app share suffixes can touch the ASCII URL without a separating space.
                    if(h.equalsIgnoreCase("xhslink.com")||h.equalsIgnoreCase("xhslink.cn")||h.equalsIgnoreCase("b23.tv")||h.equalsIgnoreCase("v.douyin.com"))value=value.replaceFirst("[\\p{IsHan}].*$","");
                    u=new URI(value);if(u.getHost()==null)continue;
                    known.add(value);
                }
                all.add(value);
            }catch(Exception ignored){}
        }
        // Discard unrelated promotional URLs when real platform links are present. Multiple platform links need a choice.
        return new ArrayList<>(known.isEmpty()?all:known);
    }
    private static boolean knownHost(String host){return PlatformLinks.packages(host).length>0||host.equalsIgnoreCase("linux.do")||host.toLowerCase(Locale.ROOT).endsWith(".linux.do");}
    static String platformName(String value){
        try{String h=new URI(value).getHost();String[] pkgs=PlatformLinks.packages(h);if(pkgs.length==0)return h;
            switch(pkgs[0]){case "com.xingin.xhs":return "小红书";case "tv.danmaku.bili":return "哔哩哔哩";case "com.zhihu.android":return "知乎";case "com.ss.android.ugc.aweme":return "抖音";case "com.google.android.youtube":return "YouTube";case "com.github.android":return "GitHub";default:return h;}
        }catch(Exception e){return "网页";}
    }
    static String sharedTitle(String text){
        if(text==null)return "";
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("【([^【】\\r\\n]{1,500})\\s*\\|\\s*小红书(?:[^【】]*)】").matcher(text);
        if(m.find()){String title=m.group(1).trim();int author=title.lastIndexOf(" - ");if(author>0)title=title.substring(0,author).trim();return title;}
        List<String> links=candidates(text);
        if(links.size()==1&&platformName(links.get(0)).equals("知乎")){
            Matcher link=LINK.matcher(text);String prefix=link.find()?text.substring(0,link.start()).trim():"";
            for(String line:prefix.split("[\\r\\n]+")){
                String title=line.trim().replaceFirst("\\s*[-—]\\s*知乎\\s*$","").replaceAll("^[「【]|[」】]$","").trim();
                if(title.length()>=2&&title.length()<=500&&!title.matches("(?:知乎|分享自知乎|打开知乎.*|来自知乎.*)"))return title;
            }
        }
        if(links.size()==1&&(platformName(links.get(0)).equals("抖音")||platformName(links.get(0)).equals("YouTube"))){
            Matcher link=LINK.matcher(text);String prefix=link.find()?text.substring(0,link.start()).trim():"";
            // Douyin's copied prose includes a share counter and trailing invitation.
            prefix=prefix.replaceFirst("^\\d+(?:\\.\\d+)?\\s+", "").replaceFirst("\\s*复制(?:此|这|本)条.*$", "").trim();
            for(String line:prefix.split("[\\r\\n]+")){
                String title=line.trim().replaceFirst("\\s*[-—]\\s*(?:抖音|YouTube)\\s*$", "").trim();
                if(title.length()>=2&&title.length()<=500&&!title.matches("(?i)(?:抖音|YouTube|分享.*|复制.*|打开.*|快来.*|在抖音.*)"))return title;
            }
        }
        return "";
    }
}
