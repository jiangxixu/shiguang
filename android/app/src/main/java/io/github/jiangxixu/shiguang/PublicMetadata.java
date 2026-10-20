package io.github.jiangxixu.shiguang;

import java.util.*;
import java.util.regex.*;

/** Bounded HTML metadata parsing; embedded state is data, never executable script. */
final class PublicMetadata {
    static String decode(String value){
        Matcher m=Pattern.compile("&#(x[0-9a-fA-F]+|[0-9]+);").matcher(value);StringBuffer out=new StringBuffer();
        while(m.find()){String replacement=m.group();try{String n=m.group(1);int code=Integer.parseInt(n.startsWith("x")?n.substring(1):n,n.startsWith("x")?16:10);if(Character.isValidCodePoint(code)&&!(code>=0xd800&&code<=0xdfff))replacement=new String(Character.toChars(code));}catch(Exception ignored){}m.appendReplacement(out,Matcher.quoteReplacement(replacement));}m.appendTail(out);
        return out.toString().replace("&quot;","\"").replace("&apos;","'").replace("&lt;","<").replace("&gt;",">").replace("&amp;","&");
    }
    static Map<String,String> parse(String html){
        Map<String,String> values=new HashMap<>();
        Matcher tags=Pattern.compile("<meta\\b[^>]{0,8192}>",Pattern.CASE_INSENSITIVE).matcher(html);
        Pattern attributes=Pattern.compile("([\\w:-]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')");
        while(tags.find()){
            Map<String,String> attrs=new HashMap<>();Matcher a=attributes.matcher(tags.group());
            while(a.find())attrs.put(a.group(1).toLowerCase(Locale.ROOT),decode(a.group(2)!=null?a.group(2):a.group(3)));
            String name=attrs.containsKey("property")?attrs.get("property"):attrs.get("name");
            if(name!=null&&attrs.containsKey("content"))values.put(name.toLowerCase(Locale.ROOT),attrs.get("content"));
        }
        String title=values.get("og:title");if(title==null)title=values.get("twitter:title");
        if(title==null){Matcher t=Pattern.compile("<title[^>]*>([^<]{0,1000})</title>",Pattern.CASE_INSENSITIVE).matcher(html);if(t.find())title=decode(t.group(1));}
        values.put("title",title==null?"":title.trim());
        String image=values.get("og:image:secure_url");if(image==null)image=values.get("og:image");if(image==null)image=values.get("twitter:image");
        values.put("thumbnail",image==null?"":image.trim());return values;
    }
    static String renderData(String html){
        if(html==null||html.length()>2000000)return null;
        Matcher scripts=Pattern.compile("<script\\b([^>]{0,8192})>([\\s\\S]*?)</script\\s*>",Pattern.CASE_INSENSITIVE).matcher(html);
        Pattern id=Pattern.compile("\\bid\\s*=\\s*([\"'])RENDER_DATA\\1",Pattern.CASE_INSENSITIVE);
        while(scripts.find())if(id.matcher(scripts.group(1)).find()){
            String data=scripts.group(2).trim();
            try{return data.startsWith("{")?data:java.net.URLDecoder.decode(data,"UTF-8");}catch(Exception ignored){return null;}
        }
        return null;
    }
    static String stateJson(String html){
        Matcher m=Pattern.compile("window\\.__INITIAL_STATE__\\s*=\\s*\\{").matcher(html);if(!m.find())return null;
        int start=m.end()-1,depth=0;boolean quoted=false,escaped=false;StringBuilder out=new StringBuilder();
        for(int i=start;i<html.length()&&i-start<2000000;i++){
            char c=html.charAt(i);
            if(quoted){out.append(c);if(escaped)escaped=false;else if(c=='\\')escaped=true;else if(c=='\"')quoted=false;continue;}
            if(c=='\"'){quoted=true;out.append(c);continue;}
            if(c=='{'){if(++depth>100)return null;}else if(c=='}'){if(--depth==0){out.append(c);return out.toString();}}
            if(html.startsWith("undefined",i)&&(i==start||!Character.isJavaIdentifierPart(html.charAt(i-1)))&&(i+9==html.length()||!Character.isJavaIdentifierPart(html.charAt(i+9)))){out.append("null");i+=8;}else out.append(c);
        }
        return null;
    }
}
