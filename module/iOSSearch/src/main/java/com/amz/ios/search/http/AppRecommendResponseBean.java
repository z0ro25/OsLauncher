package com.amz.ios.search.http;

import java.util.List;

/**
 * Created by liaozhongjun on 2017/1/16.
 */

public class AppRecommendResponseBean {

    public List<AppsBean> getData() {
        return data;
    }

    public void setData(List<AppsBean> data) {
        this.data = data;
    }

    /**
     * data : {"apps":[{"appId":"com.murka.infinityslots","title":"Infinity Slots - Spin and Win!","rating":4.6,"url":"http://api.airfind.com/stats/appclick/v1?appndex=1","icon":"http://d2ym6yrl197shi.cloudfront.ns_190.png"}]}
     * code : 200
     * message : 成功
     */

    private List<AppsBean> data;
    private int code;
    private String message;
    private boolean success;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }


    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static class AppsBean {
        /**
         * appId : com.murka.infinityslots
         * title : Infinity Slots - Spin and Win!
         * rating : 4.6
         * url : http://api.airfind.com/stats/appclick/v1?appndex=1
         * icon : http://d2ym6yrl197shi.cloudfront.ns_190.png
         */

        private String appid;
        private String title;
        private String url;
        private String icon;
        private int id;
        private int action;
        private float rating;

        public float getRating() {
            return rating;
        }

        public void setRating(float rating) {
            this.rating = rating;
        }

        public String getAppid() {
            return appid;
        }

        public void setAppid(String appid) {
            this.appid = appid;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getAction() {
            return action;
        }

        public void setAction(int action) {
            this.action = action;
        }


        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }


        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }
    }

}
