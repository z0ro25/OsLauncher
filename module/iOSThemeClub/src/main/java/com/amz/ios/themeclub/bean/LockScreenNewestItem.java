package com.amz.ios.themeclub.bean;

import com.chad.library.adapter.base.entity.MultiItemEntity;

import java.io.Serializable;
import java.util.List;

/**
 * Created by server on 17-8-18.
 */
public class LockScreenNewestItem implements Serializable, MultiItemEntity {
    private List<LockScreenBean> mLockScreenBeans;

    @Override
    public int getItemType() {
        return LockScreenBean.TYPE_LOCK_SCREEN;
    }

    public List<LockScreenBean> getmLockScreenBeans() {
        return mLockScreenBeans;
    }

    public void setmLockScreenBeans(List<LockScreenBean> mLockScreenBeans) {
        this.mLockScreenBeans = mLockScreenBeans;
    }
}
