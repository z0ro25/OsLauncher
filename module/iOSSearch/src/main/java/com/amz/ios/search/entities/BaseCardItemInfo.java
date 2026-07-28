package com.amz.ios.search.entities;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.amz.ios.search.provider.AdapterItemPresenter;
import com.amz.ios.search.view.recycleview.LayoutDecoration;

/**
 * define a new BaseCardItemInfo should register a viewType fisrt.
 * Author       : yizhihao
 * Create time  : 2016-11-15 上午10:42
 */
public abstract class BaseCardItemInfo<VH extends AdapterItemPresenter.BaseViewHolder> {

    private static final String TAG = BaseCardItemInfo.class.getSimpleName();
    public static final int NORMAL_SHOW_LEVEL = 1000;

    public static final int DEFAULT_VIEW_TYPE = 0;

    public static final int TYPE_LAYOUT_ID = 0x1;

    public static final int TYPE_VIEW = 0x2;

    public int mInitTYpe = TYPE_LAYOUT_ID;

    public int mLayoutId = 0;

    private boolean enableLayoutWraper = false;
    private boolean needWraper = true;

    private LayoutDecoration mWraperFactory;

    private int mViewType = DEFAULT_VIEW_TYPE;

    /**
     * control show position by this options
     */
    private int mShowLevel = NORMAL_SHOW_LEVEL;

    /**
     * Each view should has a viewType to register int recycleView;
     *
     * @param viewType
     */
    public BaseCardItemInfo(int viewType, int layoutId) {
        mLayoutId = layoutId;
        mViewType = viewType;
        needWraper = true;
        AdapterItemPresenter.register(viewType, this);
    }

    public int getShowLevel() {
        return mShowLevel;
    }

    public BaseCardItemInfo setShowLevel(int showLevel) {
        this.mShowLevel = showLevel;
        return this;
    }

    public boolean isViewEmpty() {
        return true;
    }

    public int getViewType() {
        return mViewType;
    }

    public BaseCardItemInfo setEnableLayoutWraper(boolean enableLayoutWraper) {
        this.enableLayoutWraper = enableLayoutWraper;
        return this;
    }

    public BaseCardItemInfo setWraperFactory(LayoutDecoration wraperFactory) {
        mWraperFactory = wraperFactory;
        enableLayoutWraper = true;
        return this;
    }

    public BaseCardItemInfo setInitTYpe(int initTYpe) {
        mInitTYpe = initTYpe;
        return this;
    }

    public BaseCardItemInfo setLayoutId(int layoutId) {
        mLayoutId = layoutId;
        mInitTYpe = TYPE_LAYOUT_ID;
        return this;
    }

    public BaseCardItemInfo setNeedWraper(boolean needWraper) {
        this.needWraper = needWraper;
        return this;
    }

    /**
     * remember to release some object assign with ui context
     * for detroy and release mem
     */
    public void onDestroy() {

    }

    //bind view
    public abstract void realBindViewHolder(VH viewHolder);

    //create view
    public VH generateViewHolder(Context context) {
        View itemView = null;
        switch (mInitTYpe) {
            case TYPE_LAYOUT_ID:
                LayoutInflater layoutInflater = LayoutInflater.from(context);
                if (mWraperFactory != null && enableLayoutWraper && needWraper) {
                    itemView = layoutInflater.inflate(mLayoutId, mWraperFactory.newWrapView());
                } else {
                    Log.d(TAG, ">>>>>>BaseCardItemInfo#generateViewHolder : no need wraper!");
                    itemView = layoutInflater.inflate(mLayoutId, null);
                }
                break;
            case TYPE_VIEW:
                itemView = mWraperFactory.newWrapView();
            default:
                // do nothing
                break;
        }
        return generateViewHolderInternal(itemView);
    }

    protected abstract VH generateViewHolderInternal(View itemView);

}
