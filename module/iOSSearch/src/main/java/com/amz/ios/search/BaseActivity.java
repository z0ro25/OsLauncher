package com.amz.ios.search;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.amz.ios.ioslite.common.CommonAppCompatActivity;
import com.amz.ios.ioslite.common.util.PermissionUtil;
import com.amz.ios.http.Internal.Action;
import com.amz.ios.http.Internal.CancelableCallBack;

import org.jetbrains.annotations.Nullable;

/**
 * Author       : yizhihao
 * Create time  : 2016-12-06 下午4:51
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class BaseActivity extends CommonAppCompatActivity {

    private static final String TAG = BaseActivity.class.getSimpleName();

    private Handler mHandler;
    private InputMethodManager mIm;
    /**
     * help for lestering key back event
     */
    private boolean mShowSoftInput = false;

    private View mShowSoftInputView;

    /**
     * check permission and run a can cancelable task
     *
     * @param action
     * @param permission
     * @param requestCode
     */
    protected void checkPermissionAndLoad(final Action action, int requestCode, final CancelableCallBack callBack, final String... permission) {
        PermissionUtil.checkSelfPermissions(this, requestCode, new PermissionUtil.PermissionsRequestCallBackAdapter() {
            @Override
            public void onPermissionAllowed() {
                action.observer(callBack);
            }

            @Override
            public String[] onGetPermissions() {
                return permission;
            }
        }, 0, permission);
    }

    public void setShowSoftInputView(View showSoftInputView) {
        mShowSoftInputView = showSoftInputView;
    }

    private Runnable mShowSoftInputTask = new Runnable() {
        @Override
        public void run() {
            if (mIm != null)
                mIm.showSoftInput(mShowSoftInputView, InputMethodManager.SHOW_IMPLICIT);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mIm = null;
    }

    public boolean isShowSoftInput() {
        return mShowSoftInput;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //try to solved the api bug
        outState.putBoolean("mShowSoftInput", isShowSoftInput());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            final boolean showSoftInput = savedInstanceState.getBoolean("mShowSoftInput");
            if (showSoftInput) {
                showSoftInput();
            }
        }
    }

    protected void hideSoftInput() {
        if (mShowSoftInputView == null) return;
        if (mIm == null) {
            mIm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        }
        mShowSoftInput = false;
        mIm.hideSoftInputFromWindow(mShowSoftInputView.getWindowToken(), 0);
    }

    protected void showSoftInput() {
        showSoftInput(false);
    }

    protected void showSoftInput(boolean delay) {
        if (mShowSoftInputView == null) return;
        if (mIm == null) {
            mIm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        }
        mShowSoftInput = true;
        if (mHandler == null) mHandler = new Handler();
        mHandler.postDelayed(mShowSoftInputTask, delay ? 500 : 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        PermissionUtil.onRequestPermissionsResult(this, requestCode, new int[]{resultCode});
    }
}
