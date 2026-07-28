package com.zhuoyi.security.batterysave.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ios.sc.common.utils.C_GC_Util;
import com.zhuoyi.security.batterysave.R;

public class SL_TitleBar extends LinearLayout implements OnClickListener{

    String TAG = "C_TitleBar";

    private Drawable leftIcon = null;
    private Drawable rightIcon = null;
    private Drawable background = null;
    private String leftTitle = null;
    private String centerTitle = null;
    private String rightTitle = null;
    private int leftTitleColor = -1;
    private int centerTitleColor = -1;
    private int rightTitleColor = -1;
    private int leftTitleSize = -1;
    private int centerTitleSize = -1;
    private int rightTitleSize = -1;
    private boolean showLeftTitle = false;
    private boolean showCenterTitle = true;
    private boolean showRightTitle = false;
    private boolean showLeftIcon = false;
    private boolean showRightIcon = false;
    private boolean centerTitleCanClick = false;
    
    private CallBack mCallBack = null;
    public interface CallBack{
        void onLeftClick();
        void onCenterClick();
        void onRightClick();
    }
    
    public void setOnCallBack(CallBack cb){
        mCallBack = cb;
    }
    
    public SL_TitleBar(Context context) {
        this(context, null);
    }

    public SL_TitleBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

    }
    public SL_TitleBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOrientation(VERTICAL);

        LayoutInflater mInflater = LayoutInflater.from(context);
        View myView = mInflater.inflate(R.layout.sl_title_bar_layout, null);
        LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT);
        lp.height = getResources().getDimensionPixelSize(R.dimen.sl_title_bar_height);
        addView(myView, lp);

        TypedArray ta = context.obtainStyledAttributes(attrs,R.styleable.sl_title_bar_attrs);
        if(null != ta){
            //backgroud
            background = ta.getDrawable(R.styleable.sl_title_bar_attrs_slbackground);

            //left title config
            leftIcon = ta.getDrawable(R.styleable.sl_title_bar_attrs_slleftIcon);
            showLeftIcon = ta.getBoolean(R.styleable.sl_title_bar_attrs_slshowLeftIcon,false);
            leftTitle = ta.getString(R.styleable.sl_title_bar_attrs_slleftTitle);
            leftTitleColor = ta.getColor(R.styleable.sl_title_bar_attrs_slleftTitleColor, -1);
            leftTitleSize = ta.getDimensionPixelSize(R.styleable.sl_title_bar_attrs_slleftTitleSize, -1);
            showLeftTitle = ta.getBoolean(R.styleable.sl_title_bar_attrs_slshowLeftTitle,false);

            //right title config
            rightIcon = ta.getDrawable(R.styleable.sl_title_bar_attrs_slrightIcon);
            showRightIcon = ta.getBoolean(R.styleable.sl_title_bar_attrs_slshowRightIcon,false);
            rightTitle = ta.getString(R.styleable.sl_title_bar_attrs_slrightTitle);
            rightTitleColor = ta.getColor(R.styleable.sl_title_bar_attrs_slrightTitleColor, -1);
            rightTitleSize = ta.getDimensionPixelSize(R.styleable.sl_title_bar_attrs_slrightTitleSize, -1);
            showRightTitle = ta.getBoolean(R.styleable.sl_title_bar_attrs_slshowRightTitle,false);

            //center title config
            centerTitle = ta.getString(R.styleable.sl_title_bar_attrs_slcenterTitle);
            centerTitleColor = ta.getColor(R.styleable.sl_title_bar_attrs_slcenterTitleColor,-1);
            centerTitleSize = ta.getDimensionPixelSize(R.styleable.sl_title_bar_attrs_slcenterTitleSize, -1);
            showCenterTitle = ta.getBoolean(R.styleable.sl_title_bar_attrs_slshowCenterTitle,true);
            centerTitleCanClick = ta.getBoolean(R.styleable.sl_title_bar_attrs_slcenterTitleCanClick,false);
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

    void initBackground(){
        RelativeLayout rl = (RelativeLayout)findViewById(R.id.sl_title_bar_rl);
        if(null != background){
            rl.setBackgroundDrawable(background);
        }
    }

    void initLeftWidget(){
        ImageView iv = (ImageView)findViewById(R.id.sl_title_bar_left_icon);
        TextView tv = (TextView)findViewById(R.id.sl_title_bar_left_title);
        LinearLayout ll = (LinearLayout)findViewById(R.id.sl_title_bar_ll_left);
        if(null != iv){
            iv.setVisibility(showLeftIcon?View.VISIBLE:View.GONE);
            if(null != leftIcon){
                iv.setBackgroundDrawable(leftIcon);
            }
        }
        if(null != tv){
            tv.setVisibility(showLeftTitle?View.VISIBLE:View.GONE);
            if(null != leftTitle){
                tv.setText(leftTitle);
            }
            if(-1 != leftTitleSize){
                tv.setTextSize(leftTitleSize);
            }
            if(-1 != leftTitleColor){
                tv.setTextColor(leftTitleColor);
            }
        }
        if(!showLeftIcon && !showLeftTitle && null != ll){
            ll.setBackgroundDrawable(null);
        }else{
            ll.setOnClickListener(this);
        }
    }

    public void setLeftTitle(String title){
        TextView tv = (TextView)findViewById(R.id.sl_title_bar_left_title);
        if(null != tv){
            tv.setText(title);
        }
    }

    void initRightWidget(){
        ImageView iv = (ImageView)findViewById(R.id.sl_title_bar_right_icon);
        TextView tv = (TextView)findViewById(R.id.sl_title_bar_right_title);
        LinearLayout ll = (LinearLayout)findViewById(R.id.sl_title_bar_ll_right);
        if(null != iv){
            iv.setVisibility(showRightIcon?View.VISIBLE:View.GONE);
            if(null != rightIcon){
                iv.setBackgroundDrawable(rightIcon);
            }
        }
        if(null != tv){
            tv.setVisibility(showRightTitle?View.VISIBLE:View.GONE);
            if(null != rightTitle){
                tv.setText(rightTitle);
            }
            if(-1 != rightTitleSize){
                tv.setTextSize(rightTitleSize);
            }
            if(-1 != rightTitleColor){
                tv.setTextColor(rightTitleColor);
            }
        }
        if(!showRightIcon && !showRightTitle && null != ll){
            ll.setBackgroundDrawable(null);
        }else{
            if(null != ll && showCenterTitle){
                ll.setOnClickListener(this);
            }
        }
    }

    public void setRightTitle(String title){
        TextView tv = (TextView)findViewById(R.id.sl_title_bar_right_title);
        if(null != tv){
            tv.setText(title);
        }
    }

    void initCenterWidget(){
        TextView tv = (TextView)findViewById(R.id.sl_title_bar_center_title);
        LinearLayout ll = (LinearLayout)findViewById(R.id.sl_title_bar_ll_center);
        if(null != tv){
            tv.setVisibility(showCenterTitle?View.VISIBLE:View.GONE);
            if(null != centerTitle){
                tv.setText(centerTitle);
            }
            if(-1 != centerTitleSize){
                tv.setTextSize(centerTitleSize);
            }
            if(-1 != centerTitleColor){
                tv.setTextColor(centerTitleColor);
            }
        }
        if(null != ll && centerTitleCanClick){
            ll.setOnClickListener(this);
        }
    }

    public void setCenterTitle(String title){
        TextView tv = (TextView)findViewById(R.id.sl_title_bar_center_title);
        if(null != tv){
            tv.setText(title);
        }
    }

    @Override
    public void onClick(View v) {
        if(null != mCallBack){
            int id = v.getId();
            if(id == R.id.sl_title_bar_ll_right){
                mCallBack.onRightClick();
            }else if(id == R.id.sl_title_bar_ll_left){
                mCallBack.onLeftClick();
            }else if(id == R.id.sl_title_bar_ll_center){
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

    public void toDestroy(){        C_GC_Util.releaseImageViewMemory((ImageView)findViewById(R.id.sl_title_bar_left_icon));
        C_GC_Util.releaseImageViewMemory((ImageView)findViewById(R.id.sl_title_bar_right_icon));
        C_GC_Util.releaseViewMemory(findViewById(R.id.sl_title_bar_rl),true);
    }
}
