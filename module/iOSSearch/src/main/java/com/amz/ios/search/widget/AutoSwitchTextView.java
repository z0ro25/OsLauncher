package com.amz.ios.search.widget;

import android.content.Context;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;

import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ioslauncher.iossearch.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-19 下午4:09
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class AutoSwitchTextView extends TextSwitcher implements TextSwitcher.ViewFactory {

    private static final String TAG = AutoSwitchTextView.class.getSimpleName();

    private AutoAnimation mInUp;
    private AutoAnimation mOutUp;

    private long mInteplaysec = TimeUnit.SECONDS.toMillis(5);

    private int mCurrentPos = 0;
    private int mMaxPool = 2;
    private boolean mStop = false;
    private LayoutInflater mLayoutInflater;

    public AutoSwitchTextView(Context context) {
        this(context, null);
        // TODO Auto-generated constructor stub
    }

    public AutoSwitchTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
        mLayoutInflater = LayoutInflater.from(context);
        init();
    }

    /**
     * text container
     */
    private final List<String> mMessage = new ArrayList<>();

    private void init() {
        // TODO Auto-generated method stub
        setFactory(this);
        mInUp = createAnim(-90, 0, true, true);
        mOutUp = createAnim(0, 90, false, true);
        setInAnimation(mInUp);
        setOutAnimation(mOutUp);
    }

    public AutoSwitchTextView setMessage(List<String> message) {
        mMessage.addAll(message);
        mMaxPool = message.size();
        setText(mMessage.get(getCurrentPos()));
        return this;
    }

    public AutoSwitchTextView setInteplaysec(long inteplaysec) {
        mInteplaysec = Math.max(inteplaysec, TimeUnit.SECONDS.toMillis(1));
        return this;
    }

    public AutoSwitchTextView start() {
        setText(mMessage.get(getCurrentPos()));
        mInUp.setAnimationListener(mAnimationListener);
        mStop = false;
        return this;
    }

    public AutoSwitchTextView stop() {
        mInUp.setAnimationListener(null);
        mStop = true;
        return this;
    }

    private int getCurrentPos() {
        int temp = mCurrentPos % mMaxPool;
        mCurrentPos = temp;
        mCurrentPos++;
        return temp;
    }

    private Animation.AnimationListener mAnimationListener = new Animation.AnimationListener() {

        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (!mStop && mMessage.size() >= 2) {
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setText(mMessage.get(getCurrentPos()));
                    }
                }, mInteplaysec);
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    };

    private AutoAnimation createAnim(float start, float end, boolean turnIn, boolean turnUp) {
        final AutoAnimation rotation = new AutoAnimation(start, end, turnIn, turnUp);
        rotation.setDuration(1000);
        rotation.setFillAfter(false);
        rotation.setInterpolator(new LinearInterpolator());
        return rotation;
    }

    @Override
    public View makeView() {
        CustomTextView mTextView = (CustomTextView) mLayoutInflater.inflate(R.layout.fmsearch_layout_autoshow_textview, null);
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.CENTER_VERTICAL;
        mTextView.setLayoutParams(lp);
        return mTextView;
    }

    private String mText;

    public void setViewText(String text) {
        mText = text;
    }

    public String getViewText() {
        return mText;
    }

    class AutoAnimation extends Animation {
        private final float mFromDegrees;
        private final float mToDegrees;
        private float mCenterX;
        private float mCenterY;
        private final boolean mTurnIn;
        private final boolean mTurnUp;
        private Camera mCamera;

        public AutoAnimation(float fromDegrees, float toDegrees, boolean turnIn, boolean turnUp) {
            mFromDegrees = fromDegrees;
            mToDegrees = toDegrees;
            mTurnIn = turnIn;
            mTurnUp = turnUp;

        }

        @Override
        public void initialize(int width, int height, int parentWidth, int parentHeight) {
            super.initialize(width, height, parentWidth, parentHeight);
            mCamera = new Camera();
            mCenterY = getHeight() / 2;
            mCenterX = getWidth() / 2;

        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            final float fromDegrees = mFromDegrees;
            float degrees = fromDegrees + ((mToDegrees - fromDegrees) * interpolatedTime);

            final float centerX = mCenterX;
            final float centerY = mCenterY;
            final Camera camera = mCamera;
            final int derection = mTurnUp ? 1 : -1;

            final Matrix matrix = t.getMatrix();

            camera.save();
            if (mTurnIn) {
                camera.translate(0.0f, derection * mCenterY * (interpolatedTime - 1.0f), 0.0f);
            } else {
                camera.translate(0.0f, derection * mCenterY * (interpolatedTime), 0.0f);
            }
            camera.getMatrix(matrix);
            camera.restore();

            matrix.preTranslate(-centerX, -centerY);
            matrix.postTranslate(centerX, centerY);
        }
    }
}
