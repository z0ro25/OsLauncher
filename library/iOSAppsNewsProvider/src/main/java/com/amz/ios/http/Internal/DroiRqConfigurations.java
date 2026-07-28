package com.amz.ios.http.Internal;

import java.util.concurrent.TimeUnit;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-17 下午5:31
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class DroiRqConfigurations {

    /**
     * time out limit
     */
    public static final long DEFAULT_TIME_OUT = TimeUnit.SECONDS.toMillis(5);

    /**
     * retry times
     */
    public static final int DEFAULT_HTTP_FAILLURE_RETRY_TIMES = 3;

}
