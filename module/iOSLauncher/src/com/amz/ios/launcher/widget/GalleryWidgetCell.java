package com.amz.ios.launcher.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Parcelable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amz.ios.launcher.DeviceProfile;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.LauncherAppWidgetProviderInfo;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.WidgetPreviewLoader;
import com.amz.ios.launcher.compat.AppWidgetManagerCompat;

/**
 * Thẻ preview widget nhỏ dùng cho lưới 2 cột ở đầu khay Add Widget.
 * Chỉ mô phỏng phần preview của {@link WidgetAppStyleCell} (không đụng file gốc để
 * không ảnh hưởng carousel level-2). Tái dùng luôn layout {@code widget_app_style_cell}.
 */
public class GalleryWidgetCell extends LinearLayout implements IWidgetPreview {

    WidgetImageView mWidgetPreview;
    TextView mWidgetName;
    TextView mWidgetDims;

    Launcher mLauncher;
    DeviceProfile mGrid;
    WidgetPreviewLoader mPreviewLoader;
    WidgetPreviewLoader.PreviewLoadRequest mActiveRequest;
    View mLivePreview;
    AppWidgetManagerCompat mAppWidgetManagerCompat;
    Parcelable mParcelable;
    int mSize;
    // Kích thước preview: mặc định vuông; thẻ rộng 2 cột dùng tỉ lệ 2:1 (cao giữ nguyên).
    int mPreviewWidth;
    int mPreviewHeight;

    public GalleryWidgetCell(Context context) {
        super(context);

        mLauncher = (Launcher) context;
        mGrid = mLauncher.getDeviceProfile();
        mPreviewLoader = LauncherAppState.getInstance().getWidgetCache();
        mAppWidgetManagerCompat = AppWidgetManagerCompat.getInstance(mLauncher);

        LayoutInflater.from(context).inflate(R.layout.widget_app_style_cell, this, true);

        mWidgetPreview = findViewById(R.id.widget_preview);
        mWidgetName = findViewById(R.id.widget_name);
        mWidgetDims = findViewById(R.id.widget_dims);
        // Lưới gallery chỉ cần tên widget, ẩn phần kích thước "w × h".
        if (mWidgetDims != null) {
            mWidgetDims.setVisibility(View.GONE);
        }
        // Màu nhãn theo Dark/Light, bỏ shadow (set tại code để KHÔNG đụng
        // widget_app_style_cell.xml đang dùng chung với carousel level-2).
        if (mWidgetName != null) {
            mWidgetName.setTextColor(WidgetSheetTheme.textPrimary(context));
            mWidgetName.setShadowLayer(0f, 0f, 0f, 0);
        }

        // Kích thước preview vuông theo grid; WidgetImageView tự scale bitmap về khung.
        mSize = (int) (mGrid.cellWidthPx * 2f);
        mPreviewWidth = mSize;
        mPreviewHeight = mSize;

        setOrientation(LinearLayout.VERTICAL);
        setClipToPadding(false);
        setFocusable(true);
    }

    /** Gán 1 widget nổi bật vào thẻ (tag + tên + parcelable để nạp preview). */
    public void bind(LauncherAppWidgetProviderInfo info) {
        setTag(new PendingAddWidgetInfo(mLauncher, info, null));
        mParcelable = info;
        mWidgetName.setText(mAppWidgetManagerCompat.loadLabel(info));
        // Thẻ có thể bị tái sử dụng (RecyclerView): gỡ preview sống cũ, khôi phục khung tĩnh.
        if (mLivePreview != null) {
            removeView(mLivePreview);
            mLivePreview = null;
            mWidgetPreview.setVisibility(View.VISIBLE);
        }
    }

    /** Chuyển thẻ sang dạng rộng 2 cột: preview render tỉ lệ 2:1, chiều cao giữ nguyên ô vuông. */
    public void setWide() {
        mPreviewWidth = mSize * 2;
        mPreviewHeight = mSize;
    }

    public void ensurePreview() {
        if (mParcelable == null) {
            return;
        }
        // Widget iOS: dựng preview SỐNG (inflate layout thật) thay ảnh tĩnh previewImage.
        if (LiveWidgetPreviewHelper.isLivePreviewSupported(mParcelable)) {
            addLivePreview((LauncherAppWidgetProviderInfo) mParcelable);
            return;
        }
        if (mActiveRequest != null) {
            return;
        }
        mActiveRequest = mPreviewLoader.getPreview(mParcelable, mPreviewWidth, mPreviewHeight, this);
    }

    /** Thay WidgetImageView tĩnh bằng host chứa widget đã inflate (preview sống). */
    private void addLivePreview(LauncherAppWidgetProviderInfo info) {
        if (mLivePreview != null) {
            removeView(mLivePreview);
            mLivePreview = null;
        }
        View host = LiveWidgetPreviewHelper.build(getContext(), info, mGrid);
        if (host == null) {
            return;
        }
        int idx = indexOfChild(mWidgetPreview);
        mWidgetPreview.setVisibility(View.GONE);
        ViewGroup.LayoutParams src = mWidgetPreview.getLayoutParams();
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(src.width, src.height);
        lp.gravity = Gravity.CENTER;
        addView(host, idx, lp);
        mLivePreview = host;
    }

    @Override
    public void applyPreview(Bitmap preview) {
        if (preview != null) {
            mWidgetPreview.setBitmap(preview);
            mWidgetPreview.setAlpha(0.0f);
            mWidgetPreview.animate().alpha(1.0f).setDuration(90L);
        }
    }

    public int getCellSize() {
        return mSize;
    }
}
