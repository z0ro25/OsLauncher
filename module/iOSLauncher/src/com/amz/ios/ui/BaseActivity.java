package com.amz.ios.ui;

import android.os.Build;
import android.os.Bundle;
import android.view.Window;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.amz.ios.launcher.R;

public class BaseActivity extends AppCompatActivity {

    AppUtils mSettings;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSettings = AppUtils.getInstance(this);
        Window window = getWindow();
        window.setStatusBarColor(getResources().getColor(R.color.color_bg_main));
        window.setNavigationBarColor(getResources().getColor(R.color.color_bg_main));
        window.getDecorView().setBackgroundColor(getResources().getColor(R.color.color_bg_main));
        int sdkInt = Build.VERSION.SDK_INT;
        int visiblity = sdkInt >= 23 ? 8192 : 0;
        if (sdkInt >= 26) {
            visiblity |= 16;
        }
        window.getDecorView().setSystemUiVisibility(visiblity);
    }
}
