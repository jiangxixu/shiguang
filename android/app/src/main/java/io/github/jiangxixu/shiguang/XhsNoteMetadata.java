package io.github.jiangxixu.shiguang;

import org.json.*;

/** Both mobile share pages and desktop pages, bound to the URL's exact note ID. */
final class XhsNoteMetadata {
    static JSONObject parse(String html,String noteId){
        JSONObject result=new JSONObject();if(noteId==null)return result;
        try{
            String raw=PublicMetadata.stateJson(html);if(raw==null)return result;
            JSONObject state=new JSONObject(raw),note=null;
            JSONObject mobile=path(state,"noteData","data","noteData");
            if(mobile!=null&&noteId.equals(mobile.optString("noteId")))note=mobile;
            JSONObject desktop=path(state,"note","noteDetailMap",noteId,"note");
            if(note==null&&desktop!=null&&(desktop.optString("noteId").isEmpty()||noteId.equals(desktop.optString("noteId"))))note=desktop;
            if(note==null)return result;
            String title=note.optString("title","").trim();
            if(title.isEmpty())title=note.optString("desc","").split("\n",2)[0].trim();
            if(!title.isEmpty())result.put("title",title);
            JSONArray images=note.optJSONArray("imageList");String image=images!=null&&images.length()>0?image(images.optJSONObject(0)):"";
            if(image.isEmpty())image=image(note.optJSONObject("cover"));
            if(!image.isEmpty())result.put("thumbnail",image);
            result.put("matched",true);
        }catch(Exception ignored){}
        return result;
    }
    private static JSONObject path(JSONObject object,String... keys){for(String key:keys){if(object==null)return null;object=object.optJSONObject(key);}return object;}
    private static String image(JSONObject image){
        if(image==null)return "";
        for(String key:new String[]{"urlDefault","urlPre","url"})if(!image.optString(key).isEmpty())return image.optString(key);
        JSONArray info=image.optJSONArray("infoList");
        if(info!=null)for(int i=0;i<info.length();i++){JSONObject entry=info.optJSONObject(i);if(entry!=null&&!entry.optString("url").isEmpty())return entry.optString("url");}
        return "";
    }
}
