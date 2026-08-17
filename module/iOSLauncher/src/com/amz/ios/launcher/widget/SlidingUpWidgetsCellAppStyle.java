package com.amz.ios.launcher.widget;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
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
        applySheetTheme();
    }

    /** Áp nền + màu tiêu đề theo Dark/Light (đọc runtime từ prefs theme). */
    private void applySheetTheme() {
        View bg = findViewById(R.id.widget_sheet_bg);
        if (bg != null) {
            bg.setBackgroundResource(WidgetSheetTheme.sheetBackgroundRes(getContext()));
        }
        TextView title = findViewById(R.id.widgets_content_text);
        if (title != null) {
            title.setTextColor(WidgetSheetTheme.textPrimary(getContext()));
        }
    }

    void setListeners(){
        mAddWidgetBtn.setOnClickListener(this);
        addPanelSlideListener(new WidgetsContainerView.WidgetsAppCellStyleSlideListener(mLauncher));
    }

    void setAdapter(){
        mWidgetViewPager.setAdapter(null);
    }

    @Override
    public void onClick(View v) {
//        if (mOnAddWidgetBtnClickListener == null) return;
//        ((WidgetsContainerView) ((r4) aVar).b).p();
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
            cell.ensurePreview();
            cell.setVisibility(View.VISIBLE);
            cells.add(cell);
            cell.setOnClickListener(onClickListener);
            cell.setOnLongClickListener(onLongClickListener);
        }

        close();

        mStyleAdapter = new WidgetAppStyleListAdapter(cells);
        mWidgetViewPager.setAdapter(mStyleAdapter);

        // Thu khung preview về ĐÚNG tỉ lệ widget khi nằm trên lưới (spanX*ô : spanY*ô) rồi
        // để ConstraintLayout căn giữa giữa tiêu đề và nút -> lề trên/dưới cân, preview đúng
        // kích cỡ hình dung như khi add vào màn page. Chỉ áp cho widget (không đụng shortcut).
        if (firstWidget != null) {
            adjustPreviewBoxToGridAspect(firstWidget);
        }
    }

    /**
     * Đặt chiều cao ViewPager preview bằng bề rộng * (tỉ lệ span thật của widget trên lưới),
     * kẹp trong khoảng trống giữa tiêu đề và nút Thêm. Khung có kích thước cố định + 2 ràng buộc
     * trên/dưới nên ConstraintLayout tự căn giữa (lề cân). CHỈ tác động màn size-picker này.
     */
    private void adjustPreviewBoxToGridAspect(final LauncherAppWidgetProviderInfo info) {
        final DeviceProfile dp = mLauncher.getDeviceProfile();
        if (dp == null) return;
        final int natW = Math.max(1, info.spanX) * dp.cellWidthPx;
        final int natH = Math.max(1, info.spanY) * dp.cellHeightPx;
        if (natW <= 0 || natH <= 0) return;

        mWidgetViewPager.post(new Runnable() {
            @Override
            public void run() {
                int boxW = mWidgetViewPager.getWidth();
                if (boxW <= 0) return;
                int targetH = Math.round(boxW * (natH / (float) natW));

                // Không vượt quá khoảng trống hiện có giữa tiêu đề và nút.
                View title = findViewById(R.id.widgets_content_text);
                int avail = mAddWidgetBtn.getTop()
                        - (title != null ? title.getBottom() : 0);
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
