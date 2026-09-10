package com.truongnt.ios.themeclub.ui.fragment;

import com.truongnt.ios.themeclub.bean.request.LockScreenNewestRequest;
import com.truongnt.ios.themeclub.presenter.LockScreenNewstPresenter;

/**
 * Created by ubuntu on 14/06/17.
 */

public class LockScreenSelectionFragment extends LockScreenBaseFragment {
    protected void loadData() {
        if (mPresenter == null) {
            mPresenter = new LockScreenNewstPresenter(this);
        }
        LockScreenNewestRequest request = new LockScreenNewestRequest(getContext(), 1, -1, mStartNum, mRequestNum);
        mPresenter.getDatas(request);
    }
}
