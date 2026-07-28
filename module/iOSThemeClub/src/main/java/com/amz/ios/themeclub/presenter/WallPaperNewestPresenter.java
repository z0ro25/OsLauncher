package com.amz.ios.themeclub.presenter;

import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.amz.ios.themeclub.bean.WallPaperJsonBean;
import com.amz.ios.themeclub.bean.request.WallPaperNewestRequest;
import com.amz.ios.themeclub.intertfaces.IModelData;
import com.amz.ios.themeclub.intertfaces.IPresenterData;
import com.amz.ios.themeclub.intertfaces.IViewShowDatas;
import com.amz.ios.themeclub.model.WallPaperNewestModel;
import com.amz.ios.themeclub.util.AppUtils;
import com.amz.ios.themeclub.util.CacheUtil;

import org.json.JSONException;

/**
 * Created by ZhangMingZhe on 11/18/16.
 */

public class WallPaperNewestPresenter implements IPresenterData<WallPaperNewestRequest> {
    private final String TAG = WallPaperNewestPresenter.class.getSimpleName();
    private IViewShowDatas mView;
    private IModelData mData;
    private final String CACHE_NAME = TAG+".cfg";
    private int mStartNum;
    public WallPaperNewestPresenter(IViewShowDatas<WallPaperJsonBean> mView) {
        this.mView = mView;
        mData = new WallPaperNewestModel();
    }

    @Override
    public void getDatas(WallPaperNewestRequest mRequest) {
        mData.setCallBack(listener,errorListener);
        mData.getDatasFromNet(mRequest);
        this.mStartNum = mRequest.getmFrom();
    }
    Response.Listener listener = new Response.Listener() {
        @Override
        public void onResponse(Object response) {
            Log.e(TAG,"result="+response.toString());
            if(mView == null){
                return;
            }
            if(!TextUtils.isEmpty(response.toString())){
                try {
                    mView.showDatas(AppUtils.spliteWallPaperNewest(response.toString()));
                    CacheUtil.saveCache(response.toString(),CACHE_NAME);
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
            String mCache = null;
            if(mStartNum == 0){
                mCache = CacheUtil.getCache(CACHE_NAME);
            }

            if(TextUtils.isEmpty(mCache)){
                mView.showDatas(null);
            }else {
                try {
                    mView.showDatas(AppUtils.spliteWallPaperNewest(mCache));
                } catch (JSONException e) {
                    e.printStackTrace();
                    mView.showDatas(null);
                }
            }
        }
    };

    public void destroyReference(){
        mView = null;
        listener = null;
        errorListener = null;
        mData = null;
    }
}