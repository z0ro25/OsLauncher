package com.amz.ios.launcher.widget;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;

public class WidgetAppStyleListAdapter extends PagerAdapter {

    ArrayList<WidgetAppStyleCell> mCells = new ArrayList<>();

    WidgetAppStyleCell mCurrentCell;

    public WidgetAppStyleListAdapter(ArrayList<WidgetAppStyleCell> cells) {
        super();
        this.mCells = cells;
    }

    @Override
    public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        super.setPrimaryItem(container, position, object);
        if (object instanceof WidgetAppStyleCell) {
            mCurrentCell = (WidgetAppStyleCell) object;
        }
    }

    @Override
    public int getCount() {
        if (this.mCells == null) return 0;
        return this.mCells.size();
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return POSITION_NONE;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        WidgetAppStyleCell cell = mCells.get(position);
        container.addView(cell);
        return cell;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }
}
