package com.amz.ios.themeclub.bean;

import java.io.Serializable;

/**
 * Created by ZhangMingZhe on 11/22/16.
 */

public class WallPapersBean implements Serializable{

    private int id;
    private String name;
    private String author;
    private int downloadNumber;
    private String source;
    private String sourceLogoUrl;
    private String sourceHintUrl;
    private String createTime;
    private BigImageBean bigImage;
    private SmallImageBean smallImage;


    @Override
    public String toString() {
        return "WallPapersBean{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", author='" + author + '\'' +
                ", downloadNumber=" + downloadNumber +
                ", source='" + source + '\'' +
                ", sourceLogoUrl='" + sourceLogoUrl + '\'' +
                ", createTime='" + createTime + '\'' +
                ", bigImage=" + bigImage +
                ", smallImage=" + smallImage +
                '}';
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getDownloadNumber() {
        return downloadNumber;
    }

    public void setDownloadNumber(int downloadNumber) {
        this.downloadNumber = downloadNumber;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceHintUrl() {
        return sourceHintUrl;
    }

    public void setSourceHintUrl(String sourceHintUrl) {
        this.sourceHintUrl = sourceHintUrl;
    }

    public String getSourceLogoUrl() {
        return sourceLogoUrl;
    }

    public void setSourceLogoUrl(String sourceLogoUrl) {
        this.sourceLogoUrl = sourceLogoUrl;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public BigImageBean getBigImage() {
        return bigImage;
    }

    public void setBigImage(BigImageBean bigImage) {
        this.bigImage = bigImage;
    }

    public SmallImageBean getSmallImage() {
        return smallImage;
    }

    public void setSmallImage(SmallImageBean smallImage) {
        this.smallImage = smallImage;
    }

    public static class BigImageBean implements Serializable {
        /**
         * name : cai-wallpaper.jpg
         * size : 186745
         * downloadUrl : http://192.168.0.52:5353/images/cai-wallpaper.jpg
         */

        private String name;
        private int size;
        private String downloadUrl;


        @Override
        public String toString() {
            return "BigImageBean{" +
                    "name='" + name + '\'' +
                    ", size=" + size +
                    ", downloadUrl='" + downloadUrl + '\'' +
                    '}';
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public void setDownloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
        }
    }

    public static class SmallImageBean implements Serializable{
        /**
         * name : cai-preview.jpg
         * size : 47668
         * downloadUrl : http://192.168.0.52:5353/images/cai-preview.jpg
         */

        private String name;
        private int size;
        private String downloadUrl;


        @Override
        public String toString() {
            return "SmallImageBean{" +
                    "name='" + name + '\'' +
                    ", size=" + size +
                    ", downloadUrl='" + downloadUrl + '\'' +
                    '}';
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public void setDownloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
        }
    }

}
