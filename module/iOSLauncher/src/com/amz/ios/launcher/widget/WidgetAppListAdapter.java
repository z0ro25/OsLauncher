package com.amz.ios.launcher.widget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
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

    // ===== View type =====
    // [TÁI CẤU TRÚC] Trước đây CẢ 5 widget nổi bật bị nhồi vào MỘT item (IOSWidgetViewHolder), mỗi
    //   lần onBindViewHolder phải grid.removeAllViews() rồi new GalleryWidgetCell() lại từ đầu.
    //   Riêng Picture còn query MediaStore + decodeFile trên MAIN THREAD -> ô trống vài giây khi
    //   cuộn qua-lại. Đó là hệ quả của việc một holder hứng nhiều loại widget khác nhau.
    //   Nay MỖI widget nổi bật là MỘT item với view type RIÊNG. RecyclerView chỉ tái dùng holder
    //   CÙNG type, nên holder của Picture chỉ bao giờ nhận Picture -> không còn xung đột khi đổ lại
    //   dữ liệu, và bind chỉ việc gán dữ liệu chứ không dựng lại view.
    private static final int TYPE_APP_ROW = 0;
    /**
     * Type của widget nổi bật = TYPE_FEATURED_BASE + chỉ số trong danh sách featured.
     * Sinh động theo danh sách (KHÔNG hardcode từng loại) để thêm/bớt widget nổi bật sau này chỉ
     * cần sửa mảng ORDER trong WidgetsModel.getFeaturedWidgets(), không phải đụng adapter.
     */
    private static final int TYPE_FEATURED_BASE = 100;

    /**
     * ẢNH CHỤP danh sách widget nổi bật đang hiển thị, chốt lại mỗi khi dữ liệu đổi.
     *
     * BẤT BIẾN PHẢI GIỮ: getItemCount(), getItemViewType() và getSpanSize() BẮT BUỘC đọc cùng một
     * ảnh chụp này. GridLayoutManager hỏi SpanSizeLookup nhiều lần TRONG lúc layout (xem
     * GridLayoutManager.getSpanIndex/getSpanGroupIndex — chúng lặp getSpanSize từ 0 tới position);
     * nếu mỗi lần hỏi lại đi đọc thẳng mWidgetsModel thì giữa chừng model đổi (search, hoặc
     * notifyWidgetProvidersChanged nạp model mới) sẽ cho số item và span size lệch nhau -> lưới tính
     * sai hàng và KHÔNG hiện gì. Đó là lý do khay trống trơn sau khi chuyển sang lưới 2 cột.
     *
     * Chốt một lần trong refreshFeatured() nên trong suốt một lượt layout mọi câu trả lời đều nhất
     * quán, dù model có bị thay ở luồng khác.
     */
    private final ArrayList<LauncherAppWidgetProviderInfo> mFeatured = new ArrayList<>();

    /**
     * Chốt lại ảnh chụp widget nổi bật từ model hiện tại.
     * Gọi từ getItemCount() (điểm đồng bộ chính, đầu mỗi lượt layout) và setWidgetModel().
     */
    private void refreshFeatured() {
        mFeatured.clear();
        if (!hasFeatured()) return;
        ArrayList<Object> featured = mWidgetsModel.getFeaturedWidgets();
        if (featured == null) return;
        for (Object o : featured) {
            if (o instanceof LauncherAppWidgetProviderInfo) {
                mFeatured.add((LauncherAppWidgetProviderInfo) o);
            }
        }
    }

    // KHÔNG đè notifyDataSetChanged() được: RecyclerView.Adapter khai báo nó là public FINAL
    // (cùng với toàn bộ họ notifyItem*). Điểm chốt ảnh chụp là getItemCount() — RecyclerView luôn
    // hỏi hàm đó ở đầu mỗi lượt layout, trước mọi câu hỏi khác, nên chốt ở đó là đủ và đúng chỗ.

    /** Widget nổi bật ở vị trí này trong ảnh chụp, hoặc null nếu ngoài phạm vi. */
    private LauncherAppWidgetProviderInfo featuredAt(int index) {
        if (index < 0 || index >= mFeatured.size()) return null;
        return mFeatured.get(index);
    }

    /** Số widget nổi bật đang hiển thị (0 khi đang search) — đọc từ ảnh chụp. */
    private int featuredCount() {
        return mFeatured.size();
    }

    /** Nửa khe giữa hai thẻ nổi bật — giá trị margin cũ, nay do FeaturedGridSpacing áp dụng. */
    public int getCellGap() {
        return mMargin;
    }

    /** Số item widget nổi bật ở đầu danh sách — cho FeaturedGridSpacing biết vùng cần chèn lề. */
    public int getFeaturedItemCount() {
        return featuredCount();
    }

    /** Thẻ rộng 2 cột (World Clock): span ngang >= 3 ô lưới. */
    private boolean isWideFeatured(LauncherAppWidgetProviderInfo info) {
        return info != null && info.spanX >= 3;
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
        // Model vừa đổi -> chốt lại ảnh chụp NGAY. Launcher.notifyWidgetProvidersChanged() gọi
        // setWidgetModel() rồi mới notifyDataSetChanged(); nếu không chốt ở đây thì khoảng giữa hai
        // lời gọi đó getItemCount() đã thấy dữ liệu mới trong khi ảnh chụp còn rỗng.
        refreshFeatured();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType >= TYPE_FEATURED_BASE) {
            // Widget nổi bật: TẠO cell đúng 1 lần cho mỗi type. RecyclerView chỉ tái dùng holder
            // cùng type nên holder này về sau chỉ nhận đúng loại widget đó -> onBindViewHolder chỉ
            // cần đổ dữ liệu, không phải dựng lại view.
            GalleryWidgetCell cell = new GalleryWidgetCell(mLauncher);
            // KHÔNG quyết định thẻ rộng/vuông ở đây. Holder được RecyclerView giữ trong pool và tái
            // dùng, sống lâu hơn ảnh chụp dữ liệu; nếu danh sách widget nổi bật đổi thứ tự thì thẻ
            // tạo cho index cũ sẽ mang sai tỉ lệ khung. Việc đó dời sang onBindViewHolder — nơi biết
            // chắc widget nào đang được gán.
            //
            // Cell tự đặt LayoutParams cỡ cố định trong constructor; ở lưới RecyclerView thì bề rộng
            // do span quyết định -> ép match_parent, chiều cao giữ theo cỡ ô.
            //
            // Phải là GridLayoutManager.LayoutParams: GridLayoutManager.checkLayoutParams() chỉ chấp
            // nhận đúng kiểu này, sai kiểu thì mỗi lần view được gắn lại sau khi tái dùng,
            // RecyclerView.addViewInt() sẽ tạo LayoutParams MỚI thay cho cái ta đặt ở đây.
            // Khoảng cách giữa/ngoài các thẻ do FeaturedGridSpacing lo, KHÔNG đặt margin ở đây
            // (margin nằm trong LayoutParams nên cũng mất theo khi bị chuyển đổi).
            cell.setLayoutParams(new GridLayoutManager.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, cell.getCellSize()));

            // Listener gắn 1 LẦN ở đây thay vì mỗi lần bind.
            cell.setOnLongClickListener(onLongClickListener);
            cell.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Object tag = v.getTag();
                    if (!(tag instanceof PendingAddWidgetInfo)) return;
                    LauncherAppWidgetProviderInfo cur = ((PendingAddWidgetInfo) tag).info;
                    // Thẻ Lịch -> carousel gia đình Calendar; Weather -> 3 size Weather;
                    // Photos -> 3 size Photos; các thẻ khác giữ carousel 1 phần tử.
                    if (isCalendarTile(cur)) {
                        openCalendarCarousel();
                    } else if (isWeatherTile(cur)) {
                        openWeatherCarousel();
                    } else if (isPictureTile(cur)) {
                        openPictureCarousel();
                    } else if (isWideFeatured(cur)) {
                        openClockCarousel();
                    } else {
                        openCarouselForSingle(cur);
                    }
                }
            });
            return new FeaturedViewHolder(cell);
        }
        View view = mLayoutInflater.inflate(R.layout.widgets_list_row_view, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof FeaturedViewHolder) {
            // Chỉ ĐỔ DỮ LIỆU — view đã dựng sẵn ở onCreateViewHolder cho đúng type.
            LauncherAppWidgetProviderInfo info = featuredAt(position);
            if (info == null) return;
            GalleryWidgetCell cell = ((FeaturedViewHolder) holder).mCell;
            // Thẻ luôn phải hiện. Phòng trường hợp một luồng kéo-thả nào đó ẩn thẻ đi mà không bật
            // lại (DragController.startDrag ẩn view nguồn khi dragAction == DRAG_ACTION_MOVE) —
            // holder được tái dùng sẽ mang theo trạng thái ẩn đó sang widget khác.
            cell.setVisibility(View.VISIBLE);
            // Tỉ lệ khung preview phải chốt TRƯỚC bind/ensurePreview vì nó quyết định kích thước ảnh
            // preview được yêu cầu. setWide/setNarrow chỉ đổi con số, không dựng lại view, nên gọi
            // mỗi lần bind là an toàn và luôn khớp với span mà getSpanSize() trả về cho vị trí này.
            if (isWideFeatured(info)) {
                cell.setWide();
            } else {
                cell.setNarrow();
            }
            cell.bind(info);
            cell.ensurePreview();
            return;
        }

        if (holder instanceof ItemViewHolder) {
            // N widget nổi bật đứng đầu danh sách -> app list bắt đầu lệch N.
            final int appIndex = position - featuredCount();
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
        // N widget nổi bật đứng đầu, mỗi cái một type RIÊNG (xem TYPE_FEATURED_BASE); phần còn lại
        // là danh sách app dùng chung một type.
        int n = featuredCount();
        if (position < n) {
            return TYPE_FEATURED_BASE + position;
        }
        return TYPE_APP_ROW;
    }

    /**
     * Số cột lưới mà item ở vị trí này chiếm — dùng cho GridLayoutManager.SpanSizeLookup.
     * Thẻ vuông chiếm 1 cột; thẻ rộng (World Clock) và mọi hàng app chiếm trọn 2 cột.
     */
    public int getSpanSize(int position, int spanCount) {
        if (position < 0) return spanCount;
        LauncherAppWidgetProviderInfo info = featuredAt(position);
        // Ngoài vùng widget nổi bật (featuredAt trả null) -> hàng danh sách app, chiếm trọn hàng.
        // Không bao giờ trả giá trị > spanCount: GridLayoutManager ném IllegalArgumentException
        // ("requires N spans but GridLayoutManager has only M") và khay sẽ không vẽ được gì.
        if (info == null) return spanCount;
        return isWideFeatured(info) ? spanCount : 1;
    }

    @Override
    public int getItemCount() {
        if (this.mWidgetsModel == null) return 0;
        if (this.mWidgetsModel.mFilteredPackageInfo == null) return 0;
        // ĐIỂM ĐỒNG BỘ: RecyclerView luôn hỏi getItemCount() ở đầu mỗi lượt layout, trước khi hỏi
        // getSpanSize()/getItemViewType()/onBindViewHolder(). Chốt lại ảnh chụp ngay tại đây nên số
        // item và dữ liệu mà lưới đọc sau đó CHẮC CHẮN cùng một nguồn.
        //
        // Không dựa vào notifyDataSetChanged() làm điểm chốt duy nhất được: filterWidget() bỏ qua
        // notify khi RecyclerView đang tính layout (xem WidgetsContainerView.filterWidget), khi đó
        // ảnh chụp sẽ đứng yên trong lúc model đã đổi -> lệch nhau -> khay không hiện gì.
        refreshFeatured();
        // Mỗi widget nổi bật là MỘT item riêng (trước đây cả nhóm gộp thành 1 item).
        return this.mWidgetsModel.mFilteredPackageInfo.size() + featuredCount();
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

    /**
     * Holder cho MỘT widget nổi bật. Mỗi loại widget có view type riêng nên holder này chỉ bao giờ
     * nhận đúng loại đó — không còn cảnh một holder hứng 5 loại rồi phải dựng lại view mỗi lần bind.
     */
    public static class FeaturedViewHolder extends RecyclerView.ViewHolder {

        final GalleryWidgetCell mCell;

        public FeaturedViewHolder(@NonNull GalleryWidgetCell cell) {
            super(cell);
            mCell = cell;
        }
    }

}
