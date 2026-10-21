package io.github.jiangxixu.shiguang;

import org.json.*;
import java.util.regex.*;

/** Exact-item metadata from the mobile router state or the desktop render state. */
final class DouyinMetadata {
    static JSONObject parse(String html,String url){
        JSONObject result=new JSONObject();
        Matcher id=Pattern.compile("/(?:video|note|slides|share/video)/(\\d+)(?:/|$|[?])|[?&]modal_id=(\\d+)(?:&|$)").matcher(url);
        if(!id.find())return result;String wanted=id.group(1)!=null?id.group(1):id.group(2);
        for(String raw:new String[]{PublicMetadata.assignedJson(html,"_ROUTER_DATA"),PublicMetadata.renderData(html)}){
            if(raw==null)continue;
            try{
                JSONObject video=find(new JSONObject(raw),wanted,0,new int[]{0});if(video==null)continue;
                result.put("matched",true);String title=video.optString("desc","").trim();
                if(!title.isEmpty())result.put("title",title);
                JSONObject media=video.optJSONObject("video");String image="";
                if(media!=null)for(String key:new String[]{"origin_cover","cover","dynamic_cover"}){image=image(media.optJSONObject(key));if(!image.isEmpty())break;}
                JSONArray photos=video.optJSONArray("images");if(image.isEmpty()&&photos!=null&&photos.length()>0)image=image(photos.optJSONObject(0));
                if(!image.isEmpty())result.put("thumbnail",image);
                if(!title.isEmpty()&&!image.isEmpty())return result;
            }catch(Exception ignored){}
        }
        return result;
    }
    private static String image(JSONObject object){
        if(object==null)return "";JSONArray urls=object.optJSONArray("url_list");if(urls==null)urls=object.optJSONArray("urlList");
        return urls!=null&&urls.length()>0?urls.optString(0):"";
    }
    private static JSONObject find(Object value,String id,int depth,int[] count){
        if(depth>30||++count[0]>10000)return null;
        if(value instanceof JSONObject){
            JSONObject object=(JSONObject)value;
            if((id.equals(object.optString("aweme_id"))||id.equals(object.optString("awemeId")))&&(object.has("desc")||object.has("video")||object.has("images")))return object;
            java.util.Iterator<String> keys=object.keys();while(keys.hasNext()){JSONObject match=find(object.opt(keys.next()),id,depth+1,count);if(match!=null)return match;}
        }else if(value instanceof JSONArray){JSONArray values=(JSONArray)value;for(int i=0;i<values.length();i++){JSONObject match=find(values.opt(i),id,depth+1,count);if(match!=null)return match;}}
        return null;
    }
}
