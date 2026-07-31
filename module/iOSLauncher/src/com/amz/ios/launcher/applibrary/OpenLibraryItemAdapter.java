package com.amz.ios.launcher.applibrary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amz.ios.launcher.AppInfo;
import com.amz.ios.launcher.BubbleTextView;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.TextViewCustomFont;

import java.util.ArrayList;

public class OpenLibraryItemAdapter extends RecyclerView.Adapter {

    public String mLabel;
    public ArrayList<AppInfo> mApps = new ArrayList<>();
    public Launcher mLauncher;

    public OpenLibraryItemAdapter(Launcher launcher){
        mLauncher = launcher;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == 0){
            View view = inflater.inflate(R.layout.apps_library_folder_header,parent,false);
            return new HeaderViewHolder(view);
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
        else if (holder instanceof ItemViewHolder){
            AppInfo appInfo = mApps.get(position - 1);
            ItemViewHolder item = (ItemViewHolder) holder;
            final BubbleTextView bubbleTextView = (BubbleTextView)item.itemView;
            bubbleTextView.setTag(appInfo);
            bubbleTextView.reapplyItemInfo(appInfo);
            bubbleTextView.setOnClickListener(
                mLauncher
            );
        }
    }

    @Override
    public int getItemCount() {
        if (mApps != null) return mApps.size() + 1;
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? 0 : 1;
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
}
