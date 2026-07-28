package com.amz.ios.search.provider;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;

import com.amz.ios.search.config.MSCConfiguration;
import com.amz.ios.search.entities.BaseCardItemInfo;
import com.amz.ios.search.view.recycleview.DefaultLayoutDecorationImpl;
import com.amz.ios.search.view.recycleview.LayoutDecoration;

import java.util.List;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-15 上午10:06
 */
public final class AdapterItemPresenter {

    private static final String TAG = AdapterItemPresenter.class.getSimpleName();

    private static final boolean DEBUG = MSCConfiguration.DEBUG;

    private List<BaseCardItemInfo> mDatas;
    /**
     * register pool for get view
     */
    private final static SparseArray<BaseCardItemInfo> VIEW_TYPE_POOL = new SparseArray<>(20);

    private LayoutDecoration mLayoutDecoration = null;

    public static void register(int viewType, BaseCardItemInfo itemInfo) {
        BaseCardItemInfo cardItemInfo = VIEW_TYPE_POOL.get(viewType);
        if (cardItemInfo != null) return;
        VIEW_TYPE_POOL.put(viewType, itemInfo);
    }

    private Context mContext;

    public AdapterItemPresenter(Context context, List<BaseCardItemInfo> datas) {
        mDatas = datas;
        mContext = context;
        mLayoutDecoration = new DefaultLayoutDecorationImpl(context);
    }

    //create view
    public BaseViewHolder getViewHolderByType(int viewType) {
        if (DEBUG)
            Log.d(TAG, ">>>>>>AdapterItemPresenter#getViewHolderByType : viewType =" + viewType);
        return VIEW_TYPE_POOL.get(viewType)
                .setWraperFactory(mLayoutDecoration)
                .generateViewHolder(mContext);
    }

    //bind view
    public void bindViewHolder(BaseViewHolder holder, BaseCardItemInfo itemInfo) {
        if (DEBUG)
            Log.e(TAG, ">>>>>>AdapterItemPresenter#bindViewHolder : " + holder.getClass().getSimpleName());
        itemInfo.realBindViewHolder(holder);
    }

    //getItemViewType
    public int getItemViewType(int position) {
        return mDatas.get(position).getViewType();
    }

    public AdapterItemPresenter setLayoutDecoration(LayoutDecoration layoutDecoration) {
        mLayoutDecoration = layoutDecoration;
        return this;
    }

    /**
     * base VH
     */
    public static class BaseViewHolder extends RecyclerView.ViewHolder {

        protected Context mContext;
//        protected DroiRequestQueue mQueue;

        public BaseViewHolder(View itemView) {
            super(itemView);
            mContext = itemView.getContext();
//            mQueue = DroiRequestQueue.getInstance(mContext);
        }

        public void setImage(final ImageView view, String url) {
//            if (view == null) return;
//            mQueue.get(url, new DroiRequestQueue.DroiImageListener() {
//
//                @Override
//                public void onPreNetResponce(DroiRequestQueue.DroiImageContainer response) {
//                    super.onPreNetResponce(response);
//                    if (response.getBitmap() != null) {
//                        view.setImageBitmap(response.getBitmap());
//                    } else {
//                        view.setImageResource(R.mipmap.ic_default_img);
//                    }
//                }
//
//                /**
//                 * remember if no data cached , data will CALL first {@link #onPreNetResponce(DroiRequestQueue.DroiImageContainer)}
//                 * @param response
//                 */
//                @Override
//                public void onResponse(DroiRequestQueue.DroiImageContainer response) {
//                    if (response.getBitmap() == null) return;
//                    view.setImageBitmap(response.getBitmap());
//                }
//            });
        }
    }
}
