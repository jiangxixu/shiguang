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
        System.out.println("Zhihu title selection, semantic menu entry and toolbar disambiguation passed.");
    }
}
