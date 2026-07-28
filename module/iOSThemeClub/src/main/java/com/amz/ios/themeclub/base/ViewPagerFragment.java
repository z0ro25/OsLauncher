package com.amz.ios.themeclub.base;

import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.adapter.TopTabPagerAdapter;
import com.amz.ios.themeclub.view.ChildViewPager;

import java.util.List;


public abstract class ViewPagerFragment extends Fragment {
    protected TabLayout mTabLayout;
    protected ChildViewPager mViewPager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.themeclub_fragment_viewpager, container, false);
        mTabLayout = (TabLayout) rootView.findViewById(R.id.tabLayout);
        mViewPager = (ChildViewPager) rootView.findViewById(R.id.viewpager);

        View tabLayoutContainer = rootView.findViewById(R.id.tablayout_container);
        tabLayoutContainer.setBackgroundColor(getTabLayoutBgColor());

        initViewPager();
        return rootView;
    }

    public void initViewPager() {
        final FragmentManager fm = getChildFragmentManager();
        final List<String> titles = getTitles();
        final List<Fragment> fragments = getFragments();
        final int count = fragments.size();
        TopTabPagerAdapter adapter = new TopTabPagerAdapter(getContext(), fm, titles, fragments);

        mViewPager.setAdapter(adapter);
        mViewPager.setOffscreenPageLimit(count);
        mTabLayout.setupWithViewPager(mViewPager);
    }

    public ViewPager getViewPager() {
        return mViewPager;
    }


    /**
     * 标题列表
     */
    protected abstract List<String> getTitles();


    /**
     * 页面列表
     */
    protected abstract List<Fragment> getFragments();

    /**
     * 标题栏背景颜色
     */
    protected int getTabLayoutBgColor() {
        return getContext().getResources().getColor(R.color.themeclub_accent_color);
    }

}
