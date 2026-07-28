package com.amz.ios.launcher.assembly;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.anim.AlphaUpdateListener;
import com.amz.ios.ioslite.common.launcher.Insettable;
import com.amz.ios.ioslite.common.launcher.LeftCustomContentCallbacks;
import com.amz.ios.ioslite.common.util.PackageUtil;
import com.amz.ios.ioslite.common.util.PermissionUtil;
import com.amz.ios.ioslite.common.util.Utilities;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.LauncherAnimUtils;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.views.CustomTextView;


public class LeftCustomContentView extends FrameLayout implements Insettable, LeftCustomContentCallbacks {
    private static final String TAG = "LeftCustomContentView";
    private Launcher mLauncher;
    private LeftCustomContentCallbacks mCallback;
    private View mBottomRect;
    private View mTopRect;
    private ViewGroup mContainerView;
    private View mLoadingContainer;
    private CustomTextView mTextView;

    public LeftCustomContentView(Context context) {
        this(context, null);
    }

    public LeftCustomContentView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LeftCustomContentView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = (Launcher) context;
    }

    private void showPermissoinRationDialog() {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(mLauncher, R.style.LauncherAlertDialog);
        builder.setTitle(R.string.sd_permission_title)
                .setMessage(R.string.sd_permission_ration)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PermissionUtil.requestPermissions(mLauncher, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builder.create().show();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mBottomRect = findViewById(R.id.launcher_left_bottomRect);
        mTopRect = findViewById(R.id.launcher_left_topRect);
        mContainerView = (ViewGroup) findViewById(R.id.launcher_left_content_container);
        mLoadingContainer = findViewById(R.id.launcher_left_loading_container);

        if (LeftCustomContentUtil.isNewsPageExists && !PackageUtil.isAppInstalled(mLauncher, LeftCustomContentUtil.NEWS_PAGE_PACKAGE_NAME)) {
            mTextView = (CustomTextView)findViewById(R.id.launcher_left_text);
            mTextView.setVisibility(View.INVISIBLE);
            View v = ((ViewStub) findViewById(R.id.news_page_tip)).inflate();
            ImageButton button = (ImageButton) v.findViewById(R.id.news_page_button);
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (PermissionUtil.hasPermissions(mLauncher, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        try {
                            Utilities.copyApkFromAssets(mLauncher, LeftCustomContentUtil.NEWS_PAGE_PACKAGE_NAME, "apps/" + LeftCustomContentUtil.NEWS_PAGE_APK_NAME);
                            Utilities.installNewApp(mLauncher, LeftCustomContentUtil.NEWS_PAGE_PACKAGE_NAME);
                        } catch (Exception e) {
                            //
                        }
                    } else {
                        showPermissoinRationDialog();
                    }
                }
            });
        }

        if (!Partner.getBoolean(getContext(), Partner.DEF_DESKTOP_LAZY_LOAD_NEWSPAGER_ENABLE, true)) {
            populateCustomContentContainer();
        }
    }


    public void populateCustomContentContainer() {
        final String customPkg = LeftCustomContentUtil.getCustomPkg();
        final int customLayout = LeftCustomContentUtil.getCustomLayout();
        View leftScreenPage = null;
        LeftCustomContentCallbacks callback = null;

        try {
            if (mLauncher.getPackageName().equals(customPkg)) {
                leftScreenPage = mLauncher.getInflater().inflate(customLayout, null);
                callback = (LeftCustomContentCallbacks) leftScreenPage;
            } else {
                Context newsPageContext = mLauncher.createPackageContext(customPkg, Context.CONTEXT_IGNORE_SECURITY | Context.CONTEXT_INCLUDE_CODE);
                LayoutInflater inflater = (LayoutInflater) newsPageContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                leftScreenPage = inflater.inflate(customLayout, null);
                callback = new CusCallbackCompactDroi(leftScreenPage);
            }
        } catch (Exception e) {
            Log.e(TAG, "populateCustomContentContainer fail", e);
        }


        if (leftScreenPage != null && callback != null) {
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mCallback = callback;
            mContainerView.removeAllViews();
            mContainerView.addView(leftScreenPage, lp);
            post(new Runnable() {
                @Override
                public void run() {
                    hideLoadingViewAndShowContainer();
                }
            });
        }
    }


    private void hideLoadingViewAndShowContainer() {
        AnimatorSet animatorSet = LauncherAnimUtils.createAnimatorSet();
        mLoadingContainer.setAlpha(1.0f);
        mContainerView.setAlpha(0.0f);
        ObjectAnimator load = LauncherAnimUtils.ofFloat(mLoadingContainer, "alpha", 0.0f);
        load.addListener(new AlphaUpdateListener(mLoadingContainer));
        ObjectAnimator container = LauncherAnimUtils.ofFloat(mContainerView, "alpha", 1.0f);
        container.addListener(new AlphaUpdateListener(mContainerView));
        animatorSet.playTogether(load, container);
        animatorSet.setDuration(250);
        animatorSet.start();
        onShow(false);
    }

    @Override
    public void setInsets(Rect insets) {
        ViewGroup.LayoutParams lp;

        lp = mBottomRect.getLayoutParams();
        lp.height = insets.bottom;
        mBottomRect.setLayoutParams(lp);
        mBottomRect.setBackgroundResource(R.drawable.launcher_bottom);
    }

    @Override
    public boolean isScrollingAllowed(float downX, float downY, float lastX, float lastY, float rawX, float rawY) {
        boolean allowScroll;
        if (mCallback != null) {
            allowScroll = mCallback.isScrollingAllowed(downX, downY, lastX, lastY, rawX, rawY);
            Log.i(TAG, "isScrollingAllowed  callback " + allowScroll);
            return allowScroll;
        }
        Log.i(TAG, "callback is null , not allow scroll");
        return false;
    }

    @Override
    public void onShow(boolean fromResume) {
        if (mCallback != null) {
            mCallback.onShow(fromResume);
        } else if (LeftCustomContentUtil.isNewsPageExists &&
                fromResume && LeftCustomContentUtil.isNewsPageInstall) {
            LeftCustomContentUtil.isNewsPageInstall = false;
            populateCustomContentContainer();
        }
    }

    @Override
    public void onHide() {
        if (mCallback != null) {
            mCallback.onHide();
        }
    }

    @Override
    public void onScrollProgressChanged(float progress) {
        if (mCallback != null) {
            mCallback.onScrollProgressChanged(progress);
        } else {
            if (progress == 1.0f) {
                populateCustomContentContainer();
            }
        }
    }

    @Override
    public boolean onBackPressed() {
        if (mCallback != null) {
            return mCallback.onBackPressed();
        }
        return false;
    }
}
