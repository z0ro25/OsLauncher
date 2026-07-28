package com.amz.ios.ioslite.common;

import android.app.Activity;
import android.os.Bundle;

import com.amz.ios.ioslite.common.analytics.AnalyticsDelegate;

/**
 *  CommonActivity with common fuctions;
 *
 *  1. data analytics include : umeng, google firbase;
 *
 */
public class CommonActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AnalyticsDelegate.onCreate(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsDelegate.onResume(this);
    }

    @Override
    protected void onPause() {
        AnalyticsDelegate.onPause(this);
        super.onPause();
    }
}
