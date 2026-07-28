package com.ios.boot.iosboot;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.BounceInterpolator;

import com.ios.boot.iosboot.animation.CircularAnim;
import com.ios.boot.iosboot.utils.Utils;

import com.amz.ios.launcher.R;
import com.amz.ios.launcher.config.PreferredHomeSetting;

/**
 * Created by YiYang on 16-12-5.
 */

public class SpeedUpActivity extends Activity implements View.OnClickListener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed_up);
        initUi();
    }

    private void initUi() {
        findViewById(R.id.boot_speedup_setting_btn).setOnClickListener(this);
        findViewById(R.id.boot_speedup_setting_tv).setOnClickListener(this);
        startAnimation(findViewById(R.id.boot_speedup_rl));
    }

    private void startAnimation(View... view) {
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.boot_speedup_animator_in);
        for (int i = 0; i < view.length; i++) {
            view[i].startAnimation(animation);
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.boot_speedup_setting_btn) {
            PreferredHomeSetting.setDefaultHomeApp(this);
            finish();
        } else if (i == R.id.boot_speedup_setting_tv) {
            finish();
        }
    }

    public static void startSpeedUpActivity(final Activity activity, final View view) {
        final Context context = activity.getApplicationContext();
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationY", 0, -(Utils.dp2Px(50)), 0);
        animator.setInterpolator(new BounceInterpolator());
        animator.setDuration(1000);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.performClick();
                CircularAnim
                        .fullActivity(activity, view)
                        .go(new CircularAnim.OnAnimationEndListener() {
                            @Override
                            public void onAnimationEnd() {
                                context.startActivity(new Intent(context, SpeedUpActivity.class));
                            }
                        });
            }
        });
        animator.start();
    }


    protected void onResume() {
        super.onResume();
        LauncherGuideManager.getInstance(this.getApplicationContext()).markSpeedGuideActivityShown();
    }
}
