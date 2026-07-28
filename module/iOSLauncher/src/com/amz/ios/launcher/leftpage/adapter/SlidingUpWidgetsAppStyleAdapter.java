package com.amz.ios.launcher.leftpage.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.leftpage.model.CustomWidgetDetailInfo;

import java.util.ArrayList;

public class SlidingUpWidgetsAppStyleAdapter extends RecyclerView.Adapter {

    Launcher mLauncher;
    LayoutInflater mLayoutInflater;
    public ArrayList<CustomWidgetDetailInfo> mWidgetDetails;

    public SlidingUpWidgetsAppStyleAdapter(Launcher mLauncher, ArrayList<CustomWidgetDetailInfo> details) {
        this.mLauncher = mLauncher;
        mLayoutInflater = LayoutInflater.from(mLauncher);
        mWidgetDetails = details;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mLayoutInflater.inflate(R.layout.widgets_app_style_item,parent,false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        CustomWidgetDetailInfo detail = mWidgetDetails.get(position);
        if (holder instanceof ItemViewHolder) {
            ItemViewHolder viewHolder = (ItemViewHolder) holder;
            viewHolder.mWidgetIV.setImageDrawable(
                    detail.mDrawable
            );
        }
    }

    @Override
    public int getItemCount() {
        if (mWidgetDetails == null) return 0;
        return mWidgetDetails.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {

        AppCompatImageView mWidgetIV;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            mWidgetIV = itemView.findViewById(R.id.widgets_app_style_item_image);
        }
    }
}
