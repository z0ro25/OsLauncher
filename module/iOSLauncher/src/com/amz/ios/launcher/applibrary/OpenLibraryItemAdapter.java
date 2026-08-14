package com.amz.ios.launcher.applibrary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amz.ios.ioslite.common.ad.IOSAdConfig;
import com.amz.ios.ioslite.common.ad.IOSAdManager;
import com.amz.ios.ioslite.common.ad.NativeAdCardView;
import com.amz.ios.launcher.AppInfo;
import com.amz.ios.launcher.BubbleTextView;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.TextViewCustomFont;

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
     * Có slot native ad hay không — tính 1 lần lúc dựng adapter. Chưa gắn SDK thì
     * getNativeAd trả null -> false -> ô ad ẩn hoàn toàn, grid không đổi.
     */
    private final boolean mHasAd;

    public OpenLibraryItemAdapter(Launcher launcher){
        mLauncher = launcher;
        mHasAd = IOSAdManager.getInstance(launcher).getNativeAd(IOSAdConfig.ID_ALL_APPS) != null;
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
            com.amz.ios.launcher.DeviceProfile dp = mLauncher.getDeviceProfile();
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
            NativeAdCardView adCardView = (NativeAdCardView) holder.itemView;
            adCardView.setAdvertiseId(IOSAdConfig.ID_ALL_APPS);
            adCardView.loadAdvertise();
        }
        else if (holder instanceof ItemViewHolder){
            AppInfo appInfo = mApps.get(appIndex(position));
            ItemViewHolder item = (ItemViewHolder) holder;
            final BubbleTextView bubbleTextView = (BubbleTextView)item.itemView;
            bubbleTextView.setTag(appInfo);
            bubbleTextView.reapplyItemInfo(appInfo);
            // Màn General "Hide Apps name": ON = ẩn tên app trong App Library (list).
            bubbleTextView.setTextVisibility(
                    !com.amz.ios.launcher.config.Settings.isHideAppLabel(mLauncher));
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
