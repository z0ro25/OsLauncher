package com.ios.boot.iosboot;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.viewpager.widget.ViewPager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.ios.boot.iosboot.adapter.SlidingAdapter;
import com.ios.boot.iosboot.animation.MorphingAnimation;
import com.ios.boot.iosboot.commoninterface.BootAnimationListener;
import com.ios.boot.iosboot.commoninterface.OnAnimationEndListener;
import com.ios.boot.iosboot.utils.BlurUtils;
import com.ios.boot.iosboot.utils.Utils;
import com.ios.boot.iosboot.viewpager.ZoomOutPageTransFormer;
import com.ios.boot.iosboot.widget.ExplosionView;
import com.ios.boot.iosboot.widget.RippleView;
import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.util.PreferencesUtil;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.views.CustomTextView;

import java.io.IOException;

import static android.animation.ObjectAnimator.ofFloat;

public class FirstRunActivity extends Activity implements View.OnClickListener {
    private final String IS_FIRST = "isfirst";
    private Button mStartBtn;
    private Animation mTextInAnimation;
    private Animation mTextOutAnimation;
    private Animation mStartBtnInAnimation;
    private ViewPager mViewPager;
    private SlidingAdapter mSlidingAdapter;
    private RelativeLayout mIOSIv;
    private RelativeLayout mWallpaperRl;
    private CustomTextView mWallpaperTv;
    private Animation mIOSOutAnimation;
    private Animation mWallpaperTvInAnimation;
    private Animation mWallpaperRlInAnimation;
    private Animation mOkBtnInAnimation;
    private RelativeLayout mMainRl;
    private ImageView mBackgroundIv2;
    private int mChoseBackground;
    private Button mOkBtn;
    private ExplosionView mExplosionView;
    private CustomTextView mSettingTv;
    private RippleView mBoBv;
    private CustomTextView mLayerTv;
    private boolean mIsOver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.IOSBootAppTheme);
        setContentView(R.layout.activity_splash_first);
        initUi();
    }

    private void initUi() {
        setLayerText();
//        mTextLl = (LinearLayout) findViewById(R.id.boot_first_text_ll);
        mStartBtn = (Button) findViewById(R.id.boot_first_start_btn);
        mOkBtn = (Button) findViewById(R.id.boot_first_ok_btn);
        mIOSIv = (RelativeLayout) findViewById(R.id.ios_rl);
        mWallpaperRl = (RelativeLayout) findViewById(R.id.wallpaper_rl);
        mWallpaperTv = (CustomTextView) findViewById(R.id.wallpaper_tv);
        mViewPager = (ViewPager) findViewById(R.id.boot_first_vp);
        mMainRl = (RelativeLayout) findViewById(R.id.activity_main);
        mBackgroundIv2 = (ImageView) findViewById(R.id.background_iv2);
        mSettingTv = (CustomTextView) findViewById(R.id.setting_tv);
        mBoBv = (RippleView) findViewById(R.id.bo_bv);
        mSlidingAdapter = new SlidingAdapter(this);
        mViewPager.setAdapter(mSlidingAdapter);
        mViewPager.setPageTransformer(true, new ZoomOutPageTransFormer());
        initAnimation();
        initClick();
    }

    private void initClick() {
        mStartBtn.setOnClickListener(this);
        mOkBtn.setOnClickListener(this);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (mBackgroundIv2.getBackground() == null) {
                    new AsyncTask<Void, Void, Bitmap>() {
                        @Override
                        protected Bitmap doInBackground(Void... params) {
                            try {
                                return BlurUtils.createBlurBitmap(BitmapFactory.decodeResource(getResources(), com.ios.theme.def.R.drawable.default_wallpaper));
                            } catch (Exception e) {
                                return null;
                            }
                        }

                        @Override
                        protected void onPostExecute(Bitmap bitmap) {
                            if (bitmap != null) {
                                mBackgroundIv2.setBackgroundDrawable(new BitmapDrawable(bitmap));
                            } else {
                                mBackgroundIv2.setBackgroundResource(com.ios.theme.def.R.drawable.default_wallpaper);
                            }
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    mBackgroundIv2.setBackgroundResource(com.ios.theme.def.R.drawable.default_wallpaper);
                }
                if (mBackgroundIv2.getVisibility() != View.VISIBLE) {
                    mBackgroundIv2.setVisibility(View.VISIBLE);
                }
                if (position == 0) {
                    mBackgroundIv2.setAlpha(positionOffset);
                } else {
                    mBackgroundIv2.setAlpha(1);
                }
            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    mChoseBackground = 0;
                } else {
                    mChoseBackground = com.ios.theme.def.R.drawable.default_wallpaper;
                }
            }
        });
    }

    private void initAnimation() {
        mTextInAnimation = AnimationUtils.loadAnimation(this, R.anim.boot_first_text_animator_in);
        mTextOutAnimation = AnimationUtils.loadAnimation(this, R.anim.boot_first_text_animator_out);
        mStartBtnInAnimation = AnimationUtils.loadAnimation(this, R.anim.boot_first_start_btn_animator_in);
        mOkBtnInAnimation = AnimationUtils.loadAnimation(this, R.anim.boot_first_ok_btn_animator_in);
        mIOSOutAnimation = AnimationUtils.loadAnimation(this, R.anim.boot_first_freeme_animator_out);
        mWallpaperTvInAnimation = AnimationUtils.loadAnimation(this, R.anim.boot_first_wallpaper_tv_animator_in);
        mWallpaperRlInAnimation = AnimationUtils.loadAnimation(this, R.anim.boot_first_wallpaper_rl_animator_in);
        mIOSOutAnimation.setAnimationListener(new BootAnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                mIOSIv.setVisibility(View.GONE);
                mWallpaperRl.setVisibility(View.VISIBLE);
                mWallpaperRl.startAnimation(mWallpaperRlInAnimation);
                mWallpaperTv.setVisibility(View.VISIBLE);
                mWallpaperTv.startAnimation(mWallpaperTvInAnimation);
            }
        });
        mTextOutAnimation.setAnimationListener(new BootAnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                mLayerTv.setVisibility(View.GONE);
                mIOSIv.startAnimation(mIOSOutAnimation);

            }
        });
        mWallpaperTvInAnimation.setAnimationListener(new BootAnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                mViewPager.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mViewPager.setCurrentItem(1);
                    }
                }, 500);
                mViewPager.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mViewPager.setCurrentItem(0);
                    }
                }, 1500);
                mOkBtn.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mOkBtn.setVisibility(View.VISIBLE);
                        mOkBtn.setEnabled(true);
                        mOkBtn.startAnimation(mOkBtnInAnimation);
                    }
                }, 2500);
            }
        });
        mLayerTv.startAnimation(mTextInAnimation);
        mStartBtn.postDelayed(new Runnable() {
            @Override
            public void run() {
                mStartBtn.setVisibility(View.VISIBLE);
                mStartBtn.setEnabled(true);
                mStartBtn.startAnimation(mStartBtnInAnimation);
            }
        }, 500);
    }

    @Override
    public void onClick(final View v) {
        int i = v.getId();
        if (i == R.id.boot_first_start_btn) {
            com.ios.boot.iosboot.LauncherGuideManager.getInstance(this.getApplicationContext()).markFirstRunActivityShown();
            PreferencesUtil.putBoolean(this, IS_FIRST, false);
            if (Partner.getBoolean(this, Partner.DEF_USER_GUIDE_ENABLE)) {
                mLayerTv.startAnimation(mTextOutAnimation);
                final MorphingAnimation m = new MorphingAnimation();
                m.setAnimatorListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        mStartBtn.setText("");
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        new AsyncTask<Void, Void, Bitmap>() {
                            @Override
                            protected Bitmap doInBackground(Void... params) {
                                try {
                                    WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());
                                    Drawable wallpaperDrawable = wallpaperManager.getDrawable();
                                    return BlurUtils.createBlurBitmap(((BitmapDrawable) wallpaperDrawable).getBitmap());
                                } catch (Exception e) {
                                    return null;
                                }
                            }

                            @Override
                            protected void onPostExecute(Bitmap bitmap) {
                                if (bitmap != null) {
//                                    mMainRl.setBackgroundDrawable(new BitmapDrawable(bitmap));
                                    mBoBv.setPositionView(mStartBtn).setShowView(mMainRl).setBitmap(bitmap).startAnimation();
                                }
//                                CircularAnim.show(mMainRl).triggerView(mStartBtn).go();
                                mStartBtn.setVisibility(View.GONE);
                            }

                        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                });
                m.smallStart(mStartBtn, 300);
            } else {
                finish();
            }
        } else if (i == R.id.boot_first_ok_btn) {
            startIOSIconInAnimation();
            if (mChoseBackground == 0) {
                return;
            }
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());
            try {
                wallpaperManager.setResource(mChoseBackground);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * 开启加载桌面动画
     */
    private void startIOSIconInAnimation() {
        mIOSIv.setVisibility(View.GONE);
        mWallpaperRl.setVisibility(View.GONE);
        mWallpaperTv.setVisibility(View.GONE);
        ObjectAnimator animator1 = ofFloat(mIOSIv, "rotationX", 90, 0);
        ObjectAnimator animator2 = ofFloat(mIOSIv, "alpha", 0.5f, 1.0f);
        AnimatorSet set = new AnimatorSet();
        set.setDuration(500);
        set.playTogether(animator1, animator2);
        set.start();
        mSettingTv.setVisibility(View.VISIBLE);
        mSettingTv.startAnimation(mOkBtnInAnimation);
        MorphingAnimation m = new MorphingAnimation();
        m.setAnimatorListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mOkBtn.setText("");
                ObjectAnimator animator = ObjectAnimator.ofFloat(mOkBtn, "translationY", 0, -(Utils.dp2Px(165))).setDuration(500);
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mOkBtn.setVisibility(View.GONE);
                        mExplosionView = ExplosionView.attach2Window(FirstRunActivity.this);
                        mExplosionView.explode(mOkBtn);
                        mExplosionView.setOnAnimatorListener(new OnAnimationEndListener() {
                            @Override
                            public void onAnimationEnd() {
                                if (mExplosionView != null) {
                                    if (!mIsOver) {
                                        mExplosionView.explode(mOkBtn);
                                    }
                                }
                            }
                        });
                        mOkBtn.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                finish();
                            }
                        }, 3000);
                    }
                });
                animator.start();
            }
        });
        m.smallStart(mOkBtn, 300);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mExplosionView = null;
    }

    /**
     * 设置法律声明
     */
    private void setLayerText() {
        mLayerTv = (CustomTextView) findViewById(R.id.boot_first_layer_tv);
        SpannableString ss1 = new SpannableString(getString(R.string.about_legal_notices));
        ss1.setSpan(new ClickableSpan() {
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(getResources().getColor(R.color.layer));
            }

            @Override
            public void onClick(View widget) {
                startActivity(new Intent(FirstRunActivity.this, com.ios.boot.iosboot.LegalNoticesActivity.class));
            }
        }, 0, ss1.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mLayerTv.append(" ");
        mLayerTv.append(ss1);
        mLayerTv.setHighlightColor(Color.TRANSPARENT);
        mLayerTv.setMovementMethod(LinkMovementMethod.getInstance());
    }


    public void stopRun() {
        mIsOver = true;
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
    }
}
