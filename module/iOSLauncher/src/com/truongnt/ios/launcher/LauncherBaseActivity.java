package com.truongnt.ios.launcher;

import android.os.Bundle;
import com.truongnt.ios.ioslite.common.CommonFragmentActivity;
import com.truongnt.ios.launcher.dynamicui.WallpaperColorInfo;
import com.truongnt.ios.launcher.util.SystemUiController;

import static com.truongnt.ios.launcher.util.SystemUiController.UI_STATE_OVERVIEW;

// Kế thừa CommonFragmentActivity vì SDK eztech chỉ show inter/native từ FragmentActivity.
// Cố ý không đổi CommonActivity: DynamicVirtualEntry/SettingsActivity cũng dùng nó.
public class LauncherBaseActivity extends CommonFragmentActivity implements WallpaperColorInfo.OnChangeListener {

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

        // Update theme (no-op nếu lớp con đã gọi applyWallpaperTheme() trước khi chạm window).
        WallpaperColorInfo wallpaperColorInfo = WallpaperColorInfo.getInstance(this);
        wallpaperColorInfo.addOnChangeListener(this);
        applyWallpaperTheme();
    }

    // Phải chạy TRƯỚC getWindow().getDecorView(): setTheme sau khi decor đã dựng làm decor lệch theme
    // -> "Window couldn't find content container view" / drag_layer null khi recreate do đổi nền.
    protected void applyWallpaperTheme() {
        int themeRes = getThemeRes(WallpaperColorInfo.getInstance(this));
        if (themeRes != mThemeRes) {
            mThemeRes = themeRes;
            setTheme(themeRes);
        }
    }

    // Màu nền đổi đúng lúc hệ thống đang thay overlay Material You (assets frro): recreate ngay lúc
    // đó dựng lại window/layout trên resource dở dang -> crash "content container" / drag_layer null.
    // Nên chỉ đánh dấu, đợi lần onStart kế tiếp (overlay đã xong) mới dựng lại theo theme mới.
    private boolean mThemeRecreatePending;
    private final android.os.Handler mUiHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    @Override
    public void onExtractedColorsChanged(WallpaperColorInfo wallpaperColorInfo) {
        if (mThemeRes != getThemeRes(wallpaperColorInfo)) {
            mThemeRecreatePending = true;
            if (!isStarted()) return;
            // Đang hiển thị: hoãn một nhịp cho hệ thống áp xong overlay mới. Handler thay vì
            // getDecorView().post(): decor hỏng thì getDecorView() ném lại "content container".
            mUiHandler.postDelayed(this::recreateForPendingTheme, THEME_RECREATE_DELAY_MS);
        }
    }

    private void recreateForPendingTheme() {
        if (!mThemeRecreatePending || isFinishing() || isDestroyed()) return;
        if (mThemeRes == getThemeRes(WallpaperColorInfo.getInstance(this))) {
            mThemeRecreatePending = false;
            return;
        }
        mThemeRecreatePending = false;
        recreate();
    }

    private static final long THEME_RECREATE_DELAY_MS = 1500L;

    protected int getAppliedThemeRes() {
        return mThemeRes;
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
        if (mThemeRecreatePending) {
            // Đổi nền khi desktop ẩn (vd từ màn Themes): quay lại desktop mới dựng theo theme mới.
            mUiHandler.post(this::recreateForPendingTheme);
        }
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
        // Decor chưa dựng được (onCreate tự cứu do lỗi window) thì không đụng: getDecorView() sẽ ném lại.
        if (getWindow().peekDecorView() == null) return;
        getSystemUiController().updateUiState(UI_STATE_OVERVIEW, 0);
    }

    @Override
    protected void onDestroy() {
        mUiHandler.removeCallbacksAndMessages(null);
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
