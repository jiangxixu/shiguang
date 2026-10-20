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
        String zhihu="com.zhihu.android",douyin="com.ss.android.ugc.aweme",youtube="com.google.android.youtube";
        for(String pkg:FavoriteRules.PACKAGES)check(FavoriteRules.supported(pkg));
        check(FavoriteRules.samePlatform(douyin,"com.ss.android.ugc.aweme.lite"));
        check(FavoriteRules.samePlatform(bili,"com.bilibili.app.in"));
        check(!FavoriteRules.samePlatform(bili,zhihu));check(!FavoriteRules.samePlatform(bili,douyin));check(!FavoriteRules.samePlatform("unknown","unknown"));
        for(String url:new String[]{"https://www.zhihu.com/question/123/answer/456","https://zhuanlan.zhihu.com/p/123","https://www.zhihu.com/zvideo/123","https://zhihu.link/ABC123"})check(FavoriteRules.contentUrl(zhihu,url));
        for(String url:new String[]{"https://v.douyin.com/AbC12/","https://www.douyin.com/video/123","https://www.iesdouyin.com/share/video/123/?a=1","https://www.douyin.com/note/123"}){check(FavoriteRules.contentUrl(douyin,url));check(FavoriteRules.contentUrl("com.ss.android.ugc.aweme.lite",url));}
        for(String url:new String[]{"https://youtu.be/AbC123_-xyZ?t=3","https://www.youtube.com/watch?v=AbC123_-xyZ&list=abc","https://m.youtube.com/shorts/AbC123_-xyZ","https://youtube.com/live/AbC123_-xyZ"})check(FavoriteRules.contentUrl(youtube,url));
        for(String pkg:new String[]{zhihu,douyin,youtube})for(String url:new String[]{"https://www.zhihu.com/","https://www.zhihu.com/people/123","https://www.douyin.com/user/123","https://www.youtube.com/playlist?list=abc","https://youtube.com/watch?v=short","https://youtube.com.evil.test/watch?v=AbC123_-xyZ","https://user@youtu.be/AbC123_-xyZ","https://b23.tv/Abc123"})check(!FavoriteRules.contentUrl(pkg,url));
        for(String label:new String[]{"保存","Save to playlist","Save to Watch later","保存到播放列表","稍后观看"})check(FavoriteRules.favoriteButton(label));
        check(!FavoriteRules.favoriteButton("Saved"));check(!FavoriteRules.favoriteButton("Remove from Watch later"));check(!FavoriteRules.favoriteButton("点赞"));
        check(FavoriteRules.success("Saved to Watch later"));check(FavoriteRules.success("已保存到稍后观看"));
        check(!FavoriteRules.success("Removed from Watch later"));check(!FavoriteRules.success("已从播放列表移除"));
        System.out.println("Favorite/cancel distinction and real platform-content URL validation passed.");
    }
}
