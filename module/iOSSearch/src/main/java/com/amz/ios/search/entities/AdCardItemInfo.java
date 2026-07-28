package com.amz.ios.search.entities;

import androidx.cardview.widget.CardView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.amz.ios.ioslite.common.util.DisplayUtil;
import com.amz.ios.search.config.MSCConfiguration;
import com.amz.ios.search.provider.AdapterItemPresenter;
import com.amz.ioslauncher.iossearch.R;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-22 下午5:00
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class AdCardItemInfo extends BaseCardItemInfo<AdCardItemInfo.AdViewHolder> {

    public static final boolean TEST = true;
    public static final int SHOW_LEVEL = 1;
    public View adView;

    /**
     * Each view should has a viewType to register int recycleView;
     */
    public AdCardItemInfo() {
        super(MSCConfiguration.MutiItemType.AD_TYPE, R.layout.fmsearch_layout_ad_text_item/*TEST ? R.layout
        .ad_view_layout : R.layout.fmsearch_layout_ad_text_item*/);
        setNeedWraper(!TEST);
        setShowLevel(SHOW_LEVEL);
    }

    public AdCardItemInfo(View adView) {
        this();
        this.adView = adView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        /*if(ad != null){
            ad.destory();
        }*/
    }

    @Override
    public void realBindViewHolder(AdViewHolder viewHolder) {
        //for temp
        if (tempBind(viewHolder)) return;

    }

    private boolean tempBind(AdViewHolder viewHolder) {
        if (!TEST) return false;
        AdViewHolderTmp viewHolderTmp = (AdViewHolderTmp) viewHolder;
        if (adView != null) {
            viewHolderTmp.mAdLayout.removeAllViews();
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(DisplayUtil.getScreenWidthInPx(adView.getContext()), ViewGroup.LayoutParams.WRAP_CONTENT);
            viewHolderTmp.mAdLayout.addView(adView, layoutParams);
        }
        return TEST;
    }


    @Override
    protected AdViewHolder generateViewHolderInternal(View itemView) {
        if (TEST && itemView instanceof CardView) {
            ((CardView) itemView).setCardElevation(itemView.getResources().getDimension(R.dimen
                    .fmsearch_dp_cardelevation));
        }
        return /*TEST ?*/ new AdViewHolderTmp(itemView) /*: new AdViewHolder(itemView)*/;
    }

    public class AdViewHolder extends AdapterItemPresenter.BaseViewHolder {


        public AdViewHolder(final View itemView) {
            super(itemView);
        }
    }


    public class AdViewHolderTmp extends AdViewHolder {
        private FrameLayout mAdLayout;


        public AdViewHolderTmp(final View itemView) {
            super(itemView);
            mAdLayout = (FrameLayout) itemView.findViewById(R.id.adview_layout);
        }
    }
}
