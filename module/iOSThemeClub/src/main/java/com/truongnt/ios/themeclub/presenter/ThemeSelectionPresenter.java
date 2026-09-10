package com.truongnt.ios.themeclub.presenter;

import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.truongnt.ios.ioslite.common.debug.DebugLog;
import com.truongnt.ios.themeclub.bean.ThemeSelectionPieceBean;
import com.truongnt.ios.themeclub.bean.request.ThemeSelectionRequest;
import com.truongnt.ios.themeclub.intertfaces.IPresenterData;
import com.truongnt.ios.themeclub.intertfaces.IViewShowDatas;
import com.truongnt.ios.themeclub.model.ThemeSelectionModel;
import com.truongnt.ios.themeclub.util.AppUtils;
import com.truongnt.ios.themeclub.util.CacheUtil;

import org.json.JSONException;

/**
 * Created by ZhangMingZhe on 11/22/16.
 */

public class ThemeSelectionPresenter implements IPresenterData<ThemeSelectionRequest> {
    private final String TAG = ThemeSelectionPresenter.class.getSimpleName();
    private IViewShowDatas mView;
    private ThemeSelectionModel mModel;
    private int mStartNumber;
    private final String CACHE_NAME = TAG+".cfg";
    public ThemeSelectionPresenter(IViewShowDatas<ThemeSelectionPieceBean> mView) {
        this.mView = mView;
        mModel = new ThemeSelectionModel();
    }

    @Override
    public void getDatas(ThemeSelectionRequest request) {
        mModel.setCallBack(listener,errorListener);
        mModel.getDatasFromNet(request);
        mStartNumber = request.getmFrom();
    }

    Response.Listener listener = new Response.Listener() {
        @Override
        public void onResponse(Object response) {
            DebugLog.w(TAG,"=================response:"+response);
            if(mView == null){
                return;
            }
            if(!TextUtils.isEmpty(response.toString())){
                try {
                    if(mStartNumber == 0){
                        CacheUtil.saveCache(response.toString(),CACHE_NAME);
                    }
                    mView.showDatas(AppUtils.spliteThemeSelection(response.toString()));
                } catch (JSONException e) {
                    mView.showDatas(null);
                    e.printStackTrace();
                }
            }

        }
    };

    Response.ErrorListener errorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            error.printStackTrace();
            if(mView == null){
                return;
            }
            String cache = null;
            if(mStartNumber == 0){
                cache = CacheUtil.getCache(CACHE_NAME);
                Log.d(TAG,"line(60) + #onErrorResponse »º´æ£½"+cache);
            }
            if(!TextUtils.isEmpty(cache)){
                try {
                    mView.showDatas(AppUtils.spliteThemeSelection(cache));
                } catch (JSONException e) {
                    mView.showDatas(null);
                    e.printStackTrace();
                }
            }else{
                mView.showDatas(null);
            }
        }
    };

    public void destroyReference(){
        mView = null;
        mModel = null;
        listener = null;
        errorListener = null;
    }
}