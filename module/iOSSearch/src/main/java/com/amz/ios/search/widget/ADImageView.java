package com.amz.ios.search.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Author       : yizhihao
 * Create time  : 2016-12-06 下午5:15
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class ADImageView extends ImageView {

    private String mText;
    private int textColor;
    private Paint mTxPaint;

    private int mAdWidth;
    private int mAdHeight;
    private Rect r;

    private int mBgColorInt;
    private int mTxColorInt;

    public ADImageView(Context context) {
        this(context, null);
    }

    public ADImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ADImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        r = new Rect();
        mAdWidth = dip2px(context, 15);
        mAdHeight = dip2px(context, 28);

        mBgColorInt = Color.parseColor("#55000000");
        mTxColorInt = Color.parseColor("#000000");

        mTxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTxPaint.setColor(mBgColorInt);
        setMinimumHeight(mAdHeight);
        setMinimumWidth(mAdWidth);
    }

    public ADImageView setAdWH(int adWidth, int adHeight) {
        if (mAdHeight != adHeight || mAdWidth != mAdWidth) {
            this.mAdWidth = adWidth;
            this.mAdHeight = adHeight;
            r.set(0, 0, mAdWidth, mAdHeight);
            invalidate();
        }
        return this;
    }

    public ADImageView setBgAndTx(int bgColor, int txColor) {
        if (mBgColorInt != bgColor || mTxColorInt != txColor) {
            this.mBgColorInt = bgColor;
            this.mTxColorInt = txColor;
            invalidate();
        }
        return this;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawAd(canvas);
    }

    private void drawAd(Canvas canvas) {
        canvas.drawRect(r, mTxPaint);
        mTxPaint.setColor(mTxColorInt);
        canvas.drawText("AD", 1, 1, 1, 1, mTxPaint);
    }

    private int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }
}
