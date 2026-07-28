package com.amz.ios.themeclub.adapter;

import android.content.Context;

import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.bean.LockScreenBean;
import com.amz.ios.themeclub.bean.LockScreenNewestItem;
import com.amz.ios.themeclub.view.LockScreenItemView;

import java.util.List;

/**
 * Created by ubuntu on 15/06/17.
 */

public class LockScreenNewestAdapter extends BaseMultiItemQuickAdapter<LockScreenNewestItem, BaseViewHolder> {
    private final String TAG = getClass().getSimpleName();

    public LockScreenNewestAdapter(Context context, List<LockScreenNewestItem> data) {
        super(data);
        mContext = context;
        addItemType(LockScreenBean.TYPE_LOCK_SCREEN, R.layout.themeclub_lock_screen_item);
    }

    @Override
    protected void convert(final BaseViewHolder baseViewHolder, LockScreenNewestItem lockScreenNewestItem) {
        final List<LockScreenBean> lockScreenBeans = lockScreenNewestItem.getmLockScreenBeans();
        DebugLog.w(TAG, "=============convert:" + lockScreenBeans.size() + "/" + lockScreenNewestItem.getItemType());
        switch (baseViewHolder.getItemViewType()) {
            case LockScreenBean.TYPE_LOCK_SCREEN:
                final LockScreenItemView convertView = (LockScreenItemView) baseViewHolder.getConvertView();
                convertView.setData(lockScreenBeans);
                break;
        }
    }
}
