package com.amz.ios.themeclub.adapter;

import android.content.Context;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.List;


public class TopTabPagerAdapter extends FragmentPagerAdapter {
    private Context mContext;
    private List<String> titles;
    private List<Fragment> fragments;

    public TopTabPagerAdapter(Context mContext, FragmentManager fm, List<String> titles, List<Fragment> fragments) {
        super(fm);
        this.mContext = mContext;
        this.titles = titles;
        this.fragments = fragments;
    }


    @Override
    public Fragment getItem(int position) {
        return fragments.get(position);
    }

    @Override
    public int getCount() {
        return fragments.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return titles.get(position);
    }
}
