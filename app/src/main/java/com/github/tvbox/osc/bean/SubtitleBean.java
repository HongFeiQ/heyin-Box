package com.github.tvbox.osc.bean;

public class SubtitleBean {

    private String name;

    private String url;

    private boolean isZip;

    public boolean getIsZip() {
        return isZip;
    }

    public void setIsZip(boolean zip) {
        isZip = zip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "SubtitleBean{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", isZip=" + isZip +
                '}';
    }
}