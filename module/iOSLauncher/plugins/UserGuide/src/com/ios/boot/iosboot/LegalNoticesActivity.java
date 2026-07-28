package com.ios.boot.iosboot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.ios.boot.iosboot.widget.ScrollWebView;
import com.amz.ios.ioslite.common.http.SslHandler;
import com.amz.ios.ioslite.common.util.PreferencesUtil;
import com.amz.ios.launcher.R;

import java.util.Locale;


public class LegalNoticesActivity extends Activity implements OnClickListener {
    private RelativeLayout mParent;
    private ScrollWebView mWebView;
    private RelativeLayout mLegalNetworkFail;
    private RelativeLayout mLegalNoticesCheck;
    private Button mLegalReload;
    private CheckBox mLegalCheckBox;
    private ProgressBar mPbLoading;

    private static final String TAG = "LegalNoticesActivity";
    private String IS_CHECKED = "isChecked";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.legal_notices_activity);
        mParent = (RelativeLayout) findViewById(R.id.parent);
        mWebView = (ScrollWebView) findViewById(R.id.legal_webView);
        mLegalNetworkFail = (RelativeLayout) findViewById(R.id.legal_network_fail);
        mLegalNoticesCheck = (RelativeLayout) findViewById(R.id.legal_notices_check);
        mLegalReload = (Button) findViewById(R.id.legal_reload);
        mLegalCheckBox = (CheckBox) findViewById(R.id.legal_notices_checkbox);
        mLegalCheckBox.setChecked(PreferencesUtil.getBoolean(this, IS_CHECKED, true));
        mPbLoading = (ProgressBar) findViewById(R.id.pb_loading);
        mLegalReload.setOnClickListener(this);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setBuiltInZoomControls(true);
        mWebView.setWebViewClient(mClient);
        if (isConnectInternet(this)) {
            mLegalNetworkFail.setVisibility(View.GONE);
            mWebView.setVisibility(View.VISIBLE);
            loadUrl();
        } else {
            mLegalNetworkFail.setVisibility(View.VISIBLE);
            mWebView.setVisibility(View.GONE);
        }

        mWebView.setDownloadListener(new DownloadListener() {
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype,
                                        long contentLength) {
                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
        mWebView.setOnCustomScroolChangeListener(new ScrollWebView.ScrollInterface() {

            @Override
            public void onSChanged(int l, int t, int oldl, int oldt) {
                float webViewContentHeight = mWebView.getContentHeight() * mWebView.getScale();
                float webViewCurrentHeight = (mWebView.getHeight() + mWebView.getScrollY());
                Log.i("legal", "webViewContentHeight - webViewCurrentHeight = " + (webViewContentHeight - webViewCurrentHeight));
                if ((webViewContentHeight - webViewCurrentHeight) <= 1) {
                    if (mLegalNoticesCheck.getVisibility() == View.GONE) {
                        mLegalNoticesCheck.setVisibility(View.VISIBLE);
                    }
                } else if (webViewCurrentHeight < webViewContentHeight * 3 / 4) {
                    if (mLegalNoticesCheck.getVisibility() == View.VISIBLE) {
                        mLegalNoticesCheck.setVisibility(View.GONE);
                    }
                }
            }
        });

        mLegalCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferencesUtil.putBoolean(LegalNoticesActivity.this, IS_CHECKED, isChecked);
            }
        });

    }

    public static boolean isConnectInternet(Context context) {
        if (context != null) {
            try {
                ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
                if (mNetworkInfo != null) {
                    return mNetworkInfo.isAvailable();
                }
            } catch (Exception e) {
                //
            }
        }
        return false;
    }

    private boolean isChinaLanguage() {
        Locale locale = getResources().getConfiguration().locale;
        String language = locale.getLanguage();
        return language.endsWith("zh");
    }

    private void loadUrl() {
        if (isChinaLanguage()) {
            mWebView.loadUrl("http://os.droi.com/notice/index_chinese.html");
        } else {
            mWebView.loadUrl("http://os.droi.com/notice/index_english.html");

        }
    }

    private WebViewClient mClient = new WebViewClient() {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            mPbLoading.setVisibility(View.VISIBLE);
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            SslHandler.onWebViewonReceivedSslError(LegalNoticesActivity.this, handler, error);
            mPbLoading.setVisibility(View.GONE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            mPbLoading.setVisibility(View.GONE);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mParent.removeAllViews();
        mWebView.destroy();
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.legal_reload) {
            if (isConnectInternet(this)) {
                mLegalNetworkFail.setVisibility(View.GONE);
                mWebView.setVisibility(View.VISIBLE);
                loadUrl();
            } else {
                mLegalNetworkFail.setVisibility(View.VISIBLE);
                mWebView.setVisibility(View.GONE);
            }

        }

    }
}
