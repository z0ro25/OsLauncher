package com.amz.ios.themeclub.ui.fragment;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.collection.SparseArrayCompat;
import androidx.core.view.ViewCompat;
import androidx.viewpager.widget.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.amz.ios.ioslite.common.Router;
import com.amz.ios.ioslite.common.config.ThemeConfig;
import com.amz.ios.themeclub.MainActivity;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.ThemeClubApplication;
import com.amz.ios.themeclub.adapter.TopTabPagerAdapter;
import com.amz.ios.themeclub.base.BaseFragment;
import com.amz.ios.themeclub.tools.ScrollableFragmentListener;
import com.amz.ios.themeclub.tools.ScrollableListener;
import com.amz.ios.themeclub.tools.ViewPagerHeaderHelper;
import com.amz.ios.themeclub.util.DensityUtils;
import com.amz.ios.themeclub.view.SlidingTabLayout;
import com.amz.ios.themeclub.view.TouchCallbackLayout;

import java.util.ArrayList;

/**
 * Created by server on 16-11-15.
 */

public class MineFragment extends BaseFragment
        implements TouchCallbackLayout.TouchEventListener,
        ViewPagerHeaderHelper.OnViewPagerTouchListener, ScrollableFragmentListener{

    private final String TAG = "MineFragment";
    private static final long DEFAULT_DURATION = 300L;
    private static final float DEFAULT_DAMPING = 1.5f;

    private TouchCallbackLayout mTouchCallbackLayout;
    private ViewPagerHeaderHelper mViewPagerHeaderHelper;
    private SparseArrayCompat<ScrollableListener> mScrollableListenerArrays =
            new SparseArrayCompat<>();

    private LinearLayout mHeaderLayoutView;
    private ViewPager mViewPager;
    private SlidingTabLayout tabs;
    RelativeLayout mProgress;

    private int mTouchSlop;
    private int mTabHeight;
    private int mHeaderHeight;
    private Interpolator mInterpolator = new DecelerateInterpolator();

    private TopTabPagerAdapter mTopPageAdapter;
    private ArrayList<String> mTitle;
    private ArrayList<Fragment> mFragments;

    @Override
    protected View createView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle saveInstaceState) {
        return inflater.inflate(R.layout.themeclub_fragment_mine, container, false);
    }

    @Override
    protected void init(View rootView) {
        mTouchCallbackLayout = (TouchCallbackLayout) rootView.findViewById(R.id.layout);
        mProgress = (RelativeLayout) rootView.findViewById(R.id.progress_set);
        mHeaderLayoutView = (LinearLayout) rootView.findViewById(R.id.heard);
        mViewPager = (ViewPager) rootView.findViewById(R.id.viewpager);
        tabs = (SlidingTabLayout) rootView.findViewById(R.id.tabs);
        tabs.setBackgroundColor(getResources().getColor(R.color.themeclub_accent_color));

        mTitle = new ArrayList<>();
        mTitle.add(getString(R.string.themeclub_tab_theme));
        mTitle.add(getString(R.string.themeclub_tab_wallpaper));
//        mTitle.add(getString(R.string.themeclub_tab_font));

        if(ThemeConfig.isLockScreenEnable()) {
            mTitle.add(getString(R.string.themeclub_lock_title));
        }
        tabs.setCustomTabView(R.layout.themeclub_mine_custom_tab, 0);
//        tabs.setSelectedIndicatorColors(getResources().getColor(R.color.themeclub_accent_color));
        tabs.setSelectedIndicatorColors(Color.WHITE);

        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        mTouchCallbackLayout.setTouchEventListener(this);
        mViewPagerHeaderHelper = new ViewPagerHeaderHelper(this.getContext(), this);
        mTabHeight = getResources().getDimensionPixelSize(R.dimen.tabs_height);
        mHeaderHeight = getResources().getDimensionPixelSize(R.dimen.viewpager_header_height);
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT){
            mHeaderLayoutView.setPadding(0,0,0,0);
        }else {
            mHeaderLayoutView.setPadding(0, DensityUtils.dip2px(getContext(),getResources().getDimension(R.dimen.themeclub_minefragment_padding)),0,0);
            mViewPager.setPadding(0,DensityUtils.dip2px(ThemeClubApplication.getContext(),getResources().getDimension(R.dimen.themeclub_minefragment_padding)),0,0);
        }
        ViewCompat.setTranslationY(mViewPager, mHeaderHeight);
    }

    @Override
    public void onResume() {
        super.onResume();
        tabs.updateTabFont();

        switch (MainActivity.PAGE_TYPE) {
            case 0:
                mViewPager.setCurrentItem(1);
                break;
            case 1:
                mViewPager.setCurrentItem(0);
                break;
            default:
                mViewPager.setCurrentItem(0);
                break;
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
    }

    @Override
    protected void fragmentLoadData() {
        mFragments = new ArrayList<>();
        mFragments.add(new MineThemeFragment());
        mFragments.add(new MineWallpaperFragment());
//        mFragments.add(new MineFontFragment());

        if(ThemeConfig.isLockScreenEnable()) {
            mFragments.add(MineLockScreenFragment.instantiate(getContext(), MineLockScreenFragment.class.getName()));
        }
        mTopPageAdapter = new TopTabPagerAdapter(getContext(), getChildFragmentManager(), mTitle, mFragments);
        mViewPager.setAdapter(mTopPageAdapter);
        mViewPager.setOffscreenPageLimit(mTopPageAdapter.getCount());
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        MainActivity.PAGE_TYPE = Router.EXTRA_VALUE_THEME;
                        break;
                    case 1:
                        MainActivity.PAGE_TYPE = Router.EXTRA_VALUE_WALLPAPER;

                        break;
                    default:
                        MainActivity.PAGE_TYPE = Router.EXTRA_VALUE_THEME;
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        tabs.setViewPager(mViewPager);
    }

    @Override
    public boolean onLayoutInterceptTouchEvent(MotionEvent ev) {
        return mViewPagerHeaderHelper.onLayoutInterceptTouchEvent(ev,
                mTabHeight + mHeaderHeight);
    }

    @Override
    public boolean onLayoutTouchEvent(MotionEvent ev) {
        return mViewPagerHeaderHelper.onLayoutTouchEvent(ev);
    }

    @Override
    public boolean isViewBeingDragged(MotionEvent event) {
        return mScrollableListenerArrays.valueAt(mViewPager.getCurrentItem()).isViewBeingDragged(event);
    }

    @Override
    public void onMoveStarted(float y) {

    }

    @Override
    public void onMove(float y, float yDx) {
        float headerTranslationY = ViewCompat.getTranslationY(mHeaderLayoutView) + yDx;
        if (headerTranslationY >= 0) { // pull end
            headerExpand(0L);
        } else if (headerTranslationY <= -mHeaderHeight) { // push end
            headerFold(0L);
        } else {
            ViewCompat.animate(mHeaderLayoutView)
                    .translationY(headerTranslationY)
                    .setDuration(0)
                    .start();
            ViewCompat.animate(mViewPager)
                    .translationY(headerTranslationY + mHeaderHeight)
                    .setDuration(0)
                    .start();
        }
    }

    @Override
    public void onMoveEnded(boolean isFling, float flingVelocityY) {

        float headerY = ViewCompat.getTranslationY(mHeaderLayoutView); // 0µ½¸ºÊý
        if (headerY == 0 || headerY == -mHeaderHeight) {
            return;
        }

        if (mViewPagerHeaderHelper.getInitialMotionY() - mViewPagerHeaderHelper.getLastMotionY()
                < -mTouchSlop) {  // pull > mTouchSlop = expand
            headerExpand(headerMoveDuration(true, headerY, isFling, flingVelocityY));
        } else if (mViewPagerHeaderHelper.getInitialMotionY()
                - mViewPagerHeaderHelper.getLastMotionY()
                > mTouchSlop) { // push > mTouchSlop = fold
            headerFold(headerMoveDuration(false, headerY, isFling, flingVelocityY));
        } else {
            if (headerY > -mHeaderHeight / 2f) {  // headerY > header/2 = expand
                headerExpand(headerMoveDuration(true, headerY, isFling, flingVelocityY));
            } else { // headerY < header/2= fold
                headerFold(headerMoveDuration(false, headerY, isFling, flingVelocityY));
            }
        }
    }

    private long headerMoveDuration(boolean isExpand, float currentHeaderY, boolean isFling,
                                    float velocityY) {

        long defaultDuration = DEFAULT_DURATION;

        if (isFling) {

            float distance = isExpand ? Math.abs(mHeaderHeight) - Math.abs(currentHeaderY)
                    : Math.abs(currentHeaderY);
            velocityY = Math.abs(velocityY) / 1000;

            defaultDuration = (long) (distance / velocityY * DEFAULT_DAMPING);

            defaultDuration =
                    defaultDuration > DEFAULT_DURATION ? DEFAULT_DURATION : defaultDuration;
        }

        return defaultDuration;
    }

    private void headerFold(long duration) {
        ViewCompat.animate(mHeaderLayoutView)
                .translationY(-mHeaderHeight)
                .setDuration(duration)
                .setInterpolator(mInterpolator)
                .start();

        ViewCompat.animate(mViewPager).translationY(0).
                setDuration(duration).setInterpolator(mInterpolator).start();

        mViewPagerHeaderHelper.setHeaderExpand(false);
    }

    private void headerExpand(long duration) {
        ViewCompat.animate(mHeaderLayoutView)
                .translationY(0)
                .setDuration(duration)
                .setInterpolator(mInterpolator)
                .start();

        ViewCompat.animate(mViewPager)
                .translationY(mHeaderHeight)
                .setDuration(duration)
                .setInterpolator(mInterpolator)
                .start();
        mViewPagerHeaderHelper.setHeaderExpand(true);
    }

    @Override
    public void onFragmentAttached(ScrollableListener listener, int position) {
        Log.d("gaol", "gaol  onFragmentAttached: listener="+listener + "position="+position);
        mScrollableListenerArrays.put(position, listener);
    }

    @Override
    public void onFragmentDetached(ScrollableListener listener, int position) {
        mScrollableListenerArrays.remove(position);
    }
}