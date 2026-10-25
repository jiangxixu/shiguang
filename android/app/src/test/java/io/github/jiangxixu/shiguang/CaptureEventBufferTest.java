package io.github.jiangxixu.shiguang;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class CaptureEventBufferTest {
    @Test public void animationBurstKeepsOneRefreshWithoutExtendingDeadline(){
        List<Integer> released=new ArrayList<>();CaptureEventBuffer<Integer> queue=new CaptureEventBuffer<>(released::add);
        assertTrue(queue.offer(0,"video:1"));
        for(int i=1;i<1000;i++)assertFalse(queue.offer(i,"video:1"));
        assertEquals(999,released.size());assertEquals(Integer.valueOf(999),queue.take());assertNull(queue.take());
        assertTrue(queue.offer(1000,"video:1"));
    }
    @Test public void navigationStartsANewBatchAndReleasesTheOldSnapshot(){
        List<Integer> released=new ArrayList<>();CaptureEventBuffer<Integer> queue=new CaptureEventBuffer<>(released::add);
        assertTrue(queue.offer(1,"xhs:1"));assertTrue(queue.offer(2,"youtube:2"));
        assertEquals(Arrays.asList(1),released);assertEquals(Integer.valueOf(2),queue.take());
    }
    @Test public void clickOrDisconnectDiscardsStaleContentExactlyOnce(){
        List<Integer> released=new ArrayList<>();CaptureEventBuffer<Integer> queue=new CaptureEventBuffer<>(released::add);
        queue.offer(1,"wechat:1");queue.clear();queue.clear();
        assertNull(queue.take());assertEquals(Arrays.asList(1),released);
        assertTrue(queue.offer(2,"wechat:1"));assertEquals(Integer.valueOf(2),queue.take());queue.clear();
        assertEquals(Arrays.asList(1),released);
    }
}
