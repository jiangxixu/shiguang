package io.github.jiangxixu.shiguang;
import org.junit.Test;
import static org.junit.Assert.*;
public class CaptureReadinessTest {
    @Test public void busyAnimationsDoNotExtendDeadline(){
        CaptureReadiness r=new CaptureReadiness();
        for(long now=100;now<2100;now+=100)assertTrue(r.retry(now,false));
        assertFalse(r.retry(2100,false));assertFalse(r.retry(4000,false));
        r.reset();assertTrue(r.retry(5000,false));
        assertFalse(r.retry(5200,true));assertTrue(r.retry(6000,false));
    }
    @Test public void backgroundEventsDoNotCancelCurrentCapture(){
        assertFalse(CaptureReadiness.foreignForeground("youtube","wechat","youtube",false));
        assertFalse(CaptureReadiness.foreignForeground("youtube","wechat","",false));
        assertFalse(CaptureReadiness.foreignForeground("youtube","wechat","wechat",true));
        assertFalse(CaptureReadiness.foreignForeground("youtube","youtube","youtube",false));
        assertTrue(CaptureReadiness.foreignForeground("youtube","wechat","wechat",false));
    }
    @Test public void originalContentMustReturnBeforeRetryingItsMenu(){
        FavoriteWait confirmation=new FavoriteWait();FavoriteScene source=new FavoriteScene();CaptureReadiness menu=new CaptureReadiness();
        confirmation.start(0);source.start(7,"original title",0);confirmation.confirm(500);
        assertTrue(confirmation.ready(1300));
        assertFalse(source.contains(8,java.util.Arrays.asList("folder picker")));
        assertFalse(source.contains(7,java.util.Arrays.asList("different title")));
        assertTrue(source.contains(7,java.util.Arrays.asList("original title")));
        assertTrue(menu.retry(1500,false));assertTrue(menu.retry(2000,false));
        assertFalse(menu.retry(2200,true));
        // A second capture starts with fresh deadlines, not the previous timeout.
        confirmation.start(10000);source.clear();menu.reset();
        assertFalse(confirmation.ready(11000));assertFalse(source.tracked());assertTrue(menu.retry(11000,false));
    }
    @Test public void smallControlMovementStillRequiresUniqueStructuralMatch(){
        int moved=FavoriteBindingMatch.score("app:id/favorite","app:id/favorite",true,true,.002,.006);
        assertTrue(FavoriteBindingMatch.unique(moved,-1));
        assertFalse(FavoriteBindingMatch.unique(moved,moved));
        assertEquals(-1,FavoriteBindingMatch.score("app:id/favorite","app:id/share",true,true,0,0));
        assertEquals(-1,FavoriteBindingMatch.score("",null,true,false,0,0));
    }
}
