package com.amz.ios.themeclub.model;

import com.android.volley.Response;
import com.amz.ios.themeclub.bean.request.WallPaperNewestRequest;
import com.amz.ios.themeclub.intertfaces.IModelData;
import com.amz.ios.themeclub.util.NetWorkUtils;

/**
 * Created by ZhangMingZhe on 11/18/16.
 */

public class WallPaperNewestModel implements IModelData<WallPaperNewestRequest> {
    Response.Listener listener;
    Response.ErrorListener errorListener;

    @Override
    public void getDatasFromNet(WallPaperNewestRequest request) {
        NetWorkUtils.getInstance().getDataFromServer(request,listener,errorListener);
    }

    @Override
    public void setCallBack(Response.Listener listener, Response.ErrorListener errorListener) {
        this.listener = listener;
        this.errorListener = errorListener;
    }

}
