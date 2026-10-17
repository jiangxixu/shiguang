package io.github.jiangxixu.shiguang;

/** Runs without Android; checks drag bounds and both edge handles. */
public class OverlayGeometryTest {
    private static void eq(int expected,int actual){if(expected!=actual)throw new AssertionError("expected "+expected+", got "+actual);}
    public static void main(String[] args){
        eq(-1,OverlayGeometry.edge(0,400,44,24));
        eq(1,OverlayGeometry.edge(356,400,44,24));
        eq(0,OverlayGeometry.edge(170,400,44,24));
        eq(0,OverlayGeometry.clamp(-300,356));
        eq(356,OverlayGeometry.clamp(800,356));
        eq(0,OverlayGeometry.clamp(100,-40));
        eq(32,OverlayGeometry.expandedX(false,400,44,32));
        eq(324,OverlayGeometry.expandedX(true,400,44,32));
        // A restored landscape position must remain reachable after portrait rotation.
        eq(356,OverlayGeometry.clamp(760,400-44));
        for(int width:new int[]{30,320,400,800})for(boolean right:new boolean[]{false,true}){
            int x=OverlayGeometry.expandedX(right,width,44,32);
            if(x<0||x>Math.max(0,width-44))throw new AssertionError("off-screen expanded control");
        }
        System.out.println("Overlay drag bounds, left/right docking, expansion and rotation checks passed.");
    }
}
