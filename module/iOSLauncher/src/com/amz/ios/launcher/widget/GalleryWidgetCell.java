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

    /**
     * Toạ độ MÀN HÌNH của điểm ngón tay tại ACTION_DOWN; -1 = chưa có.
     * Cần cho luồng "giữ preview để thêm widget" vì onLongClick(View) không mang theo toạ độ chạm.
     * Xem WidgetAppStyleCell để biết chi tiết.
     */
    private float mTouchDownRawX = -1f;
    private float mTouchDownRawY = -1f;

    /** @return toạ độ X màn hình nơi ngón tay chạm xuống, hoặc -1 nếu chưa có. */
    public float getTouchDownRawX() {
        return mTouchDownRawX;
    }

    /** @return toạ độ Y màn hình nơi ngón tay chạm xuống, hoặc -1 nếu chưa có. */
    public float getTouchDownRawY() {
        return mTouchDownRawY;
    }

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
        // [CĂN CHỈNH] 2f -> 2.3f: thẻ TO hơn nên phần trống quanh ảnh ít đi, các thẻ trông sát nhau
        // hơn mà không phải bóp lề (bóp lề nhiều sẽ làm lưới chật và bóng đổ bị cắt).
        mSize = (int) (mGrid.cellWidthPx * 2.3f);
        mPreviewWidth = mSize;
        mPreviewHeight = mSize;

        setOrientation(LinearLayout.VERTICAL);
        setClipToPadding(false);
        // Bóng toả RA NGOÀI mép ảnh; clipChildren mặc định true sẽ cắt cụt phần toả đó.
        setClipChildren(false);
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
        // Widget của APP NGOÀI -> vẽ ảnh nằm TRỌN trong khung thay vì cắt phần dưới.
        // Widget nội bộ giữ nguyên cách vẽ cũ. Xem WidgetImageView#setFitInsideBox.
        if (mWidgetPreview != null) {
            boolean isIOS = (mParcelable instanceof LauncherAppWidgetProviderInfo)
                    && ((LauncherAppWidgetProviderInfo) mParcelable).isIOSWidget;
            mWidgetPreview.setFitInsideBox(!isIOS);
            // Bóng mềm kiểu iOS ôm viền ảnh preview, bo góc khớp widget khi đặt ra màn hình.
            mWidgetPreview.setPreviewShadow(
                    getResources().getDimension(R.dimen.widget_round_corner));
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

    /**
     * [FIX] Giữ vào thẻ widget (đồng hồ/thời tiết/lịch...) ở màn đầu khay KHÔNG có tác dụng gì.
     *
     * Cùng nguyên nhân với {@link WidgetAppStyleCell}: khi widget hỗ trợ preview SỐNG,
     * {@link #addLivePreview} inflate widget THẬT vào trong thẻ; view con của widget nhận touch
     * trước và nuốt chuỗi sự kiện nên thẻ cha (nơi gắn OnLongClickListener) không đếm đủ thời gian
     * long-press. Chặn ngay tại thẻ: preview chỉ để nhìn, mọi cử chỉ thuộc về thẻ.
     */
    @Override
    public boolean onInterceptTouchEvent(android.view.MotionEvent ev) {
        if (mLivePreview != null) {
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    /**
     * [FIX] Giữ vào thẻ rồi kéo nhưng widget không nhấc ra được.
     *
     * Thẻ nằm trong khay cuộn dọc LỒNG trong SlidingUpPanelLayout (kéo đóng khay). Khi long-press vừa
     * nổ và người dùng bắt đầu DI TAY, các view cha thấy ngón tay dịch chuyển nên intercept để
     * cuộn/đóng khay -> thẻ nhận ACTION_CANCEL -> cử chỉ kéo chết ngay khi vừa bắt đầu.
     * Chặn cha intercept ĐÚNG LÚC long-press nổ; trước đó cuộn khay vẫn hoạt động bình thường.
     */
    @Override
    public boolean performLongClick() {
        android.view.ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
        return super.performLongClick();
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        // Ghi lại ĐIỂM NGÓN TAY để luồng "giữ để thêm widget" đặt widget đúng chỗ đang giữ.
        if (event.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) {
            mTouchDownRawX = event.getRawX();
            mTouchDownRawY = event.getRawY();
        }
        // Drag ĐÃ bắt đầu: nhả chuỗi touch cho DragLayer/DragController tiếp quản, nếu không
        // DragView không nhận ACTION_MOVE -> widget dính tại chỗ. Xem WidgetAppStyleCell.
        if (mLauncher != null && mLauncher.getDragController() != null
                && mLauncher.getDragController().isDragging()) {
            return false;
        }
        boolean handled = super.onTouchEvent(event);
        // LinearLayout mặc định trả false ở ACTION_DOWN khi không clickable -> mất các sự kiện sau,
        // long-press không bao giờ nổ. Có listener thì coi như đã xử lý để giữ trọn chuỗi touch.
        if (isClickable() || isLongClickable()) {
            return true;
        }
        return handled;
    }
}
