package com.amz.ios.launcher.config;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.amz.ios.ioslite.common.preference.SettingBaseActivity;
import com.amz.ios.ioslite.common.widget.SmoothCheckBox;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.launcher.views.TopTitlebar;

public class DrawerStyleActivity extends SettingBaseActivity implements View.OnClickListener {
    private TopTitlebar mTitlebar;

    public static int REQUEST_CODE = 1000;

    private View mAlphaVerticalLayout;
    private View mFullVerticalLayout;
    private SmoothCheckBox mAlphaVerticalCheckbox;
    private SmoothCheckBox mFullVerticalCheckbox;
    private ImageView mAlphaVerticalPreview;
    private ImageView mFullVerticalPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer_style_setting_activity);
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

        final Resources res = getResources();

        View standardItem = findViewById(R.id.alpha_vertical);
        mAlphaVerticalLayout = standardItem.findViewById(R.id.title_layout);
        mAlphaVerticalCheckbox = (SmoothCheckBox) standardItem.findViewById(R.id.checkbox);
        mAlphaVerticalPreview = (ImageView) standardItem.findViewById(R.id.preview);
        ((CustomTextView) standardItem.findViewById(R.id.title)).setText(res.getString(R.string.drawer_alpha_vertical_title));
        ((CustomTextView) standardItem.findViewById(R.id.desc)).setText(res.getString(R.string.drawer_alpha_vertical_des));

        View drawerItem = findViewById(R.id.full_vertical);
        mFullVerticalLayout = drawerItem.findViewById(R.id.title_layout);
        mFullVerticalCheckbox = (SmoothCheckBox) drawerItem.findViewById(R.id.checkbox);
        mFullVerticalPreview = (ImageView) drawerItem.findViewById(R.id.preview);
        ((CustomTextView) drawerItem.findViewById(R.id.title)).setText(res.getString(R.string.drawer_full_vertical_title));
        ((CustomTextView) drawerItem.findViewById(R.id.desc)).setText(res.getString(R.string.drawer_full_vertical_des));

        mAlphaVerticalPreview.setImageResource(R.drawable.drawer_alpha_preview);
        mFullVerticalPreview.setImageResource(R.drawable.drawer_full_preview);
        mAlphaVerticalLayout.setOnClickListener(this);
        mFullVerticalLayout.setOnClickListener(this);
        mAlphaVerticalCheckbox.setOnClickListener(this);
        mFullVerticalCheckbox.setOnClickListener(this);

        updateCheckState(false);
    }

    @Override
    public void onClick(View v) {
        mAlphaVerticalCheckbox.toggle(true);
        mFullVerticalCheckbox.toggle(true);
    }


    private void updateCheckState(boolean animate) {
        final int displayMode = Settings.getAllAppsDisplayMode(mContext);
    }
}
