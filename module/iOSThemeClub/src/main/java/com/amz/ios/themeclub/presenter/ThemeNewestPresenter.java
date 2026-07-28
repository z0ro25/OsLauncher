package com.amz.ios.themeclub.presenter;

import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.themeclub.bean.ThemeNewestBean;
import com.amz.ios.themeclub.bean.request.ThemeNewestRequest;
import com.amz.ios.themeclub.intertfaces.IModelData;
import com.amz.ios.themeclub.intertfaces.IViewShowDatas;
import com.amz.ios.themeclub.intertfaces.IPresenterData;
import com.amz.ios.themeclub.model.ThemeNewestModel;
import com.amz.ios.themeclub.util.AppUtils;
import com.amz.ios.themeclub.util.CacheUtil;

import org.json.JSONException;

/**
 * Created by ZhangMingZhe on 11/21/16.
 */

public class ThemeNewestPresenter implements IPresenterData<ThemeNewestRequest> {
    private final String TAG = ThemeNewestPresenter.class.getSimpleName();
    private IViewShowDatas mView;
    private IModelData mModel;
    private int mStartNum;
    private final String CACHE_NAME = TAG+".cfg";
    public ThemeNewestPresenter(IViewShowDatas<ThemeNewestBean> mView) {
        this.mView = mView;
        mModel = new ThemeNewestModel();
    }

    @Override
    public void getDatas(ThemeNewestRequest request) {
        mModel.setCallBack(listener,errorListener);
        mModel.getDatasFromNet(request);
        mStartNum = request.getmFrom();
    }

    Response.Listener listener = new Response.Listener() {
        @Override
        public void onResponse(Object response) {
            DebugLog.w(TAG,"===================onResponse:"+response);
            if(mView == null){
                return;
            }

            if(!TextUtils.isEmpty(response.toString())){
                try {
                    mView.showDatas(AppUtils.spliteThemeNewest(response.toString()));
                    if(mStartNum == 0){
                        CacheUtil.saveCache(response.toString(),CACHE_NAME);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    mView.showDatas(null);
                }
            }
        }
    };

    Response.ErrorListener errorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e(TAG,"lineNumber=63,methodName=onErrorResponse"+error.getMessage());
            error.printStackTrace();
            if(mView == null){
                return;
            }
            String cache = null;
            if(mStartNum == 0){
                cache = CacheUtil.getCache(CACHE_NAME);
            }
            if(!TextUtils.isEmpty(cache)){
                try {
                    mView.showDatas(AppUtils.spliteThemeNewest(cache));
                } catch (JSONException e) {
                    mView.showDatas(null);
                    e.printStackTrace();
                }
            }else {
                mView.showDatas(null);
            }
        }
    };

    public void destroyRefrence(){
        mView = null;
        listener = null;
        errorListener = null;
    }


}