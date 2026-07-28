package com.amz.ios.themeclub.bean;

import com.chad.library.adapter.base.entity.MultiItemEntity;

import java.io.Serializable;
import java.util.List;

/**
 * Created by ZhangMingZhe on 11/17/16.
 */

public class WallPaperSelectionPieceBean implements Serializable{

    /**
     * total : 7
     * issues : [{"title":"¾°ÎïÖ®ÃÀ","description":"¾°ÎïµÄÃÀÓÐËêÔÂµÄº¬Ïã¡£","displayNum":"3","wallPapers":[{"id":18,"name":"´óÇòÇò","author":"sdfsfsf","downloadNumber":5442,"source":"¹Ù·½»Ø¸´","sourceLogoUrl":"http://192.168.0.52:5353/images/cai-icon.jpg","createTime":"2016-03-30 16:45:06","bigImage":{"name":"cai-wallpaper.jpg","size":186745,"downloadUrl":"http://192.168.0.52:5353/images/cai-wallpaper.jpg"},"smallImage":{"name":"cai-preview.jpg","size":47668,"downloadUrl":"http://192.168.0.52:5353/images/cai-preview.jpg"}}]}]
     * errorCode : 0
     * errorMessage :
     */

    private int total;
    private int errorCode;
    private String errorMessage;
    private List<IssuesBean> issues;
    public static  final int SELECTION_TYPE_X = 1;
    public static  final int SELECTION_TYPE_Y = 2;
    public static  final int SELECTION_TYPE_Z = 3;


    @Override
    public String toString() {
        return "WallPaperSelectionPieceBean{" +
                "total=" + total +
                ", errorCode=" + errorCode +
                ", errorMessage='" + errorMessage + '\'' +
                ", issues=" + issues +
                '}';
    }

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

    public static class IssuesBean implements MultiItemEntity,Serializable{
        /**
         * title : ¾°ÎïÖ®ÃÀ
         * description : ¾°ÎïµÄÃÀÓÐËêÔÂµÄº¬Ïã¡£
         * displayNum : 3
         * wallPapers : [{"id":18,"name":"´óÇòÇò","author":"sdfsfsf","downloadNumber":5442,"source":"¹Ù·½»Ø¸´","sourceLogoUrl":"http://192.168.0.52:5353/images/cai-icon.jpg","createTime":"2016-03-30 16:45:06","bigImage":{"name":"cai-wallpaper.jpg","size":186745,"downloadUrl":"http://192.168.0.52:5353/images/cai-wallpaper.jpg"},"smallImage":{"name":"cai-preview.jpg","size":47668,"downloadUrl":"http://192.168.0.52:5353/images/cai-preview.jpg"}}]
         */

        private String title;
        private String description;
        private int displayNum;
        private List<WallPapersBean> wallPapers;

        @Override
        public String toString() {
            return "IssuesBean{" +
                    "title='" + title + '\'' +
                    ", description='" + description + '\'' +
                    ", displayNum=" + displayNum +
                    ", wallPapers=" + wallPapers +
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

        public List<WallPapersBean> getWallPapers() {
            return wallPapers;
        }

        public void setWallPapers(List<WallPapersBean> wallPapers) {
            this.wallPapers = wallPapers;
        }

        //用于识别是那个Item
        @Override
        public int getItemType() {
            int type = getDisplayNum();
            if(type == 3){
                return SELECTION_TYPE_X;
            }else if(type == 6){
                return SELECTION_TYPE_Y;
            }else if(type == 9){
                return SELECTION_TYPE_Z;
            }
            return 0;
        }



    }
}
