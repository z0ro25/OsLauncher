package com.amz.ios.http;

/**
 * Author       : yizhihao
 * Create time  : 2016-12-09 下午5:38
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public interface RecycledCallBack {

    /**
     * at end of task, recycle it.
     */
    public void recyle();

    /**
     * provider a instance by this way;
     *
     * @return
     */
    public RecycledCallBack obtain();

}
