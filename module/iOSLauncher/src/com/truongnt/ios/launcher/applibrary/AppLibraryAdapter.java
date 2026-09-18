package com.truongnt.ios.launcher.applibrary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.truongnt.ios.ioslite.common.ads.Ads;
import com.truongnt.ios.ioslite.common.ads.AdsError;
import com.truongnt.ios.ioslite.common.ads.AdsNative;
import com.truongnt.ios.ioslite.common.ads.AdsNativeCallback;
import com.truongnt.ios.ioslite.common.ads.AdsSlot;
import com.truongnt.ios.launcher.R;

import java.util.ArrayList;

public class AppLibraryAdapter extends RecyclerView.Adapter {

    /** View type của item quảng cáo — 0 là category nên dùng số âm cho chắc. */
    public static final int TYPE_NATIVE_AD = -1;

    /** Vị trí item quảng cáo: đầu danh sách. */
    private static final int AD_POSITION = 0;

    // Danh sách đầy đủ (giữ nguyên thứ tự 10 category cố định) — nguồn dữ liệu gốc.
    ArrayList<AppCategory> mCategories;
    // Danh sách HIỂN THỊ: chỉ các category có app (>0) — dùng để render/đếm, ẩn folder rỗng.
    private final ArrayList<AppCategory> mVisible = new ArrayList<>();

    /** Đã tính quyền hiện quảng cáo chưa; null = chưa tính. Tính một lần rồi giữ nguyên. */
    private Boolean mAdAllowed;

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

    /** Slot quảng cáo có được phép dùng không (tính một lần, sau đó giữ nguyên). */
    private boolean isAdAllowed() {
        if (mAdAllowed == null) {
            mAdAllowed = Ads.isSlotAllowed(AdsSlot.NATIVE_IN_APP);
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
            bindNativeAd((NativeAdViewHolder) holder);
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
    private void bindNativeAd(final NativeAdViewHolder holder) {
        AdsNative.show(holder.mContainer, AdsSlot.NATIVE_IN_APP, new AdsNativeCallback() {
            @Override
            public void onLoaded() {
                holder.itemView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onLoadFailed(AdsError error) {
                holder.itemView.setVisibility(View.GONE);
            }
        });
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
