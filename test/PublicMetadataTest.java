package io.github.jiangxixu.shiguang;
public class PublicMetadataTest {
    static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args) throws Exception {
        java.util.Map<String,String> meta=PublicMetadata.parse("<meta content='https://sns-webpic-qc.xhscdn.com/note.jpg?a=1&amp;b=2' property='og:image'><meta property=\"og:title\" content=\"研一 &#x5C0F;论文 &amp; 准备\">");
        check(meta.get("title").equals("研一 小论文 & 准备"));check(meta.get("thumbnail").endsWith("a=1&b=2"));
        check(PublicMetadata.parse("<title>笔记标题 - 小红书</title>").get("title").equals("笔记标题 - 小红书"));
        java.util.Map<String,String> fallback=PublicMetadata.parse("<meta property='og:title' content=''><meta name='twitter:title' content='实际标题'><meta property='og:image:secure_url' content=' '><meta property='og:image' content='https://example.com/cover.jpg'>");
        check(fallback.get("title").equals("实际标题"));check(fallback.get("thumbnail").endsWith("/cover.jpg"));
        check(PublicMetadata.parse("<meta property='og:title' content=''><title>实际页面标题</title><meta name='twitter:image:src' content='https://example.com/a.jpg'>").get("title").equals("实际页面标题"));
        String prefix="<meta property='og:title' content='大页面的原始标题'><meta property='og:image' content='https://example.com/large.jpg'><script>"+new String(new char[2100000]).replace('\0','x');
        check(PublicMetadata.parse(prefix).get("thumbnail").endsWith("/large.jpg"));
        java.io.ByteArrayInputStream large=new java.io.ByteArrayInputStream(prefix.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String bounded=PublicMetadata.readPrefix(large,2000000,()->false);
        check(bounded.getBytes(java.nio.charset.StandardCharsets.UTF_8).length==2000000);
        check(PublicMetadata.parse(bounded).get("title").equals("大页面的原始标题"));check(large.available()>0);
        byte[] head="<meta property='og:title' content='读取超时前的标题'>".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        java.io.InputStream delayed=new java.io.InputStream(){int pos;public int read() throws java.io.IOException{if(pos==head.length)throw new java.net.SocketTimeoutException();return head[pos++]&255;}public int read(byte[] b,int o,int len) throws java.io.IOException {if(pos==head.length)throw new java.net.SocketTimeoutException();int n=Math.min(len,head.length-pos);System.arraycopy(head,pos,b,o,n);pos+=n;return n;}};
        check(PublicMetadata.parse(PublicMetadata.readPrefix(delayed,10000,()->false)).get("title").equals("读取超时前的标题"));
        String state=PublicMetadata.stateJson("<script>window.__INITIAL_STATE__ = {\"note\":{\"title\":\"undefined } \\\" test\",\"unused\":undefined}};</script>");
        check(state.equals("{\"note\":{\"title\":\"undefined } \\\" test\",\"unused\":null}}"));
        check(PublicMetadata.stateJson("window.__INITIAL_STATE__={\"unfinished\":{")==null);
        check(PublicMetadata.stateJson("<script>alert('test')</script>")==null);
        check(PublicMetadata.stateJson("window.__INITIAL_STATE__={\"title\":\"a\"}; doSomething()").equals("{\"title\":\"a\"}"));
        check(PublicMetadata.renderData("<script type='application/json' id='RENDER_DATA'>%7B%22desc%22%3A%22test%2Bcover%22%7D</script>").equals("{\"desc\":\"test+cover\"}"));
        check(PublicMetadata.renderData("<script id='OTHER'>{}</script>")==null);
        check(PublicMetadata.renderData("<script id='RENDER_DATA'>%ZZ</script>")==null);
        check(PublicMetadata.renderData("<script id='RENDER_DATA'>{\"desc\":\"a+b\"}</script>").equals("{\"desc\":\"a+b\"}"));
        System.out.println("Public OG metadata, entities and inert bounded initial-state extraction passed.");
    }
}
