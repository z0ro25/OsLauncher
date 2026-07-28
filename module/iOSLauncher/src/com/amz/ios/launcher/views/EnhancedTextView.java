package com.amz.ios.launcher.views;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import com.amz.ios.launcher.R;

public class EnhancedTextView extends CustomTextView {

    private int mRightDrawableHeight;
    private int mRightDrawableWidth;


    public EnhancedTextView(Context context) {
        this(context, null);
    }


    public EnhancedTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EnhancedTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray array = context.obtainStyledAttributes(attrs, com.amz.ios.ioslite.common.R.styleable.EnhancedTextView);
        int padding = array.getDimensionPixelSize(com.amz.ios.ioslite.common.R.styleable.EnhancedTextView_drawablePadding, 0);
        mRightDrawableHeight = array.getDimensionPixelSize(com.amz.ios.ioslite.common.R.styleable.EnhancedTextView_drawableRightHeight, 0);
        mRightDrawableWidth = array.getDimensionPixelSize(com.amz.ios.ioslite.common.R.styleable.EnhancedTextView_drawableRightWidth, 0);
        setCompoundDrawablePadding(padding);
    }

    public void setRightDrawable(int resId) {
        if (resId > 0) {
            Resources res = getResources();
            Drawable drawable = res.getDrawable(resId);
            int width = Math.min(drawable.getIntrinsicWidth(), mRightDrawableWidth);
            int height = Math.min(drawable.getIntrinsicHeight(), mRightDrawableHeight);
            drawable.setBounds(0, 0, width, height);
            setCompoundDrawables(null, null, drawable, null);
        } else {
            setCompoundDrawables(null, null, null, null);
        }
    }

}
