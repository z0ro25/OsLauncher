package com.amz.ios.themeclub.adapter;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import android.util.Log;
import android.view.ViewGroup;

import com.amz.ios.themeclub.bean.WallPapersBean;
import com.amz.ios.themeclub.ui.fragment.SelectionWallpaperDetailsFragment;

import java.util.ArrayList;

/**
 * Created by lideqian on 17-1-9.
 */

public class WallpaperDetailAdapter extends FragmentPagerAdapter {

    private ArrayList<WallPapersBean> mWallpaperBeans;

    public WallpaperDetailAdapter(FragmentManager fm , ArrayList<WallPapersBean> list) {
        super(fm);
        this.mWallpaperBeans = list;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
       //super.destroyItem(container, position, object);
    }

    @Override
    public Fragment getItem(int position) {
        Log.e("Page","position =  " + position);
        return SelectionWallpaperDetailsFragment.newInstance(mWallpaperBeans.get(position));
    }

    @Override
    public int getCount() {
        return mWallpaperBeans.size();
    }

    public void addData(ArrayList<WallPapersBean> list) {
        mWallpaperBeans.addAll(list);
    }
}
