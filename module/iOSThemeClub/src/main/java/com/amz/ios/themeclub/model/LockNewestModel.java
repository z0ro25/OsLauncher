package com.amz.ios.themeclub.model;

import com.android.volley.Response;
import com.amz.ios.themeclub.bean.request.LockScreenNewestRequest;
import com.amz.ios.themeclub.intertfaces.IModelData;
import com.amz.ios.themeclub.util.NetWorkUtils;

/**
 * Created by ubuntu on 15/06/17.
 */

public class LockNewestModel implements IModelData<LockScreenNewestRequest> {

    Response.Listener listener;
    Response.ErrorListener errorListener;

    @Override
    public void getDatasFromNet(LockScreenNewestRequest request) {
        NetWorkUtils.getInstance().getLockDataFromServer(request,listener,errorListener);
    }

    @Override
    public void setCallBack(Response.Listener listener, Response.ErrorListener errorListener) {
        this.listener = listener;
        this.errorListener = errorListener;
    }
}
