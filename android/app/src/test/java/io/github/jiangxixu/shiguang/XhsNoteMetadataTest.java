package io.github.jiangxixu.shiguang;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class XhsNoteMetadataTest {
    private static final String ID="6ad766f70000000029014b8a";
    @Test public void mobileSharePageReturnsOriginalTitleAndFirstImage()throws Exception{
        // Structure observed on the user's public short link: desktop goes to login,
        // mobile has noteData.data.noteData even though the HTML title is just 小红书.
        String html="<title>小红书</title><script>window.__INITIAL_STATE__={\"noteData\":{\"data\":{\"noteData\":{\"noteId\":\""+ID+"\",\"title\":\"原笔记标题\",\"desc\":\"正文\",\"imageList\":[{\"url\":\"https://sns-webpic-qc.xhscdn.com/original.jpg\"}]}},\"normalNotePreloadData\":{\"title\":\"不能使用的预加载标题\"}}};</script>";
        JSONObject result=XhsNoteMetadata.parse(html,ID);
        assertEquals("原笔记标题",result.getString("title"));assertTrue(result.getBoolean("matched"));
        assertEquals("https://sns-webpic-qc.xhscdn.com/original.jpg",result.getString("thumbnail"));
        assertEquals(0,XhsNoteMetadata.parse(html,"000000000000000000000001").length());
    }
    @Test public void desktopExactNoteAndDescriptionFallbackRemainSupported()throws Exception{
        String html="window.__INITIAL_STATE__={\"note\":{\"noteDetailMap\":{\""+ID+"\":{\"note\":{\"desc\":\"第一行标题\\n后续正文\",\"imageList\":[{\"infoList\":[{\"url\":\"https://cdn.example.com/first.jpg\"}]}]}}}}}";
        JSONObject result=XhsNoteMetadata.parse(html,ID);assertEquals("第一行标题",result.getString("title"));assertEquals("https://cdn.example.com/first.jpg",result.getString("thumbnail"));
    }
    @Test public void loginPageAndUnrelatedNotesCannotProvideMetadata(){
        assertEquals(0,XhsNoteMetadata.parse("<title>小红书</title>window.__INITIAL_STATE__={\"profile\":{\"title\":\"其他人的个人页\"}}",ID).length());
        assertEquals(0,XhsNoteMetadata.parse("window.__INITIAL_STATE__={",ID).length());
    }
}
