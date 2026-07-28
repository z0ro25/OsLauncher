package com.amz.ios.themeclub.intertfaces;

import com.android.volley.Response;

/**
 * Created by ZhangMingZhe on 11/21/16.
 */

public interface IModelData<T> {

    void getDatasFromNet(T request);

    void setCallBack(Response.Listener listener, Response.ErrorListener errorListener);

}

