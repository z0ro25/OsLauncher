package com.amz.ios.themeclub.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.View;

import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.ioslite.common.launcher.LauncherRouter;
import com.amz.ios.ioslite.common.setting.IOSSettings;
import com.amz.ios.ioslite.common.util.PackageUtil;
import com.amz.ios.ioslite.common.util.ToastUtil;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.base.BaseLocalActivity;
import com.amz.ios.themeclub.bean.ThemeInfo;
import com.amz.ios.themeclub.util.ThemeUtils;

import java.util.ArrayList;

public class LocalThemeDetailActivity extends BaseLocalActivity<ThemeInfo> {
    private ArrayList<String> mThemePreviews;

    @Override
    protected ThemeInfo handleIntent(Intent intent) {
        ThemeInfo themeInfo = new ThemeInfo();
        if (intent != null) {
            themeInfo.title = intent.getStringExtra("themeInfo_title");
            themeInfo.description = intent.getStringExtra("themeInfo_description");
            themeInfo.packageName = intent.getStringExtra("themeInfo_packageName");
            themeInfo.themePath = intent.getStringExtra("themeInfo_themePath");
            DebugLog.w(TAG, "===============ThemeInfo:" + themeInfo.toString());
        }
        return themeInfo;
    }

    @Override
    protected void handleThemeUninstalled(ThemeInfo themeInfo) {
        if (!PackageUtil.isAppInstalled(this, themeInfo.packageName)) {
            ToastUtil.show(this, R.string.themeclub_theme_has_uninstall);
            finish();
        }
    }

    @Override
    protected void showFullTheme(ThemeInfo themeInfo, int curIdx, Context context) {
        Intent intent = new Intent(this, FullThemeActivity.class);
//        intent.setClass(context, FullThemeActivity.class);
        intent.putExtra("theme_info",  themeInfo);
        intent.putExtra("current_index", curIdx);
        startActivity(intent);
    }

    @Override
    protected void setupView(ThemeInfo themeInfo) {
        mTitleText.setText(themeInfo.title);
        mNameDetail1.setText(themeInfo.title);
        mIntroduceDetail.setText(themeInfo.description);
        mProgress.setVisibility(View.GONE);
        if (PackageUtil.isSystemApp(this, themeInfo.packageName)) {
            mDeleteImage.setVisibility(View.GONE);
        }
        displayGallery(themeInfo);
    }

    @Override
    protected void apply(ThemeInfo themeInfo) {
        if (mProgress != null) {
            mProgress.setVisibility(View.VISIBLE);
        }

        mApplyTheme.setClickable(false);
        LauncherRouter.launchToNewTheme(this, themeInfo.packageName);
        IOSSettings.setWallpaperId(this, -1);
//        ToastUtil.show(this, R.string.themeclub_set_theme_succeed);
    }

    @Override
    protected void unInstallApk(ThemeInfo themeInfo) {
        Intent uninstall_intent = new Intent();
        uninstall_intent.setAction(Intent.ACTION_DELETE);
        uninstall_intent.setData(Uri.parse("package:" + themeInfo.packageName));
        uninstall_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(uninstall_intent);
    }

    @Override
    protected void handleIntentData(ThemeInfo themeInfo) {
        mThemePreviews = ThemeUtils.getPicList(this, themeInfo.packageName);
    }

    private void displayGallery(ThemeInfo themeInfo) {
        BitmapDrawable mDrawable;
        if (listViews == null)
            listViews = new ArrayList<Bitmap>();
        else
            listViews.clear();

        for (int i = 0; i < mThemePreviews.size(); i++) {
            mDrawable = ThemeUtils.getThemePreview(this, themeInfo.packageName, themeInfo.themePath, mThemePreviews.get(i));
            if (mDrawable != null) {
                listViews.add(mDrawable.getBitmap());
            }
        }

        mPagedView.setViews(listViews, 0);

//        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
//        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
//        mRecyclerView.setLayoutManager(linearLayoutManager);
//        mGalleryAdapter = new GalleryAdapter(this, listViews);
//        mRecyclerView.setAdapter(mGalleryAdapter);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    @Override
    public void onLauncherSettingChanged(String key) {}

    @Override
    public void scrollLauncherToDefaultScreen(boolean animate) {}

    @Override
    public void startClockApp() {}

    @Override
    public void startCalendarApp() {}
}
