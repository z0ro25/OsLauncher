package com.amz.ios.launcher.applibrary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amz.ios.launcher.R;

import java.util.ArrayList;

public class AppLibraryAdapter extends RecyclerView.Adapter {

    ArrayList<AppCategory> mCategories;

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
                    mCategories.get(position).mApps
            );
            itemViewHolder.mItemFull.setTitle(
                    mCategories.get(position).mCategoryName
            );
        }

    }

    @Override
    public int getItemCount() {
        if (mCategories == null) return 0;
        return mCategories.size();
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
