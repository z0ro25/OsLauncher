package com.amz.ios.launcher;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.amz.ios.launcher.views.CustomTextView;


public class PreviewIcon extends CustomTextView {
    private int mIconSize;
    private int mTextColor;
    private int mTextSize;
    private int mTextFont;
    private Typeface mFace;

    private Drawable mIcon;
    private DeviceProfile mGrid;

    public PreviewIcon(Context context) {
        this(context, null, 0);
    }

    public PreviewIcon(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PreviewIcon(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        LauncherAppState app = LauncherAppState.getInstance();
//        mGrid = getResources().getConfiguration().orientation
//                == Configuration.ORIENTATION_LANDSCAPE ?
//                app.getInvariantDeviceProfile().landscapeProfile
//                : app.getInvariantDeviceProfile().portraitProfile;
        mGrid = app.getInvariantDeviceProfile().portraitProfile;

        mTextSize = mGrid.iconTextSizePx;
        mTextColor = mGrid.workspaceIconTextColor;
        mIconSize = mGrid.iconSizePx;
        setTextColor(mTextColor);
        setTextSizePx(mTextSize);
    }

    public void applyFromApplicationInfo(AppInfo info) {
        setIcon(createIconDrawable(info.iconBitmap), mIconSize);
        setText(info.title);
        setCompoundDrawablePadding(mGrid.iconDrawablePaddingPx);
    }


    public void setTextSizePx(float size) {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }

    @Override
    public void setTextColor(int color) {
        mTextColor = color;
        super.setTextColor(color);
    }

    /**
     * Sets the icon for this view based on the layout direction.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void setIcon(Drawable icon, int iconSize) {
        mIcon = icon;
        if (iconSize != -1) {
            mIcon.setBounds(0, 0, iconSize, iconSize);
        }

        setCompoundDrawables(null, mIcon, null, null);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void setIconSize(int size) {
        if (mIcon != null) {
            mIconSize = size;
            mIcon.setBounds(0, 0, mIconSize, mIconSize);
        }
        setCompoundDrawables(null, mIcon, null, null);
    }


    private FastBitmapDrawable createIconDrawable(Bitmap icon) {
        FastBitmapDrawable d = new FastBitmapDrawable(icon);
        d.setFilterBitmap(true);
        resizeIconDrawable(d);
        return d;
    }

    /**
     * Resizes an icon drawable to the correct icon size.
     */
    public void resizeIconDrawable(Drawable icon) {
        icon.setBounds(0, 0, mGrid.iconSizePx, mGrid.iconSizePx);
    }

}
