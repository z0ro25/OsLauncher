package com.amz.ios.launcher.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.media.midi.MidiManager;
import android.os.Parcelable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.amz.ios.launcher.DeviceProfile;
import com.amz.ios.launcher.ItemInfo;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.LauncherAppWidgetProviderInfo;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.StylusEventHelper;
import com.amz.ios.launcher.WidgetPreviewLoader;

public class WidgetAppStyleCell extends LinearLayout implements View.OnLayoutChangeListener, IWidgetPreview {

    public static final int[] WIDGET_CELL = {R.attr.hideWidgetText};

    WidgetImageView mWidgetPreview;
    TextView mWidgetName;
    TextView mWidgetDims;
    LinearLayout mWidgetText;
    StylusEventHelper mStylusEventHelper;
    String mWidgetDimenStrFormat;
    Launcher mLauncher;
    DeviceProfile mGrid;
    WidgetPreviewLoader mPreviewLoader;
    WidgetPreviewLoader.PreviewLoadRequest mActiveRequest;
    View mLivePreview;
    Parcelable mParcelable;
    int mSize;
    int mWidth;
    int mHeight;

    public WidgetAppStyleCell(Context context) {
        super(context);

        mLauncher = (Launcher) context;
        mGrid = mLauncher.getDeviceProfile();
        mPreviewLoader = LauncherAppState.getInstance().getWidgetCache();

        TypedArray a = context.obtainStyledAttributes(null,WIDGET_CELL,0,0);
        boolean z = a.getBoolean(0, false);
        a.recycle();

        LayoutInflater.from(context).inflate(R.layout.widget_app_style_cell,this,true);

        mWidgetPreview = findViewById(R.id.widget_preview);
        mWidgetName = findViewById(R.id.widget_name);
        mWidgetDims = findViewById(R.id.widget_dims);
        mWidgetText = findViewById(R.id.widget_text);

        if (z){
            mWidgetText.setVisibility(View.GONE);
        }

        // Màu nhãn theo Dark/Light (nền carousel đổi theo theme), bỏ shadow (set tại
        // code để KHÔNG đụng widget_app_style_cell.xml dùng chung với GalleryWidgetCell).
        if (mWidgetName != null) {
            mWidgetName.setTextColor(WidgetSheetTheme.textPrimary(context));
            mWidgetName.setShadowLayer(0f, 0f, 0f, 0);
        }
        if (mWidgetDims != null) {
            mWidgetDims.setTextColor(WidgetSheetTheme.TEXT_SECONDARY);
            mWidgetDims.setShadowLayer(0f, 0f, 0f, 0);
        }

        mStylusEventHelper = new StylusEventHelper(this);

        Resources resources = context.getResources();

        mWidgetDimenStrFormat = resources.getString(R.string.widget_dims_format);
        mSize = (int) (mGrid.cellWidthPx * 3.6f);

        mHeight = (int) (mSize * 0.8f);
//        else {
            mWidth = mHeight;
//        }

        setWillNotDraw(false);
        setClipToPadding(false);
        setAccessibilityDelegate(LauncherAppState.getInstance().getAccessibilityDelegate());

        LinearLayout.LayoutParams layoutParams;

//        if (getId() == R.id.widget_full_preview_item){
//            layoutParams = new LinearLayout.LayoutParams(-1,-1);
//        }
//        else {
            layoutParams = new LinearLayout.LayoutParams(mSize,mSize);
//        }

        int margin = z ? 0 : mGrid.edgeMarginPx;

        layoutParams.rightMargin = margin;
        layoutParams.leftMargin = margin;
        layoutParams.gravity = 17;

        setLayoutParams(layoutParams);
        setOrientation(LinearLayout.VERTICAL);
        setFocusable(true);
    }

    public void ensurePreview(){
        // Widget iOS: dựng preview SỐNG (inflate layout thật, đồng hồ tự chạy) thay ảnh tĩnh.
        if (LiveWidgetPreviewHelper.isLivePreviewSupported(mParcelable)) {
            addLivePreview((LauncherAppWidgetProviderInfo) mParcelable);
            return;
        }
        if (mActiveRequest != null) {
            return;
        }
        int[] previewSize = getPreviewSize();

        int width = previewSize[0];
        int height = previewSize[1];

        mActiveRequest = mPreviewLoader.getPreview(
                this.mParcelable,
                width,
                height,
                this
        );
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
        // Preview SỐNG là widget THẬT được inflate -> view con của nó sẽ nuốt touch. Việc chặn do
        // onInterceptTouchEvent() của cell lo (xem chú thích ở đó), không đụng vào bản thân host.
        addView(host, idx, lp);
        mLivePreview = host;
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        removeOnLayoutChangeListener(this);
        ensurePreview();
    }

    public int getActualItemWidth() {
        return Math.min(
                getPreviewSize()[0],
                ((ItemInfo) getTag()).spanX * mGrid.cellWidthPx);
    }

    public int[] getPreviewSize() {
        return new int[]{mWidth, mHeight};
    }

    /**
     * [FIX] Giữ vào ô preview ở bottom sheet chọn cỡ widget KHÔNG có tác dụng gì.
     *
     * NGUYÊN NHÂN: khi widget hỗ trợ preview SỐNG, {@link #addLivePreview} inflate widget THẬT vào
     * trong cell. Các view con của widget đó nhận touch trước và nuốt chuỗi sự kiện, nên cell cha
     * (nơi gắn OnLongClickListener) không bao giờ đếm đủ thời gian long-press.
     *
     * Chặn NGAY tại cell: giữ toàn bộ chuỗi touch ở đây, không phân phát xuống preview. Preview chỉ
     * để nhìn — mọi cử chỉ đều thuộc về cell (click = thêm widget, long-press = nhấc lên kéo).
     * Trả true ở ACTION_DOWN nên onTouchEvent của cell nhận trọn chuỗi và long-press hoạt động.
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mLivePreview != null) {
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    /**
     * [FIX] "Giữ vào preview rồi cố kéo ra nhưng không được."
     *
     * Cell nằm trong ViewPager (lật trang cỡ widget) LỒNG trong SlidingUpPanelLayout (kéo đóng sheet).
     * Khi long-press vừa nổ và người dùng bắt đầu DI TAY, hai view cha đó thấy ngón tay dịch chuyển
     * nên intercept để cuộn/lật/đóng sheet -> cell nhận ACTION_CANCEL -> cử chỉ kéo chết ngay khi vừa
     * bắt đầu, widget không nhấc ra được.
     *
     * Chặn cha intercept NGAY TẠI THỜI ĐIỂM long-press nổ (không phải từ ACTION_DOWN): trước đó vẫn
     * để cha xử lý bình thường nên VUỐT LẬT TRANG và KÉO ĐÓNG SHEET vẫn hoạt động như cũ — chỉ khi
     * người dùng đã giữ đủ lâu (tức có ý định kéo widget) mới giành quyền.
     */
    @Override
    public boolean performLongClick() {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
        return super.performLongClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Drag ĐÃ bắt đầu (long-press nổ -> startDrag): NHẢ chuỗi touch ra để DragLayer/DragController
        // tiếp quản việc kéo. Nếu cell cứ giữ (return true) thì DragView không nhận được ACTION_MOVE
        // -> widget "dính" tại chỗ, kéo không đi đâu cả.
        if (mLauncher != null && mLauncher.getDragController() != null
                && mLauncher.getDragController().isDragging()) {
            return false;
        }
        boolean touchEvent = super.onTouchEvent(event);
        if (mStylusEventHelper.checkAndPerformStylusEvent(event)) return true;
        // Cell phải TỰ nhận chuỗi touch để tính long-press. LinearLayout mặc định trả false ở
        // ACTION_DOWN khi không clickable -> mất luôn các sự kiện sau, long-press không bao giờ nổ.
        // Có listener (click/long-click) thì coi như đã xử lý.
        if (isClickable() || isLongClickable()) {
            return true;
        }
        return touchEvent;
    }

    @Override
    public void applyPreview(Bitmap preview) {
        if (preview != null){
            mWidgetPreview.setBitmap(preview);
            mWidgetPreview.setAlpha(0.0f);
            mWidgetPreview.animate().alpha(1.0f).setDuration(90L);
        }
    }

    public void setDims(int width, int height){
        String str = String.format(mWidgetDimenStrFormat,width, height);
        if (mWidgetDims != null){
            mWidgetDims.setText(str);
        }
    }
}
