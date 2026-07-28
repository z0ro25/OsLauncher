package com.amz.ios.themeclub.ui.activity;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Settings;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.MotionEvent;
import android.view.View;

import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.ioslite.common.util.PackageUtil;
import com.amz.ios.ioslite.common.util.ToastUtil;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.base.BaseLocalActivity;
import com.amz.ios.themeclub.bean.LockscreenInfo;
import com.amz.ios.themeclub.util.IOSResources;
import com.amz.ios.themeclub.util.LockScreenUtils;

import java.util.ArrayList;
import java.util.List;

public class LocalLockScreenDetailActivity extends BaseLocalActivity<LockscreenInfo> {
    private BitmapDrawable mPreview;
    private String mPackageName;
    private String mPath;
    private String mTitle;
    private static final int REQUEST_CODE_WRITE_SETTINGS = 1;

    @Override
    protected LockscreenInfo handleIntent(Intent intent) {
        LockscreenInfo lockscreenInfo = new LockscreenInfo();
        if (intent != null) {
            mPackageName = intent.getStringExtra("lockscreenInfo_packageName");
            mPath = intent.getStringExtra("lockscreenInfo_path");
            mTitle = intent.getStringExtra("lockscreenInfo_title");
            lockscreenInfo.setPackageName(mPackageName);
            lockscreenInfo.setPackagePath(mPath);
            lockscreenInfo.setTitle(mTitle);
        }
        return lockscreenInfo;
    }

    @Override
    protected void handleThemeUninstalled(LockscreenInfo lockscreenInfo) {
        if (!PackageUtil.isAppInstalled(this, lockscreenInfo.getPackageName())) {
            ToastUtil.show(this, R.string.themeclub_lockscreen_has_uninstall);
            finish();
        }
    }

    @Override
    protected void showFullTheme(LockscreenInfo lockscreenInfo, int curIdx, Context context) {

    }

    @Override
    @TargetApi(23)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_WRITE_SETTINGS) {
            if (Settings.System.canWrite(this)) {
                DebugLog.w(TAG, "onActivityResult write settings granted");
                final LockscreenInfo lockscreenInfo = new LockscreenInfo(this, mTitle, mPackageName, mPath);
                LockScreenUtils.applyLockScreen(this, lockscreenInfo);
            }
        }
    }

    @Override
    protected void setupView(LockscreenInfo lockscreenInfo) {
        List<Bitmap> listViews = new ArrayList<>();
        listViews.add(drawableToBitmap(mPreview));
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
//        mRecyclerView.setLayoutManager(linearLayoutManager);
//        mGalleryAdapter = new GalleryAdapter(this, listViews);
//        mRecyclerView.setAdapter(mGalleryAdapter);
        mNameDetail1.setText(mTitle);
        mTitleText.setText(mTitle);
        mDeleteImage.setVisibility(View.VISIBLE);
        if (PackageUtil.isSystemApp(this, lockscreenInfo.getPackageName())) {
            mDeleteImage.setVisibility(View.GONE);
        }
    }

    @Override
    protected void apply(LockscreenInfo lockscreenInfo) {
        LockScreenUtils.applyLockScreen(this, lockscreenInfo);
    }

    @Override
    protected void unInstallApk(LockscreenInfo lockscreenInfo) {
        Intent uninstall_intent = new Intent();
        uninstall_intent.setAction(Intent.ACTION_DELETE);
        uninstall_intent.setData(Uri.parse("package:" + lockscreenInfo.getPackageName()));
        uninstall_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(uninstall_intent);
    }

    @Override
    protected void handleIntentData(LockscreenInfo lockscreenInfo) {
        IOSResources res = new IOSResources();
        mPreview = res.getThemePreview(this, mPackageName, mPath, IOSResources.THEME_PREVIEW_LOCKSCREEN);
        DebugLog.w(TAG, "===============handleIntentData:" + mPreview);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    @Override
    public void onLauncherSettingChanged(String key) {

    }

    @Override
    public void scrollLauncherToDefaultScreen(boolean animate) {

    }

    @Override
    public void startClockApp() {

    }

    @Override
    public void startCalendarApp() {

    }
}
