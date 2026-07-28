package com.zhuoyi.security.batterysave.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import com.zhuoyi.security.batterysave.R;


/**
 * TODO: document your custom view class.
 */
public class BS_PrcProgressBar extends View {
    private int CricleBarStrokeWidth;
    private int MaxBgColor = Color.parseColor("#FFFFFF");// 画布的背景颜色
    private int barColor = Color.WHITE;

    private int startAngle = 135;
    private int endAngle = 270;
    private Paint mPaintBar, mPaintBg, mPainLittleCircle, mPainText;
    private RectF rectBg = null;
    int cx1, cy1;
    private int width, r;

    private int progress = 0;
    private String timeStr = "";
    private String baterry_text1 = "";
    private String baterry_text3 = "";
    private Drawable chargeDrawable;
    private int chargeState = -1; //0:uncharge  1:charging  2: charged

    private float scale;
    private Context con;
    private boolean drawBaterryNamebr = false;


    public BS_PrcProgressBar(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
        this.con = context;
    }


    public BS_PrcProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.con = context;
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.bs_progressBar);
        int resRrogress = typedArray.getInteger(R.styleable.bs_progressBar_progress1, 0);
        formatProgress(resRrogress);
        baterry_text1 = con.getResources().getString(R.string.bs_baterry_text1_uncharge);
        baterry_text3 = typedArray.getString(R.styleable.bs_progressBar_text3);
        chargeDrawable = typedArray.getDrawable(R.styleable.bs_progressBar_chargeDrawable);
        timeStr = typedArray.getString(R.styleable.bs_progressBar_timeStr);

        if (TextUtils.isEmpty(baterry_text3)) {
            baterry_text3 = "test3";//con.getResources().getString(R.string.baterry_text3);
        }
        if (TextUtils.isEmpty(timeStr)) {
            timeStr = "time";
        }

        mPainText = new Paint();

        typedArray.recycle();


    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO Auto-generated method stub
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        scale = dm.density;
        width = measureWidth(widthMeasureSpec);//dm.widthPixels;
        int height = measureHeight(heightMeasureSpec);
        setMeasuredDimension(width, height);
        InitDisplay();

        Log.d("zengrui", widthMeasureSpec + ":" + heightMeasureSpec + "======" + width + "===" + height + "===" + dm.widthPixels + ":" + dm.heightPixels + "===r:" + r + "===scale:" + scale);
    }

    private void InitDisplay() {

        CricleBarStrokeWidth = (int) (25 * scale);
        r = (int) width / 3;
        cx1 = width / 2;
        cy1 = (int) (r + CricleBarStrokeWidth / 6 + scale * 35);
    }

    /**
     * 计算组件宽度
     */
    private int measureWidth(int widthMeasureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(widthMeasureSpec);
        int specSize = MeasureSpec.getSize(widthMeasureSpec);

        if (specMode == MeasureSpec.EXACTLY) {//精确模式
            result = specSize;
        } else {
            result = getDefaultWidth();//最大尺寸模式，getDefaultWidth方法需要我们根据控件实际需要自己实现
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    private int getDefaultWidth() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int defaltWidth = (int) (dm.widthPixels / 2 + (scale * 45 + dm.widthPixels / 24 * 7) - this.getPaddingLeft() + this.getPaddingRight());
        return defaltWidth;
    }

    /**
     * 计算组件高度
     */
    private int measureHeight(int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = getDefaultHeight();
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    private int getDefaultHeight() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int defaultHeight = (int) (dm.widthPixels / 24 * 7 + 105 * scale + dm.widthPixels / 24 * 7);
        return defaultHeight;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.onDraw(canvas);
        init(canvas);

        switch (chargeState) {
            case 0:
                baterry_text1 = con.getResources().getString(R.string.bs_baterry_text1_uncharge);
                drawText2(canvas, timeStr, cx1, cy1 + 20 * scale, Color.WHITE, 60 * scale);
                drawText3(canvas, baterry_text3, cx1, cy1 + 40 * scale, Color.WHITE, 15 * scale);
                drawText1(canvas, baterry_text1, cx1, cy1, Color.WHITE, 15 * scale);
                drawBaterryNamebr(canvas);
                break;
            case 1:
                baterry_text1 = con.getResources().getString(R.string.bs_baterry_text1_charging);
                int width = chargeDrawable.getIntrinsicWidth();
                drawChargeDrawable(canvas, chargeDrawable, cx1 - width / 2, cy1 - r + CricleBarStrokeWidth);
                drawText2(canvas, timeStr, cx1, cy1 + 20 * scale, Color.WHITE, 60 * scale);
                drawText1(canvas, baterry_text1, cx1, cy1, Color.WHITE, 15 * scale);
                drawBaterryNamebr(canvas);
                break;
            case 2:
                baterry_text1 = con.getResources().getString(R.string.bs_baterry_text1_charged);
                drawText1(canvas, baterry_text1, cx1, cy1, Color.WHITE, 15 * scale);
                break;
            default:
                break;
        }


    }


    private void init(Canvas canvas) {
        // TODO Auto-generated method stub
        rectBg = new RectF(cx1 - r, cy1 - r, cx1 + r, cy1 + r);
        // 大的画布
        mPaintBg = new Paint();
        mPaintBg.setAntiAlias(true);
        mPaintBg.setStyle(Paint.Style.STROKE);
        mPaintBg.setStrokeWidth(CricleBarStrokeWidth);
        mPaintBg.setColor(MaxBgColor);
        mPaintBg.setAlpha((int) (255 * 0.1));
        canvas.drawArc(rectBg, startAngle, endAngle, false, mPaintBg);

        // 环形ProgressBar。
        drawProcess(canvas);

        //外环小点
        drawlittleCircle(canvas);

        drawStartAndEndBaterryNamebr(canvas);

    }


    //环形ProgressBar
    private void drawProcess(Canvas canvas) {

        int[] changeColors;
        if (this.progress <= endAngle / 4) {
            changeColors = new int[]{
                    Color.argb(0, 255, 255, 255),
                    Color.argb(0, 255, 255, 255),
                    Color.argb(0, 255, 255, 255),
                    Color.argb((int) (255 * 0.2), 255, 255, 255),
                    Color.argb((int) (255 * 0.6), 255, 255, 255),
                    Color.argb((int) (255 * 0.7), 255, 255, 255),
                    Color.argb(0, 255, 255, 255),
                    Color.argb(0, 255, 255, 255)
            };

        } else if (this.progress <= endAngle / 2) {
            changeColors = new int[]{
                    Color.argb(0, 255, 255, 255),
                    Color.argb(0, 255, 255, 255),
                    Color.argb(0, 255, 255, 255),
                    Color.argb((int) (255 * 0.1), 255, 255, 255),
                    Color.argb((int) (255 * 0.3), 255, 255, 255),
                    Color.argb((int) (255 * 0.5), 255, 255, 255),
                    Color.argb((int) (255 * 0.7), 255, 255, 255),
                    Color.argb(0, 255, 255, 255)
            };
        } else {
            changeColors = new int[]{
                    Color.argb((int) (255 * 0.6), 255, 255, 255),
                    Color.argb((int) (255 * 0.7), 255, 255, 255),
                    Color.argb(0, 255, 255, 255),
                    Color.argb((int) (255 * 0.1), 255, 255, 255),
                    Color.argb((int) (255 * 0.2), 255, 255, 255),
                    Color.argb((int) (255 * 0.35), 255, 255, 255),
                    Color.argb((int) (255 * 0.5), 255, 255, 255),
                    Color.argb((int) (255 * 0.6), 255, 255, 255)
            };
        }


        mPaintBar = new Paint();
        mPaintBar.setAntiAlias(true);
        mPaintBar.setStyle(Paint.Style.STROKE);
        mPaintBar.setStrokeWidth(CricleBarStrokeWidth);
        mPaintBar.setColor(barColor);
        SweepGradient sweepGradient = new SweepGradient(cx1, cy1, changeColors, null);
        mPaintBar.setShader(sweepGradient);
        canvas.drawArc(rectBg, startAngle, progress, false, mPaintBar);
        postInvalidate();
    }

    private void drawStartAndEndBaterryNamebr(Canvas canvas) {
        if (mPainText == null) {
            mPainText = new Paint();
        }
        mPainText.setTextAlign(Paint.Align.LEFT);
        mPainText.setColor(Color.WHITE);
        mPainText.setTextSize(12 * scale);
        mPainText.setAntiAlias(true);
        mPainText.setAlpha((int) (255 * 0.5));

        String startStr = "0";
        float startStrWid = mPainText.measureText(startStr);
        float startOffsetX = (float) (r * Math.cos(startAngle * 3.14 / 180));
        float startOffsetY = (float) (r * Math.sin(startAngle * 3.14 / 180));
        canvas.drawText(startStr, cx1 + startOffsetX + startStrWid, cy1 + startOffsetY + startStrWid * 3 / 2, mPainText);

        String endStr = "100";
        float endStrWid = mPainText.measureText(endStr);
        float endOffsetX = (float) (r * Math.cos((startAngle + endAngle) * 3.14 / 180));
        float endOffsetY = (float) (r * Math.sin((startAngle + endAngle) * 3.14 / 180));
        canvas.drawText(endStr, cx1 + endOffsetX - endStrWid * 4 / 3, cy1 + endOffsetY + endStrWid / 3, mPainText);


    }

    // 画具体的数字电量
    private void drawBaterryNamebr(Canvas canvas) {
        if (mPainText == null) {
            mPainText = new Paint();
        }
        mPainText.setTextAlign(Paint.Align.LEFT);
        mPainText.setColor(Color.WHITE);
        mPainText.setTextSize(12 * scale);
        mPainText.setAntiAlias(true);
        mPainText.setAlpha(255);
        String str = String.valueOf((int) (progress / ((float) endAngle) * 100));
        float strWid = mPainText.measureText(str);
        float offsetX = (float) ((scale * 24 + r) * Math.cos((startAngle + progress) * 3.14 / 180));
        float offsetY = (float) ((scale * 24 + r) * Math.sin((startAngle + progress) * 3.14 / 180));
        canvas.drawText(str, cx1 + offsetX - strWid / 2, cy1 + offsetY, mPainText);
    }

    private void drawlittleCircle(Canvas canvas) {
        mPainLittleCircle = new Paint();
        mPainLittleCircle.setAntiAlias(true);
        mPainLittleCircle.setColor(MaxBgColor);
        mPainLittleCircle.setAlpha((int) (255 * 0.4));

        int count = 10;
        int _progress = endAngle / count;//27
        int angle = startAngle;

        for (int i = 0; i <= count; i++) {
            canvas.drawCircle(
                    (float) (cx1 + (scale * 35 + r) * Math.cos(angle * 3.14 / 180)),
                    (float) (cy1 + (scale * 35 + r) * Math.sin(angle * 3.14 / 180)),
                    CricleBarStrokeWidth / 12, mPainLittleCircle);// 小圆
            angle += _progress;
        }


    }


    private void drawChargeDrawable(Canvas canvas, Drawable chargeDrawable, int cx1, int cy1) {
        Paint paint = new Paint();
        paint.setAlpha((int) (255 * 0.5));
        Bitmap bitmap = ((BitmapDrawable) chargeDrawable).getBitmap();
        canvas.drawBitmap(bitmap, cx1, cy1, paint);

    }

    // 绘制中间的文字
    private void drawText1(Canvas canvas, String str, float x, float y,
                           int color, float size) {
        Paint paint = new Paint();
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(color);
        paint.setAlpha((int) (255 * 0.3));
        paint.setTextSize(size);
        paint.setAntiAlias(true);
        float strWid = paint.measureText(str);
        canvas.drawText(str, x - strWid / 2, y - r / 3, paint);
    }


    private void drawText2(Canvas canvas, String str, float x, float y,
                           int color, float size) {
        Paint paint = new Paint();
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setAntiAlias(true);
        float strWid = paint.measureText(str);
        canvas.drawText(str, x - strWid / 2, y + r / 8, paint);
    }


    private void drawText3(Canvas canvas, String str, float x, float y,
                           int color, float size) {
        Paint paint = new Paint();
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(color);
        paint.setAlpha((int) (255 * 0.3));
        paint.setTextSize(size);
        paint.setAntiAlias(true);
        float strWid = paint.measureText(str);
        canvas.drawText(str, x - strWid / 2, y + r / 4, paint);
    }

    public void setTime(String time, String day) {
        this.timeStr = time;
        this.baterry_text3 = day;
        invalidate();
    }

    // 设置时间的回调
    public void setTimeValue(String time) {
        this.timeStr = time;
        invalidate();
    }


    public void setText3(String baterry_text3) {
        this.baterry_text3 = baterry_text3;
        invalidate();
    }

    public void setTimeInvisible() {
        setTimeValue("");
    }


    public void setText3Invisible() {
        setText3("");
    }

    /**
     * @param process(0 ~ 100)
     */
    public void setProcess(int process) {
        formatProgress(process);
        invalidate();
        /*drawBaterryNamebr = false;

        if (this.progress < process) {
            while (this.progress < process) {
                this.progress++;
                invalidate();
            }
            drawBaterryNamebr = true;
        } else {
            this.progress = 0;
            drawBaterryNamebr = false;
            setProcess(process);
        }*/
    }

    private void formatProgress(int progress) {
        this.progress = (int) (progress * 270.0 / 100);
    }

    /**
     * 0:uncharge  1:charging  2: charged
     *
     * @param chargeState
     */
    public void setChargeState(int chargeState) {
        this.chargeState = chargeState;
        invalidate();
    }


}
