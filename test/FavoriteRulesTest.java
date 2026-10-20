package io.github.jiangxixu.shiguang;
public class FavoriteRulesTest {
    static void check(boolean condition){if(!condition)throw new AssertionError();}
    public static void main(String[] args){
        check(FavoriteRules.favoriteButton("收藏"));check(FavoriteRules.favoriteButton("收藏 123"));
        check(FavoriteRules.favoriteButton("收藏,按钮,未选中"));check(FavoriteRules.favoriteButton("收藏 1.2万，按钮"));
        check(FavoriteRules.savedButton("取消收藏，按钮"));check(FavoriteRules.savedButton("已收藏 123"));
        check(!FavoriteRules.favoriteButton("已收藏,按钮"));check(!FavoriteRules.savedButton("取消收藏成功"));
        for(String label:new String[]{"取消收藏","已收藏","我的收藏","收藏夹","收藏成功","点赞","123"})check(!FavoriteRules.favoriteButton(label));
        check(FavoriteRules.success("收藏成功"));check(!FavoriteRules.success("取消收藏成功"));
        check(FavoriteRules.copy("复制链接"));check(!FavoriteRules.copy("复制图片"));
        check(FavoriteRules.share("分享笔记"));check(!FavoriteRules.share("更多"));
        check(FavoriteRules.share("分享按钮"));check(FavoriteRules.share("分享,按钮"));check(FavoriteRules.copy("复制链接，按钮"));
        check(!FavoriteRules.favoriteButton("她说收藏,按钮"));check(!FavoriteRules.share("分享给好友"));
        check(FavoriteRules.savedState("收藏,已选中"));check(FavoriteRules.savedState("已选中"));
        check(!FavoriteRules.savedState("收藏,未选中"));check(!FavoriteRules.savedState("未选中"));
        check(FavoriteRules.roleId("tv.danmaku.bili","tv.danmaku.bili:id/frame_favor","favorite"));
        check(FavoriteRules.roleId("tv.danmaku.bili","tv.danmaku.bili:id/iv_share","share"));
        check(FavoriteRules.roleId("com.xingin.xhs","com.xingin.xhs:id/copy_link","copy"));
        for(String id:new String[]{"tv.danmaku.bili:id/favorite_list","tv.danmaku.bili:id/my_collection","tv.danmaku.bili:id/share_count","evil.app:id/share","tv.danmaku.bili:id/more"})check(!FavoriteRules.roleId("tv.danmaku.bili",id,"share"));
        check(!FavoriteRules.roleId("tv.danmaku.bili","tv.danmaku.bili:id/favorite_list","favorite"));
        check(FavoriteRules.confirmedSuccess("收藏成功"));check(FavoriteRules.confirmedSuccess("已添加到收藏夹"));
        check(!FavoriteRules.confirmedSuccess("已收藏"));check(!FavoriteRules.confirmedSuccess("取消收藏成功"));
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
