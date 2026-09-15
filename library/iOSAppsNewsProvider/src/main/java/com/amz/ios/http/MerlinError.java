package com.amz.ios.http;

/**
 * Author       : yizhihao
 * Create time  : 2016-12-09 下午5:17
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public enum MerlinError {
    ERROR_NO_NET("NetWork Error"),
    ERROR_NET_OUT("TimeOut"),
    OTHER;

    MerlinError() {
    }

    MerlinError(String err) {
        this.message = err;
    }

    public String message;
}
