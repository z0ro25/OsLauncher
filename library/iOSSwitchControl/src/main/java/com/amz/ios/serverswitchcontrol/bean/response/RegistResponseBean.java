package com.amz.ios.serverswitchcontrol.bean.response;

/**
 * Created by server on 17-11-14.
 */

public class RegistResponseBean {

    /**
     * success : true
     * code    : 0
     * message : 成功
     */

    private boolean success;
    private int code;
    private String message;

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
}
