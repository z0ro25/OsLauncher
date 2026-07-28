package com.amz.ios.themeclub.adapter;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.amz.ios.ioslite.common.ad.IOSNAdResponse;
import com.amz.ios.ioslite.common.ad.NativeAdCardView;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.bean.WallPaperNewestPieceBean;

import java.util.List;



/**
 * Created by ZhangMingZhe on 11/15/16.
 */

public class WallPaperNewestAdapter extends BaseMultiItemQuickAdapter<WallPaperNewestPieceBean,BaseViewHolder> {
    private final String TAG = WallPaperNewestAdapter.class.getSimpleName();
    private Context mContext;
    public WallPaperNewestAdapter(Context mContext,List<WallPaperNewestPieceBean> data) {
        super(data);
        this.mContext = mContext;
        addItemType(WallPaperNewestPieceBean.WALLPAPER_TYPE_X, R.layout.themeclub_newest_item_type_x);
        addItemType(WallPaperNewestPieceBean.WALLPAPER_TYPE_Y, R.layout.themeclub_newest_item_type_y);
        addItemType(WallPaperNewestPieceBean.WALLPAPER_TYPE_AD, R.layout.themeclub_ad_view);

    }

    @Override
    protected void convert(BaseViewHolder baseViewHolder, WallPaperNewestPieceBean wallPaperPieceBean) {
        WallPaperNewestPieceBean bean = getData().get(baseViewHolder.getAdapterPosition());
        switch (baseViewHolder.getItemViewType()){
             case WallPaperNewestPieceBean.WALLPAPER_TYPE_X:
                    //绑定数据
                 baseViewHolder.addOnClickListener(R.id.img_1)
                               .addOnClickListener(R.id.img_2)
                               .addOnClickListener(R.id.img_2)
                               .addOnClickListener(R.id.img_3)
                               .addOnClickListener(R.id.img_4)
                               .addOnClickListener(R.id.img_5)
                               .addOnClickListener(R.id.img_6);
                 Glide.with(mContext).load(bean.getmData().get(0).getSmallImage().getDownloadUrl()).placeholder(R.drawable.img_big).centerCrop().into((ImageView) baseViewHolder.getView(R.id.img_1));
                 Glide.with(mContext).load(bean.getmData().get(1).getSmallImage().getDownloadUrl()).placeholder(R.drawable.img_small).centerCrop().into((ImageView) baseViewHolder.getView(R.id.img_2));
                 Glide.with(mContext).load(bean.getmData().get(2).getSmallImage().getDownloadUrl()).placeholder(R.drawable.img_small).centerCrop().into((ImageView) baseViewHolder.getView(R.id.img_3));
                 Glide.with(mContext).load(bean.getmData().get(3).getSmallImage().getDownloadUrl()).placeholder(R.drawable.img_small).centerCrop().into((ImageView) baseViewHolder.getView(R.id.img_4));
                 Glide.with(mContext).load(bean.getmData().get(4).getSmallImage().getDownloadUrl()).placeholder(R.drawable.img_small).centerCrop().into((ImageView) baseViewHolder.getView(R.id.img_5));
                 Glide.with(mContext).load(bean.getmData().get(5).getSmallImage().getDownloadUrl()).placeholder(R.drawable.img_small).centerCrop().into((ImageView) baseViewHolder.getView(R.id.img_6));
                 break;
             case WallPaperNewestPieceBean.WALLPAPER_TYPE_Y:
                    //绑定数据
                 baseViewHolder.addOnClickListener(R.id.y_img_1)
                         .addOnClickListener(R.id.y_img_2)
                         .addOnClickListener(R.id.y_img_3)
                         .addOnClickListener(R.id.y_img_4)
                         .addOnClickListener(R.id.y_img_5)
                         .addOnClickListener(R.id.y_img_6);
                 Glide.with(mContext).load(bean.getmData().get(0).getSmallImage().getDownloadUrl()).placeholder(R.drawable.img_small).centerCrop().into((ImageView) baseViewHolder.getView(R.id.y_img_1));
                 Glide.with(mContext).load(bean.getmData().get(1).getSmallImage().getDownloadUrl()).placeholder(R.drawable.img_big).centerCrop().into((ImageView) baseViewHolder.getView(R.id.y_img_2));
                 Glide.with(mContext).load(bean.getmData().get(2).getSmallImage().getDownloadUrl()).placeholder(R.drawable.img_small).centerCrop().into((ImageView) baseViewHolder.getView(R.id.y_img_3));
                 Glide.with(mContext).load(bean.getmData().get(3).getSmallImage().getDownloadUrl()).placeholder(R.drawable.img_small).centerCrop().into((ImageView) baseViewHolder.getView(R.id.y_img_4));
                 Glide.with(mContext).load(bean.getmData().get(4).getSmallImage().getDownloadUrl()).placeholder(R.drawable.img_small).centerCrop().into((ImageView) baseViewHolder.getView(R.id.y_img_5));
                 Glide.with(mContext).load(bean.getmData().get(5).getSmallImage().getDownloadUrl()).placeholder(R.drawable.img_small).centerCrop().into((ImageView) baseViewHolder.getView(R.id.y_img_6));
                 break;
            case WallPaperNewestPieceBean.WALLPAPER_TYPE_AD:
                IOSNAdResponse ad = bean.getAdBean();
                if(ad !=null) {
                    NativeAdCardView adCardView = baseViewHolder.getView(R.id.adview);
                    adCardView.fillIn(ad);
                }
                break;

         }
    }

}
