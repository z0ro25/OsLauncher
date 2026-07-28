package com.amz.ios.launcher;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;

import static com.amz.ios.launcher.util.SystemUiController.FLAG_DARK_NAV;
import static com.amz.ios.launcher.util.SystemUiController.UI_STATE_ROOT_VIEW;

public class LauncherWorkspaceRootView extends InsettableFrameLayout {
    private Launcher mLauncher;
    private final Rect mConsumedInsets = new Rect();
    private Rect mInsets = new Rect();

    public LauncherWorkspaceRootView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLauncher = Launcher.getLauncher(context);
    }

    @Override
    protected boolean fitSystemWindows(Rect insets) {
        mConsumedInsets.setEmpty();
        boolean drawInsetBar = false;
        if (mLauncher.isInMultiWindowModeCompat()
                && (insets.left > 0 || insets.right > 0 || insets.bottom > 0)) {
            mConsumedInsets.left = insets.left;
            mConsumedInsets.right = insets.right;
            mConsumedInsets.bottom = insets.bottom;
            insets = new Rect(0, insets.top, 0, 0);
            drawInsetBar = true;
        } else  if ((insets.right > 0 || insets.left > 0) &&
                (!Utilities.ATLEAST_MARSHMALLOW ||
                        getContext().getSystemService(ActivityManager.class).isLowRamDevice())) {
            mConsumedInsets.left = insets.left;
            mConsumedInsets.right = insets.right;
            insets = new Rect(0, insets.top, 0, insets.bottom);
            drawInsetBar = true;
        }
        mLauncher.getSystemUiController().updateUiState(
                UI_STATE_ROOT_VIEW, drawInsetBar ? FLAG_DARK_NAV : 0);
        mInsets.set(insets.left, Math.max(insets.top, mInsets.top), insets.right, insets.bottom);
        setInsets(mInsets);
        return true; // I'll take it from here
    }

    public Rect getInsets() {
        return mInsets;
    }
}
