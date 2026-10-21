package io.github.jiangxixu.shiguang;

import java.util.*;

/** Read an untruncated caption below the unique author in a full-screen video. */
final class DouyinCaption {
    static final class Text {
        final String value;final double x,y,width;
        Text(String value,double x,double y,double width){this.value=value==null?"":value.trim();this.x=x;this.y=y;this.width=width;}
    }
    static String select(List<Text> nodes){
        Text author=null;
        for(Text n:nodes)if(n.x<.2&&n.y>.45&&n.y<.92&&n.value.matches("@[^\\s@]{1,60}")){
            if(author!=null&&!author.value.equals(n.value))return "";author=n;
        }
        if(author==null)return "";String result="";
        for(Text n:nodes){
            if(n.x>.2||n.width<.45||n.y<author.y||n.y>Math.min(.96,author.y+.16))continue;
            String value=n.value.replaceFirst("\\s*(?:展开|收起)(?:[∨⌄∧^])?\\s*$","").trim();
            if(value.length()<8||value.length()>300||!value.contains("#")||value.contains("…")||value.contains("...")||value.startsWith("@"))continue;
            if(!result.isEmpty()&&!result.equals(value))return "";result=value;
        }
        return result;
    }
}
