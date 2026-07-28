
package com.amz.ios.search.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.TextView;

import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.search.http.HotwordResponseBean;
import com.amz.ioslauncher.iossearch.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * set on finish flash changed text to start animation
 * by set flash mInteplaysec to control each flash time
 * <p/>
 * Author       : yizhihao
 * Create time  : 2016-11-24 上午1:38
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class MerlinFlashTextView extends CustomTextView implements MerlinSwitchView {

    private static final String TAG = MerlinFlashTextView.class.getSimpleName();

    private FlashAnimation mAnimation;

    public static final long DELAY_FLASH_TIME = 500;

    private long mInteplaysec = TimeUnit.SECONDS.toMillis(10);

    private int mode;
    private boolean isWindowsFocus = true;
    private boolean isAnimating = false;

    private int mFlashColor;
    //flash speed
    private float mFactor = 0.05f;
    private int mIndex = 0;
    private int mViewWidth;
    private int mViewHeight;

    private int mRawWidth;
    private int mRawHeight;
    private Paint mFlashPaint;

    private int mCurrentPos = 0;
    private int mMaxPool = 2;
    private boolean mStop = true;
    private Interpolator mInterpolator;
    private LinearGradient mLinearGradient;
    private String mCurrentTxt = "";

    public MerlinFlashTextView(Context context) {
        this(context, null);
    }

    public MerlinFlashTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MerlinFlashTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mFlashColor = getResources().getColor(R.color.fmsearch_cl_flash, context.getTheme());
        } else {
            mFlashColor = getResources().getColor(R.color.fmsearch_cl_flash);
        }
        mFlashPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFlashPaint.setColor(mFlashColor);
        mode = AUTO_FLASH_MODE;
        mInterpolator = new AccelerateInterpolator(1.2f);

    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
    }


    public String getCurrentTxt() {
        return mCurrentTxt;
    }

    private Runnable mDelay = new Runnable() {
        @Override
        public void run() {
            if (isWindowsFocus) {
                mStop = false;
            } else {
                isAnimating = false;
            }
        }
    };


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawFlash(canvas);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }

    private void drawFlash(Canvas canvas) {
        if (mStop) return;
        canvas.drawRect(mRawWidth - mInterpolator.getInterpolation(mIndex) * mFactor, mRawHeight - mViewHeight, mRawWidth, mRawHeight, mFlashPaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mViewWidth = w;
        mViewHeight = h;

        mRawWidth = getMeasuredWidth();
        mRawHeight = getMeasuredHeight();
        mLinearGradient = new LinearGradient(0, 0, mViewWidth, 0,
                new int[]{0xffffffff, 0x88ffffff, 0x00ffffff},
                new float[]{0, 0.8f, 1}, Shader.TileMode.CLAMP);
        mFlashPaint.setShader(mLinearGradient);

        //for draw a oval
        //mRectF.set(mRawWidth - mViewWidth * 3 / 2, mRawHeight - mViewHeight * 5 ,mRawWidth + mViewWidth / 2 , mRawHeight + mViewHeight * 11 / 2 );
        Log.d(TAG, ">>>>>>FlashTextView#onSizeChanged : [" + mViewWidth + "," + mViewHeight + "," + mRawWidth + "," + mRawHeight + "]");
    }

    private class FlashAnimation extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    if (mStop) {
                        resetFlash();
                    } else {
                        mIndex++;
                        //run to the end of animation
                        if (mViewWidth < mInterpolator.getInterpolation(mIndex) * mFactor) {
                            resetFlash();
                            //set text
                            onFinishFlash();
                            if (mode == AUTO_FLASH_MODE) {
                                Thread.sleep(mInteplaysec);
                            } else {
                                mStop = true;
                                isAnimating = false;
                            }
                        }
                    }
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
                postInvalidate();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAnimation == null) return;
        mAnimation.interrupt();
        mAnimation = null;
    }

    private HotwordResponseBean.DataBean.WordsBean nextWord() {
        if (words == null || words.size() <= 0) {
            return null;
        }
        mCurrentPos = (++mCurrentPos) % words.size();
        return words.get(mCurrentPos);
    }

    private void onFinishFlash() {
        post(new Runnable() {
            @Override
            public void run() {
                setText(nextWord().getTitle());
            }
        });
    }

    private void resetFlash() {
        mIndex = 0;
    }

    /**
     * text container
     */
    private final List<String> mMessage = new ArrayList<>();


    public MerlinFlashTextView setInteplaysec(long inteplaysec) {
        mInteplaysec = Math.max(inteplaysec, TimeUnit.SECONDS.toMillis(1));
        return this;
    }

    public MerlinFlashTextView setMode(int mode) {
        this.mode = mode;
        return this;
    }

    public MerlinFlashTextView start() {
        if (isAnimating) return this;
        if (mAnimation == null) {
            mAnimation = new FlashAnimation();
            mAnimation.start();
        }
        isAnimating = true;
        mStop = true;
        if (TextUtils.isEmpty(getText().toString())) {
            setText(nextWord().getTitle());
        }
        Log.d(TAG, ">>>>>>FlashTextView#start : ");
        postDelayed(mDelay, DELAY_FLASH_TIME);
        return this;
    }

    @Override
    public MerlinSwitchView setTextView(CustomTextView view) {
        return this;
    }

    List<HotwordResponseBean.DataBean.WordsBean> words;

    @Override
    public MerlinSwitchView setHotword(List<HotwordResponseBean.DataBean.WordsBean> words) {
        if (words == null || words.size() <= 0) {
            return this;
        }
        this.words = words;
        mCurrentPos = 0;
        return this;
    }

    @Override
    public HotwordResponseBean.DataBean.WordsBean getCurrentHotword() {
        return words.get(mCurrentPos);
    }


    public MerlinFlashTextView stop() {
        resetFlash();
        return this;
    }


    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        isWindowsFocus = hasWindowFocus;
    }
}
