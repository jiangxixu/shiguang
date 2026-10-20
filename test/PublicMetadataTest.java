package io.github.jiangxixu.shiguang;
public class PublicMetadataTest {
    static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        java.util.Map<String,String> meta=PublicMetadata.parse("<meta content='https://sns-webpic-qc.xhscdn.com/note.jpg?a=1&amp;b=2' property='og:image'><meta property=\"og:title\" content=\"研一 &#x5C0F;论文 &amp; 准备\">");
        check(meta.get("title").equals("研一 小论文 & 准备"));check(meta.get("thumbnail").endsWith("a=1&b=2"));
        check(PublicMetadata.parse("<title>笔记标题 - 小红书</title>").get("title").equals("笔记标题 - 小红书"));
        String state=PublicMetadata.stateJson("<script>window.__INITIAL_STATE__ = {\"note\":{\"title\":\"undefined } \\\" test\",\"unused\":undefined}};</script>");
        check(state.equals("{\"note\":{\"title\":\"undefined } \\\" test\",\"unused\":null}}"));
        check(PublicMetadata.stateJson("window.__INITIAL_STATE__={\"unfinished\":{")==null);
        check(PublicMetadata.stateJson("<script>alert('test')</script>")==null);
        check(PublicMetadata.stateJson("window.__INITIAL_STATE__={\"title\":\"a\"}; doSomething()").equals("{\"title\":\"a\"}"));
        System.out.println("Public OG metadata, entities and inert bounded initial-state extraction passed.");
    }
}
