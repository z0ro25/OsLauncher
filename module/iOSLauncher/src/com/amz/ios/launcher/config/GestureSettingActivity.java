package com.amz.ios.launcher.config;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;

import com.amz.ios.ioslite.common.analytics.AnalyticsDelegate;
import com.amz.ios.ioslite.common.analytics.UMEventConstants;
import com.amz.ios.ioslite.common.preference.SettingBaseActivity;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.views.IOSPreference;
import com.amz.ios.launcher.views.TopTitlebar;


public class GestureSettingActivity extends SettingBaseActivity {
    private TopTitlebar mTitlebar;

    private IOSPreference mSwipeDownPre;
    private IOSPreference mSwipeUpPre;
    private IOSPreference mSwipeObliquelyPre;

    private IOSPreference mSleepmodePre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gesture_setting_activity);
        setupViews();
    }

    private void setupViews() {
        mTitlebar = (TopTitlebar) findViewById(R.id.titlebar);
        mTitlebar.setDividerVisible(true);
        mTitlebar.setBackListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        mSwipeDownPre = (IOSPreference) findViewById(R.id.swipe_down);
        mSwipeDownPre.setSummary(GestureEventModel.getGestureActionDes(this, GestureEventModel.GESTURE_SWIPE_DOWN));
        mSwipeDownPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestGestureConfig(mSwipeDownPre.getTitle(), mSwipeDownPre.getSummary(), GestureEventModel.GESTURE_SWIPE_DOWN);
            }
        });

        mSwipeUpPre = (IOSPreference) findViewById(R.id.swipe_up);
        mSwipeUpPre.setSummary(GestureEventModel.getGestureActionDes(this, GestureEventModel.GESTURE_SWIPE_UP));
        mSwipeUpPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestGestureConfig(mSwipeUpPre.getTitle(), mSwipeUpPre.getSummary(), GestureEventModel.GESTURE_SWIPE_UP);
            }
        });

        mSwipeObliquelyPre = (IOSPreference) findViewById(R.id.swipe_obliquely);
        mSwipeObliquelyPre.setSummary(GestureEventModel.getGestureActionDes(this, GestureEventModel.GESTURE_SWIPE_OBLIQUELY));
        mSwipeObliquelyPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestGestureConfig(mSwipeObliquelyPre.getTitle(), mSwipeObliquelyPre.getSummary(), GestureEventModel.GESTURE_SWIPE_OBLIQUELY);
            }
        });

        mSleepmodePre = (IOSPreference) findViewById(R.id.settings_sleep);
//        mSleepmodePre.setVisibility(SearchWidgetUtil.hasSearchWidget(mContext) ? View.VISIBLE : View.GONE);//xiaopeng VISIBLE
        mSleepmodePre.setChecked(Settings.isSleepModeEnabled(GestureSettingActivity.this));
        mSleepmodePre.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AnalyticsDelegate.onLauncherSettingsEvent(GestureSettingActivity.this, UMEventConstants.LAUNCHERSETTINGS_SHOWSEARCHBOX_CLICK);
                Settings.setSleepModeEnabled(GestureSettingActivity.this, isChecked);
            }
        });

//        mDoubleTapPre = (IOSPreference) findViewById(R.id.double_tap);
//        mDoubleTapPre.setSummary(GestureEventModel.getGestureActionDes(this, GestureEventModel.GESTURE_DOUBLE_TAP));
//        mDoubleTapPre.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                requestGestureConfig(mDoubleTapPre.getTitle(), mDoubleTapPre.getSummary(), GestureEventModel.GESTURE_DOUBLE_TAP);
//            }
//        });
    }

    private void requestGestureConfig(String desc, String summary, int key) {
        Intent intent = new Intent(this, GesturePickerActivity.class);
        intent.putExtra(GestureEventModel.REQUEST_GESTURE_DES, desc);
        intent.putExtra(GestureEventModel.GESTURE_ACTION_DES, summary);
        startActivityForResult(intent, key);
    }

    @Override
    protected void onActivityResult(
            final int requestCode, final int resultCode, final Intent data) {
        if (resultCode == RESULT_OK) {
            Bundle bundle = data.getExtras();
            String actionDes = bundle.getString(GestureEventModel.GESTURE_ACTION_DES);
            String actionUri = bundle.getString(GestureEventModel.GESTURE_ACTION_URI);
            switch (requestCode) {
                case GestureEventModel.GESTURE_SWIPE_DOWN:
                    mSwipeDownPre.setSummary(actionDes);
                    break;
                case GestureEventModel.GESTURE_SWIPE_UP:
                    mSwipeUpPre.setSummary(actionDes);
                    break;
                case GestureEventModel.GESTURE_SWIPE_OBLIQUELY:
                    mSwipeObliquelyPre.setSummary(actionDes);
                    break;
                case GestureEventModel.GESTURE_DOUBLE_TAP:
                    break;
                default:
                    break;
            }
            GestureEventModel.updateGestureEvent(requestCode, actionUri, actionDes);
        }
    }
}