package com.truongnt.ios.launcher.applibrary;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import androidx.fragment.app.FragmentActivity;

import com.ezt.v2.ezt.admobdemo.ads.core.AdsSdk;
import com.truongnt.ios.ioslite.common.config.AppAds;
import com.truongnt.ios.ioslite.common.util.ActivityUtil;
import com.truongnt.ios.launcher.R;

import java.util.ArrayList;

public class AppLibraryAdapter extends RecyclerView.Adapter {

    /** View type của item quảng cáo — 0 là category nên dùng số âm cho chắc. */
    public static final int TYPE_NATIVE_AD = -1;

    /** Vị trí item quảng cáo: đầu danh sách. */
    private static final int AD_POSITION = 0;

    // Placement native của App Library trong JSON (ads/app-placements.json, placements.json).
    private static final String NATIVE_PLACEMENT = "native_app_library";

    // Danh sách đầy đủ (giữ nguyên thứ tự 10 category cố định) — nguồn dữ liệu gốc.
    ArrayList<AppCategory> mCategories;
    // Danh sách HIỂN THỊ: chỉ các category có app (>0) — dùng để render/đếm, ẩn folder rỗng.
    private final ArrayList<AppCategory> mVisible = new ArrayList<>();

    /** Đã tính quyền hiện quảng cáo chưa; null = chưa tính. Tính một lần rồi giữ nguyên. */
    private Boolean mAdAllowed;

    // ── Trạng thái quảng cáo native trong MỘT lần vào màn ───────────────────────────────
    //
    // Vì sao cần: bindNativeAd() chạy mỗi lần ô ad được bind lại, mà mỗi lần gọi AdsNative.show()
    // từ Activity trần (Launcher) là một request MỚI + đổ THÊM một lớp view vào container cũ
    // (FSDAds không thay thế nội dung cũ). Bind lại liên tục -> quảng cáo nháy liên tục.
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

    /** Container đang giữ ad (hoặc đang chờ ad) — để biết khi nào phải dọn trước khi đổ lại. */
    private FrameLayout mAdContainer;

    /** Gán danh sách category đầy đủ rồi lọc lại danh sách hiển thị (ẩn folder rỗng). */
    public void setCategories(ArrayList<AppCategory> categories) {
        mCategories = categories;
        rebuildVisible();
    }

    /** Lọc lại danh sách hiển thị từ mCategories và refresh (gọi khi nội dung category đổi). */
    public void refresh() {
        rebuildVisible();
        notifyDataSetChanged();
    }

    private void rebuildVisible() {
        mVisible.clear();
        if (mCategories != null) {
            for (AppCategory c : mCategories) {
                if (c != null && c.mApps != null && !c.mApps.isEmpty()) {
                    mVisible.add(c);
                }
            }
        }
    }

    // ── Quảng cáo native ở đầu danh sách ────────────────────────────────────────────────
    //
    // Quảng cáo là một ITEM CỦA ADAPTER, KHÔNG nhét vào mVisible (mVisible là danh sách
    // category thật, dính vào SortAppsCallable + đếm app). Vì vậy mọi chỗ dùng position đều
    // phải quy về chỉ số category qua [categoryIndexFor].

    // Tính một lần rồi giữ nguyên. Chỉ còn vế isAdFree (lambda :app bơm xuống AdsHostConfig);
    // bật/tắt theo vị trí giờ do SDK lo qua serving controls + enabled của placement.
    private boolean isAdAllowed() {
        if (mAdAllowed == null) {
            mAdAllowed = !AdsSdk.INSTANCE.isAdFree();
        }
        return mAdAllowed;
    }

    /** Có chèn item quảng cáo không. Danh sách rỗng thì không chèn — chỉ có mỗi ad trơ trọi. */
    private boolean isAdVisible() {
        return isAdAllowed() && !mVisible.isEmpty();
    }

    /** Vị trí hiển thị -> chỉ số trong [mVisible]. -1 nghĩa là chính item quảng cáo. */
    private int categoryIndexFor(int position) {
        if (!isAdVisible() || position < AD_POSITION) {
            return position;
        }
        return position == AD_POSITION ? -1 : position - 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_NATIVE_AD) {
            return new NativeAdViewHolder(
                    inflater.inflate(R.layout.apps_library_native_ad, parent, false));
        }
        View view = inflater.inflate(R.layout.apps_library_item,parent,false);
        return new ItemViewHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        if (isAdVisible() && position == AD_POSITION) {
            return TYPE_NATIVE_AD;
        }
        return 0;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof NativeAdViewHolder) {
            bindNativeAd((NativeAdViewHolder) holder,position);
            return;
        }

        if (holder instanceof ItemViewHolder) {
            int index = categoryIndexFor(position);
            if (index < 0 || index >= mVisible.size()) {
                return;
            }
            ItemViewHolder itemViewHolder = (ItemViewHolder) holder;
            itemViewHolder.mItemFull.setApps(
                    mVisible.get(index).mApps
            );
            itemViewHolder.mItemFull.setTitle(
                    mVisible.get(index).mCategoryName
            );
        }

    }

    /**
     * Đổ quảng cáo native vào item. Không có ad thì ẩn item để không chừa khoảng trống.
     *
     * <p>Việc quảng cáo chiếm TRỌN 2 cột do {@code SpanSizeLookup} ở
     * {@code AppsLibraryLayout.setUpAdapter()} quyết định — KHÔNG set ở đây. GridLayoutManager
     * lấy span size từ SpanSizeLookup nên {@code LayoutParams.setFullSpan()} gọi ở đây bị bỏ
     * qua, quảng cáo sẽ chỉ rộng 1 cột.
     */
    private void bindNativeAd(final NativeAdViewHolder holder, int post) {
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
            if (post == AD_POSITION) {
                holder.itemView.setVisibility(View.GONE);
            }
            return;
        }

        // Container cao cố định (box 2:1) -> dùng bản Fill để native lấp đầy thay vì wrap_content.
        // Ghi rõ R của library vì file này đang import R của module launcher.
        AppAds.showNativeFill((FragmentActivity) activity, NATIVE_PLACEMENT, container,
                com.truongnt.ios.ioslite.common.R.layout.layout_native_app_libs,
                com.truongnt.ios.ioslite.common.R.layout.shimmer_native_app_libs, hasAd -> {
                    mAdState = hasAd ? AD_LOADED : AD_FAILED;
                    // Holder có thể đã bị tái dùng cho item khác -> chỉ đổi khi vẫn là ô quảng cáo.
                    if (post == AD_POSITION) {
                        holder.itemView.setVisibility(hasAd ? View.VISIBLE : View.GONE);
                    }
                });
    }

    /**
     * Xoá trạng thái ad của lần trước + gỡ view ad khỏi ô.
     *
     * <p>Gọi ở CẢ hai mốc: lúc vào màn (tải lại đúng một lần cho lần vào này) và lúc màn đóng hẳn
     * (để lần mở sau không thấy lại ad cũ rồi bị thay bằng skeleton — nhìn như nháy hai lần).
     */
    public void resetNativeAd() {
        mAdState = AD_IDLE;
        // FSDAds đổ THÊM view vào container chứ không thay thế -> dọn nội dung lần trước, nếu không
        // ad mới sẽ chồng lên ad cũ.
        if (mAdContainer != null) {
            mAdContainer.removeAllViews();
        }
        mAdContainer = null;
    }

    /**
     * Ép bind lại ô quảng cáo. Cần gọi SAU {@link #resetNativeAd()} thì ad mới thật sự được tải lại
     * (không có bind lại thì không có ai gọi AdsNative.show).
     */
    public void notifyAdChanged() {
        if (isAdVisible()) {
            notifyItemChanged(AD_POSITION);
        }
    }

    @Override
    public int getItemCount() {
        return mVisible.size() + (isAdVisible() ? 1 : 0);
    }

    /** ViewHolder của item quảng cáo native — xem {@link #TYPE_NATIVE_AD}. */
    public static class NativeAdViewHolder extends RecyclerView.ViewHolder {

        /** FrameLayout trong cùng của layout; FSDAds đổ nội dung quảng cáo vào đây. */
        public final FrameLayout mContainer;

        public NativeAdViewHolder(@NonNull View itemView) {
            super(itemView);
            // Container là lớp FrameLayout TRONG CÙNG của layout, không phải root — xem
            // apps_library_native_ad.xml để hiểu vì sao có thêm lớp "box" ở giữa.
            this.mContainer = itemView.findViewById(R.id.apps_library_native_ad_container);
            // Thụt lề 20 khớp ĐÚNG padding của ô category (ItemViewHolder dưới cũng setPadding
            // 20) để khung quảng cáo cao bằng khung các ô và hai mép thẳng hàng với ô đầu/cuối.
            // Đơn vị là PIXEL — setPadding/setMargins nhận px chứ không phải dp; khai margin
            // bằng dp trong XML sẽ lệch trên màn mật độ cao.
            ViewGroup.MarginLayoutParams lp =
                    (ViewGroup.MarginLayoutParams) mContainer.getLayoutParams();
            lp.setMargins(20, 20, 20, 20);
        }
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder {

        AppsLibraryItemFull mItemFull;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);

            if (itemView instanceof AppsLibraryItemFull) {
                this.mItemFull = (AppsLibraryItemFull) itemView;
                mItemFull.setPadding(20,20,20,20);
            }
        }
    }
}
