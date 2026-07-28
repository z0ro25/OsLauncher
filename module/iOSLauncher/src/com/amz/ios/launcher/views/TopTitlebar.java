package com.amz.ios.launcher.views;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.cardview.widget.CardView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;


import com.amz.ios.launcher.R;

public class TopTitlebar extends CardView {
    private View mTitlebar;
    private CustomTextView mTitleText;
    private ImageView mBackIcon;
    private View mBackBtn;
    private View mDivider;
    private View mRightBtnLayout;
    private ImageView mRefreshIcon;

    public TopTitlebar(Context context) {
        this(context, null);
    }


    public TopTitlebar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TopTitlebar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater infalter = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE));
        View contentView = infalter.inflate(R.layout.top_title_bar, this, true);
        TypedArray a = context.obtainStyledAttributes(attrs,
                com.amz.ios.ioslite.common.R.styleable.Titlebar, defStyleAttr, 0);

        mTitlebar = contentView.findViewById(R.id.titlebar_layout);
        mTitleText = (CustomTextView) contentView.findViewById(R.id.title);
        mBackBtn = findViewById(R.id.back);
        mBackIcon = (ImageView) contentView.findViewById(R.id.back_icon);
        mDivider = findViewById(R.id.title_divider);
        mRightBtnLayout = findViewById(R.id.right_btn_layout);
        mRefreshIcon = (ImageView) contentView.findViewById(R.id.refresh_icon);

        int backgroundColor = a.getColor(com.amz.ios.ioslite.common.R.styleable.Titlebar_background_color, getResources().getColor(com.amz.ios.ioslite.common.R.color.ios_accent_color));
        setBackgroundColor(backgroundColor);

        int backIconTintColor = a.getColor(com.amz.ios.ioslite.common.R.styleable.Titlebar_back_color, getResources().getColor(R.color.white));
        mBackIcon.setColorFilter(backIconTintColor);

        String title = a.getString(com.amz.ios.ioslite.common.R.styleable.Titlebar_titleText);
        if (!TextUtils.isEmpty(title)) {
            mTitleText.setText(title);
        }

        boolean hideDivider = a.getBoolean(com.amz.ios.ioslite.common.R.styleable.Titlebar_hideTitleDivider, true);
        if (hideDivider) {
            mDivider.setVisibility(GONE);
        }
        setRadius(0);
    }

    public void setTitle(String msg) {
        mTitleText.setText(msg);
    }

    public void setBackListener(OnClickListener listener) {
        mBackBtn.setOnClickListener(listener);
    }

    public void setRefreshListener(OnClickListener listener) {
        mRefreshIcon.setVisibility(VISIBLE);
        mRightBtnLayout.setOnClickListener(listener);
    }

    public void setDividerVisible(boolean visible) {
    }
}
