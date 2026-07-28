package com.amz.ios.launcher;

import android.animation.ObjectAnimator;
import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;

public class WidgetHostView extends AppWidgetHostView implements IShakeInterface {

    Launcher mLauncher;
    Path mPath;
    Paint mPaint;
    RectF mRectF;
    DeviceProfile mDeviceProfile;
    TextViewCustomFont mLabelTV;
    DragLayer mDragLayer;
    LayoutInflater mLayoutInflater;

    public WidgetHostView(Context context) {
        super(context);
        setUp();
        config();
    }

    void setUp(){
        mLauncher = (Launcher) getContext();
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPath = new Path();
        mRectF = new RectF();
        mDeviceProfile = mLauncher.getDeviceProfile();
        mLayoutInflater = LayoutInflater.from(mLauncher);
        mDragLayer = mLauncher.getDragLayer();
        mLabelTV = new TextViewCustomFont(mLauncher,null);
    }

    void config(){
        DragLayer.LayoutParams layoutParams = new DragLayer.LayoutParams(-1,-2);
        layoutParams.gravity = 80;
        addView(mLabelTV,0,layoutParams);
        mLabelTV.measure(0,0);
    }

    @Override
    public void beginOrAdjustHintAnimations() {

    }

    @Override
    public void beginOrAdjustHintAnimations(int i) {

    }

    @Override
    public void completeAndClearReorderHintAnimations() {

    }

    @Override
    public void joinAnimations(View view) {

    }
}
