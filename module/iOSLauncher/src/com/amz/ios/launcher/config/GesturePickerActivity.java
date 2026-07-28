package com.amz.ios.launcher.config;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.widget.Toolbar;
import android.view.View;

import com.amz.ios.ioslite.common.preference.SettingBaseActivity;
import com.amz.ios.ioslite.common.util.BuildUtil;
import com.amz.ios.launcher.R;

import java.util.ArrayList;
import java.util.List;


public class GesturePickerActivity extends SettingBaseActivity {
    private static final int INDEX_IOS_ACTIONS = 0;
    private static final int INDEX_APPLICATIONS = 1;
    private static final int INDEX_SHORTCUTS = 2;

    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private List<Fragment> mFragmentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gesture_picker_activity);
        setupViews();
    }

    private void setupViews() {
        final Resources res = getResources();
        Intent intent = getIntent();
        String gestureDesc = intent.getStringExtra(GestureEventModel.REQUEST_GESTURE_DES);
        String actionDesc = intent.getStringExtra(GestureEventModel.GESTURE_ACTION_DES);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(gestureDesc);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        mFragmentList = new ArrayList<>();
        mFragmentList.add(ActionPickerFragment.newInstance(ActionPickerFragment.TYPE_IOS_SHORTCUT, actionDesc));
        mFragmentList.add(ActionPickerFragment.newInstance(ActionPickerFragment.TYPE_APPLICATION, actionDesc));

        FragmentPagerAdapter adapter = new ActionFragmentAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(adapter);
        mTabLayout.setupWithViewPager(mViewPager);
    }

    class ActionFragmentAdapter extends FragmentPagerAdapter {

        public ActionFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getTitle(position);
        }
    }

    private String getTitle(int position) {
        switch (position) {
            case INDEX_IOS_ACTIONS:
                return getString(R.string.gesture_tab_ios, BuildUtil.getIOSProductName(this));
            case INDEX_APPLICATIONS:
                return getString(R.string.gesture_tab_apps);
            default:
                return "";
        }
    }

}
