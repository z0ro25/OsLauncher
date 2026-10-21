package com.truongnt.ios.launcher.leftpage.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import androidx.fragment.app.FragmentActivity;

import com.ezt.v2.ezt.admobdemo.ads.core.AdsSdk;
import com.truongnt.ios.ioslite.common.config.AppAds;
import com.truongnt.ios.ioslite.common.util.ActivityUtil;
import com.truongnt.ios.launcher.LauncherAnimUtils;
import com.truongnt.ios.launcher.R;
import com.truongnt.ios.launcher.bounce.BouncyRecyclerView;
import com.truongnt.ios.launcher.leftpage.views.CustomContentView;
import com.truongnt.ios.launcher.leftpage.custom.CustomZoomImageView;
import com.truongnt.ios.launcher.leftpage.database.WidgetInfo;

import java.util.ArrayList;

public class CustomContentWidgetAdapter extends BouncyRecyclerView.BouncyAdapter<RecyclerView.ViewHolder> {

    // Trạng thái edit khi kéo-thả: phóng to nhẹ + nâng bóng cho item đang kéo, giống
    // cảm giác "nhấc" icon lên ở màn app.
    private static final float DRAG_SCALE = 1.08f;
    private static final float DRAG_ELEVATION = 24.0f;

    /** View type của item quảng cáo. Số âm để KHÔNG đụng dải type widget (20..71, 100). */
    public static final int TYPE_NATIVE_AD = -1;

    /** Vị trí item quảng cáo: ngay dưới 2 widget đầu tiên. */
    private static final int AD_POSITION = 2;

    private final CustomContentView mCustomContentView;
    private final ArrayList<WidgetInfo> mWidgetInfoArrayList;

    // Placement native màn trái; khớp với preload ở CustomContentView.
    public static final String NATIVE_PLACEMENT = "native_left_page";

    /** Vị trí quảng cáo được phép dùng lúc dựng adapter (đã mua bản không qc/tắt native -> false). */
    private final boolean mAdAllowed;

    public CustomContentWidgetAdapter(CustomContentView customContentView, ArrayList<WidgetInfo> arrayList) {
        this.mCustomContentView = customContentView;
        this.mWidgetInfoArrayList = arrayList;
        // Đã mua bản không quảng cáo -> không chừa chỗ cho ô quảng cáo. Cờ Remote Config theo
        // vị trí đã bỏ cùng RemoteConfigs; công tắc bật/tắt do SDK lo.
        this.mAdAllowed = !AdsSdk.INSTANCE.isAdFree();
    }

    // ── Trạng thái quảng cáo native trong MỘT lần vào màn ───────────────────────────────
    //
    // Vì sao cần: bindNativeAd() chạy mỗi lần ô ad được bind lại, mà mỗi lần gọi AdsNative.show()
    // từ Activity trần (Launcher) là một request MỚI (AdsNative tự phát request khi chưa có ad
    // tải sẵn) + đổ thêm một lớp view vào container. Bind lại liên tục -> quảng cáo nháy.
    // Nên mỗi lần vào màn chỉ phát ĐÚNG MỘT request, tới khi user rời màn rồi vào lại.

    /** Chưa phát request nào trong lần vào màn này. */
    private static final int AD_IDLE = 0;
    /** Đã phát request, đang chờ kết quả. */
    private static final int AD_LOADING = 1;
    /** Đã đổ được ad vào container. */
    private static final int AD_LOADED = 2;
    /** Request thất bại — không thử lại cho tới lần vào màn sau. */
    private static final int AD_FAILED = 3;

    private int mAdState = AD_IDLE;

    /** Container đang giữ ad — để biết view cũ đã bị bỏ mà bắt đầu lại từ đầu. */
    private FrameLayout mAdContainer;

    // ── Quảng cáo native: chèn như một ITEM CỦA ADAPTER, không nhét vào mWidgetInfoArrayList ──
    //
    // Vì sao không thêm một WidgetInfo giả vào list: WidgetInfo là bản ghi DB (có save()/delete()),
    // lại dính vào kéo-thả sắp xếp + nút xoá + lưu order. Thêm giả sẽ hỏng cả ba.
    // Thay vào đó quảng cáo chiếm một vị trí HIỂN THỊ riêng, còn chỉ số trong list widget
    // được suy ra qua [widgetIndexFor] — mọi chỗ dùng position đều phải đi qua hàm này.

    // Ad lỗi trong lần vào màn này -> gỡ HẲN item để widget dồn lên (item GONE vẫn ăn margin
    // của ItemDecoration). Hạ ở resetNativeAd() để lần vào sau chèn lại và thử lại.
    private boolean mAdFailedThisVisit;

    /** Có chèn item quảng cáo không. Cần >= AD_POSITION widget để quảng cáo còn chỗ nằm. */
    private boolean isAdVisible() {
        return mAdAllowed
                && !mAdFailedThisVisit
                && mWidgetInfoArrayList != null
                && mWidgetInfoArrayList.size() >= AD_POSITION;
    }

    // Gỡ item ad khỏi list. Post trên RecyclerView: callback có thể về lúc nó đang layout.
    private void removeAdItem() {
        final RecyclerView rv = mCustomContentView.mListWidgetRV;
        if (rv == null) {
            return;
        }
        rv.post(new Runnable() {
            @Override
            public void run() {
                if (!isAdVisible()) {
                    return;
                }
                mAdFailedThisVisit = true;
                notifyItemRemoved(AD_POSITION);
            }
        });
    }

    /** Adapter hiện có chèn item quảng cáo không — để call-site biết cấu trúc list có đổi. */
    public boolean hasNativeAdItem() {
        return isAdVisible();
    }

    /** Vị trí HIỂN THỊ của widget ở chỉ số {@code widgetIndex} trong mWidgetInfoArrayList. */
    public int displayPositionForWidget(int widgetIndex) {
        return isAdVisible() && widgetIndex >= AD_POSITION ? widgetIndex + 1 : widgetIndex;
    }

    /** Vị trí hiển thị -> chỉ số trong [mWidgetInfoArrayList]. -1 nghĩa là chính item quảng cáo. */
    private int widgetIndexFor(int position) {
        if (!isAdVisible() || position < AD_POSITION) {
            return position;
        }
        return position == AD_POSITION ? -1 : position - 1;
    }

    /**
     * Như [widgetIndexFor] nhưng dùng cho kéo-thả — KHÔNG bao giờ trả -1.
     *
     * Thả trúng ngay chỗ quảng cáo thì coi như thả xuống ngay trên nó, thay vì để
     * ItemTouchHelper nhận chỉ số âm rồi làm hỏng list.
     */
    private int widgetIndexForMove(int position) {
        int index = widgetIndexFor(position);
        return index < 0 ? Math.max(0, AD_POSITION - 1) : index;
    }

    /** Đổ quảng cáo native vào item. Không có ad thì ẩn item để không chừa khoảng trống. */
    private void bindNativeAd(final NativeAdViewHolder holder,int post) {
        StaggeredGridLayoutManager.LayoutParams lp =
                (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();
        // Quảng cáo chiếm nguyên hàng, không đứng nửa hàng như widget 2x2.
        lp.setFullSpan(true);

        final FrameLayout container = holder.mContainer;
        // Container khác container đang giữ ad -> view cũ đã bị bỏ, phải bắt đầu lại từ đầu.
        if (mAdContainer != container) {
            mAdState = AD_IDLE;
            mAdContainer = container;
        }

        // Mỗi lần vào màn chỉ MỘT request: đang chờ / đã có ad / đã thất bại đều không gọi lại.
        // Đây là chỗ chặn quảng cáo nháy — xem khối comment ở khai báo mAdState.
        if (mAdState != AD_IDLE) {
            return;
        }

        mAdState = AD_LOADING;

        // AdsKit chỉ show từ FragmentActivity; dò từ context của container vì view trong
        // RecyclerView giữ ContextThemeWrapper chứ không phải chính Activity.
        final Activity activity = ActivityUtil.findActivity(container.getContext());
        if (!(activity instanceof FragmentActivity)) {
            mAdState = AD_FAILED;
            removeAdItem();
            return;
        }

        // Khung ad luôn hiện ngay với skeleton; có ad thì hiện ad, lỗi thì gỡ hẳn item.
        holder.itemView.setVisibility(View.VISIBLE);
        // Ghi rõ R của library vì file này đang import R của module launcher.
        AppAds.showNative((FragmentActivity) activity, NATIVE_PLACEMENT, container,
                com.truongnt.ios.ioslite.common.R.layout.layout_native_left_page,
                com.truongnt.ios.ioslite.common.R.layout.shimmer_native_left_page, hasAd -> {
                    mAdState = hasAd ? AD_LOADED : AD_FAILED;
                    if (!hasAd) {
                        removeAdItem();
                    }
                });
    }

    /**
     * Lần vào màn MỚI: xoá trạng thái của lần trước để ô quảng cáo được tải lại đúng một lần.
     *
     * <p>Gọi từ {@link CustomContentView} lúc user mở màn trái, KHÔNG gọi trong lúc đang ở màn.
     *
     * <p>Cố ý KHÔNG dọn container: container của màn trái là {@code wrap_content} nên nếu dọn thì
     * ô quảng cáo trống trơn trong lúc chờ ad mới (đúng thứ gây cảm giác nháy). Giữ ad cũ hiển thị
     * tới khi ad mới được đổ vào — AdsNative.pourNativeAdView tự removeAllViews trước khi addView.
     */
    public void resetNativeAd() {
        mAdState = AD_IDLE;
        mAdContainer = null;
    }

    // Lần vào màn mới: nếu lần trước ad lỗi thì chèn lại item để thử lại. Trả true nếu có chèn.
    public boolean restoreAdItemIfRemoved() {
        if (!mAdFailedThisVisit) {
            return false;
        }
        mAdFailedThisVisit = false;
        if (!isAdVisible()) {
            return false;
        }
        notifyItemInserted(AD_POSITION);
        return true;
    }

    /**
     * Gỡ view ad khỏi container — gọi khi màn trái ĐÓNG HẲN.
     *
     * <p>Khác {@link #resetNativeAd()} (cố ý giữ view để không nháy trong lúc mở màn): hàm này
     * xoá hẳn để lần mở sau ô quảng cáo bắt đầu từ trắng, không hiện lại ad cũ.
     */
    public void clearNativeAdView() {
        if (mAdContainer != null) {
            mAdContainer.removeAllViews();
        }
    }

    /** Ép bind lại ô quảng cáo — cần gọi sau {@link #resetNativeAd()} thì ad mới được tải lại. */
    public void notifyAdChanged() {
        if (isAdVisible()) {
            notifyItemChanged(AD_POSITION);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (i == TYPE_NATIVE_AD) {
            return new NativeAdViewHolder(
                    inflater.inflate(R.layout.left_page_native_ad, parent, false)
            );
        }

        if (i == 100){
            return new WidgetItemViewHolder(
                    inflater.inflate(R.layout.edit_widget_button,parent,false)
            );
        }

        int resId = -1;

        if (i == 20) {
            resId = R.layout.widget_clock_2x2;
        }
        else if (i == 21) {
            resId = R.layout.widget_clock_2x4;
        }
        else if (i == 30) {
            resId = R.layout.widget_photo_2x2;
        }
        else if (i == 40) {
            resId = R.layout.widget_calendar_2x2;
        }
        else if (i == 42) {
            // Lịch lưới tháng (đủ ngày) 2x2 — mặc định mới, đồng bộ thẻ nổi bật ở khay.
            resId = R.layout.widget_calendar_month_2x2;
        }
        else if (i == 41) {
            resId = R.layout.widget_calendar_2x4;
        }
        else if (i == 61) {
            resId = R.layout.widget_battery_2x2;
        }
        else if (i == 60) {
            resId = R.layout.widget_battery_2x4;
        }
        else if (i == 70 || i == 71) {
            resId = R.layout.widget_suggestion_2x4;
        }
        else {
            resId = R.layout.widget_clock_2x4;
//            return new LauncherWidgetListViewHolder(null);
        }
        return new LauncherWidgetListViewHolder(
                inflater.inflate(resId,parent,false)
        );

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof NativeAdViewHolder) {
            bindNativeAd((NativeAdViewHolder) holder,position);
        }
        else if (holder instanceof WidgetItemViewHolder) {
            WidgetItemViewHolder viewHolder = (WidgetItemViewHolder) holder;
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    mCustomContentView.startShaking();
                    mCustomContentView.enableButtons();
                }
            });
            ((StaggeredGridLayoutManager.LayoutParams) ((viewHolder).itemView.getLayoutParams())).setFullSpan(true);
        }
        else if (holder instanceof LauncherWidgetListViewHolder) {
            LauncherWidgetListViewHolder viewHolder = (LauncherWidgetListViewHolder) holder;
            CustomZoomImageView customZoomImageView = viewHolder.mDeleteBtn;
            // position là vị trí HIỂN THỊ (đã tính cả item quảng cáo) -> phải quy về chỉ số widget
            // trước khi đụng vào mWidgetInfoArrayList, nếu không nút xoá sẽ xoá nhầm widget.
            final int pos = widgetIndexFor(position);
            if (pos < 0 || pos >= mWidgetInfoArrayList.size()) {
                return;
            }
            if (customZoomImageView != null) {
                customZoomImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public final void onClick(View view) {
                        if (pos >= mWidgetInfoArrayList.size())
                            return;
                        if (mCustomContentView.mListWidgetRV == null || mCustomContentView.mWidgetListAdapter == null)
                            return;
                        final WidgetInfo info = mWidgetInfoArrayList.get(pos);
                        // Xoá xuống dưới mốc AD_POSITION widget thì item quảng cáo cũng biến mất
                        // -> cấu trúc list đổi ở hai chỗ, notifyItemRemoved không đủ.
                        final boolean adBefore = isAdVisible();
                        mWidgetInfoArrayList.remove(pos);
                        if (adBefore != isAdVisible()) {
                            notifyDataSetChanged();
                        } else {
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, mWidgetInfoArrayList.size(),null);
                        }
                        new Thread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        mCustomContentView.mWidgetInfoList.remove(info);
                                        info.delete();
                                    }
                                }
                        ).start();
                    }
                });
            }

            StaggeredGridLayoutManager.LayoutParams lp =
                    (StaggeredGridLayoutManager.LayoutParams) viewHolder.itemView.getLayoutParams();
            lp.setFullSpan(isFullSpanType(this.mWidgetInfoArrayList.get(pos).type));

            CustomContentView customContentView = this.mCustomContentView;
            if (customContentView != null) {
                if (customContentView.t) {
                    viewHolder.startShaking();
                } else {
                    viewHolder.hide();
                }
            }
            viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public final boolean onLongClick(View view) {
                    if (CustomContentWidgetAdapter.this.mCustomContentView == null) return true;
                    CustomContentWidgetAdapter.this.mCustomContentView.enableButtons();
                    CustomContentWidgetAdapter.this.mCustomContentView.startShaking();
                    return true;
                }
            });
        }
    }

    // Widget nào chiếm nguyên hàng (layout 2x4) thì full span; widget 2x2 thì nửa hàng.
    // Ánh xạ khớp với resId trong onCreateViewHolder (tránh dựa vào type % 10 vốn sai với
    // battery: 60=2x4 nhưng 60%10==0, 61=2x2 nhưng 61%10==1 -> bị đảo kích cỡ).
    private static boolean isFullSpanType(int type) {
        switch (type) {
            case 21:  // clock 2x4
            case 41:  // calendar 2x4
            case 60:  // battery 2x4
            case 70:  // app suggestion
            case 71:  // app suggestion
                return true;
            default:  // 20/30/40/61 -> 2x2 (nửa hàng)
                return false;
        }
    }

    @Override
    public int getItemCount() {
        // Bỏ nút "Chỉnh sửa" (item type 100 ở cuối). Vào chế độ edit bằng cách nhấn giữ
        // widget (long-press) -> rung + kéo thả sắp xếp, giống màn app. Nên chỉ đếm số widget.
        ArrayList<WidgetInfo> arrayList = this.mWidgetInfoArrayList;
        if (arrayList != null) {
            return arrayList.size() + (isAdVisible() ? 1 : 0);
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (isAdVisible() && position == AD_POSITION) {
            return TYPE_NATIVE_AD;
        }
        if (this.mWidgetInfoArrayList != null) {
            int index = widgetIndexFor(position);
            if (index >= 0 && index < this.mWidgetInfoArrayList.size()) {
                return this.mWidgetInfoArrayList.get(index).type;
            }
        }
        return 0;
    }

    /**
     * Có được kéo item ở vị trí này không. Quảng cáo đứng yên một chỗ nên cấm kéo —
     * xem {@code DragDropCallBack.getMovementFlags} và {@link #TYPE_NATIVE_AD}.
     */
    @Override
    public boolean canDragItem(int position) {
        return !(isAdVisible() && position == AD_POSITION);
    }

    @Override
    public void onItemMoved(int fromPosition, int toPosition) {
        // Tham số là vị trí HIỂN THỊ (đã tính item quảng cáo) -> quy về chỉ số widget trước khi
        // sửa list. notifyItemMoved vẫn dùng vị trí hiển thị: quảng cáo không đổi chỗ nên cách
        // ánh xạ chỉ số widget -> vị trí hiển thị giữ nguyên sau khi đổi thứ tự.
        int from = widgetIndexForMove(fromPosition);
        int to = widgetIndexForMove(toPosition);
        if (from >= this.mWidgetInfoArrayList.size() || to >= this.mWidgetInfoArrayList.size()) {
            return;
        }
        if (this.mWidgetInfoArrayList.size() > from) {
            this.mWidgetInfoArrayList.add(to, this.mWidgetInfoArrayList.remove(from));
            notifyItemMoved(fromPosition, toPosition);
            final CustomContentView customContentView = this.mCustomContentView;


            new Thread(new Runnable() {
                @Override
                public void run() {
                    customContentView.getClass();
                    for (int i = 0; i < customContentView.mWidgetInfoList.size(); i++) {
                        try {
                            WidgetInfo info = customContentView.mWidgetInfoList.get(i);
                            int indexOf = customContentView.mWidgetInfoList.indexOf(info);
                            info.order = indexOf;
                            // TODO: 2023.11.17 DataBase save
                            info.save();
                        } catch (Throwable th) {
                            th.getMessage();
                            return;
                        }
                    }
                }
            }).start();

        }
    }

    @Override
    public void onItemSwipedToStart(RecyclerView.ViewHolder viewHolder, int positionOfItem) {

    }

    @Override
    public void onItemSwipedToEnd(RecyclerView.ViewHolder viewHolder, int positionOfItem) {

    }

    @Override
    public void onItemSelected(@NonNull RecyclerView.ViewHolder viewHolder) {
        // Bắt đầu kéo: phóng to nhẹ + nâng bóng đổ giống khi kéo icon ở màn app
        // (item được "nhấc lên" khỏi lưới). Tắt rung của chính item đang kéo cho gọn.
        if (viewHolder == null) return;
        if (viewHolder instanceof LauncherWidgetListViewHolder) {
            ((LauncherWidgetListViewHolder) viewHolder).stopShaking();
        }
        View v = viewHolder.itemView;
        v.animate()
                .scaleX(DRAG_SCALE).scaleY(DRAG_SCALE)
                .setDuration(150L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        v.setElevation(DRAG_ELEVATION);
    }

    @Override
    public void onItemReleased(RecyclerView.ViewHolder viewHolder) {
        // Thả ra: item trở về kích cỡ thường + hạ bóng, và rung lại nếu vẫn đang ở
        // chế độ edit (giống các item khác) để người dùng biết vẫn có thể tiếp tục sắp xếp.
        if (viewHolder == null) return;
        View v = viewHolder.itemView;
        v.animate()
                .scaleX(1.0f).scaleY(1.0f)
                .setDuration(150L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        v.setElevation(0.0f);
        if (mCustomContentView != null && mCustomContentView.t
                && viewHolder instanceof LauncherWidgetListViewHolder) {
            ((LauncherWidgetListViewHolder) viewHolder).startShaking();
        }
    }

    /** ViewHolder của item quảng cáo native — xem {@link #TYPE_NATIVE_AD}. */
    public static class NativeAdViewHolder extends RecyclerView.ViewHolder {

        /** Chính là root FrameLayout; FSDAds đổ nội dung quảng cáo vào đây. */
        public final FrameLayout mContainer;

        public NativeAdViewHolder(@NonNull View itemView) {
            super(itemView);
            this.mContainer = (FrameLayout) itemView;
        }
    }

    public class WidgetItemViewHolder extends RecyclerView.ViewHolder{

        public WidgetItemViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public static class LauncherWidgetListViewHolder extends RecyclerView.ViewHolder {

        public CustomZoomImageView mDeleteBtn;
        public ObjectAnimator mObjectAnimator;

        public LauncherWidgetListViewHolder(View view) {
            super(view);
            float width = view.getWidth();
            float height = view.getHeight();
            this.mDeleteBtn = (CustomZoomImageView) view.findViewById(R.id.icon_delete_widget);
            this.mObjectAnimator = LauncherAnimUtils.ofPropertyValuesHolder(view, PropertyValuesHolder.ofFloat("pivotX", ((((float) Math.random()) * 0.38f) + 0.2f) * width, ((((float) Math.random()) * 0.38f) + 0.2f) * width), PropertyValuesHolder.ofFloat("pivotY", ((((float) Math.random()) * 0.38f) + 0.2f) * height, ((((float) Math.random()) * 0.38f) + 0.2f) * height), PropertyValuesHolder.ofFloat("rotation", ((float) (Math.random() * 0.10000000149011612d)) - 0.3926991f, ((float) (Math.random() * 0.10000000149011612d)) + 0.3926991f));
            mObjectAnimator.setDuration((long) ((Math.random() * 36.0d) + 113.0d));
            mObjectAnimator.setRepeatCount(ValueAnimator.INFINITE);
            mObjectAnimator.setRepeatMode(ValueAnimator.REVERSE);
            mObjectAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            mObjectAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    animation.removeAllListeners();
                }
            });
        }

        public final void hide() {
            if (this.mDeleteBtn != null) {
                this.mDeleteBtn.setVisibility(View.INVISIBLE);
            }
            if (this.mObjectAnimator != null) {
                this.mObjectAnimator.cancel();
                this.itemView.setRotation(0.0f);
            }
        }

        public final void startShaking() {
            if (this.mDeleteBtn != null) {
                this.mDeleteBtn.setVisibility(View.VISIBLE);
            }
        if (this.mObjectAnimator != null) {
                this.mObjectAnimator.start();
            }
        }

        public final void stopShaking(){
            CustomZoomImageView customZoomImageView = this.mDeleteBtn;
            if (customZoomImageView != null) {
                customZoomImageView.setVisibility(View.INVISIBLE);
            }
            ObjectAnimator objectAnimator = this.mObjectAnimator;
            if (objectAnimator != null) {
                objectAnimator.cancel();
                this.itemView.setRotation(0.0f);
            }
        }

    }
}
