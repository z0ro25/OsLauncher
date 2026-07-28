package com.amz.ios.themeclub.presenter;

import android.text.TextUtils;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.amz.ios.themeclub.bean.WallPaperSelectionPieceBean;
import com.amz.ios.themeclub.bean.request.WallPaperSelectionRequest;
import com.amz.ios.themeclub.intertfaces.IModelData;
import com.amz.ios.themeclub.intertfaces.IPresenterData;
import com.amz.ios.themeclub.intertfaces.IViewShowDatas;
import com.amz.ios.themeclub.model.WallPaperSelectionModel;
import com.amz.ios.themeclub.util.AppUtils;
import com.amz.ios.themeclub.util.CacheUtil;

import org.json.JSONException;

/**
 * Created by ZhangMingZhe on 11/21/16.
 */

public class WallPaperSelectionPresenter implements IPresenterData<WallPaperSelectionRequest> {
    private IViewShowDatas mView;
    private IModelData mModel;
    private final String TAG = WallPaperSelectionPresenter.class.getSimpleName();
    private final String CACHE_NAME = TAG + ".cfg";
    private int mStartNum;
    public WallPaperSelectionPresenter(IViewShowDatas<WallPaperSelectionPieceBean> mView) {
        this.mView = mView;
        mModel = new WallPaperSelectionModel();
    }

    @Override
    public void getDatas(WallPaperSelectionRequest request) {
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
                try {
                    WallPaperSelectionPieceBean bean = AppUtils.spliteWallPaperSelection(response.toString());
                    if(mStartNum == 0){
                        CacheUtil.saveCache(response.toString(),CACHE_NAME);
                    }
                    mView.showDatas(bean);
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
                    mView.showDatas(AppUtils.spliteWallPaperSelection(cache));
                } catch (JSONException e) {
                    e.printStackTrace();
                    mView.showDatas(null);
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
