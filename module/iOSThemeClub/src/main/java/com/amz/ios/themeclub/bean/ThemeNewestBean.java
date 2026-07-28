package com.amz.ios.themeclub.bean;

import java.io.Serializable;
import java.util.List;

/**
 * Created by ZhangMingZhe on 11/19/16.
 */

public class ThemeNewestBean implements Serializable{
    /**
     * total : 7
     * sourceDescription : ÎÄ×Ö½éÉÜÏà¹Ø½éÉÜ
     * sourceUrl : https://www.google.com
     * themes : [{"id":22,"name":"·è¿ñ¶¯Îï³Ç","source":"°Ù¶ÈÍ¼Æ¬","sourceLogoUrl":"https://www.google.com/logo.png","author":"ziop","intro":"·è¿ñ¶¯Îï³Ç","downloadNumber":0,"googlePlayUrl":"https://www.google.com./images/theme/treview_thumb.apk","iconUrl":"http://192.168.0.52:5353/images/theme/theme_preview_thumb.jpg","createTime":"2016-03-31 10:01:04","preview":{"name":"theme_preview_thumb.jpg","size":22054,"downloadUrl":"http://192.168.0.52:5353/images/theme/theme_preview_thumb.jpg"},"screenshotList":[{"name":"theme_preview_lockscreen.jpg","size":22054,"downloadUrl":"http://192.168.0.52:5353/images/theme/theme_preview_lockscreen.jpg"},{"name":"theme_preview_launcher.jpg","size":22054,"downloadUrl":"http://192.168.0.52:5353/images/theme/theme_preview_launcher.jpg"},{"name":"theme_preview_icon.jpg","size":22054,"downloadUrl":"http://192.168.0.52:5353/images/theme_preview_icon.jpg"}],"fileMd5":"58e194f0d77b2b22f396324cc28add63","fileSize":11231974,"packageName":"com.ios.theme.Zootopia","fileName":"Zootopia.apk","downloadUrl":"http://192.168.0.52:5353/images/theme/2016/03/31/ml91KsGjTR/Zootopia.apk"}]
     * errorCode : 0
     * errorMessage :
     */

    private int total;
    private String sourceDescription;
    private String sourceUrl;
    private int errorCode;
    private String errorMessage;
    private List<ThemesBean> themes;



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

    public List<ThemesBean> getThemes() {
        return themes;
    }

    public void setThemes(List<ThemesBean> themes) {
        this.themes = themes;
    }

    @Override
    public String toString() {
        return "ThemeNewestBean{" +
                "total=" + total +
                ", sourceDescription='" + sourceDescription + '\'' +
                ", sourceUrl='" + sourceUrl + '\'' +
                ", errorCode=" + errorCode +
                ", errorMessage='" + errorMessage + '\'' +
                ", themes=" + themes +
                '}';
    }


}
