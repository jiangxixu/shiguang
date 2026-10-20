package io.github.jiangxixu.shiguang;
import java.net.URI;
import java.util.Locale;

final class ContentPreviewUrls {
    static String family(String value){
        try{
            URI u=new URI(value);String h=u.getHost();if(h==null||!"https".equalsIgnoreCase(u.getScheme())||u.getRawUserInfo()!=null||(u.getPort()!=-1&&u.getPort()!=443)||value.length()>8192)return "";
            h=h.toLowerCase(Locale.ROOT);
            if(host(h,"zhihu.com")||h.equals("zhihu.link"))return "zhihu";
            if(host(h,"douyin.com")||host(h,"iesdouyin.com"))return "douyin";
            if(host(h,"youtube.com")||h.equals("youtu.be"))return "youtube";
        }catch(Exception ignored){}return "";
    }
    static boolean redirect(String original,String next){String f=family(original);return !f.isEmpty()&&f.equals(family(next));}
    static String title(String value){
        String s=value==null?"":value.trim();
        if(s.isEmpty()||s.matches("(?i)(?:知乎|抖音(?:\\s*[-—]\\s*记录美好生活)?|YouTube|登录.*|安全验证.*|验证.*|Justamoment.*|AccessDenied.*|Sign.?in.*)"))return "";
        return s.substring(0,Math.min(500,s.length()));
    }
    private static boolean host(String h,String d){return h.equals(d)||h.endsWith("."+d);}
}
