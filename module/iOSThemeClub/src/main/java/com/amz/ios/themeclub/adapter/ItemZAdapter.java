package com.amz.ios.themeclub.adapter;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.bean.WallPapersBean;
import com.amz.ios.themeclub.intertfaces.ItemZClickListener;

import java.util.List;

/**
 * Created by ZhangMingZhe on 11/16/16.
 */

public class ItemZAdapter extends RecyclerView.Adapter<ItemZAdapter.ImgViewHolder> {
    private Context mContext;
    private final String TAG = ItemZAdapter.class.getSimpleName();
    private List<WallPapersBean> list ;
    private ItemZClickListener onClickListener;
    private int resId;
    public ItemZAdapter(Context mContext,List<WallPapersBean> list,int resId) {
        this.mContext = mContext;
        this.list = list;
        this.resId = resId;
    }

    @Override
    public ImgViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mContext).inflate(resId,null);
        ImgViewHolder holder = new ImgViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(ImgViewHolder holder, int position) {

        final WallPapersBean bean = list.get(position);
        Glide.with(mContext).load(bean.getSmallImage().getDownloadUrl()).placeholder(R.drawable.img_small).into(holder.mImageView);
        holder.mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(onClickListener!=null){
                    onClickListener.onClick(bean);
                }
            }
        });
    }

    public void setOnItemZClickListener(ItemZClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    @Override
    public int getItemCount() {
        return 9;
    }


    public class ImgViewHolder extends RecyclerView.ViewHolder {
        public ImageView mImageView;
        public ImgViewHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.itemz_img);
        }
    }
}
