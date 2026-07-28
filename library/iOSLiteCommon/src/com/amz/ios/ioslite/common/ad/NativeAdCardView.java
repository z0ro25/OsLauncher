package com.amz.ios.ioslite.common.ad;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.amz.ios.ioslite.common.R;

public class NativeAdCardView extends FrameLayout {
    private static final String TAG = "NativeAdCardView";
    private static final boolean DEBUG = true;

    /**
     * @see IOSAdConfig
     */
    private int mIOSAdId = -1;

    /**
     * mobula ad data mobula广告数据类
     */
    private IOSNativeAd mNativeAd;
    private IOSAdManager mAdManager;
    private Thread mUiThread;
    final Handler mHandler = new Handler();

    private FrameLayout mRootContainer;

    public NativeAdCardView(Context context) {
        this(context, null);
    }

    public NativeAdCardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NativeAdCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.ad_native_view_container, this);
        initView();

        mAdManager = IOSAdManager.getInstance(getContext());
        mUiThread = Thread.currentThread();
    }

    /**
     * initialize the view method 初始化视图
     */
    private void initView() {
        mRootContainer = (FrameLayout) findViewById(R.id.root);
    }

    /**
     * @see IOSAdConfig
     */
    public void setAdvertiseId(int id) {
        mIOSAdId = id;
    }

    public void loadAdvertise() {
        if (DEBUG) Log.d(TAG, "loadAdvertise");
        if (mIOSAdId == -1) {
            Log.d(TAG, "id = -1, return");
            return;
        }

        if (mNativeAd == null) {
            mNativeAd = mAdManager.getNativeAd(mIOSAdId);
        }

        if (mNativeAd != null) {
            if (DEBUG) Log.d(TAG, "native advertise load start");
            mNativeAd.setNativeAdListener(mListener);
            mNativeAd.load();
        }
    }


    public void fillIn(IOSNAdResponse response) {
        setWithResponseView(response);
    }

    private IOSNativeAdListener mListener = new IOSNativeAdListener() {
        @Override
        public void onError(IOSAdError error) {
            if (DEBUG) Log.d(TAG, "onError : " + error.getErrorMessage());
        }

        @Override
        public void onAdLoaded(final IOSNAdResponse response) {
            if (DEBUG) Log.d(TAG, "onAdLoaded : ");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fillIn(response);
                }
            });
        }

        @Override
        public void onClick() {
            if (DEBUG) Log.d(TAG, "onClick : click ad");
        }
    };


    private void setWithResponseView(IOSNAdResponse response) {
        mRootContainer.setVisibility(GONE);
        mRootContainer.removeAllViews();

        final View adContentView = response.getAdContentView();
        if (adContentView != null) {
            if (adContentView.getParent() != null) {
                ((ViewGroup) adContentView.getParent()).removeView(adContentView);
            }
            mRootContainer.addView(response.getAdContentView());
        }
        mRootContainer.setVisibility(VISIBLE);
    }

    private final void runOnUiThread(Runnable action) {
        if (Thread.currentThread() != mUiThread) {
            mHandler.post(action);
        } else {
            action.run();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mNativeAd != null) {
            mNativeAd.destory();
        }
        super.onDetachedFromWindow();
    }

}
