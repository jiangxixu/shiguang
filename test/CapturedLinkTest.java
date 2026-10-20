package io.github.jiangxixu.shiguang;
public class CapturedLinkTest {
    static void check(boolean v){if(!v)throw new AssertionError();}
    public static void main(String[] args){
        String dy="com.ss.android.ugc.aweme",yt="com.google.android.youtube",zh="com.zhihu.android";
        CapturedLink d=CapturedLink.parse(dy,"3.14 机器人基础课程 #学习 https://v.douyin.com/ABC/",1000,1000,"");
        check(d!=null&&d.title.equals("机器人基础课程 #学习"));
        CapturedLink y=CapturedLink.parse(yt,"Robotics tutorial https://youtu.be/AbC123_-xyZ",1100,1000,"");
        check(y!=null&&y.title.equals("Robotics tutorial"));
        check(CapturedLink.parse(dy,"https://v.douyin.com/ABC/",700,1000,"")==null);
        check(CapturedLink.parse(yt,"https://v.douyin.com/ABC/",1000,1000,"")==null);
        check(CapturedLink.parse(dy,"https://www.douyin.com/",1000,1000,"")==null);
        check(CapturedLink.parse(dy,"https://v.douyin.com/A/ https://v.douyin.com/B/",1000,1000,"")==null);
        check(CapturedLink.parse(dy,"https://v.douyin.com/A/",1000,0,"")==null);
        CapturedLink z=CapturedLink.parse(zh,"https://www.zhihu.com/question/123/answer/456",1000,1000,"如何成为一名优秀的机器人开发工程师？");
        check(z!=null&&z.title.equals("如何成为一名优秀的机器人开发工程师？"));
        CapturedLink header=CapturedLink.parse(yt,"https://youtu.be/AbC123_-xyZ",1000,1000,"Robotics lecture title");check(header!=null&&header.title.equals("Robotics lecture title"));
        check(PageTitles.titleId(yt,"com.google.android.youtube:id/video_title"));check(!PageTitles.titleId(yt,"evil.app:id/video_title"));
        check(PageTitles.clean("保存到播放列表").isEmpty());check(PageTitles.clean("截断的内容标题…").isEmpty());
        System.out.println("Focused capture rejects old clipboard, wrong platforms, homepages and ambiguous URLs; titles survive.");
    }
}
