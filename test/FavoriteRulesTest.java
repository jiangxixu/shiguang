package io.github.jiangxixu.shiguang;
public class FavoriteRulesTest {
    static void check(boolean condition){if(!condition)throw new AssertionError();}
    public static void main(String[] args){
        check(FavoriteRules.favoriteButton("收藏"));check(FavoriteRules.favoriteButton("收藏 123"));
        for(String label:new String[]{"取消收藏","已收藏","我的收藏","收藏夹","收藏成功","点赞","123"})check(!FavoriteRules.favoriteButton(label));
        check(FavoriteRules.success("收藏成功"));check(!FavoriteRules.success("取消收藏成功"));
        check(FavoriteRules.copy("复制链接"));check(!FavoriteRules.copy("复制图片"));
        check(FavoriteRules.share("分享笔记"));check(!FavoriteRules.share("更多"));
        String xhs="com.xingin.xhs",bili="tv.danmaku.bili";
        check(FavoriteRules.contentUrl(xhs,"https://xhslink.cn/o/Abc123"));
        check(FavoriteRules.contentUrl(xhs,"https://www.xiaohongshu.com/explore/0123456789abcdef01234567?xsec_token=a%2Bb"));
        check(FavoriteRules.contentUrl(bili,"https://b23.tv/Abc123"));
        check(FavoriteRules.contentUrl(bili,"https://www.bilibili.com/video/BV123"));
        for(String url:new String[]{"https://www.xiaohongshu.com/","https://xhslink.cn/","https://www.bilibili.com/video/BV123","https://xhslink.cn.evil.test/o/abc","https://user@xhslink.cn/o/abc","javascript:alert(1)","https://www.xiaohongshu.com/user/profile/0123456789abcdef01234567"})check(!FavoriteRules.contentUrl(xhs,url));
        check(!FavoriteRules.contentUrl("com.example.other","https://b23.tv/Abc123"));
        System.out.println("Favorite/cancel distinction and real platform-content URL validation passed.");
    }
}
