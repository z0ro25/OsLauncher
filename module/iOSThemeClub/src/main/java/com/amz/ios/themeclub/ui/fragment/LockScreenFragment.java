package com.amz.ios.themeclub.ui.fragment;

import androidx.fragment.app.Fragment;

import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.base.ViewPagerFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ubuntu on 14/06/17.
 */

public class LockScreenFragment extends ViewPagerFragment {

    @Override
    protected List<String> getTitles() {
        ArrayList<String> titleList = new ArrayList();
        titleList.add(getString(R.string.themeclub_theNewest));
        titleList.add(getString(R.string.themeclub_selection));
        return titleList;
    }

    @Override
    protected List<Fragment> getFragments() {
        ArrayList<Fragment> mFragments = new ArrayList<>();
        mFragments.add(LockScreenNewestFragment.instantiate(getContext(),LockScreenNewestFragment.class.getName()));
        mFragments.add(LockScreenSelectionFragment.instantiate(getContext(),LockScreenSelectionFragment.class.getName()));
        return mFragments;
    }
}
