package com.amz.ios.themeclub.bean;

import com.chad.library.adapter.base.entity.MultiItemEntity;

import java.io.Serializable;
import java.util.List;

/**
 * Created by ubuntu on 16/06/17.
 */

public class LockScreenBean implements Serializable,MultiItemEntity {

    public boolean isDownLoad() {
        return isDownLoad;
    }

    public void setDownLoad(boolean downLoad) {
        isDownLoad = downLoad;
    }

    /**
     * id : 167
     * name : 吸血鬼
     * author : 刷
     * intro : 吸血鬼
     * downloadNumber : 0
     * isPay : 0
     * payPrice : 0
     * discountPrice : 0
     * createTime : 2016-07-01 17:15:56
     * preview : {"name":"theme_preview_thumb.jpg","size":4313,"downloadUrl":"http://mhzxres.yy845.com/images/theme/2016/07/01/fGqMd6j9dY/theme_preview_thumb.jpg"}
     * screenshotList : [{"name":"theme_preview_launcher.jpg","size":43766,"downloadUrl":"http://mhzxres.yy845.com/images/theme/2016/07/01/nKOzJgMUgU/theme_preview_launcher.jpg"},{"name":"theme_preview_icon.jpg","size":34854,"downloadUrl":"http://mhzxres.yy845.com/images/theme/2016/07/01/j9PCLAFabX/theme_preview_icon.jpg"}]
     * versionCode : 1
     * versionName : 1.0
     * fileMd5 : 3688ba1caae695130bb3796b6514c95d
     * fileSize : 3487968
     * packageName : com.ios.theme.theme_vampire
     * fileName : theme_vampire.apk
     * downloadUrl : http://mhzxres.yy845.com/images/theme/2016/07/01/fMnArr5WLC/theme_vampire.apk
     */

    private int id;
    private String name;
    private String author;
    private String intro;
    private int downloadNumber;
    private int isPay;
    private float payPrice;
    private float discountPrice;
    private String createTime;
    private boolean isDownLoad;
    /**
     * name : theme_preview_thumb.jpg
     * size : 4313
     * downloadUrl : http://mhzxres.yy845.com/images/theme/2016/07/01/fGqMd6j9dY/theme_preview_thumb.jpg
     */

    private LockScreenBean.PreviewBean preview;
    private int versionCode;
    private String versionName;
    private String fileMd5;
    private int fileSize;
    private String packageName;
    private String fileName;
    private String downloadUrl;

    public final static int TYPE_LOCK_SCREEN = 0;
    public final static int TYPE_LOCK_SCREEN_ONE = 1;
    public final static int TYPE_LOCK_SCREEN_TWO = 2;
    public final static int TYPE_LOCK_SCREEN_THREE = 3;
    /**
     * name : theme_preview_launcher.jpg
     * size : 43766
     * downloadUrl : http://mhzxres.yy845.com/images/theme/2016/07/01/nKOzJgMUgU/theme_preview_launcher.jpg
     */

    private List<ScreenshotListBean> screenshotList;

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

    public int getIsPay() {
        return isPay;
    }

    public void setIsPay(int isPay) {
        this.isPay = isPay;
    }

    public float getPayPrice() {
        return payPrice;
    }

    public void setPayPrice(float payPrice) {
        this.payPrice = payPrice;
    }

    public float getDiscountPrice() {
        return discountPrice;
    }

    public void setDiscountPrice(int discountPrice) {
        this.discountPrice = discountPrice;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public LockScreenBean.PreviewBean getPreview() {
        return preview;
    }

    public void setPreview(LockScreenBean.PreviewBean preview) {
        this.preview = preview;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getFileMd5() {
        return fileMd5;
    }

    public void setFileMd5(String fileMd5) {
        this.fileMd5 = fileMd5;
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
        return 0;
    }


    public static class PreviewBean implements Serializable {
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
    }

    @Override
    public String toString() {
        return "ThemeJsonInfo{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", author='" + author + '\'' +
                ", intro='" + intro + '\'' +
                ", downloadNumber=" + downloadNumber +
                ", isPay=" + isPay +
                ", payPrice=" + payPrice +
                ", discountPrice=" + discountPrice +
                ", createTime='" + createTime + '\'' +
                ", preview=" + preview +
                ", versionCode=" + versionCode +
                ", versionName='" + versionName + '\'' +
                ", fileMd5='" + fileMd5 + '\'' +
                ", fileSize=" + fileSize +
                ", packageName='" + packageName + '\'' +
                ", fileName='" + fileName + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", screenshotList=" + screenshotList +
                '}';
    }

    public static class ScreenshotListBean implements Serializable{
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
    }
}
