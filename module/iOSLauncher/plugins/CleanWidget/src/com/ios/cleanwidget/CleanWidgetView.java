package com.ios.cleanwidget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.ios.boot.iosboot.LauncherGuideManager;
import com.ios.cleanwidget.anim.CleanAnimatorListener;
import com.ios.cleanwidget.anim.CleanIconUpdateListener;
import com.amz.ios.ioslite.common.ContextHelper;
import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.analytics.AnalyticsDelegate;
import com.amz.ios.ioslite.common.analytics.UMEventConstants;
import com.amz.ios.ioslite.common.setting.IOSSettings;
import com.amz.ios.ioslite.common.util.DisplayUtil;
import com.amz.ios.launcher.DeviceProfile;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.views.CustomTextView;


public class CleanWidgetView extends FrameLayout implements View.OnClickListener {
    private static final String TAG = "CleanWidgetView";
    private static final boolean DEBUG = true;

    public static final String ACTION_CLEAN_WIDGET = "ios.intent.action.CleanWidget";

    private static final float DEFAULT_USED_MEMORY_PERCENT = 0.7f;

    private Launcher mLauncher;
    private Context mContext;
    private LauncherGuideManager mLauncherGuideManager;
    private DeviceProfile mDeviceProfile;

    public CustomTextView mIconName;
    public CleanIconView mCleanIcon;
    public CleanCircleView mCircleView;

    private int mIconViewSize;
    private int mIconTextColor;
    private float mIconTextSizePx;
    private int mIconViewTextPadding;

    public ValueAnimator mIconZoomOutAnimator;
    public ValueAnimator mIconZoomInAnimator;
    private CleanAnimatorListener mCleanAnimatorListener;

    private boolean mInCleanProgcess;

    private Long mCleanResult;

//    private IOSAdManager mAdManager;
//    private IOSNativeAd mNativeAd;
//    private IOSNativeAdListener mAdListener;
//    private IOSNAdResponse mAdResponse;

    private CleanResultDialog mResultDialog;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DEBUG) Log.w(TAG, "onReceive : " + intent.getAction());
            if (action.equals(ACTION_CLEAN_WIDGET)) {
                onClick(null);
            } else if (action.equals(Intent.ACTION_LOCALE_CHANGED)) {
                // Locale changed ,update icon text string;
                mIconName.setText(mContext.getString(R.string.clean_widget_name));
            } else {
                refreshCleanIcon();
            }
        }
    };

    private ContentObserver mObserver = new ContentObserver(new Handler()) {

        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(IOSSettings.getUriFor(IOSSettings.Launcher.LAUNCHER_WORKSPACE_ICON_LABEL_COLOR))) {
                mIconTextColor = IOSSettings.getInt(mContext.getContentResolver(), IOSSettings.Launcher.LAUNCHER_WORKSPACE_ICON_LABEL_COLOR);
                mIconName.setTextColor(mIconTextColor);

            } else if (uri.equals(IOSSettings.getUriFor(IOSSettings.Launcher.LAUNCHER_APP_LABEL_SIZE_PX))) {
                updateTextSize();
            }
        }
    };


    private void setUpAdvertise() {
//        mAdManager = IOSAdManager.getInstance(mContext);
//        mAdListener = new IOSNativeAdListener() {
//            @Override
//            public void onError(IOSAdError error) {
//
//            }
//
//            @Override
//            public void onAdLoaded(IOSNAdResponse response) {
//                mAdResponse = response;
//            }
//
//            @Override
//            public void onClick() {
//
//            }
//        };
    }

    private void loadAdvertise() {

    }

    public CleanWidgetView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        mLauncher = (Launcher) context;
        mContext = mLauncher.getApplicationContext();
        mLauncherGuideManager = LauncherGuideManager.getInstance(context.getApplicationContext());
        setUpAdvertise();

        mDeviceProfile = mLauncher.getDeviceProfile();
        mIconViewSize = mDeviceProfile.iconSizePx;
        mIconTextColor = mDeviceProfile.workspaceIconTextColor;
        mIconTextSizePx = mDeviceProfile.iconTextSizePx;
        mIconViewTextPadding = mDeviceProfile.iconDrawablePaddingPx;
    }


    public CleanWidgetView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CleanWidgetView(Context context) {
        this(context, null);
    }

    private void updateTextSize() {
        mIconTextSizePx = mDeviceProfile.iconTextSizePx;
        mIconName.setTextSize(TypedValue.COMPLEX_UNIT_PX, mIconTextSizePx);
    }

    private Drawable readDrawableByResName(Context context, String resName, String packageName) {
        Context themeContext;
        Resources res;
        try {
            themeContext = context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
            res = themeContext.getResources();
        } catch (Exception e) {
            return null;
        }
        int identifier = res.getIdentifier(resName, "drawable", packageName);
        if (identifier > 0) {
            return res.getDrawable(identifier);
        }
        return null;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mCleanIcon = (CleanIconView) findViewById(R.id.cleanIcon);
        mCircleView = (CleanCircleView) findViewById(R.id.cleanCircleView);
        mIconName = (CustomTextView) findViewById(R.id.widgetName);
        mIconName.setTextColor(mIconTextColor);

        updateTextSize();

        String themePackageName = IOSSettings.getString(mContext.getContentResolver(), IOSSettings.Launcher.LAUNCHER_THEME_PACKAGE, mContext.getPackageName());
        Drawable bgDrawable = null;
        /*
        if (!mContext.getPackageName().equals(themePackageName)) {
            bgDrawable = readDrawableByResName(mContext, "ic_app_background_01", themePackageName);
        }
        else {
            bgDrawable = mContext.getResources().getDrawable(R.drawable.ic_app_background_01);

        }

         */

        if (bgDrawable != null) {
            mCleanIcon.setBackground(bgDrawable);
        }

        LinearLayout.LayoutParams llp;
        llp = (LinearLayout.LayoutParams) mCleanIcon.getLayoutParams();

//        int margin = (int) (0.04f * mIconViewSize);
//        int iconSize = (int) (0.92f * mIconViewSize);
        int margin = (int) (0.00f * mIconViewSize);
        int iconSize = (int) (1.00f * mIconViewSize);
        llp.height = iconSize;
        llp.width = iconSize;
        llp.setMargins(margin, margin, margin, margin);
        mCleanIcon.setLayoutParams(llp);

        llp = (LinearLayout.LayoutParams) mIconName.getLayoutParams();
        llp.topMargin = mIconViewTextPadding;
        mIconName.setLayoutParams(llp);

        FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) mCircleView.getLayoutParams();
        int measureSpec = MeasureSpec.UNSPECIFIED;
        mIconName.measure(measureSpec, measureSpec);
        int iconNameHeight = mIconName.getMeasuredHeight();
        flp.bottomMargin = (mIconViewTextPadding + iconNameHeight) / 2;
        mCircleView.setLayoutParams(flp);

        mCleanAnimatorListener = new CleanAnimatorListener(this);
        mCircleView.setAnimtorListener(mCleanAnimatorListener);
        mCircleView.setWidgetView(this);
        mCleanIcon.setOnClickListener(this);


        mIconZoomOutAnimator = ValueAnimator.ofFloat(1.0F, 0.0F);
        mIconZoomOutAnimator.setInterpolator(new LinearInterpolator());
        mIconZoomOutAnimator.addUpdateListener(new CleanIconUpdateListener(mCleanIcon));
        mIconZoomOutAnimator.setDuration(200L);
        mIconZoomOutAnimator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                mIconName.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mCircleView.setVisibility(View.VISIBLE);
                mCleanIcon.setVisibility(View.GONE);

                mCircleView.start();
                executeCleanTask();
            }
        });

        mIconZoomInAnimator = ValueAnimator.ofFloat(0.0F, 1.0F);
        mIconZoomInAnimator.setInterpolator(new LinearInterpolator());
        mIconZoomInAnimator.addUpdateListener(new CleanIconUpdateListener(mCleanIcon));
        mIconZoomInAnimator.setDuration(200L);

        mIconZoomInAnimator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                mIconName.setVisibility(View.VISIBLE);
                mCleanIcon.setVisibility(View.VISIBLE);
                mCircleView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                refreshCleanIcon();
                showCleanResult();
            }
        });
    }


    public void postIconZoomOutAnimator() {
        if (mIconZoomOutAnimator == null) {
            return;
        }

        if (mIconZoomOutAnimator.isRunning()) {
            return;
        }

        post(new Runnable() {
            @Override
            public void run() {
                mIconZoomOutAnimator.start();
                invalidate();
            }
        });
    }


    public void postIconZoomInAnimator() {
        if (mIconZoomInAnimator == null) {
            return;
        }

        if (mIconZoomInAnimator.isRunning()) {
            return;
        }

        post(new Runnable() {
            @Override
            public void run() {
                mIconZoomInAnimator.start();
                invalidate();
            }
        });
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        filter.addAction(ACTION_CLEAN_WIDGET);

        ContextHelper.registerReceiver(mContext, mReceiver, filter);
        mContext.getContentResolver().registerContentObserver(IOSSettings.getUriFor(IOSSettings.Launcher.LAUNCHER_WORKSPACE_ICON_LABEL_COLOR), false, mObserver);
        mContext.getContentResolver().registerContentObserver(IOSSettings.getUriFor(IOSSettings.Launcher.LAUNCHER_APP_LABEL_SIZE_PX), false, mObserver);

        if (!mLauncherGuideManager.hasRunSpeedGuideActivity() && Partner.getBoolean(mContext, Partner.DEF_USER_GUIDE_ENABLE)) {
            mLauncher.getDragLayer().setBlockTouch(true);
            View widget = ((View) this.getParent());
            int offset = -DisplayUtil.dip2px(mContext, 50);
            ObjectAnimator up = ObjectAnimator.ofFloat(widget, View.TRANSLATION_Y, 0, offset);
            up.setDuration(300);


            ObjectAnimator down = ObjectAnimator.ofFloat(widget, View.TRANSLATION_Y, offset, 0);
            down.setInterpolator(new BounceInterpolator());
            down.setDuration(800);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playSequentially(up, down);
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mIconName.setVisibility(GONE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    onClick(null);
                }
            });

            animatorSet.setStartDelay(200);
            animatorSet.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        mContext.unregisterReceiver(mReceiver);
        mContext.getContentResolver().unregisterContentObserver(mObserver);
        hideResultDialog();
        super.onDetachedFromWindow();
    }

    @Override
    public void onClick(View v) {
        AnalyticsDelegate.onCleanWidgetEvent(mContext, UMEventConstants.CLEANWIDGET_CLICK);
        if (mInCleanProgcess) {
            return;
        }

        mInCleanProgcess = true;
        postIconZoomOutAnimator();
    }


    public void showCleanResult() {
        mInCleanProgcess = false;

        if (!mLauncherGuideManager.hasRunSpeedGuideActivity() && Partner.getBoolean(mContext, Partner.DEF_USER_GUIDE_ENABLE)) {
            mLauncherGuideManager.showSpeedGuideActivity(mLauncher);
            mLauncher.getDragLayer().setBlockTouch(false);
            return;
        }

//        boolean showAd = IOSAdManager.shouldShowAd(IOSAdConfig.ID_CLEAN_WIDGET);
//
//        if (!showAd) {
            showResultDialog(mCleanResult);
//            return;
//        }
//
//        if (mAdResponse != null) {
//            showResultDialog(mCleanResult, mAdResponse);
//            mAdResponse = null;
//            // Here record last ad show time;
//            IOSAdManager.afterShowAd(IOSAdConfig.ID_CLEAN_WIDGET);
//        } else {
//            loadAdvertise();
//            showResultDialog(mCleanResult, null);
//        }

    }


    private void refreshCleanIcon() {
        float usedMemoryPercent = CleanMaster.getUsedMemoryPercent(mContext.getApplicationContext());
        if (mCleanIcon != null) {
            mCleanIcon.setProgress(usedMemoryPercent);
        }
    }


    private void executeCleanTask() {
        new CleanTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
    }

    private class CleanTask extends AsyncTask<Void, Void, Long> {

        protected Long doInBackground(Void... params) {
            return CleanMaster.processClean(mContext.getApplicationContext());
        }

        protected void onPostExecute(Long result) {
            Log.w(TAG, "onPostExecute mCleanResult: " + mCleanResult);
            mCleanResult = result;
        }

    }


    private void showResultDialog(Long value) {
        if (CleanResultDialog.isResultShowing()) {
            return;
        }
        mResultDialog = new CleanResultDialog(mLauncher, value);
        mResultDialog.show();

//        if (response == null) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    hideResultDialog();
                }
            }, 2000);
//        }
    }


    private void hideResultDialog() {
        if (mResultDialog != null && mResultDialog.isShowing()) {
            mResultDialog.dismiss();
            mResultDialog = null;
        }

    }


    public void animateLauncherStartClean() {
        if (!mLauncherGuideManager.hasRunSpeedGuideActivity() && Partner.getBoolean(mContext, Partner.DEF_USER_GUIDE_ENABLE)) {
            int[] pos = new int[2];
            mCleanIcon.getLocationOnScreen(pos);
            int x = pos[0] + mCleanIcon.getMeasuredWidth() / 2;
            int y = pos[1] + mCleanIcon.getMeasuredHeight() / 2;
            mLauncher.animateLauncherStartClean(x, y);
        }
    }

    public void animateLauncherEndClean() {
        if (!mLauncherGuideManager.hasRunSpeedGuideActivity() && Partner.getBoolean(mContext, Partner.DEF_USER_GUIDE_ENABLE)) {
            mLauncher.animateLauncherEndClean();
        }
    }
}
