package com.amz.ios.search.http;

import com.amz.ios.search.utils.MD5Utils;

/**
 * Created by liaozhongjun on 2017/2/8.
 */

public class HotwordRequestBean {
    private CommonBean common;
    private TagBean tag;

    public CommonBean getCommon() {
        return common;
    }

    public void setCommon(CommonBean common) {
        this.common = common;
    }

    public TagBean getTag() {
        return tag;
    }

    public void setTag(TagBean tag) {
        this.tag = tag;
    }

    public static final HotwordRequestBean newHotwordRequestBean(CommonBean common, TagBean tag) {
        HotwordRequestBean requestBean = new HotwordRequestBean();
        common.setSign(MD5Utils.string2MD5(tag.getChannel() + common.getRequestTime()));
        requestBean.setCommon(common);
        requestBean.setTag(tag);
        return requestBean;
    }
}
