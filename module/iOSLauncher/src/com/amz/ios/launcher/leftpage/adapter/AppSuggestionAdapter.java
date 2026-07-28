package com.amz.ios.launcher.leftpage.adapter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amz.ios.launcher.DeviceProfile;
import com.amz.ios.launcher.FastBitmapDrawable;
import com.amz.ios.launcher.IconCache;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.Utilities;
import com.amz.ios.launcher.leftpage.drawables.ClockDrawable;
import com.amz.ios.launcher.leftpage.model.AppSuggestionInfo;

import java.util.ArrayList;

public class AppSuggestionAdapter extends RecyclerView.Adapter {

    public ArrayList<AppSuggestionInfo> mAppSuggestionInfoArrayList;
    public Launcher mLauncher;
    public int mLoadAnimIndex = -1;
    DeviceProfile mGrid;
    public LayoutInflater mLayoutInflater;
    IconCache mIconCache;

    public AppSuggestionAdapter(Context context) {
        mLauncher = (Launcher) context;
        mIconCache = mLauncher.getIconCache();
        mLayoutInflater = LayoutInflater.from(context);
        mGrid = mLauncher.getDeviceProfile();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mLayoutInflater.inflate(
                R.layout.suggestion_item,
                parent,
                false
        );
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ItemViewHolder) {
            ItemViewHolder itemViewHolder = (ItemViewHolder) holder;

            final AppSuggestionInfo info = mAppSuggestionInfoArrayList.get(position);
            if (info == null) return;

            Bitmap bitmap = ClockDrawable.getIconBitmap(
                    Utilities.createIconBitmap(
                            info.mBitmap,mLauncher
                    )
            );

            itemViewHolder.mAppSuggestionIV.setImageBitmap(
                    bitmap
            );


            itemViewHolder.itemView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent;
                            ComponentName componentName = ComponentName.unflattenFromString(info.mComponentName);
                            if (componentName == null || (intent = Intent.makeRestartActivityTask(componentName)) == null) {
                                return;
                            }
                            mLauncher.startActivity(intent);
                        }
                    }
            );

            if (position > mLoadAnimIndex){
                // TODO: 2023.11.20 Set Load Anim
                mLoadAnimIndex = position;
            }
        }
    }

    @Override
    public int getItemCount() {
        if (mAppSuggestionInfoArrayList == null) return 0;
        return mAppSuggestionInfoArrayList.size();
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder {

        ImageView mAppSuggestionIV;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            mAppSuggestionIV = itemView.findViewById(R.id.icon_suggestion);
        }
    }
}
