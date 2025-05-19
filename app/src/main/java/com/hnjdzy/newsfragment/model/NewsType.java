package com.hnjdzy.newsfragment.model;

public class NewsType {
    private int tid;
    private String tname;

    public NewsType(int tid, String tname) {
        this.tid = tid;
        this.tname = tname;
    }

    public NewsType(){
    }

    public int getTid() {
        return tid;
    }

    public void setTid(int tid) {
        this.tid = tid;
    }

    public String getTname() {
        return tname;
    }

    public void setTname(String tname) {
        this.tname = tname;
    }

    @Override
    public String toString() {
        return "NewsType{" +
                "tid=" + tid +
                ", tname='" + tname + '\'' +
                '}';
    }
}
