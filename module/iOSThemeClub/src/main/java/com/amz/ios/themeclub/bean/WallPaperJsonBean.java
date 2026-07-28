package com.amz.ios.themeclub.bean;

import java.util.List;

/**
 * Created by ZhangMingZhe on 11/15/16.
 */

public class WallPaperJsonBean {

    /**
     * total : 7
     * sourceDescription : ÎÄ×Ö½éÉÜÏà¹Ø½éÉÜ
     * sourceUrl : https://www.google.com
     * wallPapers : [{"id":18,"name":"´óÇòÇò","author":"sdfsfsf","downloadNumber":5442,"sourceLogoUrl":"http://192.168.0.52:5353/images/cai-icon.jpg","source":"¹Ù·½»Ø¸´","createTime":"2016-03-30 16:45:06","bigImage":{"name":"cai-wallpaper.jpg","size":186745,"downloadUrl":"http://192.168.0.52:5353/images/cai-wallpaper.jpg"},"smallImage":{"name":"cai-preview.jpg","size":47668,"downloadUrl":"http://192.168.0.52:5353/images/cai-preview.jpg"}}]
     * errorCode : 0
     * errorMessage :
     */

    private int total;
    private String sourceDescription;
    private String sourceUrl;
    private int errorCode;
    private String errorMessage;
    private List<WallPapersBean> wallPapers;

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public String getSourceDescription() {
        return sourceDescription;
    }

    public void setSourceDescription(String sourceDescription) {
        this.sourceDescription = sourceDescription;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<WallPapersBean> getWallPapers() {
        return wallPapers;
    }

    public void setWallPapers(List<WallPapersBean> wallPapers) {
        this.wallPapers = wallPapers;
    }

    @Override
    public String toString() {
        return "WallPaperJsonBean{" +
                "total=" + total +
                ", sourceDescription='" + sourceDescription + '\'' +
                ", sourceUrl='" + sourceUrl + '\'' +
                ", errorCode=" + errorCode +
                ", errorMessage='" + errorMessage + '\'' +
                ", wallPapers=" + wallPapers +
                '}';
    }
}
