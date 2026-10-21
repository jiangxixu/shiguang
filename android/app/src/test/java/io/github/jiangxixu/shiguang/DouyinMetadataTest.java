package io.github.jiangxixu.shiguang;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;

public class DouyinMetadataTest {
    @Test public void mobileRouterReturnsOnlyRequestedVideo()throws Exception{
        String html="window._ROUTER_DATA={\"loaderData\":{\"video_(id)/page\":{\"videoInfoRes\":{\"item_list\":[{\"aweme_id\":\"987\",\"desc\":\"推荐视频\"},{\"aweme_id\":\"123\",\"desc\":\"原视频文案 #学习\",\"video\":{\"cover\":{\"url_list\":[\"https://cdn.example.com/cover.jpg\"]}}}]}}}};";
        JSONObject result=DouyinMetadata.parse(html,"https://www.iesdouyin.com/share/video/123/");
        assertEquals("原视频文案 #学习",result.getString("title"));assertEquals("https://cdn.example.com/cover.jpg",result.getString("thumbnail"));
        assertEquals(0,DouyinMetadata.parse(html,"https://www.iesdouyin.com/share/video/456/").length());
    }
    @Test public void desktopRenderDataStillWorks()throws Exception{
        JSONObject video=new JSONObject().put("awemeId","123").put("desc","图文原文案").put("images",new JSONArray().put(new JSONObject().put("url_list",new JSONArray().put("https://cdn.example.com/first.jpg"))));
        String html="<script id='RENDER_DATA'>"+java.net.URLEncoder.encode(video.toString(),"UTF-8")+"</script>";
        JSONObject result=DouyinMetadata.parse(html,"https://www.douyin.com/note/123");assertEquals("图文原文案",result.getString("title"));assertEquals("https://cdn.example.com/first.jpg",result.getString("thumbnail"));
    }
    @Test public void loaderShellAndUnrelatedMetadataAreNotTitles(){
        assertEquals(0,DouyinMetadata.parse("window._ROUTER_DATA={\"loaderData\":{\"itemId\":\"123\",\"title\":\"在抖音记录美好生活\"}};","https://www.iesdouyin.com/share/video/123/").length());
    }
}
