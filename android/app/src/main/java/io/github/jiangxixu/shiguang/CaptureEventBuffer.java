package io.github.jiangxixu.shiguang;

import java.util.function.Consumer;

/** Single pending content refresh. User actions never enter this buffer. Main-thread only. */
final class CaptureEventBuffer<T> {
    private final Consumer<T> release;
    private T pending;
    private String scope;
    CaptureEventBuffer(Consumer<T> release){this.release=release;}
    // A new window starts a new bounded batch; same-window updates keep its deadline.
    boolean offer(T event,String nextScope){
        boolean schedule=pending==null||!nextScope.equals(scope);
        clear();pending=event;scope=nextScope;return schedule;
    }
    T take(){T event=pending;pending=null;scope=null;return event;}
    void clear(){T old=take();if(old!=null)release.accept(old);}
}
