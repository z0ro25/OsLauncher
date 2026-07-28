package com.amz.ios.ioslite.common.ad;

import android.text.TextUtils;

public class IOSAdError {
    private final int errorCode;
    private final String errorMessage;

    public IOSAdError(int code, String msg) {
        if (TextUtils.isEmpty(msg)) {
            msg = "unknown error";
        }

        this.errorCode = code;
        this.errorMessage = msg;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public static IOSAdError createAdError(int var0, String var1) {
        return new IOSAdError(var0, var1);
    }
}
