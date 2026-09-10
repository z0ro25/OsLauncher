package com.truongnt.ios.launcher.widget;

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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.truongnt.ios.launcher.DeviceProfile;
import com.truongnt.ios.launcher.ItemInfo;
import com.truongnt.ios.launcher.Launcher;
import com.truongnt.ios.launcher.LauncherAppState;
import com.truongnt.ios.launcher.LauncherAppWidgetProviderInfo;
import com.truongnt.ios.launcher.R;
import com.truongnt.ios.launcher.StylusEventHelper;
import com.truongnt.ios.launcher.WidgetPreviewLoader;

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
    /**
     * Khung chứa preview sống, dựng SẴN ở constructor và KHÔNG BAO GIỜ bị gỡ.
     *
     * BẤT BIẾN: cây view của cell không đổi sau khi constructor chạy xong — widget sống chỉ được
     * đổ VÀO TRONG khung này, việc bật/tắt hiển thị làm bằng setVisibility. Xem addLivePreview().
     */
    FrameLayout mLiveHost;
    View mLivePreview;
    Parcelable mParcelable;
    /**
     * Đã thử dựng preview sống cho widget iOS của cell này chưa (thành công hoặc rơi về ảnh tĩnh).
     * Chặn dựng lặp sau khi ViewPager re-instantiate cell nhiều lần.
     */
    private boolean mLiveBuildAttempted;
    int mSize;
    int mWidth;
    int mHeight;

    /**
     * Toạ độ MÀN HÌNH của điểm ngón tay tại ACTION_DOWN; -1 = chưa có.
     * Dùng cho luồng "giữ preview để thêm widget": cần biết người dùng đang giữ ở ĐÂU để đặt widget
     * đúng chỗ đó. {@code OnLongClickListener.onLongClick(View)} KHÔNG mang theo toạ độ chạm, nên
     * phải tự lưu tại {@link #onTouchEvent(MotionEvent)} — nơi duy nhất còn thấy MotionEvent.
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

        // [SỬA LỖI Ô TRỐNG TRÊN ANDROID 9] Dựng SẴN khung chứa preview sống ngay tại constructor,
        // theo đúng cách GalleryWidgetCell (màn 1) đang làm — cơ chế đó đã chạy tốt trên chính máy
        // Android 9 bị lỗi này.
        //
        // Trước đây addLivePreview() gọi addView() để chèn host vào cell tại thời điểm ensurePreview(),
        // mà ensurePreview() lại chạy trong setData() TRƯỚC khi ViewPager gắn cell vào cây view. Host
        // vì thế được dựng khi cell còn rời, phải trông chờ ViewTreeObserver tạm được gộp lúc attach
        // — điều không xảy ra trên Android 9, nên widget không bao giờ được áp scale và ô trông trống.
        //
        // Khung dựng ở đây luôn tồn tại; widget chỉ được đổ VÀO TRONG khung. Không sửa
        // widget_app_style_cell.xml vì file đó dùng CHUNG với GalleryWidgetCell.
        mLiveHost = new FrameLayout(context);
        mLiveHost.setVisibility(View.GONE);
        // Cell là LinearLayout DỌC và mWidgetPreview (ảnh tĩnh) đã khai báo height=match_parent
        // trong widget_app_style_cell.xml. Nếu khung này cũng match_parent thì view thứ hai bị đẩy
        // xuống với chiều cao 0 -> preview sống không có chỗ vẽ -> ô TRẮNG.
        // Dùng height=0 + weight=1 để khung luôn nhận phần chiều cao còn lại của cell, không phụ
        // thuộc việc ảnh tĩnh đang hiện hay đã ẩn.
        LinearLayout.LayoutParams hostLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        hostLp.gravity = Gravity.CENTER;
        addView(mLiveHost, indexOfChild(mWidgetPreview) + 1, hostLp);

        Resources resources = context.getResources();

        mWidgetDimenStrFormat = resources.getString(R.string.widget_dims_format);
        // [CĂN CHỈNH] 3.6f -> 4.2f: khung ô preview to hơn để ảnh widget hiển thị lớn, dễ nhìn.
        // Đây là TRẦN kích thước preview (WidgetImageView thu ảnh cho vừa khung này).
        mSize = (int) (mGrid.cellWidthPx * 4.2f);

        mHeight = (int) (mSize * 0.8f);
//        else {
            mWidth = mHeight;
//        }

        setWillNotDraw(false);
        setClipToPadding(false);
        setClipChildren(false);
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
            // KHÔNG dựng ngay nếu cell chưa có kích thước thật (setData chạy khi sheet còn chưa
            // hiện, cell 0x0) — inflate vào ô 0x0 rồi trông chờ relayout sau chính là nguồn gốc ô
            // trống trên Android 9. Việc dựng được chuyển cho maybeBuildLivePreview(), kích hoạt
            // mỗi khi cell có kích thước thật + attach (xem onSizeChanged/onAttachedToWindow).
            maybeBuildLivePreview();
            return;
        }
        if (mActiveRequest != null) {
            return;
        }
        // Widget của APP NGOÀI -> vẽ ảnh NẰM TRỌN trong khung thay vì cắt bớt phần dưới.
        // Widget nội bộ (isIOSWidget) giữ NGUYÊN cách vẽ cũ. Xem WidgetImageView#setFitInsideBox.
        if (mWidgetPreview != null) {
            boolean isIOS = (mParcelable instanceof LauncherAppWidgetProviderInfo)
                    && ((LauncherAppWidgetProviderInfo) mParcelable).isIOSWidget;
            mWidgetPreview.setFitInsideBox(!isIOS);
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

    /**
     * Dựng preview sống khi cell THẬT SỰ sẵn sàng: đã có kích thước thật và đã gắn vào cây view.
     *
     * Gọi lại an toàn nhiều lần (mỗi lần cell có kích thước/attach lại) vì có cờ
     * {@link #mLiveBuildAttempted} chặn dựng trùng. KHÔNG dựng khi cell còn 0x0.
     */
    private void maybeBuildLivePreview() {
        if (mLiveBuildAttempted) {
            return;
        }
        if (!(mParcelable instanceof LauncherAppWidgetProviderInfo)) {
            return;
        }
        LauncherAppWidgetProviderInfo info = (LauncherAppWidgetProviderInfo) mParcelable;
        if (!LiveWidgetPreviewHelper.isLivePreviewSupported(info)) {
            return;
        }
        if (getWidth() <= 0 || getHeight() <= 0 || !isAttachedToWindow()) {
            return;
        }
        addLivePreview(info, getWidth(), getHeight());
    }

    /**
     * Nạp preview NGAY với kích thước khung biết trước, không chờ cell được layout.
     *
     * Dùng khi adapter bind: màn chọn size đã biết khung rộng/cao bao nhiêu, nên không cần đợi
     * vòng đo nào — đây là thứ khiến trước đây phải vuốt qua lại preview mới hiện.
     *
     * Widget iOS -> preview SỐNG; mọi trường hợp còn lại (widget APP NGOÀI, shortcut) -> ảnh tĩnh
     * qua {@link #ensurePreview()}. Trước đây các nhánh đó return trắng nên widget app ngoài KHÔNG
     * hiển thị gì ở màn chọn size.
     */
    void buildLivePreviewNow(int boxW, int boxH) {
        if (mLiveBuildAttempted) {
            return;
        }
        boolean live = (mParcelable instanceof LauncherAppWidgetProviderInfo)
                && LiveWidgetPreviewHelper.isLivePreviewSupported(mParcelable);
        if (!live || boxW <= 0 || boxH <= 0) {
            // Không phải widget iOS (hoặc chưa biết kích thước khung) -> đi đường ảnh tĩnh.
            ensurePreview();
            return;
        }
        addLivePreview((LauncherAppWidgetProviderInfo) mParcelable, boxW, boxH);
    }

    /**
     * Cell được ViewPager cấp kích thước THẬT lần đầu (sheet hiện, pager layout trang). Đây là mốc
     * an toàn để dựng preview sống. Post() sang vòng layout kế để không inflate/addView ngay giữa
     * layout pass của chính cell.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        if (w <= 0 || h <= 0 || mLiveBuildAttempted) {
            return;
        }
        post(new Runnable() {
            @Override
            public void run() {
                maybeBuildLivePreview();
            }
        });
    }

    /**
     * Chốt chặn cuối: mỗi vòng layout của cell đều thử dựng preview nếu chưa dựng được.
     *
     * Cần vì các mốc kia (onSizeChanged / onAttachedToWindow / lúc adapter bind) đều có thể xảy ra
     * khi cell chưa có kích thước. onLayout thì luôn nổ ĐÚNG lúc cell vừa được cấp kích thước thật.
     * Cờ mLiveBuildAttempted chặn dựng lặp nên gọi mỗi vòng layout là an toàn.
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (mLiveBuildAttempted || r - l <= 0 || b - t <= 0) {
            return;
        }
        post(new Runnable() {
            @Override
            public void run() {
                maybeBuildLivePreview();
            }
        });
    }

    /**
     * Cell vừa được gắn lại vào cây (ViewPager re-instantiate sau notifyDataSetChanged của
     * forcePagerRelayout). Lúc này cell có thể đã có kích thước thật nhưng onSizeChanged sẽ không
     * nổ lại (kích thước không đổi) -> phải nhắc maybeBuildLivePreview() ở đây.
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mLiveBuildAttempted) {
            return;
        }
        post(new Runnable() {
            @Override
            public void run() {
                maybeBuildLivePreview();
            }
        });
    }

    /**
     * Đổ widget đã inflate vào KHUNG CÓ SẴN (mLiveHost) rồi ẩn ảnh tĩnh đi.
     *
     * Dùng {@code buildForSizeSheet} chứ KHÔNG dùng {@code build}: bản cho sheet này tự áp scale ở
     * onLayout của chính host — mốc nổ đúng khi sheet hiện ra và cell có kích thước thật (xem
     * chú thích trong constructor và javadoc của buildForSizeSheet).
     *
     * Cây view của cell KHÔNG đổi: khung đã dựng ở constructor, đây chỉ đổ nội dung vào trong.
     */
    private void addLivePreview(LauncherAppWidgetProviderInfo info, int boxW, int boxH) {
        mLiveBuildAttempted = true;   // đã thử (thành công hay rơi về ảnh tĩnh) -> không dựng lại
        mLiveHost.removeAllViews();
        mLivePreview = null;

        View host = LiveWidgetPreviewHelper.buildForSizeSheet(
                getContext(), info, mGrid, boxW, boxH);
        if (host == null) {
            // Dựng hụt -> quay về ảnh tĩnh thay vì để ô TRỐNG. Trước đây nhánh này return luôn nên
            // bất kỳ trục trặc nào ở khâu dựng preview sống cũng thành ô trống không lối thoát.
            mLiveHost.setVisibility(View.GONE);
            mWidgetPreview.setVisibility(View.VISIBLE);
            loadStaticPreview();
            return;
        }
        // Preview SỐNG là widget THẬT được inflate -> view con của nó sẽ nuốt touch. Việc chặn do
        // onInterceptTouchEvent() của cell lo (xem chú thích ở đó), không đụng vào bản thân host.
        mLiveHost.addView(host, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mLiveHost.setVisibility(View.VISIBLE);
        mWidgetPreview.setVisibility(View.GONE);
        mLivePreview = host;
    }

    /** Nạp ảnh preview tĩnh (đường dự phòng khi preview sống dựng hụt). */
    private void loadStaticPreview() {
        if (mActiveRequest != null) {
            return;
        }
        int[] previewSize = getPreviewSize();
        mActiveRequest = mPreviewLoader.getPreview(mParcelable, previewSize[0], previewSize[1], this);
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
        // Ghi lại ĐIỂM NGÓN TAY trên màn hình để luồng "giữ để thêm widget" biết đặt widget ở đâu.
        // OnLongClickListener.onLongClick(View) KHÔNG mang theo toạ độ chạm, mà đây là nơi duy nhất
        // còn thấy MotionEvent -> phải lưu tại ACTION_DOWN. Xem getTouchDownRawX/Y().
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mTouchDownRawX = event.getRawX();
            mTouchDownRawY = event.getRawY();
        }
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
