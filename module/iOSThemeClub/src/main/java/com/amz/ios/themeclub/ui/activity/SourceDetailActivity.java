package com.amz.ios.themeclub.ui.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.amz.ios.ioslite.common.CommonAppCompatActivity;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.adapter.TopTabPagerAdapter;
import com.amz.ios.themeclub.ui.fragment.ThemeNewestFragment;
import com.amz.ios.themeclub.ui.fragment.WallPaperNewestFragment;

import java.util.ArrayList;

/**
 * Created by ZhangMingZhe on 11/23/16.
 */

public class SourceDetailActivity extends CommonAppCompatActivity {
    private final String TAG = SourceDetailActivity.class.getSimpleName();
    Toolbar mToolBar;
    CustomTextView mTopName;
    CustomTextView mTopDescription;
    TabLayout mSourceTab;
    ViewPager mSourceViewpage;
    AppBarLayout mBarLayout;
    private TopTabPagerAdapter mAdapter;
    private int mId = -1;
    private String mSource = "";
    private boolean isTheme;
    private final int THEME = 1;
    private final int WALLPAPER = 0;
    private int mCurrentPosition;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.e(TAG,"lineNumber=50,methodName=onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.themeclub_source_detail_activity);
        findViewbyId();
        setSystemColor();
        setData();
        initViewConfig();
    }

    private void findViewbyId() {
        mToolBar = (Toolbar) findViewById(R.id.source_toor_bar_height);
        mTopName = (CustomTextView) findViewById(R.id.detail_name);
        mTopDescription = (CustomTextView) findViewById(R.id.detail_des);
        mSourceTab = (TabLayout) findViewById(R.id.my_source_tab);
        mSourceViewpage = (ViewPager) findViewById(R.id.my_source_viewpage);
        mBarLayout = (AppBarLayout) findViewById(R.id.bar_layout);

        mToolBar.setNavigationIcon(R.drawable.source_detail_back);
        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setData() {
        Log.e(TAG,"lineNumber=75,methodName=setData");
        Intent intent = getIntent();
        if (intent != null) {
            isTheme = intent.getBooleanExtra("isTheme", false);
            mId = intent.getIntExtra("id", -1);
            mSource = intent.getStringExtra("source");
            mTopName.setText(mSource);
            mCurrentPosition = isTheme?THEME:WALLPAPER;
        }
    }

    private void setSystemColor() {
        //设置系统状态栏
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        int color;
        if(isTheme){
            color =  getResources().getColor(R.color.themeclub_theme_color );
        }else{
//            color = getResources().getColor(R.color.source_color_wallpaper);
            color =  getResources().getColor(R.color.themeclub_theme_blue_color);
        }
        //此处应该有适配, 6.0以下
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(color);
        }
        mSourceTab.setTabTextColors(getResources().getColor(R.color.source_text),color);
        mBarLayout.setBackgroundColor(color);
        mSourceTab.setSelectedTabIndicatorColor(color);
    }

    private void initViewConfig() {

        ArrayList<String> mTitles = new ArrayList<>();
        mTitles.add(getString(R.string.themeclub_wallpaper));
        mTitles.add(getString(R.string.themeclub_theme));
        ArrayList<Fragment> mFragments = new ArrayList<>();
        WallPaperNewestFragment wallPaperNewestFragment = (WallPaperNewestFragment) WallPaperNewestFragment.instantiate(this, WallPaperNewestFragment.class.getName());
        ThemeNewestFragment themeNewestFragment = (ThemeNewestFragment) ThemeNewestFragment.instantiate(this, ThemeNewestFragment.class.getName());
        Bundle b = new Bundle();
        b.putInt("id", mId);
        b.putString("source", mSource);
        if (isTheme) {
            themeNewestFragment.setArguments(b);
        } else {
            wallPaperNewestFragment.setArguments(b);
        }
        mFragments.add(wallPaperNewestFragment);
        mFragments.add(themeNewestFragment);
        mAdapter = new TopTabPagerAdapter(this, getSupportFragmentManager(), mTitles, mFragments);
        mSourceViewpage.setAdapter(mAdapter);
        mSourceTab.setupWithViewPager(mSourceViewpage);
        mSourceViewpage.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                isTheme= position != 0;
                setSystemColor();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mSourceViewpage.setCurrentItem(mCurrentPosition);
    }

    public void setTopViews(String mDescriptions) {
        mTopDescription.setText(mDescriptions);
    }

    public boolean isDescriptionNull() {
        return TextUtils.isEmpty(mTopDescription.getText().toString());
    }
}
