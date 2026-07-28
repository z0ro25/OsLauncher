package com.oslauncher.applauncher.themelauncher.views.dot_Indicator;

public abstract class AbstractViewPagerAttacker<T> implements DotIndicator.PagerAttacker<T> {

    public void updateIndicatorOnPagerScrolled(DotIndicator indicator, int position, float positionOffset) {
        final float offset;
        // ViewPager may emit negative positionOffset for very fast scrolling
        if (positionOffset < 0) {
            offset = 0;
        } else if (positionOffset > 1) {
            offset = 1;
        } else {
            offset = positionOffset;
        }
        indicator.onPageScrolled(position, offset);
    }
}
