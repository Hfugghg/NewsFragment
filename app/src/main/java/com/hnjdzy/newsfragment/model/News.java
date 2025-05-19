package com.hnjdzy.newsfragment.model;

public class News {
    private String nauthor;
    private long ncreatedate;
    private int nid;
    private String npicpath;
    private String nsummary;
    private int ntid;
    private String ntitle;
    private String ntname;
    private boolean read; // 新增的已读字段

    // Getter 和 Setter 方法

    public String getNauthor() {
        return nauthor;
    }

    public void setNauthor(String nauthor) {
        this.nauthor = nauthor;
    }

    public long getNcreatedate() {
        return ncreatedate;
    }

    public void setNcreatedate(long ncreatedate) {
        this.ncreatedate = ncreatedate;
    }

    public int getNid() {
        return nid;
    }

    public void setNid(int nid) {
        this.nid = nid;
    }

    public String getNpicpath() {
        return npicpath;
    }

    public void setNpicpath(String npicpath) {
        this.npicpath = npicpath;
    }

    public String getNsummary() {
        return nsummary;
    }

    public void setNsummary(String nsummary) {
        this.nsummary = nsummary;
    }

    public int getNtid() {
        return ntid;
    }

    public void setNtid(int ntid) {
        this.ntid = ntid;
    }

    public String getNtitle() {
        return ntitle;
    }

    public void setNtitle(String ntitle) {
        this.ntitle = ntitle;
    }

    public String getNtname() {
        return ntname;
    }

    public void setNtname(String ntname) {
        this.ntname = ntname;
    }

    public boolean isRead() { // 新增的 get 方法
        return read;
    }

    public void setRead(boolean read) { // 新增的 set 方法
        this.read = read;
    }
}