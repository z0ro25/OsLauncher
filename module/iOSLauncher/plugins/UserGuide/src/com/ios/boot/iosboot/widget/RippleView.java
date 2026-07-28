package com.ios.boot.iosboot.widget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import androidx.palette.graphics.Palette;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;


/**
 * Created by YiYang on 16-12-8.
 */

public class RippleView extends ImageView {
    private Paint mBitPaint;
    private PorterDuffXfermode mXfermode1;
    private PorterDuffXfermode mXfermode2;
    private int mX;
    private int mY;
    private boolean mIsDraw;
    private int maxRadius;
    private int mRadius;
    private int increaseSpeed = 5;
    private Bitmap mBitmap;
    private Rect mTargetRect;
    private int mTotalWidth;
    private int mTotalHeight;
    private View mShowView;

    public RippleView(Context context) {
        this(context,null);
    }

    public RippleView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public RippleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mBitPaint = new Paint();
        mXfermode1 = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
        mXfermode2 = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
        mBitPaint.setAntiAlias(false);
        mBitPaint.setFilterBitmap(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        maxRadius = h;
        mTargetRect = new Rect(0,0,w,h);
        mTotalWidth = w;
        mTotalHeight = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mIsDraw) {
//                canvas.drawBitmap(mBitmap,new Rect(0,0,mBitmap.getWidth(),mBitmap.getHeight()),mTargetRect, mBitPaint);
//                mBitPaint.setXfermode(mXfermode2);
            canvas.drawCircle(mX, mY, mRadius, mBitPaint);
            mBitPaint.setXfermode(null);
            mRadius += increaseSpeed;
            increaseSpeed += 6;
            if (mRadius < maxRadius) {
                invalidate();
            } else {
                mShowView.setBackground(new BitmapDrawable(mBitmap));
                ObjectAnimator animator = ObjectAnimator.ofFloat(this, "alpha", 1.0f, 0.0f).setDuration(1000);
                animator.setInterpolator(new DecelerateInterpolator());
                animator.start();
                mIsDraw = false;
            }

        }
        super.onDraw(canvas);

    }

    public void startAnimation(){
        setVisibility(VISIBLE);
        mIsDraw = true;
        invalidate();
    }
    public RippleView setPositionView(View view){
        int[] i = new int[2];
        view.getLocationOnScreen(i);
        mX = i[0];
        mY = i[1];
        return this;
    }
    public RippleView setBitmap(Bitmap bitmap){
        mBitmap = bitmap;
        Palette palette = Palette.from(mBitmap).generate();
        mBitPaint.setColor(palette.getMutedColor(Color.rgb(135, 206, 255)));
        return this;
    }

    public RippleView setShowView(View showView) {
        mShowView = showView;
        return this;
    }
}
