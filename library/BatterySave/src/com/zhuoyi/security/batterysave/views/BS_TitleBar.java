package com.zhuoyi.security.batterysave.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.zhuoyi.security.batterysave.R;


/**
 * Created by zengrui on 2016/8/17.
 */

public class BS_TitleBar extends LinearLayout implements View.OnClickListener {


    String TAG = "C_TitleBar";

    private Drawable leftIcon = null;
    private Drawable rightIcon = null;
    private Drawable background = null;
    private String centerTitle = null;
    private int centerTitleSize = -1;
    private int centerTitleColor;
    private boolean showCenterTitle = true;
    private boolean showLeftIcon = false;
    private boolean showRightIcon = false;
    private boolean centerTitleCanClick = false;

    private CallBack mCallBack = null;
    public interface CallBack{
        void onLeftClick();
        void onCenterClick();
        void onRightClick();
    }

    /**
     * 相应点击事件
     * @param cb
     */
    public void setOnCallBack(CallBack cb){
        mCallBack = cb;
    }

    public BS_TitleBar(Context context) {
        this(context, null);
    }

    public BS_TitleBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BS_TitleBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOrientation(VERTICAL);
        LayoutInflater mInflater = LayoutInflater.from(context);
        View myView = mInflater.inflate(R.layout.bs_title_bar_layout, null);
        LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT);
        lp.height = getResources().getDimensionPixelSize(R.dimen.bs_title_bar_height);
        lp.weight = getResources().getDisplayMetrics().widthPixels;
        addView(myView, lp);

        TypedArray ta = context.obtainStyledAttributes(attrs,R.styleable.bs_title_bar_attrs);
        if(null != ta){
            //backgroud
            background = ta.getDrawable(R.styleable.bs_title_bar_attrs_bs_background);

            //left title config
            leftIcon = ta.getDrawable(R.styleable.bs_title_bar_attrs_leftIcon);
            showLeftIcon = ta.getBoolean(R.styleable.bs_title_bar_attrs_showLeftIcon,false);

            //right title config
            rightIcon = ta.getDrawable(R.styleable.bs_title_bar_attrs_rightIcon);
            showRightIcon = ta.getBoolean(R.styleable.bs_title_bar_attrs_showRightIcon,false);

            //center title config
            centerTitle = ta.getString(R.styleable.bs_title_bar_attrs_centerTitle);
            centerTitleColor = ta.getColor(R.styleable.bs_title_bar_attrs_centerTitleColor,-1);
            centerTitleSize = ta.getDimensionPixelSize(R.styleable.bs_title_bar_attrs_centerTitleSize, -1);
            //centerTitleSize = (int) ta.getDimension(R.styleable.bs_title_bar_attrs_centerTitleSize, -1);
            showCenterTitle = ta.getBoolean(R.styleable.bs_title_bar_attrs_showCenterTitle,true);
            centerTitleCanClick = ta.getBoolean(R.styleable.bs_title_bar_attrs_centerTitleCanClick,false);
        }
        ta.recycle();

        if (!isInEditMode()) {
            initBackground();
            initLeftWidget();
            initRightWidget();
            initCenterWidget();
        }else{
            return;
        }

    }

    private void initBackground(){
        RelativeLayout rl = (RelativeLayout)findViewById(R.id.bs_title_bar_rl);
        if(null != background){
            rl.setBackgroundDrawable(background);
        }
    }

    private void initLeftWidget(){
        ImageView iv = (ImageView)findViewById(R.id.bs_title_bar_left_icon);
        LinearLayout ll = (LinearLayout)findViewById(R.id.bs_title_bar_ll_left);
        if(null != iv){
            iv.setVisibility(showLeftIcon?View.VISIBLE:View.GONE);
            if(null != leftIcon){
                iv.setBackgroundDrawable(leftIcon);
            }
        }
        if(!showLeftIcon && null != ll){
            ll.setBackgroundDrawable(null);
        }else{
            ll.setOnClickListener(this);
        }
    }

    private void initRightWidget(){
        ImageView iv = (ImageView)findViewById(R.id.bs_title_bar_right_icon);
        LinearLayout ll = (LinearLayout)findViewById(R.id.bs_title_bar_ll_right);
        if(null != iv){
            iv.setVisibility(showRightIcon?View.VISIBLE:View.GONE);
            if(null != rightIcon){
                iv.setBackgroundDrawable(rightIcon);
            }
        }

        if(!showRightIcon && null != ll){
            ll.setBackgroundDrawable(null);
        }else{
            if(null != ll && showCenterTitle){
                ll.setOnClickListener(this);
            }
        }
    }

    private void initCenterWidget(){
        TextView tv = (TextView)findViewById(R.id.bs_title_bar_center_title);
        LinearLayout ll = (LinearLayout)findViewById(R.id.bs_title_bar_ll_center);
        if(null != tv){
            tv.setVisibility(showCenterTitle?View.VISIBLE:View.GONE);
            if(null != centerTitle){
                tv.setText(centerTitle);
            }
            if(-1 != centerTitleSize){
                tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, centerTitleSize);
                //Log.e("zengrui",""+tv.getTextSize());
                //tv.setTextSize(centerTitleSize);
            }
            if(-1 != centerTitleColor){
                tv.setTextColor(centerTitleColor);
            }
        }
        if(null != ll && centerTitleCanClick){
            ll.setOnClickListener(this);
        }
    }

    /**
     * 公开接口
     * @param title: center tile
     */
    public void setCenterTitle(String title){
        TextView tv = (TextView)findViewById(R.id.bs_title_bar_center_title);
        if(null != tv){
            tv.setText(title);
        }
    }


    @Override
    public void onClick(View v) {
        if(null != mCallBack){
            int id = v.getId();
            if(id == R.id.bs_title_bar_ll_right){
                mCallBack.onRightClick();
            }else if(id == R.id.bs_title_bar_ll_left){
                mCallBack.onLeftClick();
            }else if(id == R.id.bs_title_bar_ll_center){
                mCallBack.onCenterClick();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            toDestroy();
        }
        return super.onKeyDown(keyCode, event);
    }

    public void toDestroy(){
        releaseImageViewMemory((ImageView)findViewById(R.id.bs_title_bar_left_icon),false);
        releaseImageViewMemory((ImageView)findViewById(R.id.bs_title_bar_right_icon), false);
        releaseViewMemory(findViewById(R.id.bs_title_bar_rl),true);
    }

    private void releaseImageViewMemory(ImageView iv,boolean isNowGC) {
        if (null != iv) {
            iv.clearAnimation();
            iv.setBackgroundDrawable(null);
            Drawable drawable = iv.getDrawable();
            if(null != drawable ){
                if(drawable instanceof BitmapDrawable){
                    Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                    if (bitmap != null && !bitmap.isRecycled()) {
                        //bitmap.recycle();
                        bitmap = null;
                    }
                }
            }
            iv.setImageResource(android.R.color.transparent);
            if(isNowGC){
                System.gc();
            }
        }
    }

    private void releaseViewMemory(View v,boolean isNowGC) {
        if(null != v){
            v.clearAnimation();
            v.setBackgroundDrawable(null);
            v.setBackgroundResource(0);
            v.setBackgroundResource(android.R.color.transparent);
        }
    }
}
