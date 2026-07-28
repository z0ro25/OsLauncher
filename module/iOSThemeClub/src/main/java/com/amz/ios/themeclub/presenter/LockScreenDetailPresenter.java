package com.amz.ios.themeclub.presenter;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.themeclub.bean.LockScreenMoreBean;
import com.amz.ios.themeclub.bean.request.LockScreenDetailRequest;
import com.amz.ios.themeclub.intertfaces.IPresenterData;
import com.amz.ios.themeclub.intertfaces.IViewShowDatas;
import com.amz.ios.themeclub.model.LockScreenDetailModel;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by server on 17-8-21.
 */
public class LockScreenDetailPresenter implements IPresenterData<LockScreenDetailRequest> {
    private final String TAG = getClass().getSimpleName();
    private LockScreenDetailModel mModel;
    private IViewShowDatas<LockScreenMoreBean> mView;

    public LockScreenDetailPresenter(IViewShowDatas<LockScreenMoreBean> view) {
        mModel = new LockScreenDetailModel();
        this.mView = view;
    }

    @Override
    public void getDatas(LockScreenDetailRequest request) {
        mModel.setCallBack(listener,errorListener);
        mModel.getDatasFromNet(request);
    }

    Response.Listener listener = new Response.Listener() {
        @Override
        public void onResponse(Object response) {
            DebugLog.w(TAG,"================onResponse:"+response.toString());
            Gson gson = new Gson();
            try {
                JSONObject obj = new JSONObject(response.toString());
                String body = obj.getString("body");
                DebugLog.w(TAG,"==============body:"+body);
                LockScreenMoreBean lockScreenMoreBean = gson.fromJson(body, LockScreenMoreBean.class);
                mView.showDatas(lockScreenMoreBean);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    Response.ErrorListener errorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            error.printStackTrace();
            DebugLog.w(TAG,"================onErrorResponse:"+error.toString());
        }
    };
}
