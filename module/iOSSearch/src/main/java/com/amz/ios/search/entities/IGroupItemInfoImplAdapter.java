package com.amz.ios.search.entities;


import com.amz.ios.search.provider.AdapterItemPresenter;

import java.util.ArrayList;
import java.util.List;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-16 下午4:59
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public abstract class IGroupItemInfoImplAdapter<VH extends AdapterItemPresenter.BaseViewHolder> extends BaseCardItemInfo<VH> implements IGroupItemInfo {
    /**
     * Each view should has a viewType to register int recycleView;
     *
     * @param viewType
     */
    public IGroupItemInfoImplAdapter(int viewType, int layoutid) {
        super(viewType, layoutid);
    }

    protected List<BaseCardItemInfo> mGroup;

    @Override
    public void addToGroup(BaseCardItemInfo itemInfo) {
        checkGroup();
        mGroup.add(itemInfo);
    }

    @Override
    public void addAllToGroup(List<BaseCardItemInfo> itemInfos) {
        checkGroup();
        mGroup.addAll(itemInfos);
    }

    @Override
    public List<BaseCardItemInfo> getGroup() {
        return mGroup;
    }

    private void checkGroup() {
        if (mGroup == null) {
            mGroup = new ArrayList<>();
        }
    }

    public void clear() {
        if (mGroup != null) mGroup.clear();
    }

    public static boolean instanceOfGroupItem(List<BaseCardItemInfo> datas) {
        if (datas == null) return false;
        final int size = datas.size();
        BaseCardItemInfo itemInfo;

        for (int i = 0; i < size; i++) {
            itemInfo = datas.get(i);
            if (itemInfo instanceof IGroupItemInfo) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void realBindViewHolder(VH viewHolder) {
        if (mGroup == null) return;
    }
}
