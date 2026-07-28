package com.amz.ios.themeclub.model;

import com.android.volley.Response;
import com.amz.ios.themeclub.bean.request.LockScreenDetailRequest;
import com.amz.ios.themeclub.intertfaces.IModelData;
import com.amz.ios.themeclub.util.NetWorkUtils;

/**
 * Created by server on 17-8-21.
 */
public class LockScreenDetailModel implements IModelData<LockScreenDetailRequest> {
    private Response.Listener mSuccessfulListener;
    private Response.ErrorListener mErrorListener;

    @Override
    public void getDatasFromNet(LockScreenDetailRequest request) {
        NetWorkUtils.getInstance().getLockDataFromServer(request, mSuccessfulListener, mErrorListener);
    }

    @Override
    public void setCallBack(Response.Listener listener, Response.ErrorListener errorListener) {
        mSuccessfulListener = listener;
        mErrorListener = errorListener;
    }
}
