package com.amz.ios.launcher;

import android.os.Bundle;
import com.amz.ios.ioslite.common.CommonActivity;
import com.amz.ios.launcher.dynamicui.WallpaperColorInfo;
import com.amz.ios.launcher.util.SystemUiController;

import static com.amz.ios.launcher.util.SystemUiController.UI_STATE_OVERVIEW;

public class LauncherBaseActivity extends CommonActivity implements WallpaperColorInfo.OnChangeListener {

    private static final int ACTIVITY_STATE_STARTED = 1 << 0;
    private static final int ACTIVITY_STATE_RESUMED = 1 << 1;
    private static final int ACTIVITY_STATE_USER_ACTIVE = 1 << 2;

    private int mActivityFlags;

    private int mForceInvisible;

    private int mThemeRes = R.style.LauncherTheme;

    protected SystemUiController mSystemUiController;

    // Localize desktop: ép ngôn ngữ + tự recreate khi ngôn ngữ lưu đổi được xử lý ở base chung
    // CommonActivity (iOSLiteCommon) qua IosLocale — không cần làm lại ở đây.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Update theme
        WallpaperColorInfo wallpaperColorInfo = WallpaperColorInfo.getInstance(this);
        wallpaperColorInfo.addOnChangeListener(this);
        int themeRes = getThemeRes(wallpaperColorInfo);
        if (themeRes != mThemeRes) {
            mThemeRes = themeRes;
            setTheme(themeRes);
        }
    }

    @Override
    public void onExtractedColorsChanged(WallpaperColorInfo wallpaperColorInfo) {
        if (mThemeRes != getThemeRes(wallpaperColorInfo)) {
            recreate();
        }
    }

    protected int getThemeRes(WallpaperColorInfo wallpaperColorInfo) {
        if (wallpaperColorInfo.isDark()) {
            return wallpaperColorInfo.supportsDarkText() ?
                    R.style.LauncherThemeDark_DarKText : R.style.LauncherThemeDark;
        } else {
            return wallpaperColorInfo.supportsDarkText() ?
                    R.style.LauncherTheme_DarkText : R.style.LauncherTheme;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mActivityFlags |= ACTIVITY_STATE_STARTED;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mActivityFlags |= ACTIVITY_STATE_RESUMED | ACTIVITY_STATE_USER_ACTIVE;
    }

    @Override
    protected void onUserLeaveHint() {
        mActivityFlags &= ~ACTIVITY_STATE_USER_ACTIVE;
        super.onUserLeaveHint();
    }

    @Override
    protected void onStop() {
        mActivityFlags &= ~ACTIVITY_STATE_STARTED & ~ACTIVITY_STATE_USER_ACTIVE;
        mForceInvisible = 0;
        super.onStop();
        getSystemUiController().updateUiState(UI_STATE_OVERVIEW, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WallpaperColorInfo.getInstance(this).removeOnChangeListener(this);
    }

    public boolean isInMultiWindowModeCompat() {
        return Utilities.ATLEAST_NOUGAT && isInMultiWindowMode();
    }

    public SystemUiController getSystemUiController() {
        if (mSystemUiController == null) {
            mSystemUiController = new SystemUiController(getWindow());
        }
        return mSystemUiController;
    }

    @Override
    protected void onPause() {
        mActivityFlags &= ~ACTIVITY_STATE_RESUMED;
        super.onPause();
    }

    public boolean isStarted() {
        return (mActivityFlags & ACTIVITY_STATE_STARTED) != 0;
    }

    /**
     * isResumed in already defined as a hidden final method in Activity.java
     */
    public boolean hasBeenResumed() {
        return (mActivityFlags & ACTIVITY_STATE_RESUMED) != 0;
    }

    public boolean isUserActive() {
        return (mActivityFlags & ACTIVITY_STATE_USER_ACTIVE) != 0;
    }

    public void addForceInvisibleFlag(int flag) {
        mForceInvisible |= flag;
    }

    public void clearForceInvisibleFlag(int flag) {
        mForceInvisible &= ~flag;
    }

    /**
     * @return Wether this activity should be considered invisible regardless of actual visibility.
     */
    public boolean isForceInvisible() {
        return mForceInvisible != 0;
    }
}
