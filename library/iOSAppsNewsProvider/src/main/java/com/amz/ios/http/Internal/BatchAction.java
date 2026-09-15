package com.amz.ios.http.Internal;

import java.util.List;

/**
 * Author       : yizhihao
 * Create time  : 2016-12-12 下午6:20
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class BatchAction extends Action {

    private List<Action> mBatch;

    public BatchAction(BaseProvider handler, String name) {
        super(handler, name);
    }

    public BatchAction(BaseProvider handler) {
        super(handler);
    }

    @Override
    protected void work(CancelableCallBack callBack) {

    }

    @Override
    public int compareTo(Object another) {
        return 0;
    }
}
