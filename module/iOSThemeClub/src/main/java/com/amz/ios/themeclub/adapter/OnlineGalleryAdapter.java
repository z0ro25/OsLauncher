package com.amz.ios.themeclub.adapter;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.app.AppConfig;
import com.amz.ios.themeclub.util.DensityUtils;

import java.util.List;

/**
 * Created by lideqian on 16-12-12.
 */
public class OnlineGalleryAdapter extends RecyclerView.Adapter<OnlineGalleryAdapter.ViewHolder> {
    private LayoutInflater mInflater;
    List<String> data;
    private Context mContext;
    private int mType;

    public OnlineGalleryAdapter(Context context, List<String> datas, int type) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        data = datas;
        mType = type;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View arg0) {
            super(arg0);
        }
        ImageView mImg;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = null;
        if (mType== AppConfig.TAB_LOCK){
            view = mInflater.inflate(R.layout.themeclub_lock_screen_gallery, viewGroup, false);
        }else if (mType== AppConfig.TAB_THEME){
            view = mInflater.inflate(R.layout.themeclub_theme_gallery, viewGroup, false);
        }
        ViewHolder viewHolder = new ViewHolder(view);
        viewHolder.mImg = (ImageView) view.findViewById(R.id.preview_view_pager);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int i) {
        if (mType== AppConfig.TAB_LOCK){
            Glide.with(mContext).load(data.get(i)).override(DensityUtils.dip2px(mContext, mContext.getResources().getDimension(R.dimen.themeclub_lock_screen_detail_shot_width)), DensityUtils.dip2px(mContext, mContext.getResources().getDimension(R.dimen.themeclub_lock_screen_detail_shot_height))).centerCrop().into(viewHolder.mImg);
        }else if (mType== AppConfig.TAB_THEME){
            Glide.with(mContext).load(data.get(i)).into(viewHolder.mImg);
        }
    }

}
