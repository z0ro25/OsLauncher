package com.amz.ios.launcher.applibrary;

import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.amz.ios.launcher.InsettableFrameLayout;

public class OpenedView extends InsettableFrameLayout {

    public Paint mPaint;
    public boolean canRun;
    public int right;
    public View mView;

    public OpenedView(Context context){
        super(context,null);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (this.canRun) {
            int width = getWidth();
            canvas.drawRect(width - this.right, 0.0f, width, getHeight(), this.mPaint);
        }
    }

    @Override
    protected boolean fitSystemWindows(Rect insets) {
        boolean sdkVersion = Build.VERSION.SDK_INT > 23;
        ActivityManager activityManager = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);

        canRun = insets.right > 0 && (!sdkVersion || activityManager.isLowRamDevice());
        this.right = insets.right;
        setInsets(canRun ? new Rect(0, insets.top, 0, insets.bottom) : insets);
        if (mView != null && this.canRun) {
            MarginLayoutParams marginLayoutParams = (MarginLayoutParams) mView.getLayoutParams();
            int leftMargin = marginLayoutParams.leftMargin;
            int left = insets.left;
            if (leftMargin != left || marginLayoutParams.rightMargin != insets.right) {
                marginLayoutParams.leftMargin = left;
                marginLayoutParams.rightMargin = insets.right;
                this.mView.setLayoutParams(marginLayoutParams);
            }
        }
        return true;
    }

    @Override // android.view.View
    public final void onFinishInflate() {
        if (getChildCount() > 0) {
            this.mView = getChildAt(0);
        }
        super.onFinishInflate();
    }

    @Override // android.view.View
    public void setBackground(Drawable drawable) {
        super.setBackground(drawable);
        if (drawable != null) {
            ObjectAnimator duration = ObjectAnimator.ofInt(drawable, "alpha", 0, 255).setDuration(268L);
            duration.setInterpolator(new DecelerateInterpolator());
            duration.start();
        }
    }
}
