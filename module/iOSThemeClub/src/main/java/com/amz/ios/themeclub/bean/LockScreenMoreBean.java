package com.amz.ios.themeclub.bean;

import java.io.Serializable;
import java.util.List;

/**
 * Created by server on 17-8-21.
 */
public class LockScreenMoreBean implements Serializable{

    /**
     * screens : [{"id":102,"name":"文字","author":"匿名","intro":"文字","isPay":1,"downloadNumber":219,"payPrice":1,"discountPrice":0.5,"createTime":"2016-09-29 12:05:27","preview":{"name":"theme_preview_lockscreen_thumb.jpg","size":51671,"downloadUrl":"http://mhzxres.yy845.com/images/screen/2016/09/29/zPthcKxgTt/theme_preview_lockscreen_thumb.jpg"},"screenshotList":[{"name":"theme_preview_lockscreen.jpg","size":51671,"downloadUrl":"http://mhzxres.yy845.com/images/screen/2016/09/29/MgRfUHaoTw/theme_preview_lockscreen.jpg"}],"versionCode":1,"versionName":"1.0","fileMd5":"afdb310602fdc114f226afdf2f83856c","fileSize":805192,"packageName":"com.ios.lockscreen.sheet","fileName":"IOSLockscreenSheet.apk","downloadUrl":"http://mhzxres.yy845.com/images/screen/2016/09/29/5NMQlzlwzQ/IOSLockscreenSheet.apk"},{"id":81,"name":"五色彩贝","author":"匿名","intro":"五色彩贝","isPay":0,"downloadNumber":2390,"payPrice":0,"discountPrice":0,"createTime":"2016-09-29 11:34:29","preview":{"name":"theme_preview_lockscreen_thumb.jpg","size":24700,"downloadUrl":"http://mhzxres.yy845.com/images/screen/2016/09/29/Y7VPXGPD9F/theme_preview_lockscreen_thumb.jpg"},"screenshotList":[{"name":"theme_preview_lockscreen.jpg","size":109934,"downloadUrl":"http://mhzxres.yy845.com/images/screen/2016/09/29/YVsB8UCE1X/theme_preview_lockscreen.jpg"}],"versionCode":1,"versionName":"1.0","fileMd5":"40d43cfba5165e8bf281e678e1c53bd1","fileSize":1115752,"packageName":"com.ios.lockscreen.conch","fileName":"IOSLockscreenConch.apk","downloadUrl":"http://mhzxres.yy845.com/images/screen/2016/09/29/yP6ozAT5b9/IOSLockscreenConch.apk"},{"id":84,"name":"充电气泡","author":"匿名","intro":"充电气泡","isPay":1,"downloadNumber":93,"payPrice":1,"discountPrice":0,"createTime":"2016-09-29 11:37:20","preview":{"name":"theme_preview_lockscreen_thumb.jpg","size":7386,"downloadUrl":"http://mhzxres.yy845.com/images/screen/2016/09/29/lotcHEPuqb/theme_preview_lockscreen_thumb.jpg"},"screenshotList":[{"name":"theme_preview_lockscreen.jpg","size":105227,"downloadUrl":"http://mhzxres.yy845.com/images/screen/2016/09/29/9IGdXIp9aR/theme_preview_lockscreen.jpg"}],"versionCode":1,"versionName":"1.0","fileMd5":"1787bb881d5814104628fc4b8de2fd13","fileSize":1074176,"packageName":"com.ios.lockscreen.expand","fileName":"IOSLockscreenExpand.apk","downloadUrl":"http://mhzxres.yy845.com/images/screen/2016/09/29/O6oqldM5JV/IOSLockscreenExpand.apk"}]
     * errorCode : 0
     */

    private int errorCode;
    private List<LockScreenBean> screens;

    @Override
    public String toString() {
        return "LockScreenMoreBean{" +
                "errorCode=" + errorCode +
                ", screens=" + screens +
                '}';
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public List<LockScreenBean> getScreens() {
        return screens;
    }

    public void setScreens(List<LockScreenBean> screens) {
        this.screens = screens;
    }
}
