package com.amz.ios.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;

public class CustomIconView extends AppCompatImageView {

    Drawable mDrawable;
    Context mContext;
    Rect mRect;
    Path mPath;
    RectF mRectF;
    int[] mLocations = new int[2];
    int mIconCorner;
    int mIconLeft;
    int mIconTop;
    boolean isClockView = false;
    boolean isAttachedToWindow = false;

    public CustomIconView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomIconView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        isClockView = false;
        isAttachedToWindow = false;
        mPath = new Path();
        mRect = new Rect();
        mRectF = new RectF();
        mIconLeft = -1;
        mIconTop = -1;
        mIconCorner = (int) context.getResources().getDimension(R.dimen.icon_round_corner);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        isAttachedToWindow = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isAttachedToWindow = false;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!changed)return;
        if (mDrawable == null) return;
        try {
            int width = getWidth();
            int height = getHeight();
            if (Build.VERSION.SDK_INT >= 18){
                mRect.set(0,0,width,height);
                setClipBounds(mRect);
            }
            getLocationOnScreen(mLocations);
            mIconLeft = mLocations[0];
            mIconTop = mLocations[1];
            mRectF.set(0.f,0.f,width,height);
            mPath.addRoundRect(mRectF,mIconCorner,mIconCorner, Path.Direction.CW);
        }
        catch (Throwable th){
            th.getMessage();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isAttachedToWindow && isClockView && isShown())
            postInvalidateDelayed(1000L);
        super.onDraw(canvas);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (Build.VERSION.SDK_INT >= 18 && mDrawable != null){
            canvas.clipPath(mPath);
        }
        super.draw(canvas);
    }

    public void setClockView(boolean flag){
        isClockView = flag;
    }
}
