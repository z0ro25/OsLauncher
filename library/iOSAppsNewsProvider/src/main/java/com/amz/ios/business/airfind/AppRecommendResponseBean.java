package com.amz.ios.business.airfind;

import android.view.View;

import com.google.gson.annotations.Expose;

import java.util.List;

/**
 * Created by liaozhongjun on 2017/1/16.
 */

public class AppRecommendResponseBean {

    @Override
    public String toString() {
        return "AppRecommendResponseBean{" +
                "data=" + data +
                ", code=" + code +
                ", message='" + message + '\'' +
                ", success=" + success +
                '}';
    }

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
    @Expose
    private List<AppsBean> data;
    @Expose
    private int code;
    @Expose
    private String message;
    @Expose
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

        @Override
        public String toString() {
            return "AppsBean{" +
                    "appid='" + appid + '\'' +
                    ", title='" + title + '\'' +
                    ", url='" + url + '\'' +
                    ", icon='" + icon + '\'' +
                    ", id=" + id +
                    ", action=" + action +
                    ", rating=" + rating +
                    '}';
        }

        /**
         * appId : com.murka.infinityslots
         * title : Infinity Slots - Spin and Win!
         * rating : 4.6
         * url : http://api.airfind.com/stats/appclick/v1?appndex=1
         * icon : http://d2ym6yrl197shi.cloudfront.ns_190.png
         */
        @Expose
        private String appid;
        @Expose
        private String title;
        @Expose
        private String url;
        @Expose
        private String icon;
        @Expose
        private int id;
        @Expose
        private int action;
        @Expose
        private float rating;
        private View view;

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

        public void setView(View view) {
            this.view = view;
        }

        public View getView() {
            return view;
        }
    }

}
