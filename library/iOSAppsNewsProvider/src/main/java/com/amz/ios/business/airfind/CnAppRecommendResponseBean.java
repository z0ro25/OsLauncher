package com.amz.ios.business.airfind;

import com.google.gson.annotations.Expose;

import java.util.List;

/**
 * Created by server on 17-11-10.
 */

public class CnAppRecommendResponseBean {

    /**
     * success : true
     * code : 0
     * message : success
     * data : {"explorations":[{"id":3,"downloadImageUrl":"http://192.168.0.52:5353/images/push/icon/2017/11/02/d2324971-b05f-4cf9-92c0-84dd598069a3.png","packageName":"233","downloadUrl":"456","applicationName":"456","isOnline":0,"createTime":1509614514000},{"id":4,"downloadImageUrl":"http://192.168.0.52:5353/images/push/icon/2017/11/06/6dac2373-b783-4234-aee3-c02a95eb1338.png","packageName":"com.jingdong.app.mall","downloadUrl":"http://yntvmbzh.droibaascdn.com/droi/yntvmbzhFy5HpSMkDvo15tmlTypM-r6ClQBaYdEA/925969109454426112/V5.6.0_oem-szhuoyou2.apk","applicationName":"京东","isOnline":0,"createTime":1509962448000},{"id":5,"downloadImageUrl":"http://192.168.0.52:5353/images/push/icon/2017/11/06/0e570437-e198-4054-bb35-0a14e1e23ea0.png","packageName":"天天快报","downloadUrl":"http://yntvmbzh.droibaascdn.com/droi/yntvmbzhFy5HpSMkDvo15tmlTypM-r6ClQBaYdEA/925632068242829312/ase_1100100104_171020035429a.apk","applicationName":"com.tencent.reading","isOnline":0,"createTime":1509962495000},{"id":6,"downloadImageUrl":"http://192.168.0.52:5353/images/push/icon/2017/11/06/ffa318cf-17b2-4ec4-92f3-ee2f77d8c9d3.png","packageName":"com.memezhibo.android","downloadUrl":"http://yntvmbzh.droibaascdn.com/droi/yntvmbzhFy5HpSMkDvo15tmlTypM-r6ClQBaYdEA/924184899211104256/memezhibo_android_zhuoyi03.apk","applicationName":"么么直播","isOnline":0,"createTime":1509962532000},{"id":7,"downloadImageUrl":"http://192.168.0.52:5353/images/push/icon/2017/11/06/184997d2-f1ee-4c5f-b033-316787f871e3.png","packageName":"com.tianqi2345","downloadUrl":"http://yntvmbzh.droibaascdn.com/droi/yntvmbzhFy5HpSMkDvo15tmlTypM-r6ClQBaYdEA/882499194642894848/G_CHANNEL_VALUE_5.2_47_20170505-update1_sc-tianyida6_ins_www.apk","applicationName":"2345天气王","isOnline":0,"createTime":1509962583000},{"id":8,"downloadImageUrl":"http://192.168.0.52:5353/images/push/icon/2017/11/06/95cba6f7-dab9-45e0-b72a-9c7a91addc51.png","packageName":"com.sina.weibo","downloadUrl":"http://yntvmbzh.droibaascdn.com/droi/yntvmbzhFy5HpSMkDvo15tmlTypM-r6ClQBaYdEA/895852526518788096/33bc45b05a920ed1132304475e03.apk","applicationName":"微博","isOnline":0,"createTime":1509962627000},{"id":9,"downloadImageUrl":"http://192.168.0.52:5353/images/push/icon/2017/11/06/e7447d98-d0a8-44d1-a521-f452f4d70956.png","packageName":"cn.jj","downloadUrl":"http://yntvmbzh.droibaascdn.com/droi/yntvmbzhFy5HpSMkDvo15tmlTypM-r6ClQBaYdEA/883164856524607488/jj.apk","applicationName":"JJ斗地主","isOnline":0,"createTime":1509962681000},{"id":10,"downloadImageUrl":"http://192.168.0.52:5353/images/push/icon/2017/11/06/1b410edc-4807-4f26-9dc0-6b09c4b7ce03.png","packageName":"com.browser2345","downloadUrl":"http://yntvmbzh.droibaascdn.com/droi/yntvmbzhFy5HpSMkDvo15tmlTypM-r6ClQBaYdEA/910701972908941312/ase-8.9.1_sc-tianyida_as_www.apk","applicationName":"2345浏览器","isOnline":0,"createTime":1509962729000},{"id":11,"downloadImageUrl":"http://192.168.0.52:5353/images/push/icon/2017/11/06/1d40c8a0-d293-461c-ae12-7a39c2b76bbb.png","packageName":"com.zyqpdt.zyqpdt.zy","downloadUrl":"http://yntvmbzh.droibaascdn.com/droi/yntvmbzhFy5HpSMkDvo15tmlTypM-r6ClQBaYdEA/913605993669914624/13.0.02810029_20170926_184328_29.apk","applicationName":"卓易棋牌大厅","isOnline":0,"createTime":1509962783000},{"id":12,"downloadImageUrl":"http://192.168.0.52:5353/images/push/icon/2017/11/06/9e9b610e-4efe-4c96-a88f-c012f5e54012.png","packageName":"com.idroi.weiprint","downloadUrl":"http://yntvmbzh.droibaascdn.com/droi/yntvmbzhFy5HpSMkDvo15tmlTypM-r6ClQBaYdEA/898463606260756480/-.apk","applicationName":"维印","isOnline":0,"createTime":1509962837000},{"id":13,"downloadImageUrl":"http://192.168.0.52:5353/images/push/icon/2017/11/06/35059998-df9c-4c7a-9c85-b340922616f0.png","packageName":"com.uma.qproom","downloadUrl":"http://www.droibaas.com/Droifile/getFile?id=59a76b96bf85361057b74934&appid=yntvmbzhFy5HpSMkDvo15tmlTypM-r6ClQBaYdEA","applicationName":"圈子牛","isOnline":0,"createTime":1509962886000}]}
     */
    @Expose
    private boolean success;
    @Expose
    private int code;
    @Expose
    private String message;
    @Expose
    private DataBean data;

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

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public static class DataBean {
        @Expose
        private List<ExplorationsBean> explorations;

        public List<ExplorationsBean> getExplorations() {
            return explorations;
        }

        public void setExplorations(List<ExplorationsBean> explorations) {
            this.explorations = explorations;
        }

        public static class ExplorationsBean {
            /**
             * id : 3
             * downloadImageUrl : http://192.168.0.52:5353/images/push/icon/2017/11/02/d2324971-b05f-4cf9-92c0-84dd598069a3.png
             * packageName : 233
             * downloadUrl : 456
             * applicationName : 456
             * isOnline : 0
             * createTime : 1509614514000
             */
            @Expose
            private int id;
            @Expose
            private String downloadImageUrl;
            @Expose
            private String packageName;
            @Expose
            private String downloadUrl;
            @Expose
            private String applicationName;
            @Expose
            private int isOnline;
            @Expose
            private long createTime;

            public int getId() {
                return id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public String getDownloadImageUrl() {
                return downloadImageUrl;
            }

            public void setDownloadImageUrl(String downloadImageUrl) {
                this.downloadImageUrl = downloadImageUrl;
            }

            public String getPackageName() {
                return packageName;
            }

            public void setPackageName(String packageName) {
                this.packageName = packageName;
            }

            public String getDownloadUrl() {
                return downloadUrl;
            }

            public void setDownloadUrl(String downloadUrl) {
                this.downloadUrl = downloadUrl;
            }

            public String getApplicationName() {
                return applicationName;
            }

            public void setApplicationName(String applicationName) {
                this.applicationName = applicationName;
            }

            public int getIsOnline() {
                return isOnline;
            }

            public void setIsOnline(int isOnline) {
                this.isOnline = isOnline;
            }

            public long getCreateTime() {
                return createTime;
            }

            public void setCreateTime(long createTime) {
                this.createTime = createTime;
            }
        }
    }
}
