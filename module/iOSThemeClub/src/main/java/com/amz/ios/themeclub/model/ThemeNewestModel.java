package com.amz.ios.themeclub.model;

import com.android.volley.Response;
import com.amz.ios.themeclub.bean.request.ThemeNewestRequest;
import com.amz.ios.themeclub.intertfaces.IModelData;
import com.amz.ios.themeclub.util.NetWorkUtils;

/**
 * Created by ZhangMingZhe on 11/21/16.
 */

public class ThemeNewestModel implements IModelData<ThemeNewestRequest> {


    Response.Listener listener;
    Response.ErrorListener errorListener;

    @Override
    public void getDatasFromNet(ThemeNewestRequest request) {
        NetWorkUtils.getInstance().getDataFromServer(request,listener,errorListener);
    }

    @Override
    public void setCallBack(Response.Listener listener, Response.ErrorListener errorListener) {
        this.listener = listener;
        this.errorListener = errorListener;
    }
}
