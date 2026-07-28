package com.zhuoyi.security.batterysave.views;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.zhuoyi.security.batterysave.R;

public class SL_LoadingDialog extends Dialog {

    private TextView c_loading_tv_title = null;
    View view = null;

    public SL_LoadingDialog(Context context) {
        super(context, R.style.sl_loading_dialog_style);
        view = LayoutInflater.from(getContext()).inflate(R.layout.sl_loading, null);
        c_loading_tv_title = (TextView) view.findViewById(R.id.sl_loading_tv_title);
    }

    public void setShowLoadingText() {
        if (null != c_loading_tv_title    && c_loading_tv_title.getVisibility() == View.GONE) {
            c_loading_tv_title.setVisibility(View.VISIBLE);
        }
    }

    public void setLoadingText(String text) {
        if (null != c_loading_tv_title) {
            setShowLoadingText();
            c_loading_tv_title.setText(text);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        Animation anim = AnimationUtils.loadAnimation(getContext(),R.anim.sl_round_loading);
        view.findViewById(R.id.sl_loading_iv_pic).startAnimation(anim);
        setContentView(view);
    }

    @Override
    public void show() {
        super.show();
        Window window = getWindow();
        LayoutParams lp = window.getAttributes();
        lp.dimAmount = 0.03f;
        getWindow().setAttributes(lp);
    }

    @Override
    public boolean isShowing() {
        return super.isShowing();
    }

    @Override
    public void dismiss() {
        super.dismiss();
    }
}
