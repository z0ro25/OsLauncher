package com.amz.ios.launcher.widget;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Parcelable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
    /**
     * Khung chứa preview sống, dựng SẴN ở constructor và không bao giờ bị gỡ.
     * Xem giải thích về bất biến "cây view không đổi sau constructor" trong constructor.
     */
    FrameLayout mLiveHost;
    /**
     * Widget sống đã inflate nằm trong {@link #mLiveHost}, hoặc null nếu chưa dựng.
     * Chỉ được gán MỘT lần cho mỗi thẻ — thẻ có view type riêng nên chỉ nhận đúng một loại widget.
     */
    View mLivePreview;
    /** Provider của widget sống đang nằm trong khung, để biết có phải dựng lại hay không. */
    ComponentName mLiveProvider;
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
        // hơn mà không phải bóp lề (bóp lề nhiều sẽ làm lưới chật).
        mSize = (int) (mGrid.cellWidthPx * 2.3f);
        mPreviewWidth = mSize;
        mPreviewHeight = mSize;

        // [SỬA LỖI Ô TRỐNG] Dựng SẴN khung chứa preview sống ngay tại constructor.
        //
        // Trước đây addLivePreview() gọi addView()/removeView() trên chính thẻ này để nhét widget
        // sống vào, còn bind() thì gỡ ra. Đó là thay đổi CẤU TRÚC CÂY VIEW ở mỗi lần bind — mỗi lần
        // như vậy widget phải inflate + measure + layout lại từ đầu, nên khi RecyclerView tái dùng
        // thẻ lúc cuộn, ô trống rỗng cho tới khi vòng layout kế tiếp chạy xong. Riêng Photos còn
        // query MediaStore + decode ảnh nên chờ lâu thấy rõ.
        //
        // BẤT BIẾN TỪ ĐÂY: cây view của thẻ KHÔNG BAO GIỜ đổi sau khi constructor chạy xong.
        // Khung này luôn tồn tại; widget sống được inflate đúng MỘT lần vào trong nó (xem
        // ensurePreview) rồi ở nguyên đó suốt đời holder. Thẻ chỉ bật/tắt hiển thị giữa khung này
        // và ảnh tĩnh bằng setVisibility — thao tác rẻ, không đụng tới cấu trúc.
        //
        // Không sửa widget_app_style_cell.xml vì file đó dùng CHUNG với carousel level-2
        // (WidgetAppStyleCell); thêm khung ở đây bằng code nên không ảnh hưởng màn kia.
        mLiveHost = new FrameLayout(context);
        mLiveHost.setVisibility(View.GONE);
        // Khung đặt NGAY SAU ảnh tĩnh và dùng match_parent giống hệt ảnh tĩnh (xem
        // widget_app_style_cell.xml), nên dù thẻ ở dạng vuông 1 cột hay rộng 2 cột, khung vẫn tự
        // bám theo bề rộng thẻ do span của lưới quyết định — không phải chỉnh lại khi đổi tỉ lệ.
        LinearLayout.LayoutParams hostLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        hostLp.gravity = Gravity.CENTER;
        addView(mLiveHost, indexOfChild(mWidgetPreview) + 1, hostLp);

        setOrientation(LinearLayout.VERTICAL);
        setClipToPadding(false);
        setClipChildren(false);
        setFocusable(true);
    }

    /**
     * Gán 1 widget nổi bật vào thẻ (tag + tên + parcelable để nạp preview).
     *
     * CHỈ đổi dữ liệu, TUYỆT ĐỐI không thêm/gỡ view con. Việc dựng cây view đã xong ở constructor;
     * widget sống (nếu có) do ensurePreview() inflate đúng một lần rồi ở nguyên trong mLiveHost.
     */
    public void bind(LauncherAppWidgetProviderInfo info) {
        setTag(new PendingAddWidgetInfo(mLauncher, info, null));
        mParcelable = info;
        mWidgetName.setText(mAppWidgetManagerCompat.loadLabel(info));

        // Widget sống đang trong khung có đúng là widget lần này không? Thẻ có view type riêng nên
        // bình thường luôn đúng; so provider để phòng trường hợp danh sách featured đổi thứ tự.
        boolean liveMatches = mLivePreview != null && mLiveProvider != null
                && mLiveProvider.equals(info.provider);
        if (liveMatches) {
            // Giữ NGUYÊN widget sống đang hiển thị. Không gỡ, không dựng lại -> không có khoảng
            // trống chờ inflate/measure/layout khi cuộn qua rồi cuộn về.
            return;
        }

        // Sang widget KHÁC: dọn phần hiển thị về trạng thái đầu. Chỉ đụng tới nội dung và
        // visibility — cây view vẫn nguyên vẹn.
        if (mLivePreview != null) {
            mLiveHost.removeAllViews();   // dọn trong khung, KHÔNG gỡ chính khung khỏi thẻ
            mLivePreview = null;
            mLiveProvider = null;
        }
        mLiveHost.setVisibility(View.GONE);
        mWidgetPreview.setVisibility(View.VISIBLE);

        // Huỷ request ảnh đang chạy dở + trả cờ về null, nếu không chốt "mActiveRequest != null"
        // trong ensurePreview() sẽ chặn lần nạp mới. Theo đúng mẫu WidgetCell.reset() của AOSP.
        if (mActiveRequest != null) {
            mActiveRequest.cleanup();
            mActiveRequest = null;
        }
        mWidgetPreview.animate().cancel();
        mWidgetPreview.setBitmap(null);
        // cancel() có thể để alpha đang dở (0..1) của lần fade trước -> đặt lại 1.
        mWidgetPreview.setAlpha(1.0f);
    }

    /** Chuyển thẻ sang dạng rộng 2 cột: preview render tỉ lệ 2:1, chiều cao giữ nguyên ô vuông. */
    public void setWide() {
        mPreviewWidth = mSize * 2;
        mPreviewHeight = mSize;
    }


    /**
     * Trả thẻ về dạng vuông 1 cột (mặc định).
     * Cần có vì thẻ được RecyclerView tái dùng: một thẻ từng ở dạng rộng phải quay lại vuông được,
     * nếu không preview sẽ bị yêu cầu render sai tỉ lệ.
     */
    public void setNarrow() {
        mPreviewWidth = mSize;
        mPreviewHeight = mSize;
    }

    public void ensurePreview() {
        if (mParcelable == null) {
            return;
        }
        // Widget iOS: dựng preview SỐNG (inflate layout thật) thay ảnh tĩnh previewImage.
        if (LiveWidgetPreviewHelper.isLivePreviewSupported(mParcelable)) {
            // Widget sống đã nằm sẵn trong khung -> KHÔNG đụng tới. bind() đã dọn khung nếu lần này
            // là widget khác, nên tới đây mLivePreview != null nghĩa là đúng widget cần hiển thị.
            // Đây là chỗ quyết định "không dựng lại": inflate + measure + layout chỉ xảy ra một lần.
            if (mLivePreview == null) {
                addLivePreview((LauncherAppWidgetProviderInfo) mParcelable);
            }
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
        }
        mActiveRequest = mPreviewLoader.getPreview(mParcelable, mPreviewWidth, mPreviewHeight, this);
    }

    /**
     * Inflate widget sống vào KHUNG CÓ SẴN (mLiveHost) rồi ẩn ảnh tĩnh đi.
     *
     * Chỉ chạy MỘT lần cho mỗi widget: khung là view con cố định của thẻ, ta chỉ đổ widget vào
     * trong khung chứ không đụng tới cây view của thẻ. Nhờ vậy lần bind sau (cuộn qua rồi về)
     * không phải dựng lại gì -> không có ô trống chờ hiển thị.
     */
    private void addLivePreview(LauncherAppWidgetProviderInfo info) {
        mLiveHost.removeAllViews();
        mLivePreview = null;
        mLiveProvider = null;

        View host = LiveWidgetPreviewHelper.build(getContext(), info, mGrid);
        if (host == null) {
            // Dựng hụt -> quay về ảnh tĩnh, khung để trống và ẩn đi.
            mLiveHost.setVisibility(View.GONE);
            mWidgetPreview.setVisibility(View.VISIBLE);
            return;
        }
        mLiveHost.addView(host, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mLiveHost.setVisibility(View.VISIBLE);
        mWidgetPreview.setVisibility(View.GONE);
        mLivePreview = host;
        mLiveProvider = info.provider;
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
