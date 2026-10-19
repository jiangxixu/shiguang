package io.github.jiangxixu.shiguang;

import java.net.URI;
import java.util.Locale;

/** Only follow Xiaohongshu-owned short-link redirects, retaining all original access parameters. */
final class XhsLinks {
    interface Redirects {String location(String url) throws Exception;}
    static boolean shortLink(String value){try{if(!allowed(value.replaceFirst("(?i)^http:","https:")))return false;String h=new URI(value).getHost();return "xhslink.com".equalsIgnoreCase(h)||"xhslink.cn".equalsIgnoreCase(h);}catch(Exception e){return false;}}
    static boolean allowed(String value){
        try{URI u=new URI(value);String h=u.getHost();if(h==null||u.getRawUserInfo()!=null||!"https".equalsIgnoreCase(u.getScheme())||(u.getPort()!=-1&&u.getPort()!=443)||value.length()>8192)return false;h=h.toLowerCase(Locale.ROOT);return h.equals("xhslink.com")||h.equals("xhslink.cn")||h.equals("xiaohongshu.com")||h.endsWith(".xiaohongshu.com");}catch(Exception e){return false;}
    }
    static String noteId(String value){
        if(!allowed(value))return null;
        try{URI u=new URI(value);String h=u.getHost().toLowerCase(Locale.ROOT);if(!(h.equals("xiaohongshu.com")||h.endsWith(".xiaohongshu.com")))return null;
            java.util.regex.Matcher m=java.util.regex.Pattern.compile("^/(?:explore|discovery/item|item)/([a-fA-F0-9]{24})/?$").matcher(u.getPath());return m.matches()?m.group(1):null;
        }catch(Exception e){return null;}
    }
    static String resolve(String value,Redirects redirects){
        String current=value.replaceFirst("(?i)^http:","https:");
        for(int i=0;i<5;i++){
            if(!allowed(current))return value;if(noteId(current)!=null)return current;
            try{String next=redirects.location(current);if(next==null)return value;current=new URI(current).resolve(next).toString().replaceFirst("(?i)^http:","https:");}catch(Exception e){return value;}
        }
        return noteId(current)!=null?current:value;
    }
}
