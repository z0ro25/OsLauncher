package com.amz.ios.themeclub.bean;

import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.amz.ios.ioslite.common.ad.IOSNAdResponse;

import java.io.Serializable;
import java.util.List;

/**
 * Created by lideqian on 16-11-24.
 */
public class ThemesBean implements Serializable,MultiItemEntity{
    /**
     * id : 22
     * name : ·è¿ñ¶¯Îï³Ç
     * source : °Ù¶ÈÍ¼Æ¬
     * sourceLogoUrl : https://www.google.com/logo.png
     * author : ziop
     * intro : ·è¿ñ¶¯Îï³Ç
     * downloadNumber : 0
     * googlePlayUrl : https://www.google.com./images/theme/treview_thumb.apk
     * iconUrl : http://192.168.0.52:5353/images/theme/theme_preview_thumb.jpg
     * createTime : 2016-03-31 10:01:04
     * preview : {"name":"theme_preview_thumb.jpg","size":22054,"downloadUrl":"http://192.168.0.52:5353/images/theme/theme_preview_thumb.jpg"}
     * screenshotList : [{"name":"theme_preview_lockscreen.jpg","size":22054,"downloadUrl":"http://192.168.0.52:5353/images/theme/theme_preview_lockscreen.jpg"},{"name":"theme_preview_launcher.jpg","size":22054,"downloadUrl":"http://192.168.0.52:5353/images/theme/theme_preview_launcher.jpg"},{"name":"theme_preview_icon.jpg","size":22054,"downloadUrl":"http://192.168.0.52:5353/images/theme_preview_icon.jpg"}]
     * fileMD5 : 58e194f0d77b2b22f396324cc28add63
     * fileSize : 11231974
     * packageName : com.ios.theme.Zootopia
     * fileName : Zootopia.apk
     * downloadUrl : http://192.168.0.52:5353/images/theme/2016/03/31/ml91KsGjTR/Zootopia.apk
     */

    private int id;
    private String name;
    private String source;
    private String sourceLogoUrl;
    private String author;
    private String intro;
    private int downloadNumber;
    private String googlePlayUrl;
    private String iconUrl;
    private String createTime;
    private PreviewBean preview;
    private String fileMD5;
    private int fileSize;
    private String packageName;
    private String fileName;
    private String downloadUrl;
    private List<ScreenshotListBean> screenshotList;

    private IOSNAdResponse mAd;
    private int position = -1;
    public static final int TYPE_X =1;
    public static final int TYPE_AD =2;
    public int fileType;

    public int getFileType() {
        return fileType;
    }

    public void setFileType(int fileType) {
        this.fileType = fileType;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public IOSNAdResponse getmAd() {
        return mAd;
    }

    public void setmAd(IOSNAdResponse mAd) {
        this.mAd = mAd;
    }

    @Override
    public String toString() {
        return "ThemesBean{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", source='" + source + '\'' +
                ", sourceLogoUrl='" + sourceLogoUrl + '\'' +
                ", author='" + author + '\'' +
                ", intro='" + intro + '\'' +
                ", downloadNumber=" + downloadNumber +
                ", googlePlayUrl='" + googlePlayUrl + '\'' +
                ", iconUrl='" + iconUrl + '\'' +
                ", createTime='" + createTime + '\'' +
                ", preview=" + preview +
                ", fileMD5='" + fileMD5 + '\'' +
                ", fileSize=" + fileSize +
                ", packageName='" + packageName + '\'' +
                ", fileName='" + fileName + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", screenshotList=" + screenshotList +
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceLogoUrl() {
        return sourceLogoUrl;
    }

    public void setSourceLogoUrl(String sourceLogoUrl) {
        this.sourceLogoUrl = sourceLogoUrl;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }

    public int getDownloadNumber() {
        return downloadNumber;
    }

    public void setDownloadNumber(int downloadNumber) {
        this.downloadNumber = downloadNumber;
    }

    public String getGooglePlayUrl() {
        return googlePlayUrl;
    }

    public void setGooglePlayUrl(String googlePlayUrl) {
        this.googlePlayUrl = googlePlayUrl;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public PreviewBean getPreview() {
        return preview;
    }

    public void setPreview(PreviewBean preview) {
        this.preview = preview;
    }

    public String getFileMD5() {
        return fileMD5;
    }

    public void setFileMD5(String fileMD5) {
        this.fileMD5 = fileMD5;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public List<ScreenshotListBean> getScreenshotList() {
        return screenshotList;
    }

    public void setScreenshotList(List<ScreenshotListBean> screenshotList) {
        this.screenshotList = screenshotList;
    }

    @Override
    public int getItemType() {
        return position==-1?TYPE_X:TYPE_AD;
    }

    public static class PreviewBean implements Serializable {
        /**
         * name : theme_preview_thumb.jpg
         * size : 22054
         * downloadUrl : http://192.168.0.52:5353/images/theme/theme_preview_thumb.jpg
         */

        private String name;
        private int size;
        private String downloadUrl;

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

        @Override
        public String toString() {
            return "PreviewBean{" +
                    "name='" + name + '\'' +
                    ", size=" + size +
                    ", downloadUrl='" + downloadUrl + '\'' +
                    '}';
        }
    }

//    private NativeAd nativeAd;
//
//    public NativeAd getNativeAd() {
//        return nativeAd;
//    }
//
//    public void setNativeAd(NativeAd nativeAd) {
//        this.nativeAd = nativeAd;
//    }

    public static class ScreenshotListBean implements Serializable {
        /**
         * name : theme_preview_lockscreen.jpg
         * size : 22054
         * downloadUrl : http://192.168.0.52:5353/images/theme/theme_preview_lockscreen.jpg
         */

        private String name;
        private int size;
        private String downloadUrl;

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

        @Override
        public String toString() {
            return "ScreenshotListBean{" +
                    "name='" + name + '\'' +
                    ", size=" + size +
                    ", downloadUrl='" + downloadUrl + '\'' +
                    '}';
        }
    }
}
