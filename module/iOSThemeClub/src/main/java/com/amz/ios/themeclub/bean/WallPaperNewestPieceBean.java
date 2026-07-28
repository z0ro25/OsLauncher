package com.amz.ios.themeclub.bean;

import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.amz.ios.ioslite.common.ad.IOSNAdResponse;

import java.util.List;

/**
 * Created by ZhangMingZhe on 11/17/16.
 */

public class WallPaperNewestPieceBean implements MultiItemEntity {
    public static final int WALLPAPER_TYPE_Y = 2;
    public static final int WALLPAPER_TYPE_X = 1;
    public static final int WALLPAPER_TYPE_Q = 4;

    public static final int WALLPAPER_TYPE_AD = 3;
    private int itemType = 1;
    private List<WallPapersBean> mData;
    private String mSourceDescription;
    private String mSourceUrl;

    private IOSNAdResponse mAdBean;

    public IOSNAdResponse getAdBean() {
        return mAdBean;
    }

    public void setAdBean(IOSNAdResponse mAdBean) {
        this.mAdBean = mAdBean;
    }

    public List<WallPapersBean> getmData() {
        return mData;
    }
    public void setItemType(int itemType) {
        this.itemType = itemType;
    }

    public void setmData(List<WallPapersBean> mData) {
        this.mData = mData;
    }

    @Override
    public int getItemType() {
        return itemType;
    }


    public String getmSourceDescription() {
        return mSourceDescription;
    }

    public void setmSourceDescription(String mSourceDescription) {
        this.mSourceDescription = mSourceDescription;
    }

    public String getmSourceUrl() {
        return mSourceUrl;
    }

    public void setmSourceUrl(String mSourceUrl) {
        this.mSourceUrl = mSourceUrl;
    }

    @Override
    public String toString() {
        return "WallPaperNewestPieceBean{" +
                "itemType=" + itemType +
                ", mData=" + mData +
                '}';
    }
}
