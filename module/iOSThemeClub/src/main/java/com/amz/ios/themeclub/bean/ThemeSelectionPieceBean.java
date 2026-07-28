package com.amz.ios.themeclub.bean;

import com.chad.library.adapter.base.entity.MultiItemEntity;

import java.io.Serializable;
import java.util.List;

/**
 * Created by ZhangMingZhe on 11/21/16.
 */

public class ThemeSelectionPieceBean {

    /**
     * total : 7
     * issues : [{"title":"¾°ÎïÖ®ÃÀ","description":"¾°ÎïµÄÃÀÓÐËêÔÂµÄº¬Ïã¡£","displayNum":3,"themes":[{"id":22,"name":"·è¿ñ¶¯Îï³Ç","source":"°Ù¶ÈÍ¼Æ¬","sourceLogoUrl":"https://www.google.com/logo.png","author":"ziop","intro":"·è¿ñ¶¯Îï³Ç","downloadNumber":0,"googlePlayUrl":"https://www.google.com./images/theme/treview_thumb.apk","iconUrl":"http://192.168.0.52:5353/images/theme/theme_preview_thumb.jpg","createTime":"2016-03-31 10:01:04","preview":{"name":"theme_preview_thumb.jpg","size":22054,"downloadUrl":"http://192.168.0.52:5353/images/theme/theme_preview_thumb.jpg"},"screenshotList":[{"name":"theme_preview_lockscreen.jpg","size":22054,"downloadUrl":"http://192.168.0.52:5353/images/theme/theme_preview_lockscreen.jpg"},{"name":"theme_preview_launcher.jpg","size":22054,"downloadUrl":"http://192.168.0.52:5353/images/theme/theme_preview_launcher.jpg"},{"name":"theme_preview_icon.jpg","size":22054,"downloadUrl":"http://192.168.0.52:5353/images/theme_preview_icon.jpg"}],"fileMd5":"58e194f0d77b2b22f396324cc28add63","fileSize":11231974,"packageName":"com.ios.theme.Zootopia","fileName":"Zootopia.apk","downloadUrl":"http://192.168.0.52:5353/images/theme/2016/03/31/ml91KsGjTR/Zootopia.apk"}]}]
     * errorCode : 0
     * errorMessage :
     */

    private int total;
    private int errorCode;
    private String errorMessage;
    private List<IssuesBean> issues;
    public static final int THEME_SELECTION_ITEM_X = 1;
    public static final int THEME_SELECTION_ITEM_Y = 2;
    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
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

    public List<IssuesBean> getIssues() {
        return issues;
    }

    public void setIssues(List<IssuesBean> issues) {
        this.issues = issues;
    }

    @Override
    public String toString() {
        return "ThemeSelectionPieceBean{" +
                "total=" + total +
                ", errorCode=" + errorCode +
                ", errorMessage='" + errorMessage + '\'' +
                ", issues=" + issues +
                '}';
    }

    public static class IssuesBean implements MultiItemEntity,Serializable {
        /**
         * title : ¾°ÎïÖ®ÃÀ
         * description : ¾°ÎïµÄÃÀÓÐËêÔÂµÄº¬Ïã¡£
         * displayNum : 3
         * themes : [{"id":22,"name":"·è¿ñ¶¯Îï³Ç","source":"°Ù¶ÈÍ¼Æ¬","sourceLogoUrl":"https://www.google.com/logo.png","author":"ziop","intro":"·è¿ñ¶¯Îï³Ç","downloadNumber":0,"googlePlayUrl":"https://www.google.com./images/theme/treview_thumb.apk","iconUrl":"http://192.168.0.52:5353/images/theme/theme_preview_thumb.jpg","createTime":"2016-03-31 10:01:04","preview":{"name":"theme_preview_thumb.jpg","size":22054,"downloadUrl":"http://192.168.0.52:5353/images/theme/theme_preview_thumb.jpg"},"screenshotList":[{"name":"theme_preview_lockscreen.jpg","size":22054,"downloadUrl":"http://192.168.0.52:5353/images/theme/theme_preview_lockscreen.jpg"},{"name":"theme_preview_launcher.jpg","size":22054,"downloadUrl":"http://192.168.0.52:5353/images/theme/theme_preview_launcher.jpg"},{"name":"theme_preview_icon.jpg","size":22054,"downloadUrl":"http://192.168.0.52:5353/images/theme_preview_icon.jpg"}],"fileMd5":"58e194f0d77b2b22f396324cc28add63","fileSize":11231974,"packageName":"com.ios.theme.Zootopia","fileName":"Zootopia.apk","downloadUrl":"http://192.168.0.52:5353/images/theme/2016/03/31/ml91KsGjTR/Zootopia.apk"}]
         */

        private String title;
        private String description;
        private int displayNum;
        private List<ThemesBean> themes;

        @Override
        public String toString() {
            return "IssuesBean{" +
                    "title='" + title + '\'' +
                    ", description='" + description + '\'' +
                    ", displayNum=" + displayNum +
                    ", themes=" + themes +
                    '}';
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public int getDisplayNum() {
            return displayNum;
        }

        public void setDisplayNum(int displayNum) {
            this.displayNum = displayNum;
        }

        public List<ThemesBean> getThemes() {
            return themes;
        }

        public void setThemes(List<ThemesBean> themes) {
            this.themes = themes;
        }

        @Override
        public int getItemType() {
            if (displayNum == 3) {
                return THEME_SELECTION_ITEM_Y;
            } else {
                return THEME_SELECTION_ITEM_X;
            }
        }

    }
}
