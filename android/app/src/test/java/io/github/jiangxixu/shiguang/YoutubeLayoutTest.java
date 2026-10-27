package io.github.jiangxixu.shiguang;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class YoutubeLayoutTest {
    private YoutubeLayout.Box b(int l,int t,int r,int bottom,int role){return new YoutubeLayout.Box(l,t,r,bottom,role);}
    private List<YoutubeLayout.Box> watch(){return Arrays.asList(b(421,819,479,886,YoutubeLayout.LIKE),b(525,819,583,886,YoutubeLayout.DISLIKE),b(625,815,696,886,YoutubeLayout.OTHER),b(731,820,800,884,YoutubeLayout.SAVE),b(845,821,894,884,YoutubeLayout.MORE));}
    private List<YoutubeLayout.Box> shorts(){return Arrays.asList(b(805,1320,877,1392,YoutubeLayout.SAVE),b(801,1170,878,1240,YoutubeLayout.COMMENT),b(800,1480,879,1557,YoutubeLayout.OTHER),b(805,1018,878,1083,YoutubeLayout.LIKE),b(844,134,879,185,YoutubeLayout.MORE));}
    @Test public void watchPlayerShareIsDistinctFromSaveThanksAndAnAdMenu(){
        List<YoutubeLayout.Box> page=new ArrayList<>(watch());page.add(b(821,1740,893,1804,YoutubeLayout.MORE));
        assertEquals(2,YoutubeLayout.share(page,922,2048,false));assertEquals(4,YoutubeLayout.more(page,922,2048,false));
        page.set(3,b(731,820,800,884,YoutubeLayout.OTHER)); // Super Thanks replaces Save.
        assertEquals(2,YoutubeLayout.share(page,922,2048,false));
        assertEquals(-1,YoutubeLayout.share(Arrays.asList(page.get(2),page.get(4)),922,2048,false));
        assertEquals(-1,YoutubeLayout.share(page,2048,922,false));
        page.add(b(640,815,710,887,YoutubeLayout.SHARE)); // Two overlapping, distinct actions are ambiguous.
        assertEquals(-1,YoutubeLayout.share(page,922,2048,false));
    }
    @Test public void shortsShareUsesTheSaveAndCommentRailAndNotHeaderMore(){
        List<YoutubeLayout.Box> page=shorts();assertEquals(2,YoutubeLayout.share(page,922,2048,true));assertEquals(4,YoutubeLayout.more(page,922,2048,true));
        assertEquals(-1,YoutubeLayout.share(Arrays.asList(page.get(0),page.get(2)),922,2048,true));
        List<YoutubeLayout.Box> scaled=new ArrayList<>();for(YoutubeLayout.Box b:page)scaled.add(b(b.left*2,b.top*2,b.right*2,b.bottom*2,b.role));
        assertEquals(2,YoutubeLayout.share(scaled,1844,4096,true));
    }
    @Test public void playerIdentityCanComeFromAccessibleDescriptionWithoutPersistingTruncatedMetadata(){
        List<YoutubeLayout.Caption> watchText=Arrays.asList(new YoutubeLayout.Caption(b(32,650,890,715,0),"视频原始标题内容"),new YoutubeLayout.Caption(b(32,735,890,780,0),"@tvbstalk 591 人赞过 5.6万次观看"));
        assertEquals("视频原始标题内容",YoutubeLayout.caption(watchText,watch(),922,2048,false));
        String raw="What If China Became a global Superpower? ...";
        List<YoutubeLayout.Caption> shortsText=Arrays.asList(new YoutubeLayout.Caption(b(40,1750,840,1800,0),raw),new YoutubeLayout.Caption(b(40,1650,840,1690,0),"@ANGeography"),new YoutubeLayout.Caption(b(40,1840,840,1880,0),"Amour plastique"));
        assertEquals(raw,YoutubeLayout.caption(shortsText,shorts(),922,2048,true));assertEquals("",PageTitles.clean(raw));
        assertEquals("",YoutubeLayout.caption(shortsText,Collections.emptyList(),922,2048,true));
    }
}
