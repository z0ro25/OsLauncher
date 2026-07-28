package com.ios.boot.iosboot;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.ios.boot.iosboot.commoninterface.BootAnimationListener;
import com.amz.ios.launcher.R;

/**
 * Created by YiYang on 16-12-5.
 */

public class UpdateInfoActivity extends Activity implements View.OnClickListener {
    private final String MINIMALIST_THEME = "minimalist_theme";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_info);
        initUi();
    }

    private void initUi() {
        findViewById(R.id.boot_updateinfo_setting_btn).setOnClickListener(this);
        findViewById(R.id.boot_updateinfo_setting_tv).setOnClickListener(this);
        startAnimation(0
                , findViewById(R.id.boot_updateinfo_title_tv)
                , findViewById(R.id.boot_updateinfo_speed_ll)
                , findViewById(R.id.boot_updateinfo_news_ll)
                , findViewById(R.id.boot_updateinfo_theme_ll)
                , findViewById(R.id.boot_updateinfo_setting_rl));
    }

    private void startAnimation(final int i, final View... view) {
        Log.e("动画", i + ":" + view.length);
        Animation mAnimation = AnimationUtils.loadAnimation(this, R.anim.boot_updateinfo_animator_in);
        mAnimation.setAnimationListener(new BootAnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                int j = i + 1;
                startAnimation(j, view);
            }
        });
        if (i < view.length) {
            view[i].setVisibility(View.VISIBLE);
            view[i].startAnimation(mAnimation);
        }

    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.boot_updateinfo_setting_btn) {
            Intent intent = new Intent();
            intent.putExtra(MINIMALIST_THEME, MINIMALIST_THEME);
            intent.putExtra("themeclubtype", 1);
            ComponentName componentName = new ComponentName("com.oslauncher.applauncher.themelauncher", "com.amz.ios.themeclub.MainActivity");
            intent.setComponent(componentName);
            startActivity(intent);
            finish();
        } else if (i == R.id.boot_updateinfo_setting_tv) {
            finish();
        }
    }



    protected void onResume(){
        super.onResume();
        LauncherGuideManager.getInstance(this.getApplicationContext()).markNewVersionInfoActivityShown();
    }


}
