package com.amz.ios.themeclub.ui.fragment;

import com.amz.ios.themeclub.bean.request.LockScreenNewestRequest;
import com.amz.ios.themeclub.presenter.LockScreenNewstPresenter;

/**
 * Created by ubuntu on 14/06/17.
 */

public class LockScreenNewestFragment extends LockScreenBaseFragment{
    protected void loadData() {
        if(mPresenter==null){
            mPresenter = new LockScreenNewstPresenter(this);
        }
        LockScreenNewestRequest request = new LockScreenNewestRequest(getContext(), 4, -1, mStartNum, mRequestNum);
        mPresenter.getDatas(request);
    }
}
