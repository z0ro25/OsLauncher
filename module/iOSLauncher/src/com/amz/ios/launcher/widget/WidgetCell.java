/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amz.ios.launcher.widget;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;

import com.amz.ios.launcher.DeviceProfile;
import com.amz.ios.launcher.InvariantDeviceProfile;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.LauncherAppWidgetProviderInfo;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.StylusEventHelper;
import com.amz.ios.launcher.WidgetPreviewLoader;
import com.amz.ios.launcher.WidgetPreviewLoader.PreviewLoadRequest;
import com.amz.ios.launcher.compat.AppWidgetManagerCompat;
import com.amz.ios.launcher.model.PackageItemInfo;
import com.amz.ios.launcher.model.WidgetItem;
import com.amz.ios.launcher.model.WidgetsModel;
import com.amz.ios.launcher.views.CustomTextView;

/**
 * Represents the individual cell of the widget inside the widget tray. The preview is drawn
 * horizontally centered, and scaled down if needed.
 * <p>
 * This view does not support padding. Since the image is scaled down to fit the view, padding will
 * further decrease the scaling factor. Drag-n-drop uses the view bounds for showing a smooth
 * transition from the view to drag view, so when adding padding support, DnD would need to
 * consider the appropriate scaling factor.
 */
public class WidgetCell extends FrameLayout implements IWidgetPreview {
    private static final String TAG = "WidgetCell";
    private static final boolean DEBUG = false;

    private static final int FADE_IN_DURATION_MS = 90;

    /**
     * Widget cell width is calculated by multiplying this factor to grid cell width.
     */
    public static final float WIDTH_SCALE = 2.6f;

    /**
     * Widget preview width is calculated by multiplying this factor to the widget cell width.
     */
    private static final float PREVIEW_SCALE = 0.8f;

    private int mPresetPreviewWidth;
    private int mPresetPreviewHeight;
    private int mPresetPreviewIconWidth;
    private int mPresetPreviewIconHeight;

    protected int mPresetPreviewSize;

    private WidgetImageView mWidgetImage;
    private CustomTextView mWidgetName;
    private CustomTextView mWidgetDims;

    private String mDimensionsFormatString;
    protected Object mInfo;

    private WidgetPreviewLoader mWidgetPreviewLoader;
    protected PreviewLoadRequest mActiveRequest;
    private StylusEventHelper mStylusEventHelper;

    protected Context mContext;

    public WidgetCell(Context context) {
        this(context, null);
    }

    public WidgetCell(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidgetCell(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final Resources r = context.getResources();
        mContext = context;
        mStylusEventHelper = new StylusEventHelper(this);

        mDimensionsFormatString = r.getString(R.string.widget_dims_format);
        setContainerWidth();
        setWillNotDraw(false);
        setClipToPadding(false);
        setAccessibilityDelegate(LauncherAppState.getInstance().getAccessibilityDelegate());
    }

    private void setContainerWidth() {
        DeviceProfile profile = LauncherAppState.getIDP(mContext).portraitProfile;
        mPresetPreviewWidth = (int) (1.6f * profile.widgetPreviewImageSizePx);
        mPresetPreviewIconWidth = profile.widgetPreviewImageSizePx;
        mPresetPreviewHeight = mPresetPreviewIconHeight = profile.widgetPreviewImageSizePx;

        mPresetPreviewSize = (int) (profile.cellWidthPx * WIDTH_SCALE * PREVIEW_SCALE);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mWidgetImage = (WidgetImageView) findViewById(R.id.widget_preview);
        mWidgetName = ((CustomTextView) findViewById(R.id.widget_name));
        mWidgetDims = ((CustomTextView) findViewById(R.id.widget_dims));
    }

    /**
     * Called to clear the view and free attached resources. (e.g., {@link Bitmap}
     */
    public void clear() {
        if (DEBUG) {
            Log.d(TAG, "reset called on:" + mWidgetName.getText());
        }
        mWidgetImage.animate().cancel();
        mWidgetImage.setBitmap(null);
        mWidgetName.setText(null);
        mWidgetDims.setText(null);

        if (mActiveRequest != null) {
            mActiveRequest.cleanup();
            mActiveRequest = null;
        }
    }

    public void applyFromCellItem(WidgetItem item, WidgetPreviewLoader loader) {
        mInfo = item;
        mWidgetName.setText(item.label);
        mWidgetDims.setText(getContext().getString(R.string.widget_dims_format,
                item.spanX, item.spanY));
        mWidgetDims.setContentDescription(getContext().getString(
                R.string.widget_accessible_dims_format, item.spanX, item.spanY));
        mWidgetPreviewLoader = loader;
    }

    public WidgetImageView getWidgetView() {
        return mWidgetImage;
    }

    /**
     * Apply the widget provider info to the view.
     */
    public void applyFromAppWidgetProviderInfo(LauncherAppWidgetProviderInfo info,
                                               WidgetPreviewLoader loader) {

        InvariantDeviceProfile profile =
                LauncherAppState.getInstance().getInvariantDeviceProfile();
        mInfo = info;
        // TODO(hyunyoungs): setup a cache for these labels.
        mWidgetName.setText(AppWidgetManagerCompat.getInstance(getContext()).loadLabel(info));
        int hSpan = Math.min(info.spanX, profile.numColumns);
        int vSpan = Math.min(info.spanY, profile.numRows);
        mWidgetDims.setText(String.format(mDimensionsFormatString, hSpan, vSpan));
        mWidgetPreviewLoader = loader;
    }

    /**
     * Apply the resolve info to the view.
     */
    public void applyFromResolveInfo(
            PackageManager pm, ResolveInfo info, WidgetPreviewLoader loader) {
        mInfo = info;
        CharSequence label = info.loadLabel(pm);
        mWidgetName.setText(label);
        mWidgetDims.setText(String.format(mDimensionsFormatString, 1, 1));
        mWidgetPreviewLoader = loader;
    }


    public void applyFromPackageItemInfo(PackageItemInfo info, WidgetsModel widgetsModel) {
        Bitmap bitmap = Bitmap.createScaledBitmap(info.iconBitmap, mPresetPreviewIconWidth, mPresetPreviewIconWidth, true);
        mWidgetImage.setBitmap(bitmap);
        mWidgetName.setText(info.title);
        mWidgetDims.setText("(" + widgetsModel.getSortedWidgets(info).size() + ")");
    }


    public int[] getPreviewSize() {
        int[] maxSize = new int[2];

        if (mInfo instanceof LauncherAppWidgetProviderInfo) {
            maxSize[0] = mPresetPreviewWidth;
            maxSize[1] = mPresetPreviewHeight;
        } else if (mInfo instanceof WidgetItem) {
            maxSize[0] = mPresetPreviewSize;
            maxSize[1] = mPresetPreviewSize;
        } else {
            maxSize[0] = mPresetPreviewIconWidth;
            maxSize[1] = mPresetPreviewIconWidth;
        }
        return maxSize;
    }

    public void applyPreview(Bitmap bitmap) {
        if (bitmap != null) {
            mWidgetImage.setBitmap(bitmap);
            mWidgetImage.setAlpha(0f);
            ViewPropertyAnimator anim = mWidgetImage.animate();
            anim.alpha(1.0f).setDuration(FADE_IN_DURATION_MS);
        }
    }

    public void ensurePreview() {
        if (mActiveRequest != null) {
            return;
        }
        int[] size = getPreviewSize();
        if (DEBUG) {
            Log.d(TAG, String.format("[tag=%s] ensurePreview (%d, %d):",
                    getTagToString(), size[0], size[1]));
        }
        mActiveRequest = mWidgetPreviewLoader.getPreview(mInfo, size[0], size[1], this);
    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean handled = super.onTouchEvent(ev);
        if (mStylusEventHelper.checkAndPerformStylusEvent(ev)) {
            return true;
        }
        return handled;
    }

    /**
     * Helper method to get the string info of the tag.
     */
    private String getTagToString() {
        if (getTag() instanceof PendingAddWidgetInfo ||
                getTag() instanceof PendingAddShortcutInfo) {
            return getTag().toString();
        }
        return "";
    }
}
