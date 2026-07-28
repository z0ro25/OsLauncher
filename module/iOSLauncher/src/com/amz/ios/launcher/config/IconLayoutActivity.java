package com.amz.ios.launcher.config;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;

import com.amz.ios.ioslite.common.anim.PropertyHolderUtis;
import com.amz.ios.ioslite.common.preference.SettingBaseActivity;
import com.amz.ios.ioslite.common.widget.ColorPickerView;
import com.amz.ios.launcher.AppInfo;
import com.amz.ios.launcher.DeviceProfile;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.LauncherModel;
import com.amz.ios.launcher.PreviewIcon;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.launcher.views.IOSPreference;
import com.amz.ios.launcher.views.ScaleSeekBar;
import com.amz.ios.launcher.views.TopTitlebar;

import java.util.List;


public class IconLayoutActivity extends SettingBaseActivity implements ColorPickerView.PickerCallback {
    private static final int ANIMATION_DURATION = 200;
    private static final int NUM_LEVEL = 7;
    private static final float[] ICON_SIZE_RANGE = new float[]{
            0.85f,
            1.15f
    };

    private static final float[] ICON_TEXT_SIZE_SCALE_RANGE = new float[]{
            0.7f,
            1.3f
    };

    private TopTitlebar mTitlebar;

    private ViewGroup mMenuLayout;
    private ViewGroup mColorLayout;

    private ViewGroup mIconPreContainer;
    private IOSPreference mColorPref;
    private IOSPreference mCommonColorPref;
    private ColorPickerView mPickerView;

    private CustomTextView mCancelBtn;
    private CustomTextView mOkBtn;

    private ScaleSeekBar mIconSizeSeekbar;
    private ScaleSeekBar mLabelSizeSeekbar;

    private DeviceProfile mGrid;
    private int mIconTextColor;
    private int mIconSize;
    private int mLabelSize;
    private float mIconSizeScale;
    private float mLabelSizeScale;

    private float mOriginIconSizeScale;
    private float mOriginLabelSizeScale;

    private boolean mColorPickerMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.icon_layout_settings_activity);

        LauncherAppState app = LauncherAppState.getInstance();
//        mGrid = getResources().getConfiguration().orientation
//                == Configuration.ORIENTATION_LANDSCAPE ?
//                app.getInvariantDeviceProfile().landscapeProfile
//                : app.getInvariantDeviceProfile().portraitProfile;
        mGrid = app.getInvariantDeviceProfile().portraitProfile;
        mIconTextColor = mGrid.workspaceIconTextColor;
        mOriginIconSizeScale = mIconSizeScale = Settings.getWorkspaceIconSizeScale(this);
        mOriginLabelSizeScale = mLabelSizeScale = Settings.getWorkspaceTextSizeScale(this);
        mIconSize = (int) (mGrid.originalIconSizePx * mIconSizeScale);
        mLabelSize = (int) (mGrid.originalIconTextSizePx * mLabelSizeScale);
        setupViews();
    }

    private void setupViews() {
        mTitlebar = (TopTitlebar) findViewById(R.id.titlebar);
        mTitlebar.setDividerVisible(true);
        mTitlebar.setBackListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mIconPreContainer = (ViewGroup) findViewById(R.id.iconContainer);
        mColorLayout = (ViewGroup) findViewById(R.id.colorLayout);
        mMenuLayout = (ViewGroup) findViewById(R.id.menuLayout);

        mIconSizeSeekbar = (ScaleSeekBar) findViewById(R.id.icon_size_setting).findViewById(R.id.seek_bar);
        mLabelSizeSeekbar = (ScaleSeekBar) findViewById(R.id.label_size_setting).findViewById(R.id.seek_bar);
        mIconSizeSeekbar.setSlideResponseOnTouch(mIconResponse);
        mLabelSizeSeekbar.setSlideResponseOnTouch(mLabelResponse);
        mIconSizeSeekbar.setProgress(caculateLevel(mIconSizeScale, ICON_SIZE_RANGE, NUM_LEVEL));
        mLabelSizeSeekbar.setProgress(caculateLevel(mLabelSizeScale, ICON_TEXT_SIZE_SCALE_RANGE, NUM_LEVEL));

        mColorPref = (IOSPreference) findViewById(R.id.color_pref);
        mColorPref.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorLayout(true);
            }
        });

        mCommonColorPref = (IOSPreference) findViewById(R.id.common_color);
        mCommonColorPref.setupSpinnerMenu(R.array.common_color_entries, -1, new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String color = getResources().getStringArray(R.array.common_color_values)[position];
                mPickerView.setColor(Color.parseColor(color));
                mCommonColorPref.dismissSpinnerMenu();
            }
        });
        
        mCommonColorPref.setVisibility(View.GONE);//bug 0057049

        mPickerView = (ColorPickerView) findViewById(R.id.colorPicker);
        mPickerView.setCallback(this);

        mCancelBtn = (CustomTextView) findViewById(R.id.btn_cancel);
        mOkBtn = (CustomTextView) findViewById(R.id.btn_ok);
        mCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                undoColorChange();
            }
        });
        mOkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyColorAndExit();
            }
        });

        populatePreviews();
        showMenuLayout(false);
    }


    private ScaleSeekBar.SlideResponseOnTouch mIconResponse = new ScaleSeekBar.SlideResponseOnTouch() {
        @Override
        public void onResponse(int index) {
            mIconSizeScale = caculateValue(index, ICON_SIZE_RANGE, NUM_LEVEL);
            mIconSize = (int) (mGrid.originalIconSizePx * mIconSizeScale);
            changeIconSize(mIconSize);
        }
    };


    private ScaleSeekBar.SlideResponseOnTouch mLabelResponse = new ScaleSeekBar.SlideResponseOnTouch() {
        @Override
        public void onResponse(int index) {
            mLabelSizeScale = caculateValue(index, ICON_TEXT_SIZE_SCALE_RANGE, NUM_LEVEL);
            mLabelSize = (int) (mGrid.originalIconTextSizePx * mLabelSizeScale);
            changeLabelSize(mLabelSize);
        }
    };
    ;

    @Override
    public void onColorPicked(int color) {
        changePreviewTextColor(color);
    }

    private void changePreviewTextColor(int color) {
        PreviewIcon icon;
        int count = mIconPreContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            icon = (PreviewIcon) mIconPreContainer.getChildAt(i);
            icon.setTextColor(color);
        }
    }

    private void changeIconSize(int size) {
        PreviewIcon icon;
        int count = mIconPreContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            icon = (PreviewIcon) mIconPreContainer.getChildAt(i);
            icon.setIconSize(size);
        }
    }

    private void changeLabelSize(int size) {
        PreviewIcon icon;
        int count = mIconPreContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            icon = (PreviewIcon) mIconPreContainer.getChildAt(i);
            icon.setTextSizePx(size);
        }
    }

    private void showColorLayout(boolean animate) {
        int width = mMenuLayout.getMeasuredWidth();
        if (animate) {
            Animator animMenu = ObjectAnimator.ofPropertyValuesHolder(mMenuLayout,
                    PropertyHolderUtis.translateX(0, -width));
            animMenu.setInterpolator(new DecelerateInterpolator());
            animMenu.setDuration(ANIMATION_DURATION);

            Animator animColor = ObjectAnimator.ofPropertyValuesHolder(mColorLayout,
                    PropertyHolderUtis.translateX(width, 0));
            animColor.setInterpolator(new DecelerateInterpolator());
            animColor.setDuration(ANIMATION_DURATION);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.play(animColor);
            animatorSet.play(animMenu);
            animatorSet.start();
        } else {
            mMenuLayout.setTranslationX(-width);
            mColorLayout.setTranslationX(0);
        }
        mColorPickerMode = true;
        mPickerView.setColor(mIconTextColor);
    }

    private void showMenuLayout(boolean animate) {
        int width = mMenuLayout.getMeasuredWidth();
        if (animate) {
            Animator animMenu = ObjectAnimator.ofPropertyValuesHolder(mMenuLayout,
                    PropertyHolderUtis.translateX(-width, 0));
            animMenu.setInterpolator(new DecelerateInterpolator());
            animMenu.setDuration(ANIMATION_DURATION);

            Animator animColor = ObjectAnimator.ofPropertyValuesHolder(mColorLayout,
                    PropertyHolderUtis.translateX(0, width));
            animColor.setInterpolator(new DecelerateInterpolator());
            animColor.setDuration(ANIMATION_DURATION);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.play(animColor);
            animatorSet.play(animMenu);
            animatorSet.start();
        } else {
            mMenuLayout.setTranslationX(0);
            mColorLayout.setTranslationX(width);
        }
        mColorPickerMode = false;
        mPickerView.setColor(mIconTextColor);
    }

    private void applyColorAndExit() {
        mIconTextColor = mPickerView.getColor();
        Settings.setWorkspaceLabelColor(this, mIconTextColor);
        showMenuLayout(true);
    }

    private void undoColorChange() {
        mPickerView.setColor(mIconTextColor);
    }

    private void populatePreviews() {
        LayoutInflater inflater = LayoutInflater.from(this);
        LauncherAppState app = LauncherAppState.getInstance();
        int count = app.getInvariantDeviceProfile().numColumns;
        LauncherModel model = app.getModel();
        List<AppInfo> appInfos = model.getAllAppInfo();
        if (appInfos.size() == 0) {
            return;
        }

        AppInfo appInfo;
        for (int i = 0; i < count; i++) {
            appInfo = appInfos.get(i);
            PreviewIcon icon = (PreviewIcon) inflater.inflate(
                    R.layout.preview_app_icon, mIconPreContainer, false);
            icon.applyFromApplicationInfo(appInfo);
            mIconPreContainer.addView(icon);
        }
    }


    @Override
    public void onBackPressed() {
        if (mColorPickerMode) {
            showMenuLayout(true);
            return;
        }
        finish();
    }

    protected void onPause() {
        super.onPause();

        if (mOriginIconSizeScale != mIconSizeScale) {
            Settings.setWorkspaceIconSizeScale(this, mIconSizeScale);
        }

        if (mOriginLabelSizeScale != mLabelSizeScale) {
            Settings.setWorkspaceTextSizeScale(this, mLabelSizeScale);
        }
    }


    private static int caculateLevel(float value, float[] range, int levelCount) {
        float degree = (1.0f * Math.round(100 * (range[1] - range[0]) / (levelCount - 1)) / 100);
        int result = Math.round((value - range[0]) / degree);
        return result;
    }

    private static float caculateValue(int level, float[] range, int levelCount) {
        float degree = (1.0f * Math.round(100 * (range[1] - range[0]) / (levelCount - 1)) / 100);
        float result = degree * level + range[0];
        return result;
    }
}
