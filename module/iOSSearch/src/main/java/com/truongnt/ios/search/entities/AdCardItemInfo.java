package com.truongnt.ios.search.entities;

import android.app.Activity;
import androidx.cardview.widget.CardView;
import android.view.View;
import android.widget.FrameLayout;

import com.ezt.v2.ezt.admobdemo.ads.NativeAds;
import com.ezt.v2.ezt.admobdemo.ads.core.AdsSdk;
import com.truongnt.ios.ioslite.common.util.ActivityUtil;
import com.truongnt.ios.search.config.MSCConfiguration;
import com.truongnt.ios.search.provider.AdapterItemPresenter;
import com.truongnt.ios.search.R;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-22 下午5:00
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class AdCardItemInfo extends BaseCardItemInfo<AdCardItemInfo.AdViewHolder> {

    public static final boolean TEST = true;
    public static final int SHOW_LEVEL = 1;

    /** Ad unit ID cần hiển thị. Null = không có ad. */
    public String mAdUnitId;

    /** Hạn chờ SDK trả native; 15000ms là đúng default của {@code NativeAds.initNativeInline}. */
    private static final long NATIVE_LOAD_TIMEOUT_MS = 15000L;

    /**
     * Each view should has a viewType to register int recycleView;
     */
    public AdCardItemInfo() {
        super(MSCConfiguration.MutiItemType.AD_TYPE, R.layout.fmsearch_layout_ad_text_item/*TEST ? R.layout
        .ad_view_layout : R.layout.fmsearch_layout_ad_text_item*/);
        setNeedWraper(!TEST);
        setShowLevel(SHOW_LEVEL);
    }

    public AdCardItemInfo(String adUnitId) {
        this();
        this.mAdUnitId = adUnitId;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void realBindViewHolder(AdViewHolder viewHolder) {
        //for temp
        if (tempBind(viewHolder)) return;

    }

    private boolean tempBind(AdViewHolder viewHolder) {
        if (!TEST) return false;
        AdViewHolderTmp viewHolderTmp = (AdViewHolderTmp) viewHolder;
        // SDK tự đổ native vào container. CardView ở đây kế thừa FrameLayout nên dùng được.
        // Chỉ đổ khi container còn trống — RecyclerView tái dùng view nên bind lại sẽ
        // gọi tới đây lần nữa.
        // Đã mua bản không quảng cáo -> không đổ ad (card rỗng đã bị UiHandler bỏ từ trước vì
        // DataFlowProvider không trả về AdCardItemInfo nào).
        if (mAdUnitId != null
                && viewHolderTmp.mAdLayout.getChildCount() == 0
                && !AdsSdk.INSTANCE.isAdFree()) {
            Activity activity = ActivityUtil.findActivity(viewHolderTmp.mAdLayout.getContext());
            if (activity != null) {
                NativeAds.INSTANCE.initNativeInline(
                        activity,
                        viewHolderTmp.mAdLayout,
                        null,
                        mAdUnitId,
                        com.truongnt.ios.ioslite.common.R.layout.layout_native_inline,
                        NATIVE_LOAD_TIMEOUT_MS);
            }
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
