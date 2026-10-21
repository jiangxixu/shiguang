package io.github.jiangxixu.shiguang;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class WechatArticleTest {
    private static final String SHORT="https://mp.weixin.qq.com/s/AbCdEfGh_1234567890-xyz";
    @Test public void articleUrlsRejectChatContactsLoginAndSpoofedHosts(){
        assertTrue(FavoriteRules.contentUrl("com.tencent.mm",SHORT));
        assertTrue(FavoriteRules.contentUrl("com.tencent.mm","https://mp.weixin.qq.com/s?__biz=MzA1AA%3D%3D&mid=123&idx=1&sn=0123456789abcdef0123456789abcdef&scene=1"));
        for(String url:Arrays.asList("https://mp.weixin.qq.com/","https://mp.weixin.qq.com/s?__biz=abcd","https://mp.weixin.qq.com/mp/profile_ext?action=home","https://mp.weixin.qq.com.evil.test/s/AbCdEfGh_123456","https://user@mp.weixin.qq.com/s/AbCdEfGh_123456","https://weixin.qq.com/s/AbCdEfGh_123456","https://mp.weixin.qq.com/s/AbCdEfGh_123456/extra"))assertFalse(url,FavoriteRules.contentUrl("com.tencent.mm",url));
        assertEquals("wechat",ContentPreviewUrls.family(SHORT));
        assertFalse(ContentPreviewUrls.redirect(SHORT,"https://example.com/article"));
    }
    @Test public void chatTextAndShareButtonsAreNotArticleMarkers(){
        assertFalse(WechatArticle.marker("com.tencent.mm:id/chatting_content","收藏"));
        assertFalse(WechatArticle.marker("","这是聊天中提到的公众号"));
        assertFalse(WechatArticle.marker("","https://mp.weixin.qq.com/"));
        assertTrue(WechatArticle.marker("activity-name","文章标题"));
        assertTrue(WechatArticle.marker("",SHORT));
        assertTrue(WechatArticle.more("更多，按钮"));
        assertFalse(WechatArticle.more("发送给朋友"));
        assertFalse(WechatArticle.more("分享到朋友圈"));
    }
    @Test public void parseArticleHeadingAndOriginalCoverWithoutExecutingScripts(){
        Map<String,String> meta=WechatArticle.metadata("<title>微信公众平台</title><meta property='og:image' content='https://mmbiz.qpic.cn/mmbiz_jpg/test/0?wx_fmt=jpeg&amp;x=1'><h1 id='activity-name'> 机器人 &amp; 学习 </h1><script>throw 'never execute';</script>");
        assertEquals("机器人 & 学习",meta.get("title"));assertTrue(meta.get("thumbnail").endsWith("wx_fmt=jpeg&x=1"));
        meta=WechatArticle.metadata("<title>该内容已被发布者删除</title><meta property='og:image' content='https://example.com/error.png'>");
        assertEquals("",meta.get("title"));assertEquals("",meta.get("thumbnail"));
    }
    @Test public void copiedLinkKeepsArticleTitleAndFreshness(){
        CapturedLink saved=CapturedLink.parse("com.tencent.mm",SHORT,1000,1000,"公众号文章原始标题");
        assertNotNull(saved);assertEquals("公众号文章原始标题",saved.title);
        assertNull(CapturedLink.parse("com.tencent.mm",SHORT,1,1000,"公众号文章原始标题"));
        assertEquals("微信公众号",ShareLinks.platformName(SHORT));
        assertEquals("com.tencent.mm",PlatformLinks.packages("mp.weixin.qq.com")[0]);
        assertEquals(0,PlatformLinks.packages("mp.weixin.qq.com.evil.test").length);
    }
}
