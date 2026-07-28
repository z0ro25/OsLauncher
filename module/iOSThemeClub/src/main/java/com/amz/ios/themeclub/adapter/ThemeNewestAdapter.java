package com.amz.ios.themeclub.adapter;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.amz.ios.ioslite.common.ad.IOSNAdResponse;
import com.amz.ios.ioslite.common.ad.NativeAdCardView;
import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.bean.ThemesBean;

import java.util.List;


/**
 * Created by ZhangMingZhe on 11/16/16.
 */

public class ThemeNewestAdapter extends BaseMultiItemQuickAdapter<ThemesBean,BaseViewHolder>{
    private Context mContext;
    private final String TAG = ThemeNewestAdapter.class.getSimpleName();

    public ThemeNewestAdapter(Context mContext,List<ThemesBean> data) {
        super(data);
        this.mContext = mContext;
        addItemType(ThemesBean.TYPE_X,R.layout.theme_newest_item);
        addItemType(ThemesBean.TYPE_AD,R.layout.themeclub_ad_view);
    }

    @Override
    protected void convert(BaseViewHolder baseViewHolder, ThemesBean themesBean) {
        switch (themesBean.getItemType()){
            case ThemesBean.TYPE_X:
                 baseViewHolder.setText(R.id.theme_newest_name,themesBean.getName())
                .setText(R.id.newest_downlaod_number,themesBean.getDownloadNumber()+"")
                .addOnClickListener(R.id.theme_newest_item);
                Glide.with(mContext).load(themesBean.getPreview().getDownloadUrl()).placeholder(R.drawable.theme_horizental).fitCenter().into((ImageView) baseViewHolder.getView(R.id.theme_top_banner));
                Glide.with(mContext).load(themesBean.getIconUrl()).placeholder(R.drawable.place_holder_img).into((ImageView) baseViewHolder.getView(R.id.theme_icon));
                DebugLog.i(TAG, "===========TYPE_X:" + themesBean.getName());
                break;
            case ThemesBean.TYPE_AD:
                IOSNAdResponse ad = themesBean.getmAd();
                DebugLog.i(TAG, "===========TYPE_AD:" + ad);
                if(ad !=null) {
                    NativeAdCardView adCardView = baseViewHolder.getView(R.id.adview);
                    adCardView.fillIn(ad);
                }
                break;
        }
    }

}
