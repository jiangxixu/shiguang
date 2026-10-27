package io.github.jiangxixu.shiguang;

/** Collapse only nested wrappers of one semantic action; separate actions stay ambiguous. */
final class CaptureTargets {
    static final class Box {
        final int left,top,right,bottom;
        Box(int left,int top,int right,int bottom){this.left=left;this.top=top;this.right=right;this.bottom=bottom;}
        long area(){return (long)(right-left)*(bottom-top);}
        boolean contains(Box b){return left<=b.left&&top<=b.top&&right>=b.right&&bottom>=b.bottom;}
    }
    static int unique(java.util.List<Box> boxes){
        if(boxes.isEmpty())return -1;int smallest=0;
        for(int i=0;i<boxes.size();i++){Box b=boxes.get(i);if(b.right<=b.left||b.bottom<=b.top)return -1;if(b.area()<boxes.get(smallest).area())smallest=i;}
        for(Box b:boxes)if(!b.contains(boxes.get(smallest)))return -1;
        return smallest;
    }
}
