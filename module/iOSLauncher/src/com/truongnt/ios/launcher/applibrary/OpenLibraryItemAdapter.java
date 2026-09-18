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
import com.truongnt.ios.launcher.AppInfo;
import com.truongnt.ios.launcher.BubbleTextView;
import com.truongnt.ios.launcher.Launcher;
import com.truongnt.ios.launcher.R;
import com.truongnt.ios.launcher.TextViewCustomFont;

import java.util.ArrayList;

public class OpenLibraryItemAdapter extends RecyclerView.Adapter {

    // View types: 0 = header (label category), 1 = app icon, 2 = ô quảng cáo native.
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_APP = 1;
    private static final int VIEW_TYPE_AD = 2;

    // Vị trí ô ad (ngay sau header). Chỉ tồn tại khi có ad -> mHasAd.
    private static final int AD_POSITION = 1;

    public String mLabel;
    public ArrayList<AppInfo> mApps = new ArrayList<>();
    public Launcher mLauncher;

    /**
     * Có chừa ô quảng cáo hay không — quyết định theo CẤU HÌNH slot (tắt cờ / user đã mua
     * bản không quảng cáo), không phải theo việc ad đã tải xong chưa, vì việc tải là bất
     * đồng bộ. Không chừa -> grid giữ nguyên như trước.
     */
    private final boolean mHasAd;

    public OpenLibraryItemAdapter(Launcher launcher){
        mLauncher = launcher;
        mHasAd = Ads.isSlotAllowed(AdsSlot.NATIVE_IN_APP);

        if (mHasAd) {
            // Tải trước ngay khi dựng adapter; lúc bind chỉ việc đổ vào container.
            // onLoaded có thể về sau lúc bind đầu tiên, nên phải bind lại ô ad khi xong.
            AdsNative.preload(launcher, AdsSlot.NATIVE_IN_APP, new AdsNativeCallback() {
                @Override
                public void onLoaded() {
                    mLauncher.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            notifyItemChanged(AD_POSITION);
                        }
                    });
                }
            });
        }
    }

    /** Ô ad có hiển thị ở {@code position} không. */
    public boolean isAdPosition(int position) {
        return mHasAd && position == AD_POSITION;
    }

    /** Ánh xạ position của list sang index trong {@link #mApps} (bù header + ô ad). */
    private int appIndex(int position) {
        int offset = 1; // header
        if (mHasAd && position > AD_POSITION) {
            offset++; // ô ad nằm trước vị trí này
        }
        return position - offset;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_HEADER){
            View view = inflater.inflate(R.layout.apps_library_folder_header,parent,false);
            return new HeaderViewHolder(view);
        }
        else if (viewType == VIEW_TYPE_AD){
            View view = inflater.inflate(R.layout.all_apps_ad_view,parent,false);
            return new AdViewHolder(view);
        }
        else {
            View view = inflater.inflate(R.layout.apps_library_folder_item,parent,false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder header = (HeaderViewHolder) holder;
            TextViewCustomFont textViewCustomFont = (TextViewCustomFont)header.itemView;
            textViewCustomFont.setText(mLabel);

            // Căn tiêu đề category thẳng với mép trái của icon app hàng đầu.
            // Mỗi icon nằm GIỮA ô lưới (4 cột) nên icon đầu bị thụt vào (cellWidth - iconSize)/2,
            // trong khi tiêu đề bắt đầu từ mép ô => trông "sát lề trái" hơn app bên dưới.
            // Bù đúng khoảng thụt đó vào paddingStart để tiêu đề khớp với icon đầu tiên.
            com.truongnt.ios.launcher.DeviceProfile dp = mLauncher.getDeviceProfile();
            int gridWidth = dp.getCurrentWidth() - (dp.edgeMarginPx * 2);
            int cellWidth = gridWidth / 4;
            int indent = Math.max(0, (cellWidth - dp.iconSizePx) / 2);
            textViewCustomFont.setPaddingRelative(
                    indent,
                    textViewCustomFont.getPaddingTop(),
                    textViewCustomFont.getPaddingEnd(),
                    textViewCustomFont.getPaddingBottom());
        }
        else if (holder instanceof AdViewHolder){
            final View container = holder.itemView;
            // Adapter tái dùng view, mà FSDAds đổ nội dung vào container — chỉ đổ khi
            // container còn trống, tránh nhồi chồng khi bind lại.
            if (container instanceof FrameLayout && ((FrameLayout) container).getChildCount() == 0) {
                AdsNative.show((FrameLayout) container, AdsSlot.NATIVE_IN_APP, new AdsNativeCallback() {

                    @Override
                    public void onLoaded() {
                        container.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onLoadFailed(AdsError error) {
                        // Chưa kịp có ad -> thu ô lại thay vì để khoảng trống.
                        container.setVisibility(View.GONE);
                    }
                });
            }
        }
        else if (holder instanceof ItemViewHolder){
            AppInfo appInfo = mApps.get(appIndex(position));
            ItemViewHolder item = (ItemViewHolder) holder;
            final BubbleTextView bubbleTextView = (BubbleTextView)item.itemView;
            bubbleTextView.setTag(appInfo);
            bubbleTextView.reapplyItemInfo(appInfo);
            // Màn General "Hide Apps name": ON = ẩn tên app trong App Library (list).
            bubbleTextView.setTextVisibility(
                    !com.truongnt.ios.launcher.config.Settings.isHideAppLabel(mLauncher));
            bubbleTextView.setOnClickListener(
                mLauncher
            );
        }
    }

    @Override
    public int getItemCount() {
        if (mApps == null) return 0;
        return mApps.size() + 1 + (mHasAd ? 1 : 0); // + header (+ ô ad)
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return VIEW_TYPE_HEADER;
        if (isAdPosition(position)) return VIEW_TYPE_AD;
        return VIEW_TYPE_APP;
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder {
        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            if (itemView instanceof BubbleTextView)
            {
                BubbleTextView view = (BubbleTextView) itemView;
                view.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            }
        }
    }

    public class HeaderViewHolder extends RecyclerView.ViewHolder {

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public class AdViewHolder extends RecyclerView.ViewHolder {

        public AdViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
