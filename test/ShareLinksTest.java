package io.github.jiangxixu.shiguang;
import java.util.*;
public class ShareLinksTest {
    static void check(boolean v){if(!v)throw new AssertionError();}
    public static void main(String[] args){
        String xhs="http://xhslink.com/a/Abc123",bili="https://b23.tv/Abc123";
        check(ShareLinks.candidates("67 学习笔记 http://xhslink.com/a/Abc123，复制本条信息，打开【小红书】查看！").equals(Arrays.asList(xhs)));
        check(ShareLinks.candidates("下载 https://example.com/app 原视频 "+bili).equals(Arrays.asList(bili)));
        check(ShareLinks.candidates("https://xhslink.cn/o/Abc123复制后打开小红书").equals(Arrays.asList("https://xhslink.cn/o/Abc123")));
        check(ShareLinks.candidates("打开“xhslink.cn/o/Abc123”查看笔记").equals(Arrays.asList("https://xhslink.cn/o/Abc123")));
        check(ShareLinks.candidates(xhs+" 和 "+bili).equals(Arrays.asList(xhs,bili)));
        check(ShareLinks.candidates(bili+"\n"+bili).size()==1);
        check(ShareLinks.platformName(xhs).equals("小红书"));check(ShareLinks.platformName(bili).equals("哔哩哔哩"));
        check(ShareLinks.candidates("javascript:alert(1) https://user@xhslink.cn/o/a").isEmpty());
        check(ShareLinks.candidates("https://example.com/中文路径?title=中文").equals(Arrays.asList("https://example.com/中文路径?title=中文")));
        check(ShareLinks.candidates("https://xhslink.cn.evil.test/o/a").equals(Arrays.asList("https://xhslink.cn.evil.test/o/a")));
        check(ShareLinks.platformName("https://xhslink.cn.evil.test/o/a").equals("xhslink.cn.evil.test"));
        check(ShareLinks.candidates("《https://xhslink.cn/o/abc?xsec_token=a%2Bb&xsec_source=app》").get(0).endsWith("xsec_token=a%2Bb&xsec_source=app"));
        check(ShareLinks.sharedTitle("79 【看懂爆火的具身智能 - 彩霞满天 | 小红书 - 你的生活兴趣社区】 https://xhslink.cn/o/abc").equals("看懂爆火的具身智能"));
        check(ShareLinks.sharedTitle("【标题 - 第二部分 - 作者 | 小红书】").equals("标题 - 第二部分"));
        check(ShareLinks.sharedTitle("https://xhslink.cn/o/abc").isEmpty());
        check(ShareLinks.sharedTitle("复制链接，打开【小红书】查看").isEmpty());
        check(ShareLinks.sharedTitle("为什么要学习机器人？ - 知乎 https://www.zhihu.com/question/123/answer/456").equals("为什么要学习机器人？"));
        check(ShareLinks.sharedTitle("问题标题\nhttps://zhihu.link/abc").equals("问题标题"));
        check(ShareLinks.sharedTitle("https://zhihu.link/abc").isEmpty());
        check(ShareLinks.sharedTitle("3.14 学会机器人控制 #机器人 https://v.douyin.com/abc/ 复制此链接，打开抖音").equals("学会机器人控制 #机器人"));
        check(ShareLinks.sharedTitle("复制此链接，打开抖音 https://v.douyin.com/abc/").isEmpty());
        check(ShareLinks.sharedTitle("Learning robotics - YouTube\nhttps://youtu.be/AbC123_-xyZ").equals("Learning robotics"));
        check(ShareLinks.sharedTitle("https://youtu.be/AbC123_-xyZ").isEmpty());
        System.out.println("Copied share prose, platform priority, URL choice, Unicode, access-token and domain-spoof regression tests passed.");
    }
}
