package io.github.jiangxixu.shiguang;

/** Absolute deadlines: a busy video page must never postpone confirmation or timeout. */
final class FavoriteWait {
    private long deadline,shareAt;
    void start(long now){deadline=now+15000;shareAt=Long.MAX_VALUE;}
    void confirm(long now){if(shareAt==Long.MAX_VALUE)shareAt=now+800;}
    boolean ready(long now){return now>=shareAt;}
    boolean returnExpired(long now){return shareAt!=Long.MAX_VALUE&&now>=shareAt+5000;}
    boolean expired(long now){return now>=deadline&&shareAt==Long.MAX_VALUE;}
}
