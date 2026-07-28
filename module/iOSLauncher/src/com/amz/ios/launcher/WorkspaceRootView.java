package com.amz.ios.launcher;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewConfiguration;

import com.amz.ios.ioslite.common.util.DisplayUtil;
import com.amz.ios.launcher.config.GestureEventModel;

import java.util.Calendar;

/**
 * RootView , container : workspace, pageindicator, hotseat, overviewpanel;
 */
public class WorkspaceRootView extends InsettableFrameLayout {
    private Launcher mLauncher;

    private int mScrollTouchSlop;
    private int mClickTouchSlop;
    private static final int INVALID_POINTER = -1;
    private int mActivePointerId = INVALID_POINTER;
    private float mXDown, mYDown;
    private long mLastClickTime;
    private boolean mHandled = false;

    final static int DOUBLE_CLICK_TIME_PERIOD = 500;
    final static int GESTURE_SCROLL_SLOP_DP = 50;

    final static float VERTICAL_MIN_SWIPE_ANGLE = (float) Math.PI / 3;

    private ScaleGestureDetector mScaleGestureDetector;

    public WorkspaceRootView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLauncher = (Launcher) context;
        mScrollTouchSlop = DisplayUtil.dip2px(context, GESTURE_SCROLL_SLOP_DP);
        mClickTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.mScaleGestureDetector = new ScaleGestureDetector(context, new CustomScaleGestureListener());
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        this.mScaleGestureDetector.onTouchEvent(ev);

        int touchCount = ev.getPointerCount();
        if (touchCount > 1) {
//            return true;
        }


        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mHandled = false;
                mActivePointerId = ev.getPointerId(0);
                mXDown = ev.getX();
                mYDown = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (!mHandled && pointerIndex > -1) {
                    mHandled = determineScrollGestureStart(ev);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (!mHandled) {
                    mHandled = determineClickGestureStart(ev);
                }
                mActivePointerId = INVALID_POINTER;
                break;
            default:
                break;

        }
        return super.onInterceptTouchEvent(ev);
    }

    protected boolean determineScrollGestureStart(MotionEvent ev) {
        if (!mLauncher.enableGestureEvent()) {
            return false;
        }

        boolean result = false;
        float deltaY = ev.getY() - mYDown;
        float absDeltaY = Math.abs(deltaY);
        float absDeltaX = Math.abs(ev.getX() - mXDown);

        float slope = absDeltaY / absDeltaX;
        float theta = (float) Math.atan(slope);

        if (theta > VERTICAL_MIN_SWIPE_ANGLE) {
            if (absDeltaY > mScrollTouchSlop) {
                if (deltaY > 0) {
//                    GestureEventModel.triggerGestureEvent(mLauncher, GestureEventModel.GESTURE_SWIPE_DOWN);
                } else {
//                    GestureEventModel.triggerGestureEvent(mLauncher, GestureEventModel.GESTURE_SWIPE_UP);
                }

                result = true;
            }
        }

        return result;
    }

    protected boolean determineClickGestureStart(MotionEvent ev) {
        if (!mLauncher.enableGestureEvent()) {
            return false;
        }

        boolean result = false;
        float absDeltaY = Math.abs(ev.getY() - mYDown);
        float absDeltaX = Math.abs(ev.getX() - mXDown);
        if (absDeltaX < mClickTouchSlop && absDeltaY < mClickTouchSlop) {
            long time = Calendar.getInstance().getTimeInMillis();
            if ((time - mLastClickTime) < DOUBLE_CLICK_TIME_PERIOD) {
                GestureEventModel.triggerGestureEvent(mLauncher, GestureEventModel.GESTURE_DOUBLE_TAP, GestureEventModel.DOUBLE_CLICK);
                result = true;
            }
            mLastClickTime = time;
        }

        return result;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.mLauncher.setWorkspaceWidth(getMeasuredWidth());
        this.mLauncher.setWorkspaceHeight(getMeasuredHeight());
    }

    private class CustomScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
        }
        @Override
        public boolean onScaleBegin (ScaleGestureDetector detector) {
            return true;
        }
        @Override
        public boolean onScale (ScaleGestureDetector detector) {
            if (detector.getScaleFactor() <= 1) {
                mLauncher.popWorkspace(true);
            }
            return true;
        }

    }
}
