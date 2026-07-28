package com.zhuoyi.security.batterysave.views;

import android.app.Activity;
import android.os.Bundle;

public class SL_GlobalActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageCount();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    protected void setStartRemeber(boolean state) {
        SL_CountUi.setStartRemeber(state);
    }

    private void pageCount() {
        SL_CountUi.pageCount();
    }

    protected void pageClean() {
        SL_CountUi.pageClean();
    }

    protected int getPageCount() {
        return SL_CountUi.getPageCount();
    }

    protected void setStartTime(long startTime) {
        SL_CountUi.setStartTime(startTime);
    }

    protected long getStartTime() {
        return SL_CountUi.getStartTime();
    }
}
