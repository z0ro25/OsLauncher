package com.amz.ios.launcher.widget;

import android.content.ComponentName;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amz.ios.launcher.BubbleTextView;
import com.amz.ios.launcher.DeviceProfile;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.LauncherAppWidgetProviderInfo;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.TextViewCustomFont;
import com.amz.ios.launcher.WidgetPreviewLoader;
import com.amz.ios.launcher.compat.AppWidgetManagerCompat;
import com.amz.ios.launcher.model.PackageItemInfo;
import com.amz.ios.launcher.model.WidgetsModel;
import com.google.gson.internal.$Gson$Preconditions;

import java.util.ArrayList;

public class WidgetAppListAdapter extends RecyclerView.Adapter {

    public Launcher mLauncher;
    LayoutInflater mLayoutInflater;
    WidgetsModel mWidgetsModel;
    WidgetPreviewLoader mWidgetPreviewLoader;
    View.OnClickListener onClickListener;
    View.OnLongClickListener onLongClickListener;
    IWidgetListAdapter mWidgetListDelegate;
    boolean shouldUpdate = false;
    AppWidgetManagerCompat mAppWidgetManagerCompat;
    DeviceProfile mGrid;
    int mMargin;

    public interface IWidgetListAdapter {

    }

    public WidgetAppListAdapter(Launcher launcher, View.OnClickListener clickListener, View.OnLongClickListener longClickListener, IWidgetListAdapter delegate) {
        this.mLauncher = launcher;
        this.mGrid = launcher.getDeviceProfile();
        this.mLayoutInflater = LayoutInflater.from(mLauncher);
        this.onClickListener = clickListener;
        this.onLongClickListener = longClickListener;
        this.mWidgetListDelegate = delegate;
        this.mAppWidgetManagerCompat = AppWidgetManagerCompat.getInstance(mLauncher);
        this.mWidgetPreviewLoader = LauncherAppState.getInstance().getWidgetCache();
        // [CĂN CHỈNH] Lề quanh mỗi thẻ ở lưới widget nổi bật. Giảm một nửa (edgeMargin/2 -> /4) để
        // khoảng hở giữa các thẻ hẹp lại theo yêu cầu. Lề này áp CHUNG cho cả thẻ vuông 1 cột lẫn
        // thẻ ngang 2 cột, nên mép trái/phải của hàng ngang vẫn THẲNG với mép lưới 2 cột phía trên.
        this.mMargin = mGrid.edgeMarginPx / 4;
    }

    public void setWidgetModel(WidgetsModel widgetModel){
        this.mWidgetsModel = widgetModel;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 1){
            View view = mLayoutInflater.inflate(R.layout.widgets_full_row_view, parent, false);
            return new IOSWidgetViewHolder(view);
        }
        else {
            View view = mLayoutInflater.inflate(R.layout.widgets_list_row_view,parent,false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof IOSWidgetViewHolder) {
            bindFeaturedGrid((IOSWidgetViewHolder) holder);
            return;
        }

        if (holder instanceof ItemViewHolder) {
            // Header lưới nổi bật là item riêng ở vị trí 0 -> app list bắt đầu lệch 1.
            final int appIndex = position - (hasFeatured() ? 1 : 0);
            PackageItemInfo info = mWidgetsModel.mFilteredPackageInfo.get(appIndex);
            if (info == null) return;

            final ArrayList<Object> widgets = mWidgetsModel.mFilteredWidgetList.get(info);
            shouldUpdate = true;
            if (widgets == null) return;

            final ItemViewHolder itemViewHolder = (ItemViewHolder) holder;
            itemViewHolder.mWidgetSection.applyFromPackageItemInfo(info);
            itemViewHolder.mWidgetSection.setTextVisibility(false);
            itemViewHolder.mWidgetSection.setText("");
            itemViewHolder.mWidgetAppName.setText(info.title);
            // Màu tên app theo Dark/Light (nền sheet đổi theo theme).
            itemViewHolder.mWidgetAppName.setTextColor(WidgetSheetTheme.textPrimary(mLauncher));
            itemViewHolder.mWidgetSection.setOnLongClickListener(onLongClickListener);
            itemViewHolder.mWidgetSection.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!shouldUpdate){
                                notifyItemChanged(position);
                            }
                            if (mWidgetListDelegate != null){
                                if (mWidgetListDelegate instanceof WidgetsContainerView) {
                                    final WidgetsContainerView containerView = (WidgetsContainerView) mWidgetListDelegate;
                                    mLauncher.mWidgetsAppStyle.setData(
                                            widgets,
                                            onClickListener,
                                            onLongClickListener
                                    );
                                    mLauncher.mWidgetsAppStyle.post(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    containerView.expandAppStyleView();
                                                }
                                            }
                                    );
                                }
                            }
                            shouldUpdate = false;
                        }
                    }
            );
            itemViewHolder.itemView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            itemViewHolder.mWidgetSection.performClick();
                        }
                    }
            );
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return (hasFeatured() && position == 0) ? 1 : 2;
    }

    @Override
    public int getItemCount() {
        if (this.mWidgetsModel == null) return 0;
        if (this.mWidgetsModel.mFilteredPackageInfo == null) return 0;
        return this.mWidgetsModel.mFilteredPackageInfo.size() + (hasFeatured() ? 1 : 0);
    }

    /**
     * Có hiện lưới widget nổi bật ở đầu khay không.
     * Chỉ hiện khi CHƯA search (danh sách filter == full) và có widget nổi bật.
     */
    private boolean hasFeatured() {
        if (mWidgetsModel == null || mWidgetsModel.mFilteredPackageInfo == null) return false;
        boolean notSearching =
                mWidgetsModel.mFilteredPackageInfo.size() == mWidgetsModel.getPackageSize();
        ArrayList<Object> featured = mWidgetsModel.getFeaturedWidgets();
        return notSearching && featured != null && !featured.isEmpty();
    }

    /**
     * Đổ lưới widget nổi bật vào header. Thẻ vuông (Calendar/Photos/Batteries/Weather)
     * xếp 2 ô/hàng; thẻ rộng (World Clock, spanX>=3) chiếm cả hàng, chiều cao = ô vuông.
     */
    private void bindFeaturedGrid(IOSWidgetViewHolder holder) {
        LinearLayout grid = holder.mFeaturedGrid;
        if (grid == null) return;
        grid.removeAllViews();

        ArrayList<Object> featured = mWidgetsModel.getFeaturedWidgets();
        if (featured == null || featured.isEmpty()) return;

        LinearLayout squareRow = null;
        int squareInRow = 0;

        for (int i = 0; i < featured.size(); i++) {
            Object obj = featured.get(i);
            if (!(obj instanceof LauncherAppWidgetProviderInfo)) continue;
            final LauncherAppWidgetProviderInfo info = (LauncherAppWidgetProviderInfo) obj;

            // Thẻ đồng hồ ngang: rộng 2 cột, tự chiếm 1 hàng riêng.
            if (info.spanX >= 3) {
                if (squareRow != null && squareInRow == 1) {
                    addSquareSpacer(squareRow);
                }
                squareRow = null;
                squareInRow = 0;

                LinearLayout wideRow = new LinearLayout(mLauncher);
                wideRow.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(wideRow, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

                final GalleryWidgetCell wideCell = new GalleryWidgetCell(mLauncher);
                wideCell.setWide();
                wideCell.bind(info);
                wideCell.ensurePreview();
                wideCell.setOnLongClickListener(onLongClickListener);
                wideCell.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openClockCarousel();
                    }
                });

                LinearLayout.LayoutParams wideLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, wideCell.getCellSize());
                wideLp.setMargins(mMargin, mMargin, mMargin, mMargin);
                wideRow.addView(wideCell, wideLp);
                continue;
            }

            // Thẻ vuông 1 cột (2 ô / hàng).
            if (squareInRow == 0) {
                squareRow = new LinearLayout(mLauncher);
                squareRow.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(squareRow, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            final GalleryWidgetCell cell = new GalleryWidgetCell(mLauncher);
            cell.bind(info);
            cell.ensurePreview();
            cell.setOnLongClickListener(onLongClickListener);
            cell.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Thẻ Lịch -> carousel gia đình Calendar; thẻ Weather -> carousel 3 size
                    // Weather; thẻ Photos -> carousel 3 size Photos (nhỏ/medium/large); các thẻ
                    // vuông khác giữ carousel 1 phần tử.
                    if (isCalendarTile(info)) {
                        openCalendarCarousel();
                    } else if (isWeatherTile(info)) {
                        openWeatherCarousel();
                    } else if (isPictureTile(info)) {
                        openPictureCarousel();
                    } else {
                        openCarouselForSingle(info);
                    }
                }
            });

            LinearLayout.LayoutParams cellLp =
                    new LinearLayout.LayoutParams(0, cell.getCellSize(), 1f);
            cellLp.setMargins(mMargin, mMargin, mMargin, mMargin);
            squareRow.addView(cell, cellLp);
            squareInRow++;
            if (squareInRow == 2) {
                squareRow = null;
                squareInRow = 0;
            }
        }

        // Hàng vuông còn dư 1 ô -> chèn 1 ô trống giữ cân đối lưới 2 cột.
        if (squareRow != null && squareInRow == 1) {
            addSquareSpacer(squareRow);
        }
    }

    /** Chèn 1 ô trống (weight 1) để giữ cân đối hàng vuông khi lẻ ô. */
    private void addSquareSpacer(LinearLayout row) {
        View spacer = new View(mLauncher);
        row.addView(spacer, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
    }

    /** Bấm thẻ Clock -> mở carousel level-2 gồm 4 size đồng hồ làm sẵn. */
    private void openClockCarousel() {
        if (!(mWidgetListDelegate instanceof WidgetsContainerView)) return;
        final WidgetsContainerView containerView = (WidgetsContainerView) mWidgetListDelegate;
        ArrayList<Object> variants = mWidgetsModel.getClockVariants();
        if (variants == null || variants.isEmpty()) return;
        mLauncher.mWidgetsAppStyle.setData(variants, onClickListener, onLongClickListener);
        mLauncher.mWidgetsAppStyle.post(new Runnable() {
            @Override
            public void run() {
                containerView.expandAppStyleView();
            }
        });
    }

    /** Nhận diện thẻ Lịch (đại diện = lưới tháng "đủ ngày") trong lưới featured qua tên class provider. */
    private boolean isCalendarTile(LauncherAppWidgetProviderInfo info) {
        return info != null && info.provider != null
                && info.provider.getClassName().endsWith(".CalendarMonthWidgetProvider");
    }

    /** Bấm thẻ Lịch -> mở carousel level-2 gồm cả gia đình Calendar làm sẵn. */
    private void openCalendarCarousel() {
        if (!(mWidgetListDelegate instanceof WidgetsContainerView)) return;
        final WidgetsContainerView containerView = (WidgetsContainerView) mWidgetListDelegate;
        ArrayList<Object> variants = mWidgetsModel.getCalendarVariants();
        if (variants == null || variants.isEmpty()) return;
        mLauncher.mWidgetsAppStyle.setData(variants, onClickListener, onLongClickListener);
        mLauncher.mWidgetsAppStyle.post(new Runnable() {
            @Override
            public void run() {
                containerView.expandAppStyleView();
            }
        });
    }

    /** Nhận diện thẻ Weather (đại diện = bản nhỏ 2x2) trong lưới featured qua tên class provider. */
    private boolean isWeatherTile(LauncherAppWidgetProviderInfo info) {
        return info != null && info.provider != null
                && info.provider.getClassName().endsWith(".WeatherWidgetProvider");
    }

    /** Bấm thẻ Weather -> mở carousel level-2 gồm 3 size Weather (nhỏ 2x2 / medium 4x2 / large 4x4). */
    private void openWeatherCarousel() {
        if (!(mWidgetListDelegate instanceof WidgetsContainerView)) return;
        final WidgetsContainerView containerView = (WidgetsContainerView) mWidgetListDelegate;
        ArrayList<Object> variants = mWidgetsModel.getWeatherVariants();
        if (variants == null || variants.isEmpty()) return;
        mLauncher.mWidgetsAppStyle.setData(variants, onClickListener, onLongClickListener);
        mLauncher.mWidgetsAppStyle.post(new Runnable() {
            @Override
            public void run() {
                containerView.expandAppStyleView();
            }
        });
    }

    /** Nhận diện thẻ Photos (đại diện = bản nhỏ 2x2) trong lưới featured qua tên class provider. */
    private boolean isPictureTile(LauncherAppWidgetProviderInfo info) {
        return info != null && info.provider != null
                && info.provider.getClassName().endsWith(".PictureAppWidgetProvider");
    }

    /** Bấm thẻ Photos -> mở carousel level-2 gồm 3 size Photos (nhỏ 2x2 / medium 4x2 / large 4x4). */
    private void openPictureCarousel() {
        if (!(mWidgetListDelegate instanceof WidgetsContainerView)) return;
        final WidgetsContainerView containerView = (WidgetsContainerView) mWidgetListDelegate;
        ArrayList<Object> variants = mWidgetsModel.getPictureVariants();
        if (variants == null || variants.isEmpty()) return;
        mLauncher.mWidgetsAppStyle.setData(variants, onClickListener, onLongClickListener);
        mLauncher.mWidgetsAppStyle.post(new Runnable() {
            @Override
            public void run() {
                containerView.expandAppStyleView();
            }
        });
    }

    /** Bấm 1 thẻ lưới -> mở carousel level-2 với đúng widget đó (list 1 phần tử). */
    private void openCarouselForSingle(LauncherAppWidgetProviderInfo info) {
        if (!(mWidgetListDelegate instanceof WidgetsContainerView)) return;
        final WidgetsContainerView containerView = (WidgetsContainerView) mWidgetListDelegate;
        ArrayList<Object> one = new ArrayList<>();
        one.add(info);
        mLauncher.mWidgetsAppStyle.setData(one, onClickListener, onLongClickListener);
        mLauncher.mWidgetsAppStyle.post(new Runnable() {
            @Override
            public void run() {
                containerView.expandAppStyleView();
            }
        });
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {

        BubbleTextView mWidgetSection;
        TextViewCustomFont mWidgetAppName;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            mWidgetSection = itemView.findViewById(R.id.section);
            mWidgetAppName = itemView.findViewById(R.id.widget_app_name);
        }


    }

    public static class IOSWidgetViewHolder extends RecyclerView.ViewHolder {

        // Container rỗng của header; adapter đổ các hàng ngang (2 GalleryWidgetCell/hàng) vào.
        LinearLayout mFeaturedGrid;

        public IOSWidgetViewHolder(@NonNull View itemView) {
            super(itemView);
            mFeaturedGrid = itemView.findViewById(R.id.featured_widget_grid);
        }
    }

}
