package com.amz.ios.search.entities;

import java.util.List;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-16 下午4:54
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public interface IGroupItemInfo {
    // for declear
    void addToGroup(BaseCardItemInfo itemInfo);

    void addAllToGroup(List<BaseCardItemInfo> itemInfos);

    List<BaseCardItemInfo> getGroup();

    void clear();
}
