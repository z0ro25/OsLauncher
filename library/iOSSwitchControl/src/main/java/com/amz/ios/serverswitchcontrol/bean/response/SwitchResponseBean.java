package com.amz.ios.serverswitchcontrol.bean.response;

import java.util.List;

/**
 * Created by server on 17-11-14.
 */

public class SwitchResponseBean {

    /**
     * success : true
     * code : 0
     * message : 成功
     * version : 0
     * data : [{"key":"folder_discovery_show","value":null,"flag":1},{"key":"newspage_switch_show","value":null,"flag":1},{"key":"not_uninstall_app_list","value":"com.baidu.searchbox,com.tianqi2345,cn.kuwo.player","flag":1}]
     */

    private boolean success;
    private int code;
    private String message;
    private int version;
    private List<ServerResponseBean> data;

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

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<ServerResponseBean> getData() {
        return data;
    }

    public void setData(List<ServerResponseBean> data) {
        this.data = data;
    }

    public static class ServerResponseBean {
        /**
         * key : folder_discovery_show
         * value : null
         * flag : 1
         */

        private String key;
        private String value;
        private int flag;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public int getFlag() {
            return flag;
        }

        public void setFlag(int flag) {
            this.flag = flag;
        }

        @Override
        public String toString() {
            return "ServerResponseBean{" +
                    "key='" + key + '\'' +
                    ", value='" + value + '\'' +
                    ", flag=" + flag +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "SwitchResponseBean{" +
                "success=" + success +
                ", code=" + code +
                ", message='" + message + '\'' +
                ", version=" + version +
                ", data=" + data +
                '}';
    }
}
