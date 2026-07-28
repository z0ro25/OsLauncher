package com.amz.ios.search.http;

import java.util.List;

/**
 * Created by liaozhongjun on 2017/2/8.
 */

public class HotwordResponseBean {


    /**
     * success : true
     * code : 200
     * message : 成功
     * data : {"engine":{"id":1,"engine":"www.baidu.com"},"words":[{"id":1,"title":"测试","url":"www","action":1}]}
     */

    private boolean success;
    private int code;
    private String message;
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
        /**
         * engine : {"id":1,"engine":"www.baidu.com"}
         * words : [{"id":1,"title":"测试","url":"www","action":1}]
         */

        private EngineBean engine;
        private List<WordsBean> words;

        public EngineBean getEngine() {
            return engine;
        }

        public void setEngine(EngineBean engine) {
            this.engine = engine;
        }

        public List<WordsBean> getWords() {
            return words;
        }

        public void setWords(List<WordsBean> words) {
            this.words = words;
        }

        public static class EngineBean {
            /**
             * id : 1
             * engine : www.baidu.com
             */

            private int id;
            private String engine;

            public int getId() {
                return id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public String getEngine() {
                return engine;
            }

            public void setEngine(String engine) {
                this.engine = engine;
            }
        }

        public static class WordsBean {
            /**
             * id : 1
             * title : 测试
             * url : www
             * action : 1
             */

            private int id;
            private String title;
            private String url;
            private int action;
            private String apkId;
            private String packageName;

            public String getPackageName() {
                return packageName;
            }

            public void setPackageName(String packageName) {
                this.packageName = packageName;
            }

            public String getApkId() {
                return apkId;
            }

            public void setApkId(String apkId) {
                this.apkId = apkId;
            }

            public int getId() {
                return id;
            }

            public void setId(int id) {
                this.id = id;
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

            public int getAction() {
                return action;
            }

            public void setAction(int action) {
                this.action = action;
            }
        }
    }
}
