package com.ios.boot.iosboot.widget;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.ios.boot.iosboot.utils.Utils;

import java.util.Random;


/**
 * Created by YiYang on 16-12-7.
 */

public class TailBall extends View {
    private Paint mPaint;
    private float x;
    private float y;

    public TailBall(Context context) {
        this(context,null);
    }

    public TailBall(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TailBall(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mPaint = new Paint();
        x = ((Activity)context).getWindowManager().getDefaultDisplay().getWidth();
        y = ((Activity)context).getWindowManager().getDefaultDisplay().getHeight();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 1;i<16;i++) {
            for(int j=1;j<16;j++){
                mPaint.setColor(Utils.createRandomColor());
                mPaint.setAlpha(new Random().nextInt(100)+155);
                float cx = i*(x/16f);
                float cy = j*(y/16f);
                float radius = new Random().nextFloat()*20+5;
                canvas.drawCircle(cx, cy, radius, mPaint);
            }
        }
    }

    public static TailBall attach2Window(Activity activity) {
        ViewGroup rootView = (ViewGroup) activity.findViewById(Window.ID_ANDROID_CONTENT);
        TailBall explosionField = new TailBall(activity);
        rootView.addView(explosionField, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return explosionField;
    }

    public void start(){
        this.invalidate();
    }
}
