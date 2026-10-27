package io.github.jiangxixu.shiguang;
public class ZhihuPageRulesTest {
    static void check(boolean v){if(!v)throw new AssertionError();}
    public static void main(String[] args){
        String title="如何成为一名优秀的AIAgent工程师？需要掌握哪些核心技能和知识体系？";
        check(ZhihuPageRules.titleScore(title,"",false,0.12,0.90)>0);
        check(ZhihuPageRules.titleScore("一篇没有问号的知乎文章标题","com.zhihu.android:id/article_title",false,0.15,0.8)>0);
        check(ZhihuPageRules.titleScore("作者在正文里提出的一个问题？","",false,0.55,0.9)<0);
        check(ZhihuPageRules.titleScore("只是普通的一段正文内容","",false,0.15,0.9)<0);
        check(ZhihuPageRules.titleScore("收藏成功，请复制链接后继续","",true,0.1,0.9)<0);
        check(ZhihuPageRules.title("如何学习\n机器人控制？").equals("如何学习 机器人控制？"));
        check(ZhihuPageRules.shareId("com.zhihu.android:id/answer_share_button"));
        check(ZhihuPageRules.shareId("com.zhihu.android:id/menu_share"));
        check(!ZhihuPageRules.shareId("other.app:id/answer_share_button"));
        check(!ZhihuPageRules.shareId("com.zhihu.android:id/share_count"));
        check(ZhihuPageRules.more("更多操作，按钮",null));
        check(ZhihuPageRules.more("","com.zhihu.android:id/toolbar_more"));
        check(!ZhihuPageRules.more("查看更多回答",null));
        check(!ZhihuPageRules.more("","com.zhihu.android:id/follow"));
        check(ZhihuPageRules.toolbarScore(.94,.22)>ZhihuPageRules.toolbarScore(.94,.95));
        check(ZhihuPageRules.toolbarScore(.2,.22)<0);check(ZhihuPageRules.toolbarScore(.9,.60)<0);
        check(ZhihuPageRules.authorAction("+ 关注"));check(!ZhihuPageRules.authorAction("邀请回答"));
        check(ZhihuPageRules.articleTitleScore("AI Agent 的 Skill 系统设计",.12,.16,.51,.18)>0);
        check(ZhihuPageRules.articleTitleScore("阿里巴巴淘宝技术团队",.18,.22,.60,.18)<0);
        check(ZhihuPageRules.articleTitleScore("本文系统阐述了相关的设计理念",.26,.32,.9,.18)<0);
        check(ZhihuPageRules.articleTitleScore("AI Agent 的 Skill 系统设计",.12,.16,.51,-1)<0);
        check(ZhihuPageRules.more("更多操作,按钮",null));check(ZhihuPageRules.more("More options, button",null));
        check(ZhihuPageRules.more("","com.zhihu.android:id/toolbar_more_container"));
        for(String label:new String[]{"复制链接","复制回答链接，按钮","复制文章链接","复制问题链接","Copy link, button"})check(ZhihuPageRules.copyLink(label));
        for(String label:new String[]{"复制正文","复制图片","分享给朋友","复制回答链接到微信"})check(!ZhihuPageRules.copyLink(label));
        for(String label:new String[]{"分享问题","分享回答，按钮","分享文章","Share answer, button"})check(ZhihuPageRules.share(label));
        for(String label:new String[]{"分享给朋友","分享成功","分享到微信","更多回答"})check(!ZhihuPageRules.share(label));
        ZhihuPageRules.Target outer=new ZhihuPageRules.Target(100,100,300,300),inner=new ZhihuPageRules.Target(150,150,250,250);
        check(ZhihuPageRules.nestedTarget(java.util.Arrays.asList(outer,inner))==1);
        check(ZhihuPageRules.nestedTarget(java.util.Arrays.asList(inner,outer))==0);
        check(ZhihuPageRules.nestedTarget(java.util.Arrays.asList(outer,inner,new ZhihuPageRules.Target(200,200,290,290)))==-1);
        check(ZhihuPageRules.nestedTarget(java.util.Arrays.asList(inner,new ZhihuPageRules.Target(400,150,500,250)))==-1);
        check(ZhihuPageRules.nestedTarget(java.util.Collections.emptyList())==-1);
        check(ZhihuPageRules.volatileSceneText("航海家 星期五文艺 等2098人赞同"));
        check(!ZhihuPageRules.volatileSceneText("解决gpt过度防御和过度收窄创新性的方法"));
        ZhihuPageRules.Target follow=new ZhihuPageRules.Target(610,265,794,345);
        check(ZhihuPageRules.authorShare(new ZhihuPageRules.Target(820,276,887,330),follow,922,2048));
        check(!ZhihuPageRules.authorShare(new ZhihuPageRules.Target(820,130,887,190),follow,922,2048));
        check(!ZhihuPageRules.authorShare(new ZhihuPageRules.Target(820,1930,887,2010),follow,922,2048));
        check(!ZhihuPageRules.authorShare(follow,follow,922,2048));
        check(!ZhihuPageRules.authorShare(new ZhihuPageRules.Target(820,276,887,330),follow,2048,922));
        System.out.println("Zhihu title selection, semantic menu entry and toolbar disambiguation passed.");
    }
}
