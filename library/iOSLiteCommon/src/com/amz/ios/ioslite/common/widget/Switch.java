package com.amz.ios.ioslite.common.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.widget.CompoundButton;

import com.amz.ios.ioslite.common.R;


public class Switch extends CompoundButton implements ViewTreeObserver.OnGlobalLayoutListener {
    private static final String TAG = "Switch";

    private Paint paint;
    private int width;
    private int height;
    private float startPosition;
    private float endPosition;
    private int btnWidth;
    private float nowX;
    private float preX;
    private boolean mChecked = true;

    int lineCheckedColor;
    int lineUnCheckedColor;
    int lineHeight;
    int innerCheckedBroderColor;
    int innerUnCheckedBroderColor;
    int innerBroderWidth;
    int btnCheckedColor;
    int btnUnCheckedColor;
    int innerPadding;

    private Rect lineRect;

    private RectF buttonRect;
    private RectF buttonInnerRect;

    public Switch(Context context) {
        this(context, null);
    }

    public Switch(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.Switch);
        lineCheckedColor = a.getColor(R.styleable.Switch_lineCheckedColor, Color.parseColor("#00000000"));
        lineUnCheckedColor = a.getColor(R.styleable.Switch_lineUnCheckedColor, Color.parseColor("#00000000"));
        lineHeight = a.getDimensionPixelSize(R.styleable.Switch_lineHeight, 0);
        innerCheckedBroderColor = a.getColor(R.styleable.Switch_innerCheckedBroderColor, Color.parseColor("#00000000"));
        innerUnCheckedBroderColor = a.getColor(R.styleable.Switch_innerUnCheckedBroderColor, Color.parseColor("#00000000"));
        innerBroderWidth = a.getDimensionPixelSize(R.styleable.Switch_innerBroderWidth, 0);
        btnCheckedColor = a.getColor(R.styleable.Switch_btnCheckedColor, Color.parseColor("#2ECC71"));
        btnUnCheckedColor = a.getColor(R.styleable.Switch_btnUnCheckedColor, Color.parseColor("#BDC3C7"));
        innerPadding = a.getDimensionPixelSize(R.styleable.Switch_innerPadding, 0);
        init();
    }

    public void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        ViewTreeObserver vo = this.getViewTreeObserver();
        vo.addOnGlobalLayoutListener(this);
    }

    @Override
    public void onGlobalLayout() {
        width = getWidth();
        height = getHeight();
        btnWidth = height - getPaddingTop() - getPaddingBottom();
        startPosition = getPaddingLeft() + innerPadding;
        endPosition = width - btnWidth - getPaddingRight() - innerPadding;
        lineRect = new Rect(btnWidth / 2 + getPaddingLeft(), (height - lineHeight) / 2, width - btnWidth / 2 - getPaddingRight(), (height + lineHeight) / 2);
        if (mChecked) {
            nowX = endPosition;
            buttonRect = new RectF(endPosition, getPaddingTop() + innerPadding, btnWidth + endPosition, height - getPaddingBottom() - innerPadding);
        } else {
            nowX = startPosition;
            buttonRect = new RectF(startPosition, getPaddingTop() + innerPadding, btnWidth + startPosition, height - getPaddingTop() - innerPadding);
        }
        buttonInnerRect = new RectF(buttonRect.left + innerBroderWidth, buttonRect.top + innerBroderWidth, buttonRect.right - innerBroderWidth, buttonRect.bottom - innerBroderWidth);
        invalidate();
    }


    @Override
    public void setChecked(boolean checked) {
        this.mChecked = checked;
        if (mChecked) {
            nowX = endPosition;
        } else {
            nowX = startPosition;
        }

        if (mOnCheckedChangeListener != null) {
            mOnCheckedChangeListener.onCheckedChanged(this, checked);
        }
        invalidate();
    }

    public void toggle() {
        setChecked(!mChecked);
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }


    OnCheckedChangeListener mOnCheckedChangeListener;

    public void setOnCheckedChangeListener(OnCheckedChangeListener changeListener) {
        this.mOnCheckedChangeListener = changeListener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        buttonRect.left = nowX;
        buttonRect.right = nowX + btnWidth;
        buttonInnerRect.left = buttonRect.left + innerBroderWidth;
        buttonInnerRect.right = buttonRect.right - innerBroderWidth;
        if (isRight()) {
            paint.setColor(lineCheckedColor);
            canvas.drawRect(lineRect, paint);
            paint.setColor(innerCheckedBroderColor);
            canvas.drawOval(buttonRect, paint);
            paint.setColor(btnCheckedColor);
            canvas.drawOval(buttonInnerRect, paint);
        } else {
            paint.setColor(lineUnCheckedColor);
            canvas.drawRect(lineRect, paint);
            paint.setColor(innerUnCheckedBroderColor);
            canvas.drawOval(buttonRect, paint);
            paint.setColor(btnUnCheckedColor);
            canvas.drawOval(buttonInnerRect, paint);
        }
    }

    private boolean isRight() {
        return nowX > (width - btnWidth) / 2;
    }

}