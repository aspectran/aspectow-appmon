package com.aspectran.appmon.persist.counter;

/**
 * <p>Created: 2025-02-14</p>
 */
public class CounterVO {

    private String inst;

    private String ymd;

    private String tm;

    private long cnt1;

    private long cnt2;

    public String getInst() {
        return inst;
    }

    public void setInst(String inst) {
        this.inst = inst;
    }

    public String getYmd() {
        return ymd;
    }

    public void setYmd(String ymd) {
        this.ymd = ymd;
    }

    public String getTm() {
        return tm;
    }

    public void setTm(String tm) {
        this.tm = tm;
    }

    public long getCnt1() {
        return cnt1;
    }

    public void setCnt1(long cnt1) {
        this.cnt1 = cnt1;
    }

    public long getCnt2() {
        return cnt2;
    }

    public void setCnt2(long cnt2) {
        this.cnt2 = cnt2;
    }
}
