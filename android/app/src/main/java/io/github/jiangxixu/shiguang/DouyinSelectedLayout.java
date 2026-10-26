package io.github.jiangxixu.shiguang;

import java.util.List;

/** The portrait Selected video layout: bottom Share circle immediately left of Fullscreen. */
final class DouyinSelectedLayout {
    static final class Box {
        final int left,top,right,bottom;final boolean excluded;
        Box(int left,int top,int right,int bottom,boolean excluded){this.left=left;this.top=top;this.right=right;this.bottom=bottom;this.excluded=excluded;}
        double x(){return (left+right)/2.0;}double y(){return (top+bottom)/2.0;}
        int width(){return right-left;}int height(){return bottom-top;}
        boolean contains(Box b){return left<=b.left&&top<=b.top&&right>=b.right&&bottom>=b.bottom;}
    }
    static boolean circle(Box b,int width,int height){
        if(width<=0||height<width*1.4||b.left<0||b.top<0||b.right>width||b.bottom>height||b.height()<=0)return false;
        double x=b.x()/width,y=b.y()/height,w=(double)b.width()/width,h=(double)b.height()/height,ratio=(double)b.width()/b.height();
        return x>=.72&&x<=.98&&y>=.92&&y<=.99&&w>=.05&&w<=.16&&h>=.018&&h<=.085&&ratio>=.65&&ratio<=1.55;
    }
    static int select(List<Box> boxes,int width,int height){
        int found=-1;
        for(int i=0;i<boxes.size();i++){
            Box candidate=boxes.get(i);
            if(candidate.excluded||!circle(candidate,width,height)||candidate.x()/width>.86)continue;
            boolean neighbor=false;
            for(Box next:boxes)if(circle(next,width,height)&&next.x()/width>=.87&&next.left>=candidate.right-2
                &&Math.abs(next.y()-candidate.y())<=Math.min(candidate.height(),next.height())*.3){neighbor=true;break;}
            if(!neighbor)continue;
            if(found<0){found=i;continue;}
            Box previous=boxes.get(found);
            // Collapse nested clickable wrappers for the same circle; reject two distinct targets.
            if(previous.contains(candidate))found=i;
            else if(!candidate.contains(previous))return -1;
        }
        return found;
    }
}
