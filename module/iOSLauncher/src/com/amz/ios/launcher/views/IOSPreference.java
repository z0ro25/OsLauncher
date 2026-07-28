package com.amz.ios.launcher.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.amz.ios.ioslite.common.widget.Switch;
import com.amz.ios.launcher.R;


public class IOSPreference extends LinearLayout {

    private final ImageView mRefreshIcon;
    private View mRootView;
    private ImageView mIcon;
    private EnhancedTextView mTitleText;
    private CustomTextView mSummary;
    private ImageView mGuideIcon;
    private Switch mSwitch;
    private ImageView mRightArrow;
    private View mSpinner;
    private View mSpinnerAnchor;
    private View mDivider;
    private SpinnerPopupWindow mSpinnerMenu;
    private int mSpinnerEntriesId;
    private int mPosition;
    private AdapterView.OnItemClickListener mListener;

    private static final int IMG_RED_DOT = 0;
    private static final int IMG_SWITCH = 1;
    private static final int IMG_RIGHT_ARROW = 2;
    private static final int IMG_SPINNER = 3;

    public IOSPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray array = context.obtainStyledAttributes(attrs, com.amz.ios.ioslite.common.R.styleable.IOSPreference);
        inflate(context, R.layout.ios_preference, this);

        mRootView = findViewById(R.id.root);
        mIcon = (ImageView) findViewById(R.id.icon);
        mTitleText = (EnhancedTextView) findViewById(R.id.title);
        mSummary = (CustomTextView) findViewById(R.id.summary);
        mGuideIcon = (ImageView) findViewById(R.id.red_new_guide);
        mRefreshIcon = (ImageView) findViewById(R.id.red_new_refresh);
        mSwitch = (Switch) findViewById(R.id.switch1);
        mRightArrow = (ImageView) findViewById(R.id.right_arrow);
        mSpinner = findViewById(R.id.spinner);
        mSpinnerAnchor = findViewById(R.id.spinner_anchor);
        mDivider = findViewById(R.id.divider);


        int backgroundResId = array.getResourceId(com.amz.ios.ioslite.common.R.styleable.IOSPreference_preferenceBackground, 0);
        if (backgroundResId != 0) {
            mRootView.setBackgroundResource(backgroundResId);
        }

        int iconResId = array.getResourceId(com.amz.ios.ioslite.common.R.styleable.IOSPreference_preferenceIcon, 0);
        if (iconResId != 0) {
            mIcon.setVisibility(VISIBLE);
            mIcon.setImageResource(iconResId);
        }

        String title = array.getString(com.amz.ios.ioslite.common.R.styleable.IOSPreference_preferenceTitle);
        if (!TextUtils.isEmpty(title)) {
            mTitleText.setText(title);
        }

        int rightIconResId = array.getResourceId(com.amz.ios.ioslite.common.R.styleable.IOSPreference_preferenceTitleRightIcon, 0);
        if (rightIconResId != 0) {
            mTitleText.setRightDrawable(rightIconResId);
        }

        String summary = array.getString(com.amz.ios.ioslite.common.R.styleable.IOSPreference_preferenceSummary);
        if (!TextUtils.isEmpty(summary)) {
            mSummary.setText(summary);
            mSummary.setVisibility(VISIBLE);
        }

        int titleColor = array.getColor(com.amz.ios.ioslite.common.R.styleable.IOSPreference_PreferencetitleColor, context.getResources().getColor(com.amz.ios.ioslite.common.R.color.pref_title_selector));
        mTitleText.setTextColor(titleColor);

        int summaryColor = array.getColor(com.amz.ios.ioslite.common.R.styleable.IOSPreference_PreferenceSummaryColor, context.getResources().getColor(com.amz.ios.ioslite.common.R.color.pref_summary));
        mSummary.setTextColor(summaryColor);

        int rightImg = array.getInteger(com.amz.ios.ioslite.common.R.styleable.IOSPreference_rightImage, -1);
        if (rightImg == IMG_RED_DOT) {
            mGuideIcon.setVisibility(VISIBLE);
        } else if (rightImg == IMG_SWITCH) {
            mSwitch.setVisibility(VISIBLE);
        } else if (rightImg == IMG_RIGHT_ARROW) {
            mRightArrow.setVisibility(VISIBLE);
        } else if (rightImg == IMG_SPINNER) {
            mSpinner.setVisibility(VISIBLE);
        }

        boolean hideDivider = array.getBoolean(com.amz.ios.ioslite.common.R.styleable.IOSPreference_hideDivider, false);
        if (hideDivider) {
            mDivider.setVisibility(GONE);
        }
    }


    public final void setupSpinnerMenu(int entriesResId, int position, AdapterView.OnItemClickListener listener) {
//        mSpinnerMenu = new SpinnerPopupWindow(getContext(), entriesResId, position, listener);
        mPosition = position;
        mListener = listener;
        mSpinnerEntriesId = entriesResId;
        mSpinner.setVisibility(VISIBLE);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showSpinnerMenu();
            }
        });
    }

    public final void setSpinnerSelectPosition(int position) {
        if (mSpinnerMenu != null) {
            mSpinnerMenu.setSelectedPosition(position);
            mSpinnerMenu.dismiss();
        }
    }

    public void dismissSpinnerMenu() {
        if (mSpinnerMenu != null) {
            mSpinnerMenu.dismiss();
        }
    }


    private final void showSpinnerMenu() {
        mSpinnerMenu = new SpinnerPopupWindow(getContext(), mSpinnerEntriesId, mPosition, mListener);
        if (mSpinner != null && mSpinnerMenu != null) {
            if (mSpinnerMenu.mPopupWindow == null) {
                mSpinnerMenu.mPopupWindow = new PopupWindow(mSpinnerMenu.mSpinnerView, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, true);
                mSpinnerMenu.mPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
                mSpinnerMenu.mPopupWindow.setTouchable(true);
                mSpinnerMenu.mPopupWindow.setOutsideTouchable(true);
                mSpinnerMenu.mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
            }
            mSpinnerMenu.show(mSpinnerAnchor/*anchor*/);
        }

    }

    public void showDivider(boolean bool) {
        if (bool) {
            mDivider.setVisibility(VISIBLE);
        } else {
            mDivider.setVisibility(GONE);
        }
    }

    public void setTitle(String title) {
        mTitleText.setText(title);
    }

    public String getTitle() {
        return mTitleText.getText().toString();
    }

    public String getSummary() {
        return mSummary.getText().toString();
    }

    public void setTitleRightDrawable(int resId) {
        mTitleText.setRightDrawable(resId);
    }

    public void setSummary(String summary) {
        if (!TextUtils.isEmpty(summary)) {
            mSummary.setText(summary);
            mSummary.setVisibility(VISIBLE);
        } else {
            mSummary.setVisibility(GONE);
        }
    }

    public void setGuideIconVisible(boolean visible) {
        mGuideIcon.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setRefreshIconVisible(boolean visible) {
        mRefreshIcon.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener listener) {
        mSwitch.setOnCheckedChangeListener(listener);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggole();
            }
        });
    }

    public boolean isChecked() {
        return mSwitch.isChecked();
    }

    public void setChecked(boolean checked) {
        mSwitch.setChecked(checked);
    }

    public void toggole() {
        mSwitch.toggle();
    }

    public void setRefresh(final OnClickListener listener) {
        mRefreshIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                RotateAnimation rotationAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                rotationAnimation.setRepeatMode(Animation.RESTART);
                rotationAnimation.setDuration(500);
                rotationAnimation.setRepeatCount(-1);
                rotationAnimation.setInterpolator(new LinearInterpolator());
                v.startAnimation(rotationAnimation);
                listener.onClick(v);
            }
        });
    }

    public void startRefresh() {
        mRefreshIcon.performClick();
    }

    public ImageView getRefreshIcon() {
        return mRefreshIcon;
    }
}
