package io.github.jiangxixu.shiguang;
import org.junit.Test;
import static org.junit.Assert.*;

public class WechatConfirmationTest {
    @Test public void acceptsReceiptOnlyDuringSameAppPendingFavorite(){
        assertTrue(WechatConfirmation.pending(1,"com.tencent.mm","com.tencent.mm",1000));
        for(int phase:new int[]{0,2,3,4})assertFalse(WechatConfirmation.pending(phase,"com.tencent.mm","com.tencent.mm",1000));
        assertFalse(WechatConfirmation.pending(1,"com.tencent.mm","com.android.systemui",1000));
        assertFalse(WechatConfirmation.pending(1,"com.tencent.mm","com.tencent.mm",15000));
        assertFalse(WechatConfirmation.pending(1,"com.tencent.mm","com.tencent.mm",-1));
    }
    @Test public void toastAndAnnouncementDoNotRequireAnArticleWindow(){
        assertTrue(WechatConfirmation.transientEvent(64));
        assertTrue(WechatConfirmation.transientEvent(16384));
        assertFalse(WechatConfirmation.transientEvent(1));
        assertFalse(WechatConfirmation.transientEvent(2048));
        for(String text:new String[]{"已收藏","收藏成功","已收藏到微信","已添加到收藏。"})assertTrue(WechatConfirmation.receipt(text));
        for(String text:new String[]{"收藏","取消收藏","取消收藏成功","收藏失败","分享成功","正文里提到已收藏",""})assertFalse(WechatConfirmation.receipt(text));
    }
}
