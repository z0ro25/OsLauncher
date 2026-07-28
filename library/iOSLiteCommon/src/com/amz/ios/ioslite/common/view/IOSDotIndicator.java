package com.amz.ios.ioslite.common.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.amz.ios.ioslite.common.R;

import java.util.ArrayList;

/**
 *
 *  AVLoadingIndicatorView 包含一组漂亮的Android加载中动画。
 *  博客： http://www.open-open.com/lib/view/open1445846143992.html；
 *  Github: https://github.com/81813780/AVLoadingIndicatorView
 *
 */
public class IOSDotIndicator extends Indicator {
    private Context mContext;

    public IOSDotIndicator(Context mContext) {
        this.mContext = mContext;
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        ArrayList<Integer> colors = new ArrayList();
        colors.add(mContext.getResources().getColor(R.color.ios_gray1));
        colors.add(mContext.getResources().getColor(R.color.ios_gray2));
        colors.add(mContext.getResources().getColor(R.color.ios_gray3));

        ArrayList<Integer> radiuss = new ArrayList<>();
        int radius1 = getWidth()/2;
        int radius2 = (int) (radius1*0.65);
        int radius3 = (int) (radius2*0.3);
        radiuss.add(radius1);
        radiuss.add(radius2);
        radiuss.add(radius3);
        for (int i = 0; i < colors.size(); i++) {
            paint.setColor(colors.get(i));
            canvas.drawCircle(getWidth()/2,getHeight()/2,radiuss.get(i),paint);
        }
    }

    @Override
    public ArrayList<ValueAnimator> onCreateAnimators() {
        return null;
    }
}
