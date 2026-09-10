package com.amz.ios.launcher.widget;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.amz.ios.ioslite.common.setting.IOSSettings;
import com.amz.ios.launcher.DeviceProfile;
import com.amz.ios.launcher.InvariantDeviceProfile;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.LauncherAppWidgetProviderInfo;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.WidgetPreviewLoader;
import com.amz.ios.launcher.compat.AppWidgetManagerCompat;
import com.amz.ios.launcher.slideup.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.List;

public class SlidingUpWidgetsCellAppStyle extends SlidingUpPanelLayout implements View.OnClickListener {

    Launcher mLauncher;
    WidgetPreviewLoader mWidgetPreviewLoader;
    /**
     * Carousel cỡ widget. ĐÃ ĐỔI ViewPager -> RecyclerView (cuộn ngang + PagerSnapHelper):
     * ViewPager chỉ dựng trang từ onMeasure, mà ở màn này setAdapter chạy lúc sheet chưa hiện và
     * trên Android 9 không có traversal nào chạm tới nó -> không bao giờ có trang -> preview trống.
     * Giữ NGUYÊN tên biến/ID để các chỗ khác (WidgetsContainerView) không phải sửa theo.
     */
    RecyclerView mWidgetViewPager;
    /** Snap từng trang như ViewPager (mỗi lần vuốt sang đúng 1 cỡ widget). */
    PagerSnapHelper mSnapHelper;
    IAddWidgetListener mOnAddWidgetBtnClickListener;
    AppCompatButton mAddWidgetBtn;
    AppWidgetManagerCompat mAppWidgetManagerCompat;
    PackageManager mPackageManager;
    WidgetAppStyleRecyclerAdapter mStyleAdapter;

    // Header + tiêu đề + mô tả + chấm trang: đổi theo trang khi vuốt (xem updatePage).
    ImageView mHeaderIcon;
    TextView mHeaderName;
    TextView mTitle;
    TextView mDesc;
    ImageView mCloseBtn;
    LinearLayout mDots;

    public interface IAddWidgetListener {

    }

    public SlidingUpWidgetsCellAppStyle(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLauncher = (Launcher) context;
        mAppWidgetManagerCompat = AppWidgetManagerCompat.getInstance(mLauncher);
        mPackageManager = context.getPackageManager();
    }

    public WidgetPreviewLoader getWidgetPreviewLoader(){
        if (mWidgetPreviewLoader == null){
            mWidgetPreviewLoader = LauncherAppState.getInstance().getWidgetCache();
        }
        return mWidgetPreviewLoader;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setUpView();
        setListeners();
        setAdapter();
    }

    void setUpView(){
        mAddWidgetBtn = findViewById(R.id.add_button_widgets_app_style);
        mWidgetViewPager = findViewById(R.id.view_pager_widgets_scroll_container);
        mHeaderIcon = findViewById(R.id.widget_sheet_icon);
        mHeaderName = findViewById(R.id.widget_sheet_header_name);
        mTitle = findViewById(R.id.widget_sheet_title);
        mDesc = findViewById(R.id.widget_sheet_desc);
        mCloseBtn = findViewById(R.id.widget_sheet_close);
        mDots = findViewById(R.id.widget_sheet_dots);

        // Carousel ngang + snap từng trang (thay ViewPager cũ).
        if (mWidgetViewPager != null) {
            mWidgetViewPager.setLayoutManager(
                    new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            mWidgetViewPager.setHasFixedSize(true);
            // Giữ đủ mọi cỡ widget trong bộ nhớ: danh sách chỉ vài item, và preview sống dựng lại
            // tốn kém -> không cho RecyclerView huỷ view khi cuộn qua, tránh cảnh "thiếu loại"
            // hoặc phải chờ dựng lại khi vuốt về.
            mWidgetViewPager.setItemViewCacheSize(16);
            mSnapHelper = new PagerSnapHelper();
            mSnapHelper.attachToRecyclerView(mWidgetViewPager);
        }
        applySheetTheme();
    }

    /** Áp nền + màu chữ theo Dark/Light (đọc runtime từ prefs theme). */
    private void applySheetTheme() {
        View bg = findViewById(R.id.widget_sheet_bg);
        if (bg != null) {
            bg.setBackgroundResource(WidgetSheetTheme.sheetBackgroundRes(getContext()));
        }
        int primary = WidgetSheetTheme.textPrimary(getContext());
        if (mHeaderName != null) mHeaderName.setTextColor(primary);
        if (mTitle != null) mTitle.setTextColor(primary);
        if (mDesc != null) mDesc.setTextColor(WidgetSheetTheme.TEXT_SECONDARY);
    }

    void setListeners(){
        // Tắt nền đen mờ như khay widget (sheet 1) — xem WidgetsContainerView.setListeners().
        // Sheet này chồng LÊN sheet 1 nên nếu còn phủ mờ sẽ thành 2 lớp đen chồng nhau.
        setCoveredFadeColor(0);
        mAddWidgetBtn.setOnClickListener(this);
        if (mCloseBtn != null) {
            mCloseBtn.setOnClickListener(this);
        }
        if (mWidgetViewPager != null) {
            // Trang "đang xem" = trang mà SnapHelper chọn khi cuộn dừng -> cập nhật header/chấm.
            mWidgetViewPager.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                    if (newState != RecyclerView.SCROLL_STATE_IDLE) {
                        return;
                    }
                    int pos = findSnappedPosition();
                    if (pos >= 0) {
                        updatePage(pos);
                    }
                }
            });
        }
        addPanelSlideListener(new WidgetsContainerView.WidgetsAppCellStyleSlideListener(mLauncher));

        // Panel mở xong -> chốt bề rộng trang + ÉP carousel layout lần nữa.
        //
        // Vẫn cần mốc này: setData chạy khi sheet còn chưa hiện nên lúc đó RecyclerView chưa biết
        // bề rộng thật. Khi panel EXPANDED thì kích thước đã chốt — cập nhật lại và ép layout để
        // item chắc chắn được bind, không phải chờ người dùng vuốt (xem forceLayoutCarousel).
        addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState,
                                            SlidingUpPanelLayout.PanelState newState) {
                if (newState == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    syncPageWidth();
                }
            }
        });
    }

    /** Cập nhật bề rộng trang + ÉP carousel layout để item được bind ngay (không cần vuốt). */
    private void syncPageWidth() {
        if (mStyleAdapter == null || mWidgetViewPager == null) {
            return;
        }
        // Chạy vài lượt cách nhau ngắn: sheet mở kèm animation trượt, lượt đầu có thể chưa có
        // kích thước thật. Các hàm bên trong đều idempotent nên gọi lặp là an toàn.
        for (int attempt = 0; attempt < 3; attempt++) {
            mWidgetViewPager.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mStyleAdapter == null || mWidgetViewPager == null) {
                        return;
                    }
                    int w = mWidgetViewPager.getWidth();
                    if (w > 0) {
                        mStyleAdapter.setPageWidth(w);
                    }
                    forceLayoutCarousel();
                }
            }, attempt * 90L);
        }
    }

    /**
     * ĐO + ĐẶT VỊ TRÍ carousel bằng tay để RecyclerView bind item NGAY.
     *
     * LÝ DO: ở màn này, sheet được mở bằng cách đổi visibility + trượt panel theo offset — không
     * có vòng traversal nào chạm tới cây con bên trong (đã xác minh bằng log trên Android 9: chiều
     * cao đặt cho khung không hề có hiệu lực, cell không nhận onMeasure). RecyclerView chỉ tạo và
     * bind item bên trong vòng layout của CHÍNH nó, nên không đo = không có item = ô trống, phải
     * vuốt tay (chạm sinh ra layout) thì preview mới xuất hiện.
     *
     * Gọi thẳng measure()/layout() ở đây buộc RecyclerView chạy dispatchLayout -> bind item ->
     * preview hiện ngay từ lần mở đầu tiên. CHỈ tác động carousel của sheet này.
     */
    private void forceLayoutCarousel() {
        if (mWidgetViewPager == null) {
            return;
        }
        // View đang GONE thì KHÔNG bao giờ được đo/đặt vị trí — ép cũng vô ích.
        //
        // Đây là lý do từ lần mở THỨ HAI trở đi ô bị trống: closeAppStyle() đặt sheet về GONE, nên
        // khi setData của lần mở sau chạy, cả cây con vẫn đang GONE. Đưa các khâu trung gian về
        // VISIBLE ngay để lần ép layout này có tác dụng (bản thân sheet do expandAppStyleView bật).
        if (getVisibility() != View.VISIBLE) {
            setVisibility(View.VISIBLE);
        }
        if (mWidgetViewPager.getVisibility() != View.VISIBLE) {
            mWidgetViewPager.setVisibility(View.VISIBLE);
        }
        int w = mWidgetViewPager.getWidth();
        if (w <= 0) {
            w = getResources().getDisplayMetrics().widthPixels;
        }
        ViewGroup.LayoutParams lp = mWidgetViewPager.getLayoutParams();
        int h = (lp != null && lp.height > 0) ? lp.height : mWidgetViewPager.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        // BẮT BUỘC forceLayout() TRƯỚC KHI ĐO.
        //
        // Lần mở THỨ HAI trở đi, carousel có kích thước y hệt lần trước: measure() thấy spec không
        // đổi nên BỎ QUA onMeasure, kéo theo layout() nhận changed=false nên cũng BỎ QUA onLayout
        // -> RecyclerView không dispatchLayout -> không bind item -> ô TRỐNG (chỉ lần 2 trở đi, lần
        // đầu thì kích thước đổi từ 0 nên vẫn chạy). forceLayout() bật cờ buộc đo/đặt lại thật sự.
        mWidgetViewPager.forceLayout();
        // Ép cả view cha trung gian: nếu cha đang giữ cờ "đã đo xong" thì chuỗi đo có thể dừng
        // trước khi xuống tới carousel.
        if (mWidgetViewPager.getParent() instanceof View) {
            ((View) mWidgetViewPager.getParent()).forceLayout();
        }
        mWidgetViewPager.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(w, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(h, android.view.View.MeasureSpec.EXACTLY));
        int left = mWidgetViewPager.getLeft();
        int top = mWidgetViewPager.getTop();
        mWidgetViewPager.layout(left, top, left + w, top + h);
    }

    /** Vị trí trang đang được SnapHelper "bắt" (trang nằm giữa khung). -1 nếu chưa xác định. */
    private int findSnappedPosition() {
        if (mWidgetViewPager == null || mSnapHelper == null) {
            return -1;
        }
        RecyclerView.LayoutManager lm = mWidgetViewPager.getLayoutManager();
        if (lm == null) {
            return -1;
        }
        View snapped = mSnapHelper.findSnapView(lm);
        if (snapped == null) {
            return -1;
        }
        return lm.getPosition(snapped);
    }

    void setAdapter(){
        mWidgetViewPager.setAdapter(null);
    }

    @Override
    public void onClick(View v) {
        // Nút X: thu sheet size (level-2) về danh sách widget như khi vuốt xuống.
        if (v == mCloseBtn) {
            setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            return;
        }
        if (mStyleAdapter == null) return;
        // Cell đang hiển thị = trang mà SnapHelper đang bắt (thay cho mCurrentCell của PagerAdapter
        // cũ). Chưa xác định được thì lấy trang đầu để nút ADD WIDGET luôn có tác dụng.
        int pos = findSnappedPosition();
        WidgetAppStyleCell activeCell = mStyleAdapter.getCell(pos >= 0 ? pos : 0);
        if (activeCell == null) return;
        Object tag = activeCell.getTag();
        if (tag instanceof PendingAddWidgetInfo) {
            PendingAddWidgetInfo widgetInfo = (PendingAddWidgetInfo) tag;
            mLauncher.closeWidgetViewWithAnimation();
            mLauncher.addAppWidgetFromScreenEditView(widgetInfo);
        }
        else if (tag instanceof PendingAddShortcutInfo){
            PendingAddShortcutInfo shortcutInfo = (PendingAddShortcutInfo) tag;
            mLauncher.addAppShortcutFromScreenEditView(shortcutInfo);
        }
    }

    public void setOnAddWidgetButtonClickListener(IAddWidgetListener aVar) {
        this.mOnAddWidgetBtnClickListener = aVar;
    }

    public void setData(List<Object> data, View.OnClickListener onClickListener, View.OnLongClickListener onLongClickListener){
        if (getWidgetPreviewLoader() == null){
            return;
        }
        ArrayList<WidgetAppStyleCell> cells = new ArrayList<>();
        // Khung preview dùng chung cho mọi cỡ -> lấy theo cỡ CAO NHẤT để bản 4x4 không bị bó vào
        // khung của bản 2x2 (xem computePreviewBoxHeight).
        int maxSpanY = 0;
        for (Object obj : data){
            if (obj == null) continue;
            WidgetAppStyleCell cell = new WidgetAppStyleCell(mLauncher);
            cell.mPreviewLoader = mWidgetPreviewLoader;
            if (obj instanceof LauncherAppWidgetProviderInfo){
                LauncherAppWidgetProviderInfo info = (LauncherAppWidgetProviderInfo) obj;
                InvariantDeviceProfile profile = LauncherAppState.getIDP(mLauncher);
                maxSpanY = Math.max(maxSpanY, Math.min(info.spanY, profile.numRows));
                cell.setTag(
                        new PendingAddWidgetInfo(mLauncher, info,null)
                );
                cell.mParcelable = info;
                cell.mWidgetName.setText(mAppWidgetManagerCompat.loadLabel(info));
                cell.setDims(
                        Math.min(info.spanX, profile.numColumns),
                        Math.min(info.spanY, profile.numRows)
                );
            }
            else if (obj instanceof ResolveInfo) {
                ResolveInfo resolveInfo = (ResolveInfo) obj;
                cell.setTag(new PendingAddShortcutInfo(((ResolveInfo) obj).activityInfo));
                cell.mParcelable = resolveInfo;
                cell.mWidgetName.setText(resolveInfo.loadLabel(mPackageManager));
                cell.setDims(1,1);
            }
            // Nhãn tên/kích cỡ giờ hiện ở HEADER -> ẩn nhãn trong từng ô preview.
            if (cell.mWidgetText != null) {
                cell.mWidgetText.setVisibility(View.GONE);
            }
            // KHÔNG gọi ensurePreview() ở đây: lúc này sheet còn chưa hiện, cell chưa có kích thước
            // -> dựng preview vào ô 0x0 chính là nguyên nhân ô trống trên Android 9. Việc nạp
            // preview để adapter làm khi bind (lúc trang sắp được layout thật).
            cell.setVisibility(View.VISIBLE);
            cells.add(cell);
            cell.setOnClickListener(onClickListener);
            cell.setOnLongClickListener(onLongClickListener);
        }

        close();

        mStyleAdapter = new WidgetAppStyleRecyclerAdapter(cells);

        // TÍNH TRƯỚC kích thước khung rồi mới gắn adapter: cell dựng preview ngay lúc bind bằng
        // kích thước này, không phải chờ vòng đo nào. Đây là điểm khiến preview hiện NGAY từ lần
        // mở đầu tiên (trước đây phải vuốt qua lại mới thấy) và ĐÚNG CỠ (không bị phóng to).
        int boxW = getResources().getDisplayMetrics().widthPixels;
        int boxH = computePreviewBoxHeight(maxSpanY);
        mStyleAdapter.setPageWidth(boxW);
        mStyleAdapter.setPageHeight(boxH);

        // Đặt chiều cao khung TRƯỚC khi gắn adapter để trang được layout đúng ngay từ đầu.
        if (boxH > 0) {
            ViewGroup.LayoutParams boxLp = mWidgetViewPager.getLayoutParams();
            if (boxLp != null && boxLp.height != boxH) {
                boxLp.height = boxH;
                mWidgetViewPager.setLayoutParams(boxLp);
            }
        }

        mWidgetViewPager.setAdapter(mStyleAdapter);
        mWidgetViewPager.scrollToPosition(0);

        buildDots(cells.size());
        updatePage(0);
        // Ép layout NGAY tại đây (không đợi panel mở xong): RecyclerView chỉ bind item trong vòng
        // layout của chính nó, mà cây con của sheet không được traversal ở màn này -> không ép thì
        // phải vuốt tay preview mới hiện. syncPageWidth() còn chạy thêm vài lượt nữa để chắc chắn.
        forceLayoutCarousel();
        syncPageWidth();
    }

    /** Dựng lại hàng chấm chỉ trang cho đúng số size. */
    private void buildDots(int count) {
        if (mDots == null) return;
        mDots.removeAllViews();
        if (count <= 1) {
            mDots.setVisibility(View.GONE);
            return;
        }
        mDots.setVisibility(View.VISIBLE);
        int size = dp(7);
        int gap = dp(3);
        for (int i = 0; i < count; i++) {
            View dot = new View(getContext());
            dot.setBackgroundResource(R.drawable.widget_sheet_dot);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.leftMargin = gap;
            lp.rightMargin = gap;
            mDots.addView(dot, lp);
        }
    }

    /** Chấm trang hiện tại rõ, còn lại mờ. */
    private void updateDots(int selected) {
        if (mDots == null) return;
        for (int i = 0; i < mDots.getChildCount(); i++) {
            mDots.getChildAt(i).setAlpha(i == selected ? 1f : 0.3f);
        }
    }

    /** Cập nhật header (icon + tên) + tiêu đề + mô tả + chấm theo trang đang xem. */
    private void updatePage(int position) {
        if (mStyleAdapter == null) return;
        WidgetAppStyleCell cell = mStyleAdapter.getCell(position);
        if (cell == null) return;
        Parcelable p = cell.mParcelable;

        if (p instanceof LauncherAppWidgetProviderInfo) {
            WidgetSheetMeta meta = WidgetSheetMeta.forWidget(
                    getContext(), (LauncherAppWidgetProviderInfo) p);
            setTextOrHide(mHeaderName, meta.group);
            setTextOrHide(mTitle, meta.title);
            setTextOrHide(mDesc, meta.desc);
            if (mHeaderIcon != null) {
                if (meta.icon != null) {
                    mHeaderIcon.setImageDrawable(meta.icon);
                    mHeaderIcon.setVisibility(View.VISIBLE);
                } else {
                    mHeaderIcon.setVisibility(View.INVISIBLE);
                }
            }
        } else {
            // Shortcut / loại khác: dùng nhãn trong ô, không có mô tả.
            CharSequence label = (cell != null && cell.mWidgetName != null)
                    ? cell.mWidgetName.getText() : "";
            setTextOrHide(mHeaderName, label);
            setTextOrHide(mTitle, label);
            if (mDesc != null) mDesc.setVisibility(View.GONE);
            if (mHeaderIcon != null) mHeaderIcon.setVisibility(View.INVISIBLE);
        }
        updateDots(position);
    }

    private static void setTextOrHide(TextView tv, CharSequence text) {
        if (tv == null) return;
        if (TextUtils.isEmpty(text)) {
            tv.setText("");
            tv.setVisibility(View.GONE);
        } else {
            tv.setText(text);
            tv.setVisibility(View.VISIBLE);
        }
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    // Lề thoáng trên/dưới quanh preview (mỗi bên), tính theo dp.
    private static final int PREVIEW_BOX_PADDING_DP = 12;

    /** Trần chiều cao khung preview so với chiều cao màn hình (chừa chỗ cho header + nút). */
    private static final float PREVIEW_BOX_MAX_SCREEN_RATIO = 0.42f;

    /**
     * Tính SẴN chiều cao khung carousel — thuần số học, KHÔNG phụ thuộc việc đo view.
     *
     * Vì sao không đo: các mốc đo (post/getBottom/getTop) chỉ có số liệu sau khi sheet đã hiện,
     * nên preview hoặc dựng sai cỡ, hoặc phải chờ tới lần vuốt sau mới đúng. Ở đây tính trước
     * theo lưới + màn hình rồi truyền cho adapter dựng preview ngay từ lần bind đầu tiên.
     *
     * Khung dùng CHUNG cho mọi cỡ nên lấy theo cỡ CAO NHẤT (vd 4x4) + lề thoáng, nhưng không vượt
     * quá {@link #PREVIEW_BOX_MAX_SCREEN_RATIO} chiều cao màn để còn chỗ cho tiêu đề và nút thêm.
     * Việc mỗi cỡ hiện to/nhỏ ra sao do applyScaleNoUpscale lo (chỉ thu nhỏ, không phóng to).
     */
    private int computePreviewBoxHeight(int maxSpanY) {
        DeviceProfile dp = mLauncher.getDeviceProfile();
        if (dp == null || maxSpanY <= 0) {
            return 0;
        }
        int natH = Math.max(1, maxSpanY) * dp.cellHeightPx;
        int targetH = natH + 2 * dp(PREVIEW_BOX_PADDING_DP);
        int maxH = Math.round(
                getResources().getDisplayMetrics().heightPixels * PREVIEW_BOX_MAX_SCREEN_RATIO);
        if (maxH > 0 && targetH > maxH) {
            targetH = maxH;
        }
        return targetH;
    }
}
