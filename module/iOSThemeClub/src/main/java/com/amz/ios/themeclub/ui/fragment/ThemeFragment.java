package com.amz.ios.themeclub.ui.fragment;

import androidx.fragment.app.Fragment;

import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.base.ViewPagerFragment;

import java.util.ArrayList;
import java.util.List;

public class ThemeFragment extends ViewPagerFragment {
    protected List<String> getTitles() {
        ArrayList<String> titles = new ArrayList<>();
        titles.add(getString(R.string.themeclub_theNewest));
        titles.add(getString(R.string.themeclub_Minimalist));
        titles.add(getString(R.string.themeclub_selection));
        return titles;
    }

    protected List<Fragment> getFragments() {
        ArrayList<Fragment> fragments = new ArrayList<>();
        fragments.add(new ThemeNewestFragment());
        fragments.add(new ThemeMinimalistFragment());
        fragments.add(new ThemeSelectionFragment());
        return fragments;
    }


    protected int getTabLayoutBgColor() {
        return getContext().getResources().getColor(R.color.themeclub_accent_color);
    }
}