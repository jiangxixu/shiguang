package io.github.jiangxixu.shiguang;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class CaptureMenuTest {
    @Test public void fullWidthMenuRowsAreActionsButSheetsAndVideoCardsAreNot(){
        assertTrue(MenuActions.compact(1080,144,1080,3));
        assertFalse(MenuActions.compact(1080,1800,1080,3));
        assertFalse(MenuActions.compact(1080,600,1080,3));
        assertTrue(MenuActions.youtubeMore("","More options, button",null));
        assertFalse(MenuActions.youtubeMore("查看更多视频","",null));
        assertFalse(MenuActions.youtubeMore("","","other:id/overflow_button"));
    }
    @Test public void overflowSaveKeepsOriginalVideoRatherThanMenuHeading(){
        MenuOrigin state=new MenuOrigin();state.observe("youtube",7,"Original video","Original video",1000);state.open("youtube",7,1200);
        state.observe("youtube",9,"","保存到播放列表",1500);
        MenuOrigin.Source origin=state.source("youtube",2000);
        assertEquals(7,origin.window);assertEquals("Original video",origin.title);
        FavoriteScene scene=new FavoriteScene();scene.start(origin.window,origin.anchor,2000);
        assertFalse(scene.contains(9,Arrays.asList("保存到播放列表")));
        assertTrue(scene.contains(7,Arrays.asList("评论","Original video")));
        assertFalse(scene.contains(7,Arrays.asList("Next video")));
        assertNull(state.source("other",2000));assertNull(state.source("youtube",31201));
        state.clear();assertNull(state.source("youtube",2000));
    }
    @Test public void unlabelledTitleCanStillAnchorOriginalPageWithoutInventingMetadata(){
        MenuOrigin state=new MenuOrigin();state.observe("youtube",7,"","Actual visible video heading",1000);state.open("youtube",7,1100);
        assertEquals("",state.source("youtube",1200).title);
        assertEquals("Actual visible video heading",state.source("youtube",1200).anchor);
    }
    @Test public void staleAndDifferentWindowSnapshotsCannotBeUsed(){
        MenuOrigin state=new MenuOrigin();state.observe("youtube",7,"Original video","Original video",1000);state.open("youtube",8,1100);assertNull(state.source("youtube",1200));
        state.open("youtube",7,4000);assertNull(state.source("youtube",4000));
    }
    @Test public void originalDouyinCaptionIsBoundToAuthorAndNotLocationOrTruncatedText(){
        List<DouyinCaption.Text> page=Arrays.asList(new DouyinCaption.Text("@吴小食",.03,.84,.3),new DouyinCaption.Text("雨花台区 | 门店优惠",.03,.77,.65),new DouyinCaption.Text("人均60多！爽吃一顿牛肋排火锅 #美食 #火锅 展开",.03,.88,.8));
        assertEquals("人均60多！爽吃一顿牛肋排火锅 #美食 #火锅",DouyinCaption.select(page));
        assertEquals("",DouyinCaption.select(Arrays.asList(new DouyinCaption.Text("@吴小食",.03,.84,.3),new DouyinCaption.Text("人均60多！ #美食… 展开",.03,.88,.8))));
        assertEquals("",DouyinCaption.select(Arrays.asList(new DouyinCaption.Text("@吴小食",.03,.84,.3),new DouyinCaption.Text("@另一作者",.03,.7,.3),page.get(2))));
    }
}
