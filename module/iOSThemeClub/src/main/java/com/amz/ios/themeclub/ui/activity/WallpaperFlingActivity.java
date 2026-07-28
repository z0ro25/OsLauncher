package com.amz.ios.themeclub.ui.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;

import com.amz.ios.ioslite.common.CommonAppCompatActivity;
import com.amz.ios.themeclub.util.AppUtils;


abstract class WallpaperFlingActivity extends CommonAppCompatActivity{


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtils.changeStatusBarStyle(this);
        setContentView(getLayoutResId());

        initConfig();
    }

    protected abstract void initConfig();

    protected abstract int getLayoutResId();




    @Override
    public boolean onTouchEvent(MotionEvent event) {

        Log.e("Newest","ONTOUCHEVENT 000 ");
        return super.onTouchEvent(event);
    }

}
