package com.truongnt.ios.launcher.applibrary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.truongnt.ios.launcher.R;

import java.util.ArrayList;

public class AppLibraryAdapter extends RecyclerView.Adapter {

    // Danh sách đầy đủ (giữ nguyên thứ tự 10 category cố định) — nguồn dữ liệu gốc.
    ArrayList<AppCategory> mCategories;
    // Danh sách HIỂN THỊ: chỉ các category có app (>0) — dùng để render/đếm, ẩn folder rỗng.
    private final ArrayList<AppCategory> mVisible = new ArrayList<>();

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

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.apps_library_item,parent,false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof ItemViewHolder) {
            ItemViewHolder itemViewHolder = (ItemViewHolder) holder;
            itemViewHolder.mItemFull.setApps(
                    mVisible.get(position).mApps
            );
            itemViewHolder.mItemFull.setTitle(
                    mVisible.get(position).mCategoryName
            );
        }

    }

    @Override
    public int getItemCount() {
        return mVisible.size();
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
