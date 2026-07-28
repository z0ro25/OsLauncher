package com.amz.ios.search.widget;

import android.widget.TextView;

import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.search.http.HotwordResponseBean;

import java.util.List;

/**
 * Author       : yizhihao
 * Create time  : 2016-12-13 上午11:36
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public interface MerlinSwitchView {

    public static final int AUTO_FLASH_MODE = 0x1;

    public static final int CONTROL_FLASH_MODE = 0x2;

    MerlinSwitchView setMode(int mode);

    MerlinSwitchView start();

    MerlinSwitchView setTextView(CustomTextView view);

    MerlinSwitchView setHotword(List<HotwordResponseBean.DataBean.WordsBean> words);

    HotwordResponseBean.DataBean.WordsBean getCurrentHotword();
}
