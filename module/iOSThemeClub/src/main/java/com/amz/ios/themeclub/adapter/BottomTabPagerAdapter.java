package com.amz.ios.themeclub.adapter;

import android.content.Context;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.amz.ios.ioslite.common.config.ThemeConfig;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.app.AppConfig;
import com.amz.ios.themeclub.ui.fragment.LockScreenFragment;
import com.amz.ios.themeclub.ui.fragment.MineFragment;
import com.amz.ios.themeclub.ui.fragment.ThemeFragment;
import com.amz.ios.themeclub.ui.fragment.WallPaperFragment;

/**
 * Created by server on 16-11-14.
 */

public class BottomTabPagerAdapter extends FragmentPagerAdapter {
    private FragmentManager mFragmentManager;
    private Context mContext;
    private TabLayout tabs;
    private int mCount;
    private String TAG=BottomTabPagerAdapter.class.getName();

    public BottomTabPagerAdapter(FragmentManager fm,Context mContext){
        this(fm, mContext, null);
    }

    public BottomTabPagerAdapter(FragmentManager fm, Context context, TabLayout tabLayout){
        super(fm);
        this.tabs = tabLayout;
        this.mContext = context;
        Log.i(TAG,"ThemeConfig.isLockScreenEnable() = "+ThemeConfig.isLockScreenEnable());
        if(ThemeConfig.isLockScreenEnable()){
            mCount = 2;
        }else{
            mCount = 1;
        }
    }

    @Override
    public Fragment getItem(int position) {

        switch (position){
            case AppConfig.TAB_WALLPAPER:
                return Fragment.instantiate(mContext,WallPaperFragment.class.getName());
            case AppConfig.TAB_THEME:
                return Fragment.instantiate(mContext,ThemeFragment.class.getName());
            case AppConfig.TAB_LOCK:
                if(ThemeConfig.isLockScreenEnable()) {
                    return Fragment.instantiate(mContext, LockScreenFragment.class.getName());
                }else{
                    return Fragment.instantiate(mContext, MineFragment.class.getName());
                }
            case AppConfig.TAB_MINE:
                return Fragment.instantiate(mContext,MineFragment.class.getName());
        }
        return null;
    }

    @Override
    public int getCount() {
        return mCount;
    }

    public View getView(Context context, int position){
        ImageView image;
        CustomTextView text;
        View view = LayoutInflater.from(context).inflate(R.layout.themeclub_bottom_tab_item, null);
        image = (ImageView) view.findViewById(R.id.icon);
        text = (CustomTextView) view.findViewById(R.id.text);
        switch (position){
            case AppConfig.TAB_WALLPAPER:
                image.setImageResource(R.drawable.themeclub_tab_wallpapaer_icon);
                text.setText(R.string.themeclub_tab_wallpaper);
                break;
            case AppConfig.TAB_THEME:
                image.setImageResource(R.drawable.themeclub_tab_theme_icon);
                text.setText(R.string.themeclub_tab_theme);
                break;
            case AppConfig.TAB_LOCK:
                if(ThemeConfig.isLockScreenEnable()){
                    image.setImageResource(R.drawable.themeclub_tab_lock_screen_icon);
                    text.setText(R.string.themeclub_tab_lock);
                }else {
                    image.setImageResource(R.drawable.themeclub_tab_mine_icon);
                    text.setText(R.string.themeclub_tab_mine);
                }
                break;
            case AppConfig.TAB_MINE:
                image.setImageResource(R.drawable.themeclub_tab_mine_icon);
                text.setText(R.string.themeclub_tab_mine);
                break;
        }
        return view;
    }
}
