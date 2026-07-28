package com.amz.ios.themeclub.ui.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import android.view.View;
import android.widget.LinearLayout;

import com.amz.ios.ioslite.common.launcher.LauncherSettingCallback;
import com.amz.ios.launcher.config.Settings;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.model.FontModel;


/**
 * Created by TUDOL on 10/18/2019.
 */

public class FontDetailActivity extends FragmentActivity implements LauncherSettingCallback {

    private FontModel fontModel;
    private CustomTextView fontText;
    private CustomTextView fontTextName;
    private CustomTextView fontTextDescription;
    private LinearLayout backButton;
    private CustomTextView applyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fontdetail);

        Intent intent = getIntent();
        fontModel = (FontModel) intent.getSerializableExtra("fontdata");
        fontTextName = (CustomTextView) findViewById(R.id.themeclub_font_name);
        fontText = (CustomTextView) findViewById(R.id.theme_name);
        fontText.setTextSize(18);
        fontTextDescription= (CustomTextView) findViewById(R.id.themeclub_font_description);

        backButton = (LinearLayout) findViewById(R.id.back);
        applyButton = (CustomTextView) findViewById(R.id.setting_button);
        fontText.setText(fontModel.getFontName());
        fontTextName.setText(fontModel.getFontName());

        fontTextDescription.setText(com.amz.ios.launcher.R.string.font_description);
        fontTextDescription.setSpecialFont(fontModel.getType());


        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        fontText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        applyButtonState();

        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //todo
                if (fontModel.getChecked())
                    return;

                new AlertDialog.Builder(FontDetailActivity.this)
                        .setTitle(R.string.themeclub_warning)
                        .setMessage(R.string.themeclub_font_using)
                        .setPositiveButton(R.string.themeclub_font_setting_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int fontType = fontModel.getType();
                                Settings.setWorkspaceTextFont(FontDetailActivity.this, fontType);
                                fontModel.setChecked(true);
                                applyButtonState();

                            }
                        })
                        .setNegativeButton(R.string.themeclub_font_setting_cancel, null)
                        .show();

            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    private void applyButtonState(){
        if (fontModel.getChecked()) {
            applyButton.setText(R.string.themeclub_font_applid);
            applyButton.setAlpha(0.5f);
        }else {
            applyButton.setText(R.string.themeclub_font_apply);
        }

        applyButton.updateFont();
        fontText.updateFont();
        fontTextName.updateFont();
    }

    @Override
    public void onLauncherSettingChanged(String key) {

    }
}
