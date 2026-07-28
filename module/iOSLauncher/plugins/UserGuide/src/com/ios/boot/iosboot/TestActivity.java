package com.ios.boot.iosboot;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.ios.boot.iosboot.widget.TailBall;
import com.amz.ios.launcher.R;

/**
 * Created by YiYang on 16-12-7.
 */

public class TestActivity extends Activity {

    private LinearLayout mLl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        mLl = (LinearLayout) findViewById(R.id.ll);
        mLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TailBall.attach2Window(TestActivity.this).start();
                mLl.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mLl.performClick();
                    }
                }, 300);
            }
        });
    }
}
