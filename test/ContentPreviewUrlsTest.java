package io.github.jiangxixu.shiguang;
public class ContentPreviewUrlsTest {
    static void check(boolean v){if(!v)throw new AssertionError();}
    public static void main(String[] args){
        check(ContentPreviewUrls.redirect("https://zhihu.link/abc","https://zhuanlan.zhihu.com/p/123"));
        check(ContentPreviewUrls.redirect("https://v.douyin.com/abc/","https://www.iesdouyin.com/share/video/123/"));
        check(ContentPreviewUrls.redirect("https://youtu.be/AbC123_-xyZ","https://www.youtube.com/oembed?format=json"));
        for(String url:new String[]{"https://127.0.0.1/","http://www.zhihu.com/","https://zhihu.com.evil.test/p/123","https://user@zhihu.com/p/123","https://www.zhihu.com:123/p/123"})check(ContentPreviewUrls.family(url).isEmpty());
        check(!ContentPreviewUrls.redirect("https://zhihu.link/abc","https://www.douyin.com/video/123"));
        check(ContentPreviewUrls.title("登录知乎").isEmpty());check(ContentPreviewUrls.title("安全验证").isEmpty());check(ContentPreviewUrls.title("真正的问题标题 - 知乎").equals("真正的问题标题 - 知乎"));
        System.out.println("Platform-bound metadata redirects, private-host rejection and title validation passed.");
    }
}
