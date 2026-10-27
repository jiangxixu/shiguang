package io.github.jiangxixu.shiguang;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.Collections;
public class CaptureTargetsTest {
    @Test public void mergesOnlyNestedCopiesOfOneAction(){
        CaptureTargets.Box parent=new CaptureTargets.Box(20,700,200,900),child=new CaptureTargets.Box(50,740,170,860);
        assertEquals(1,CaptureTargets.unique(Arrays.asList(parent,child)));
        assertEquals(0,CaptureTargets.unique(Arrays.asList(child,parent)));
        assertEquals(-1,CaptureTargets.unique(Arrays.asList(parent,new CaptureTargets.Box(250,700,430,900))));
        assertEquals(-1,CaptureTargets.unique(Arrays.asList(parent,new CaptureTargets.Box(100,800,250,950))));
        assertEquals(-1,CaptureTargets.unique(Collections.emptyList()));
        assertEquals(-1,CaptureTargets.unique(Arrays.asList(new CaptureTargets.Box(0,0,0,50))));
    }
}
