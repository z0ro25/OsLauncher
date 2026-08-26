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

import androidx.appcompat.widget.AppCompatButton;
import androidx.viewpager.widget.ViewPager;

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
    ViewPager mWidgetViewPager;
    IAddWidgetListener mOnAddWidgetBtnClickListener;
    AppCompatButton mAddWidgetBtn;
    AppWidgetManagerCompat mAppWidgetManagerCompat;
    PackageManager mPackageManager;
    WidgetAppStyleListAdapter mStyleAdapter;

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
            mWidgetViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    updatePage(position);
                }
            });
        }
        addPanelSlideListener(new WidgetsContainerView.WidgetsAppCellStyleSlideListener(mLauncher));
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
        WidgetAppStyleCell activeCell = mStyleAdapter.mCurrentCell;
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
        LauncherAppWidgetProviderInfo firstWidget = null;
        for (Object obj : data){
            if (obj == null) continue;
            WidgetAppStyleCell cell = new WidgetAppStyleCell(mLauncher);
            cell.mPreviewLoader = mWidgetPreviewLoader;
            if (obj instanceof LauncherAppWidgetProviderInfo){
                LauncherAppWidgetProviderInfo info = (LauncherAppWidgetProviderInfo) obj;
                if (firstWidget == null) firstWidget = info;
                InvariantDeviceProfile profile = LauncherAppState.getIDP(mLauncher);
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
            cell.ensurePreview();
            cell.setVisibility(View.VISIBLE);
            cells.add(cell);
            cell.setOnClickListener(onClickListener);
            cell.setOnLongClickListener(onLongClickListener);
        }

        close();

        mStyleAdapter = new WidgetAppStyleListAdapter(cells);
        mWidgetViewPager.setAdapter(mStyleAdapter);

        buildDots(cells.size());
        updatePage(0);

        // Thu khung preview về ĐÚNG tỉ lệ widget khi nằm trên lưới (spanX*ô : spanY*ô) rồi
        // để ConstraintLayout căn giữa giữa mô tả và hàng chấm -> lề trên/dưới cân, preview
        // đúng kích cỡ hình dung như khi add vào màn page. Chỉ áp cho widget (không đụng shortcut).
        if (firstWidget != null) {
            adjustPreviewBoxToGridAspect(firstWidget);
        }
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
        if (mStyleAdapter == null || mStyleAdapter.mCells == null) return;
        if (position < 0 || position >= mStyleAdapter.mCells.size()) return;
        WidgetAppStyleCell cell = mStyleAdapter.mCells.get(position);
        Parcelable p = cell != null ? cell.mParcelable : null;

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

    // Hệ số thu preview so với cỡ thật trên lưới (spanY*ô). Càng nhỏ đồng hồ càng nhỏ.
    private static final float PREVIEW_SCALE = 0.75f;

    /**
     * Đặt chiều cao khung ViewPager = (chiều cao THẬT của widget trên lưới) * PREVIEW_SCALE.
     * Preview sống scale để lấp đầy khung theo min(rộng, cao); vì bề rộng ViewPager là cả sheet
     * nên CHIỀU CAO khung mới là thứ quyết định cỡ đồng hồ. Trước đây đặt cao ≈ cả bề rộng sheet
     * nên đồng hồ bị phóng to. Kẹp trong khoảng trống giữa mô tả và hàng chấm. CHỈ tác động màn này.
     */
    private void adjustPreviewBoxToGridAspect(final LauncherAppWidgetProviderInfo info) {
        final DeviceProfile dp = mLauncher.getDeviceProfile();
        if (dp == null) return;
        final int natH = Math.max(1, info.spanY) * dp.cellHeightPx;
        if (natH <= 0) return;

        mWidgetViewPager.post(new Runnable() {
            @Override
            public void run() {
                int targetH = Math.round(natH * PREVIEW_SCALE);

                // Không vượt quá khoảng trống hiện có giữa mô tả và hàng chấm.
                View top = findViewById(R.id.widget_sheet_desc);
                int topY = (top != null) ? top.getBottom() : 0;
                int bottomY = (mDots != null && mDots.getTop() > 0)
                        ? mDots.getTop() : mAddWidgetBtn.getTop();
                int avail = bottomY - topY;
                if (avail > 0 && targetH > avail) {
                    targetH = avail;
                }
                if (targetH <= 0) return;

                ViewGroup.LayoutParams lp = mWidgetViewPager.getLayoutParams();
                if (lp.height != targetH) {
                    lp.height = targetH;
                    mWidgetViewPager.setLayoutParams(lp);
                }
            }
        });
    }
}
