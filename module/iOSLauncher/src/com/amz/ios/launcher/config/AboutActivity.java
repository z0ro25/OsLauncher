package com.amz.ios.launcher.config;

import android.os.Bundle;
import android.view.View;

import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.preference.SettingBaseActivity;
import com.amz.ios.ioslite.common.util.BuildUtil;
import com.amz.ios.ioslite.common.util.SharedUtil;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.launcher.views.IOSPreference;
import com.amz.ios.launcher.views.TopTitlebar;


public class AboutActivity extends SettingBaseActivity {
    private TopTitlebar mTitlebar;

    private IOSPreference mWebsitePre;
    private IOSPreference mFacebookPre;
    private IOSPreference mCommunicatePre;
    private IOSPreference mVersionPre;
    private IOSPreference mEmailPre;
    private IOSPreference mServicePre;
    private IOSPreference mPrivacyPre;
    private IOSPreference mDevelopPre;

    private int mClickCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_activity);
        setupViews();
    }


    private void setupViews() {
        CustomTextView copyRightText = (CustomTextView) findViewById(R.id.copyright);
        copyRightText.setText(getString(R.string.copyright, BuildUtil.getIOSProductName(this)));

        mTitlebar = (TopTitlebar) findViewById(R.id.titlebar);
        mTitlebar.setBackListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mWebsitePre = (IOSPreference) findViewById(R.id.website);
        mWebsitePre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedUtil.accessWebsite(AboutActivity.this);
            }
        });
        mWebsitePre.setVisibility(View.GONE);
        mFacebookPre = (IOSPreference) findViewById(R.id.facebook);
        if (BuildUtil.isCNBuild()) {
            mFacebookPre.setVisibility(View.GONE);
        } else {
            mFacebookPre.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedUtil.accessDroiFacebookMainPage(AboutActivity.this);
                }
            });
        }
        mFacebookPre.setVisibility(View.GONE);

        mEmailPre = (IOSPreference) findViewById(R.id.email);
        mEmailPre.setSummary(Partner.getString(this, Partner.PRODUCT_EMAIL));
        mEmailPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedUtil.sendEmailToUs(AboutActivity.this);
            }
        });

        mEmailPre.setVisibility(View.GONE);
        mVersionPre = (IOSPreference) findViewById(R.id.version);
        mVersionPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickVersionPref();
            }
        });


        String versionName = BuildUtil.getIOSVersionName(this);
        int versionCode = BuildUtil.getIOSVersionCode(this);
        mVersionPre.setSummary(versionName + "_" + versionCode);

        mCommunicatePre = (IOSPreference) findViewById(R.id.communities);
        mCommunicatePre.setVisibility(View.GONE);

        mServicePre = (IOSPreference) findViewById(R.id.terms_of_service);
        mServicePre.setVisibility(View.GONE);

        mPrivacyPre = (IOSPreference) findViewById(R.id.privacy);
        mPrivacyPre.setVisibility(View.GONE);


        boolean showDevelopPre = Settings.isDeveloperEnabled(this);
        mVersionPre.showDivider(true);
        mDevelopPre = (IOSPreference) findViewById(R.id.develop);
        mDevelopPre.setVisibility(showDevelopPre ? View.VISIBLE : View.GONE);
        mDevelopPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(DeveloperActivity.class);
            }
        });
    }

    private void onClickVersionPref() {
        mClickCount++;
        if (mClickCount == 15) {
            mDevelopPre.setVisibility(View.VISIBLE);
            Settings.setDeveloperEnabled(this, true);
        }
    }
}
