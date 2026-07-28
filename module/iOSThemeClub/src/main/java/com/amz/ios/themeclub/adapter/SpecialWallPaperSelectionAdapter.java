package com.amz.ios.themeclub.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.bean.WallPaperNewestPieceBean;
import com.amz.ios.themeclub.intertfaces.ItemZClickListener;

import java.util.List;

/**
 * Created by ZhangMingZhe on 11/23/16.
 */

public class SpecialWallPaperSelectionAdapter extends BaseMultiItemQuickAdapter<WallPaperNewestPieceBean,BaseViewHolder> {
    private Context mContext;
    private ItemZClickListener itemZClickListener;
    private final String TAG = SpecialWallPaperSelectionAdapter.class.getSimpleName();
    public SpecialWallPaperSelectionAdapter( Context mContext,List<WallPaperNewestPieceBean> data) {
        super(data);
        this.mContext = mContext;
        //复用部分布局
        //11.29 产品　提出修改此布局
//        addItemType(WallPaperNewestPieceBean.WALLPAPER_TYPE_X, R.layout.themeclub_item_x);
        addItemType(WallPaperNewestPieceBean.WALLPAPER_TYPE_Y,  R.layout.themeclub_newest_item_type_x);
        addItemType(WallPaperNewestPieceBean.WALLPAPER_TYPE_Q,  R.layout.themeclub_newest_item_type_y);
//        addItemType(WallPaperNewestPieceBean.WALLPAPER_TYPE_Z, R.layout.themeclub_item_z);
    }

    public void setItemZClickListener(ItemZClickListener itemZClickListener) {
        this.itemZClickListener = itemZClickListener;
    }

    @Override
    protected void convert(BaseViewHolder baseViewHolder, WallPaperNewestPieceBean wallPaperNewestPieceBean) {
        switch (baseViewHolder.getItemViewType()){
//            case WallPaperNewestPieceBean.WALLPAPER_TYPE_X:
//                 baseViewHolder
//                .addOnClickListener(R.id.img_one)
//                .addOnClickListener(R.id.img_two)
//                .addOnClickListener(R.id.img_three);
//                Glide.with(mContext).load(wallPaperNewestPieceBean.getmData().get(0).getSmallImage().getDownloadUrl()).crossFade().into((ImageView) baseViewHolder.getView(R.id.img_one));
//                Glide.with(mContext).load(wallPaperNewestPieceBean.getmData().get(1).getSmallImage().getDownloadUrl()).crossFade().into((ImageView) baseViewHolder.getView(R.id.img_two));
//                Glide.with(mContext).load(wallPaperNewestPieceBean.getmData().get(2).getSmallImage().getDownloadUrl()).crossFade().into((ImageView) baseViewHolder.getView(R.id.img_three));
//                break;
            case WallPaperNewestPieceBean.WALLPAPER_TYPE_Y:
                baseViewHolder
                .addOnClickListener(R.id.img_1)
                .addOnClickListener(R.id.img_2)
                .addOnClickListener(R.id.img_3)
                .addOnClickListener(R.id.img_4)
                .addOnClickListener(R.id.img_5)
                .addOnClickListener(R.id.img_6);
                String url1 = wallPaperNewestPieceBean.getmData().get(0).getSmallImage().getDownloadUrl();
                String url2 = wallPaperNewestPieceBean.getmData().get(1).getSmallImage().getDownloadUrl();
                String url3 = wallPaperNewestPieceBean.getmData().get(2).getSmallImage().getDownloadUrl();
                String url4 = wallPaperNewestPieceBean.getmData().get(3).getSmallImage().getDownloadUrl();
                String url5 = wallPaperNewestPieceBean.getmData().get(4).getSmallImage().getDownloadUrl();
                String url6 = wallPaperNewestPieceBean.getmData().get(5).getSmallImage().getDownloadUrl();
                showImge(url1, !TextUtils.isEmpty(url1),R.drawable.img_big,R.id.img_1,baseViewHolder);
                showImge(url2, !TextUtils.isEmpty(url2),R.drawable.img_small,R.id.img_2,baseViewHolder);
                showImge(url3, !TextUtils.isEmpty(url3),R.drawable.img_small,R.id.img_3,baseViewHolder);
                showImge(url4, !TextUtils.isEmpty(url4),R.drawable.img_small,R.id.img_4,baseViewHolder);
                showImge(url5, !TextUtils.isEmpty(url5),R.drawable.img_small,R.id.img_5,baseViewHolder);
                showImge(url6, !TextUtils.isEmpty(url6),R.drawable.img_small,R.id.img_6,baseViewHolder);
                break;
//            case WallPaperNewestPieceBean.WALLPAPER_TYPE_Z:
//                RecyclerView itemZ = baseViewHolder.getView(R.id.itemz_recyle);
//                Log.e(TAG,"size="+wallPaperNewestPieceBean.getmData().size());
//                ItemZAdapter itemZAdapter = new ItemZAdapter(mContext,wallPaperNewestPieceBean.getmData(),R.layout.themeclub_ryc_itemz);
//                itemZAdapter.setOnItemZClickListener(itemZClickListener);
//                itemZ.setLayoutManager(new GridLayoutManager(mContext,3));
//                itemZ.setAdapter(itemZAdapter);
//                if(itemZ.getTag(R.string.themeclub_app_name)==null){
//                    //防止重复设置ItemDecotation
//                    Log.e(TAG,"lineNumber=77,methodName=convert");
//                    itemZ.setTag(R.string.themeclub_app_name,baseViewHolder.getPosition());
//                    itemZ.addItemDecoration(new ItemDecoration(4,0,4,4, ItemDecoration.ITEMDECORATION_TYPE_Z));
//                }
//                break;
            case WallPaperNewestPieceBean.WALLPAPER_TYPE_Q:
                String murl1 = wallPaperNewestPieceBean.getmData().get(0).getSmallImage().getDownloadUrl();
                String murl2 = wallPaperNewestPieceBean.getmData().get(1).getSmallImage().getDownloadUrl();
                String murl3 = wallPaperNewestPieceBean.getmData().get(2).getSmallImage().getDownloadUrl();
                String murl4 = wallPaperNewestPieceBean.getmData().get(3).getSmallImage().getDownloadUrl();
                String murl5 = wallPaperNewestPieceBean.getmData().get(4).getSmallImage().getDownloadUrl();
                String murl6 = wallPaperNewestPieceBean.getmData().get(5).getSmallImage().getDownloadUrl();
                baseViewHolder.addOnClickListener(R.id.y_img_1)
                        .addOnClickListener(R.id.y_img_2)
                        .addOnClickListener(R.id.y_img_3)
                        .addOnClickListener(R.id.y_img_4)
                        .addOnClickListener(R.id.y_img_5)
                        .addOnClickListener(R.id.y_img_6);
                showImge(murl1, !TextUtils.isEmpty(murl1),R.drawable.img_small,R.id.y_img_1,baseViewHolder);
                showImge(murl2, !TextUtils.isEmpty(murl2),R.drawable.img_big,R.id.y_img_2,baseViewHolder);
                showImge(murl3, !TextUtils.isEmpty(murl3),R.drawable.img_small,R.id.y_img_3,baseViewHolder);
                showImge(murl4, !TextUtils.isEmpty(murl4),R.drawable.img_small,R.id.y_img_4,baseViewHolder);
                showImge(murl5, !TextUtils.isEmpty(murl5),R.drawable.img_small,R.id.y_img_5,baseViewHolder);
                showImge(murl6, !TextUtils.isEmpty(murl6),R.drawable.img_small,R.id.y_img_6,baseViewHolder);
                break;
        }
    }

    private void showImge(String url, boolean isShowPlaceHolder, int drawableResID,int viewId,BaseViewHolder baseViewHolder){
        if(isShowPlaceHolder){
            Glide.with(mContext).load(url).placeholder(drawableResID).into((ImageView) baseViewHolder.getView(viewId));
        }else{
            Glide.with(mContext).load(url).into((ImageView) baseViewHolder.getView(viewId));
        }
    }
}
