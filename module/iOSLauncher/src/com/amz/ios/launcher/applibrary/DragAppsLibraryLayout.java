package com.amz.ios.launcher.applibrary;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amz.ios.launcher.InsertAbleDragLayout;

public class DragAppsLibraryLayout extends InsertAbleDragLayout {

    public Paint mPaint;
    public boolean canRun;
    public int mRight;
    public View mView;

    public DragAppsLibraryLayout(@NonNull Context context) {
        this(context,null);
    }

    public DragAppsLibraryLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public DragAppsLibraryLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.TRANSPARENT);
        mPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        try {
            super.dispatchDraw(canvas);
        }
        catch (Throwable e){
        }

        if (this.canRun) {
            int width = getWidth();
            canvas.drawRect(width - this.mRight, 0.0f, width, getHeight(), this.mPaint);
        }
    }

    @Override
    protected boolean fitSystemWindows(Rect rect) {
        Context context = getContext();
        int sdkVersion = Build.VERSION.SDK_INT;
        boolean checkSdk = sdkVersion >= 23;
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        this.canRun = rect.right > 0 && (checkSdk || (activityManager).isLowRamDevice());
        this.mRight = rect.right;
        setInsets(canRun ? new Rect(0, rect.top, 0, rect.bottom) : rect);
        View view = this.mView;
        if (view != null && this.canRun) {
            MarginLayoutParams marginLayoutParams = (MarginLayoutParams) view.getLayoutParams();
            int left = rect.left;
            if (marginLayoutParams.leftMargin != left || marginLayoutParams.rightMargin != rect.right) {
                marginLayoutParams.leftMargin = left;
                marginLayoutParams.rightMargin = rect.right;
                this.mView.setLayoutParams(marginLayoutParams);
            }
        }
        return true;
    }

    @Override
    public final void onFinishInflate() {
        if (getChildCount() > 0) {
            this.mView = getChildAt(0);
        }
        super.onFinishInflate();
    }
}
