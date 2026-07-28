package com.amz.ios.themeclub.view;

import android.content.Context;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.amz.ios.themeclub.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lideqian on 16-11-16.
 */
public class PreviewGallery extends LinearLayout {

    private ViewPager mViewPager;

    private PagerAdapter mAdapter;

    public PreviewGallery(Context context) {
        super(context);
    }

    public PreviewGallery(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mViewPager = (ViewPager) findViewById(R.id.preview_view_pager);
        mViewPager.setOffscreenPageLimit(5);
        mViewPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.themeclub_viewpager_margin));

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                Log.e("ldq00001", position + "---" + positionOffset + "---" + positionOffsetPixels);
                if (positionOffset > 0.2826 && position == mAdapter.getCount() - 1) {
                   mViewPager.setCurrentItem(0);
                }
                if (positionOffset > 0.00129) {
                    postInvalidate();
                }
            }

        });

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mViewPager.onTouchEvent(event);
    }

    public void setAdapter(PagerAdapter adapter) {
        mViewPager.setAdapter(adapter);
        mAdapter = adapter;
        mViewPager.getAdapter().notifyDataSetChanged();

        if (adapter.getCount() > 1)
            setCurrentItem(0);
    }

    public void setCurrentItem(int item) {
        mViewPager.setCurrentItem(item);
    }

    public static class PreviewGalleryPagerAdapter extends PagerAdapter {
        public List<View> dataList;

        public PreviewGalleryPagerAdapter() {

        }

        public void setData(ArrayList<View> priviewList) {
            dataList = priviewList;
            notifyDataSetChanged();
        }

        public int getCount() {
            return dataList.size();
        }

        public void destroyItem(View container, int position, Object object) {
            ((ViewGroup) container).removeView((View) object);
            object = null;
        }

        public Object instantiateItem(View container, int position) {
            ((ViewGroup) container).addView(dataList.get(position), 0, new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            return dataList.get(position);
        }

        public boolean isViewFromObject(View container, Object object) {
            return container == (object);
        }
    }
}
