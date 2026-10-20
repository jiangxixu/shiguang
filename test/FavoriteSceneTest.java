package io.github.jiangxixu.shiguang;

public class FavoriteSceneTest {
    static void check(boolean v){if(!v)throw new AssertionError();}
    public static void main(String[] args){
        FavoriteScene scene=new FavoriteScene();scene.start(7,"original-video-title",1000);
        check(scene.matches(7,"original-video-title"));
        check(scene.contains(7,java.util.Arrays.asList("收藏成功","changed view count","original-video-title")));
        check(!scene.contains(7,java.util.Arrays.asList("original-video-title extended","next-video-title")));
        check(!scene.contains(8,java.util.Arrays.asList("original-video-title")));
        check(!scene.matches(8,"folder-picker")); // a separately-windowed picker can scroll
        check(!scene.matches(7,"folder-picker")); // a picker may also share the content window
        check(scene.matches(7,"original-video-title")); // returns to original after confirmation
        check(!scene.matches(7,"next-video-title")); // scrolling must not capture the next video
        check(!scene.expired(17999));check(scene.expired(18000)); // fixed deadline
        scene.start(7,"",20000);check(!scene.tracked());check(!scene.matches(7,"")); // no guessing
        scene.clear();check(!scene.matches(7,"original-video-title"));
        System.out.println("Folder scrolling, original-page return, next-video rejection, absolute timeout and missing context passed.");
    }
}
