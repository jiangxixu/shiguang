package io.github.jiangxixu.shiguang;

/** A receipt can only finish an already requested article favorite. */
final class WechatConfirmation {
    static boolean pending(int phase,String pending,String eventPackage,long elapsed){
        return phase==1&&"com.tencent.mm".equals(pending)&&pending.equals(eventPackage)&&elapsed>=0&&elapsed<15000;
    }
    static boolean receipt(String text){
        return FavoriteRules.clean(text).matches("(?:已收藏|收藏成功|已收藏到微信|已添加到收藏|已加入收藏|已添加到收藏夹)[!！。]?");
    }
    static boolean transientEvent(int type){return type==64||type==16384;} // notification / announcement
}
