package com.zhuoyi.security.batterysave.views;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.RelativeLayout;

import com.ios.sc.common.utils.C_C_Util;

public class SL_FloatView extends RelativeLayout {

    public static boolean addViewState = false;

    public SL_FloatView(Context context) {
        super(context);

    }

    public SL_FloatView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    public SL_FloatView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private Handler mHandler = null;

    public void setWM(Handler handler) {
        mHandler = handler;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void setOnKeyListener(OnKeyListener l) {
        super.setOnKeyListener(l);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && (event.getKeyCode() == KeyEvent.KEYCODE_BACK || event.getKeyCode() == KeyEvent.KEYCODE_HOME)) {
            if (!C_C_Util.isFastMultipleClick()) {
                /*Intent hIntent = new Intent(Intent.ACTION_MAIN);

                hIntent.addCategory(Intent.CATEGORY_HOME);*/
                Intent hIntent = new Intent("ioslite.intent.action.IOSLITE");
                hIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(hIntent);
                if (null != mHandler) {
                    mHandler.sendEmptyMessageDelayed(92004, 500L);
                }
            }
        }
        return true;
        //return super.dispatchKeyEvent(event);
    }

}
