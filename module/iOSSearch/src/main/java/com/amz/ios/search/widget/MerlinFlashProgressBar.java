package com.amz.ios.search.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ProgressBar;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-29 下午5:27
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class MerlinFlashProgressBar extends ProgressBar {

    private static final String TAG = MerlinFlashProgressBar.class.getSimpleName();

    public MerlinFlashProgressBar(Context context) {
        this(context, null);
    }

    public MerlinFlashProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MerlinFlashProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initWitchContext(context);
    }

    private LinearGradient mLinearGradient;
    private Paint mFlashPaint;

    private boolean mStop = false;
    private Matrix mGradientMatrix;
    private int mViewWidth = 0;
    private int mViewHeight = 0;
    private int mTranslate = 0;

    private void initWitchContext(Context context) {
        mFlashPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFlashPaint.setColor(Color.parseColor("#ffffff"));

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //nothing change then jump init
        if (mViewWidth != 0 || (mViewWidth == w && mViewHeight == h)) return;
        mViewWidth = getMeasuredWidth();
        mViewHeight = getMeasuredHeight();
        // 创建LinearGradient对象
        // 起始点坐标（-mViewWidth, 0） 终点坐标（0，0）
        // 第一个,第二个参数表示渐变起点 可以设置起点终点在对角等任意位置
        // 第三个,第四个参数表示渐变终点
        // 第五个参数表示渐变颜色
        // 第六个参数可以为空,表示坐标,值为0-1
        // 如果这是空的，颜色均匀分布，沿梯度线。
        // 第七个表示平铺方式
        // CLAMP重复最后一个颜色至最后
        // MIRROR重复着色的图像水平或垂直方向已镜像方式填充会有翻转效果
        // REPEAT重复着色的图像水平或垂直方向
        Log.d(TAG, ">>>>>>MerlinFlashProgressBar#onSizeChanged : init ");
        mLinearGradient = new LinearGradient(-w / 10, 0, 0, 0,
                new int[]{0x33ffffff, 0xffffffff, 0x33ffffff},
                new float[]{0, 0.5f, 1}, Shader.TileMode.CLAMP);
        mFlashPaint.setShader(mLinearGradient);
        mGradientMatrix = new Matrix();
    }

    public void start() {
        mStop = false;
    }

    public void stop() {
        mStop = true;
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!mStop) {
            drawFlash(canvas);
        }
    }


    private void drawFlash(Canvas canvas) {
        mTranslate += mViewWidth / 10;
        if (mTranslate > mViewWidth) {
            mTranslate = -mViewWidth;
        }
        mGradientMatrix.setTranslate(mTranslate, 0);
        mLinearGradient.setLocalMatrix(mGradientMatrix);
        postInvalidateDelayed(50);
    }
}
