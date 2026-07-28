package com.amz.ios.themeclub.model;

import com.android.volley.Response;
import com.amz.ios.themeclub.bean.request.ThemeSelectionRequest;
import com.amz.ios.themeclub.intertfaces.IModelData;
import com.amz.ios.themeclub.util.NetWorkUtils;

/**
 * Created by ZhangMingZhe on 11/22/16.
 */

public class ThemeSelectionModel implements IModelData<ThemeSelectionRequest> {
    Response.Listener listener;
    Response.ErrorListener errorListener;

    @Override
    public void getDatasFromNet(ThemeSelectionRequest request) {
        NetWorkUtils.getInstance().getDataFromServer(request,listener,errorListener);
    }

    @Override
    public void setCallBack(Response.Listener listener, Response.ErrorListener errorListener) {
        this.listener = listener;
        this.errorListener = errorListener;
    }


}
