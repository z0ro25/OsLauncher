package com.amz.ios.themeclub.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.viewpager.widget.ViewPager;
import android.view.KeyEvent;

import com.amz.ios.ioslite.common.CommonAppCompatActivity;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.adapter.WallpaperDetailAdapter;
import com.amz.ios.themeclub.bean.WallPapersBean;
import com.amz.ios.themeclub.util.AppUtils;
import com.amz.ios.themeclub.util.WallpaperUtil;
import com.amz.ios.themeclub.view.DepthPageTransformer;

import java.util.ArrayList;

public class OnlineWallpapersDetailActivity extends CommonAppCompatActivity {

    private String TAG = OnlineWallpapersDetailActivity.class.getSimpleName();
    private ViewPager mWallpapersViewPages;
    private WallpaperDetailAdapter mWallpaperDetailAdapter;
    private ArrayList<WallPapersBean> wallpaperList;
    private int position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //AppUtils.changeStatusBarStyle(this);
        setContentView(R.layout.activity_online_wallpapers_detail);
        AppUtils.immersive(this);
        mWallpapersViewPages = (ViewPager) findViewById(R.id.wallpapers_fragment);
        initConfig();
    }

    private void initConfig() {
        Intent intent = getIntent();
        wallpaperList = (ArrayList<WallPapersBean>)intent.getSerializableExtra(WallpaperUtil.ONLINEWALLPAPERLIST);
        mWallpaperDetailAdapter = new WallpaperDetailAdapter(getSupportFragmentManager(),wallpaperList);
        mWallpaperDetailAdapter.notifyDataSetChanged();
        position = intent.getIntExtra(WallpaperUtil.ONLINEWALLPAPER_POSITION,0);
        mWallpapersViewPages.setAdapter(mWallpaperDetailAdapter);
        mWallpaperDetailAdapter.notifyDataSetChanged();
        mWallpapersViewPages.setCurrentItem(position);
        mWallpapersViewPages.setPageTransformer(true, new DepthPageTransformer());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_MENU) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
