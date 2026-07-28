package com.amz.ios.themeclub.presenter;

import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.amz.ios.themeclub.bean.LockScreenNewestBean;
import com.amz.ios.themeclub.bean.request.LockScreenNewestRequest;
import com.amz.ios.themeclub.intertfaces.IPresenterData;
import com.amz.ios.themeclub.intertfaces.IViewShowDatas;
import com.amz.ios.themeclub.model.LockNewestModel;
import com.amz.ios.themeclub.util.AppUtils;
import com.amz.ios.themeclub.util.CacheUtil;

import org.json.JSONException;

/**
 * Created by ubuntu on 14/06/17.
 */

public class LockScreenNewstPresenter implements IPresenterData<LockScreenNewestRequest> {
    private final static String TAG = LockScreenNewstPresenter.class.getSimpleName();
    private IViewShowDatas<LockScreenNewestBean> mView;
    private LockNewestModel mModel;
    private int mStartNum;
    private final String CACHE_NAME = TAG+".cfg";
    public LockScreenNewstPresenter(IViewShowDatas<LockScreenNewestBean> view){
        mView = view;
        mModel = new LockNewestModel();
    }
    @Override
    public void getDatas(LockScreenNewestRequest request) {
        mModel.setCallBack(listener,errorListener);
        mModel.getDatasFromNet(request);
        mStartNum = request.getmFrom();
    }

    Response.Listener listener = new Response.Listener() {
        @Override
        public void onResponse(Object response) {
            if(mView == null){
                return;
            }
            if(!TextUtils.isEmpty(response.toString())){
                Log.i("luch","lock response = "+response.toString());
                try {
                    mView.showDatas(AppUtils.spliteLockScreenNewest(response.toString()));
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
                    mView.showDatas(AppUtils.spliteLockScreenNewest(cache));
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
