package io.github.jiangxixu.shiguang;

import java.util.*;
import java.util.regex.*;

/** Public article markers only; chat messages and contacts are not article evidence. */
final class WechatArticle {
    static boolean marker(String id,String value){
        if(id!=null&&id.matches("(?:.*[:/])?(?:js_article|js_content|activity-name|js_name)"))return true;
        if(value==null)return false;
        return FavoriteRules.contentUrl("com.tencent.mm",value.trim())||value.trim().equals("微信公众平台");
    }
    static boolean more(String text){return text!=null&&text.trim().replaceAll("[,，]\\s*(?:按钮|(?i:button))$","").matches("(?i)更多|更多信息|更多功能|更多选项|更多操作|More|More options");}
    static String title(String value){
        String s=PageTitles.clean(value);
        return s.matches("微信|微信公众平台|公众号|微信公众号|.*(?:访问过于频繁|环境异常|内容已被删除|内容已删除|该内容已被发布者删除|该内容无法查看).*")?"":s;
    }
    static Map<String,String> metadata(String html){
        Map<String,String> meta=PublicMetadata.parse(html);String title=title(meta.get("title"));
        Matcher heading=Pattern.compile("<h[12]\\b(?=[^>]{0,2048}\\bid=['\"]activity-name['\"])[^>]*>([\\s\\S]{0,2000}?)</h[12]>",Pattern.CASE_INSENSITIVE).matcher(html);
        if(heading.find()){String actual=title(PublicMetadata.decode(heading.group(1).replaceAll("<[^>]*>"," ")));if(!actual.isEmpty())title=actual;}
        meta.put("title",title);if(title.isEmpty())meta.put("thumbnail","");return meta;
    }
}
