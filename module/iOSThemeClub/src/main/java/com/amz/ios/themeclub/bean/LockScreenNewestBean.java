package com.amz.ios.themeclub.bean;

import com.chad.library.adapter.base.entity.MultiItemEntity;

import java.io.Serializable;
import java.util.List;

/**
 * Created by ubuntu on 15/06/17.
 */

public class LockScreenNewestBean implements Serializable ,MultiItemEntity{
    private int total;
    private int errorCode;
    private String errorMessage;
    private List<LockScreenBean> screens;

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

    public List<LockScreenBean> getScreens() {
        return screens;
    }

    public void setScreens(List<LockScreenBean> screens) {
        this.screens = screens;
    }

    @Override
    public int getItemType() {
        return 0;
    }
}
