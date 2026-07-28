package com.amz.ios.themeclub.adapter;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.bean.ThemeSelectionPieceBean;

import java.util.List;

/**
 * Created by ZhangMingZhe on 11/16/16.
 */

public class ThemeSelectionAdapter extends BaseMultiItemQuickAdapter<ThemeSelectionPieceBean.IssuesBean,BaseViewHolder> {
    private Context mContext;
    public ThemeSelectionAdapter(Context mContext,List<ThemeSelectionPieceBean.IssuesBean> data) {
        super(data);
        this.mContext = mContext;
        addItemType(ThemeSelectionPieceBean.THEME_SELECTION_ITEM_X, R.layout.theme_selection_item_x);
        addItemType(ThemeSelectionPieceBean.THEME_SELECTION_ITEM_Y, R.layout.theme_selection_item_y);
    }

    @Override
    protected void convert(BaseViewHolder baseViewHolder, ThemeSelectionPieceBean.IssuesBean issuesBean) {

        switch (baseViewHolder.getItemViewType()){
            case ThemeSelectionPieceBean.THEME_SELECTION_ITEM_X:
                    baseViewHolder.setText(R.id.special_name,issuesBean.getTitle())
                            .setText(R.id.special_des,issuesBean.getDescription())
                            .addOnClickListener(R.id.theme_selection_item_x);
                Glide.with(mContext).load(issuesBean.getThemes().get(0).getPreview().getDownloadUrl()).placeholder(R.drawable.theme_horizental).into((ImageView) baseViewHolder.getView(R.id.theme_bottom_banner));
                break;
            case ThemeSelectionPieceBean.THEME_SELECTION_ITEM_Y:
                baseViewHolder.setText(R.id.special_name,issuesBean.getTitle())
                        .setText(R.id.special_des,issuesBean.getDescription())
                        .addOnClickListener(R.id.top_area)
                        .addOnClickListener(R.id.theme_selection_y_1)
                        .addOnClickListener(R.id.theme_selection_y_2)
                        .addOnClickListener(R.id.theme_selection_y_3);
                //此处修改，preview中的图片是横幅，这里要显示竖幅，所以换成读取scrennlist的第一张图片．
//                Log.e("vertical_zmz","url="+issuesBean.getThemes().get(0).getScreenshotList().get(0));
                Log.e("zmz_123","lineNumber=48,methodName=convert");

                Glide.with(mContext).load(issuesBean.getThemes().get(0).getScreenshotList().get(0).getDownloadUrl()).placeholder(R.drawable.theme_vertical).into((ImageView) baseViewHolder.getView(R.id.theme_selection_y_1));
                Glide.with(mContext).load(issuesBean.getThemes().get(1).getScreenshotList().get(0).getDownloadUrl()).placeholder(R.drawable.theme_vertical).into((ImageView) baseViewHolder.getView(R.id.theme_selection_y_2));
                Glide.with(mContext).load(issuesBean.getThemes().get(2).getScreenshotList().get(0).getDownloadUrl()).placeholder(R.drawable.theme_vertical).into((ImageView) baseViewHolder.getView(R.id.theme_selection_y_3));
                break;
        }
    }
}
