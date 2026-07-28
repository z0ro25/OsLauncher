package com.amz.ios.themeclub.adapter;

import android.content.Context;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.bean.WallPaperSelectionPieceBean;
import com.amz.ios.themeclub.intertfaces.ItemZClickListener;
import com.amz.ios.themeclub.view.ItemDecoration;

import java.util.List;

/**
 * Created by ZhangMingZhe on 11/16/16.
 */
//精选适配器
public class WallPaperSelectionAdapter extends BaseMultiItemQuickAdapter<WallPaperSelectionPieceBean.IssuesBean,BaseViewHolder>{
    private Context mContext;
    private ItemZClickListener onItemZClickListener;
    public WallPaperSelectionAdapter(Context mContext,List<WallPaperSelectionPieceBean.IssuesBean> data) {
        super(data);
        this.mContext = mContext;
        addItemType(WallPaperSelectionPieceBean.SELECTION_TYPE_X,R.layout.wallpaper_selection_item_x);
        addItemType(WallPaperSelectionPieceBean.SELECTION_TYPE_Y,R.layout.wallpaper_selection_item_y);
        addItemType(WallPaperSelectionPieceBean.SELECTION_TYPE_Z,R.layout.wallpaper_selection_item_z);
    }
    //必须调用
    public void setOnItemZClickListener(ItemZClickListener onItemZClickListener) {
        if(onItemZClickListener == null){
            throw new RuntimeException("ItemZClickListener can't be null");
        }
        this.onItemZClickListener = onItemZClickListener;
    }



    @Override
    protected void convert(BaseViewHolder baseViewHolder, WallPaperSelectionPieceBean.IssuesBean issuesBean) {
        switch (baseViewHolder.getItemViewType()){
            case WallPaperSelectionPieceBean.SELECTION_TYPE_X:
                if(issuesBean.getWallPapers().size()<3){
                    return;
                }
                baseViewHolder.setText(R.id.special_name,issuesBean.getTitle())
                        .setText(R.id.special_des,issuesBean.getDescription())
                        .addOnClickListener(R.id.img_one)
                        .addOnClickListener(R.id.img_two)
                        .addOnClickListener(R.id.img_three)
                        .addOnClickListener(R.id.top_area);
                Glide.with(mContext).load(issuesBean.getWallPapers().get(0).getSmallImage().getDownloadUrl()).placeholder(R.drawable.img_big).centerCrop().into((ImageView) baseViewHolder.getView(R.id.img_one));
                Glide.with(mContext).load(issuesBean.getWallPapers().get(1).getSmallImage().getDownloadUrl()).placeholder(R.drawable.img_small).centerCrop().into((ImageView) baseViewHolder.getView(R.id.img_two));
                Glide.with(mContext).load(issuesBean.getWallPapers().get(2).getSmallImage().getDownloadUrl()).placeholder(R.drawable.img_small).centerCrop().into((ImageView) baseViewHolder.getView(R.id.img_three));
                break;
            case WallPaperSelectionPieceBean.SELECTION_TYPE_Y:
                if(issuesBean.getWallPapers().size()<6){
                    return;
                }
                baseViewHolder.setText(R.id.special_name,issuesBean.getTitle())
                        .setText(R.id.special_des,issuesBean.getDescription())
                        .addOnClickListener(R.id.img_1)
                        .addOnClickListener(R.id.img_2)
                        .addOnClickListener(R.id.img_3)
                        .addOnClickListener(R.id.img_4)
                        .addOnClickListener(R.id.img_5)
                        .addOnClickListener(R.id.img_6)
                        .addOnClickListener(R.id.top_area);
                Glide.with(mContext).load(issuesBean.getWallPapers().get(0).getSmallImage().getDownloadUrl()).placeholder(R.drawable.img_big).centerCrop().into((ImageView) baseViewHolder.getView(R.id.img_1));
                Glide.with(mContext).load(issuesBean.getWallPapers().get(1).getSmallImage().getDownloadUrl()).placeholder(R.drawable.img_small).centerCrop().into((ImageView) baseViewHolder.getView(R.id.img_2));
                Glide.with(mContext).load(issuesBean.getWallPapers().get(2).getSmallImage().getDownloadUrl()).placeholder(R.drawable.img_small).centerCrop().into((ImageView) baseViewHolder.getView(R.id.img_3));
                Glide.with(mContext).load(issuesBean.getWallPapers().get(3).getSmallImage().getDownloadUrl()).placeholder(R.drawable.img_small).centerCrop().into((ImageView) baseViewHolder.getView(R.id.img_4));
                Glide.with(mContext).load(issuesBean.getWallPapers().get(4).getSmallImage().getDownloadUrl()).placeholder(R.drawable.img_small).centerCrop().into((ImageView) baseViewHolder.getView(R.id.img_5));
                Glide.with(mContext).load(issuesBean.getWallPapers().get(5).getSmallImage().getDownloadUrl()).placeholder(R.drawable.img_small).centerCrop().into((ImageView) baseViewHolder.getView(R.id.img_6));
                break;
            case WallPaperSelectionPieceBean.SELECTION_TYPE_Z:
                if(issuesBean.getWallPapers().size()<9){
                    return;
                }
                baseViewHolder.setText(R.id.special_name,issuesBean.getTitle())
                        .setText(R.id.special_des,issuesBean.getDescription())
                        .addOnClickListener(R.id.top_area);
                RecyclerView itemZ = baseViewHolder.getView(R.id.itemz_recyle);
                ItemZAdapter itemZAdapter = new ItemZAdapter(mContext,issuesBean.getWallPapers(),R.layout.themeclub_ryc_itemz);
                itemZAdapter.setOnItemZClickListener(onItemZClickListener);
                itemZ.setLayoutManager(new GridLayoutManager(mContext,3));
                itemZ.setAdapter(itemZAdapter);
                if(itemZ.getTag(R.string.themeclub_app_name)==null){
                    //防止重复设置ItemDecotation
                    itemZ.setTag(R.string.themeclub_app_name,baseViewHolder.getPosition());
                    itemZ.addItemDecoration(new ItemDecoration(4,0,4,4, ItemDecoration.ITEMDECORATION_TYPE_Z));
                }
                break;
        }
    }

}
