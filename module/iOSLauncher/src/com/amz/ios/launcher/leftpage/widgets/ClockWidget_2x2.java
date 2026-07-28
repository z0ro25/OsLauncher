package com.amz.ios.launcher.leftpage.widgets;

import android.content.Context;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageView;

import com.amz.ios.launcher.R;
import com.amz.ios.launcher.leftpage.drawables.ClockDrawable;

public class ClockWidget_2x2 extends AppCompatImageView {

    public Context mContext;
    public int mRoundCorner;
    public int mWidth;
    public int mHeight;

    public ClockWidget_2x2(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ClockWidget_2x2(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, 0);
        new Rect();
        new Path();
        new RectF();
        this.mWidth = -1;
        this.mHeight = -1;
        this.mContext = context;
        this.mRoundCorner = getResources().getDimensionPixelSize(R.dimen.widget_round_corner);
    }

    @Override
    public final void onAttachedToWindow() {
        super.onAttachedToWindow();
        setBackground(new ClockDrawable(this.mContext, this.mWidth, this.mHeight, this.mRoundCorner));
    }

    @Override
    public final void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        this.mWidth = getWidth();
        this.mHeight = getHeight();
    }

    @Override
    public final void onMeasure(int i, int i2) {
        super.onMeasure(i, i);
    }
}
