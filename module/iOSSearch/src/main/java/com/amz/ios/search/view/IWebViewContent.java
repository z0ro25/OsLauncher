package com.amz.ios.search.view;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.amz.ios.search.utils.IntentUtils;
import com.amz.ios.search.widget.MerlinFlashProgressBar;
import com.amz.ioslauncher.iossearch.R;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-22 下午2:45
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public interface IWebViewContent {

    void initWebView();

    WebView getWebView();

    void loadUrl(String url);

    void showWebView();

    void hideWebView();

    boolean goBack();

    class WebViewContentProxy implements IWebViewContent {

        private static final String TAG = WebViewContentProxy.class.getSimpleName();

        private ViewStub mContainer;

        private WebView mWebView;

        private MerlinFlashProgressBar mProgressBar;

        /**
         * whether current quit webview mode
         */
        private boolean mQuit;

        public WebViewContentProxy(ViewStub containerView, ProgressBar progress) {
            mContainer = containerView;
            mProgressBar = (MerlinFlashProgressBar) progress;
        }

        private boolean isInflatered = false;

        private void checkInflater() {
            if (isInflatered) return;
            isInflatered = true;
            final View inflater = mContainer.inflate();
            mWebView = (WebView) inflater.findViewById(R.id.search_webview);
            initWebView();
        }


        @Override
        public void initWebView() {
            WebSettings webSettings = mWebView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    // TODO Auto-generated method stub
                    final Context context = view.getContext();
                    Log.d(TAG, ">>>>>>WebViewContentProxy#shouldOverrideUrlLoading : " + url);
                    if (handleUrl(url)) {
                        //搜索栏的关键字搜索打开自身的webview
                        view.loadUrl(url);
                        return true;
                    } else {
                        //点击页面后跳转到浏览器
                        IntentUtils.openUrl(context, url);
                        return true;
                    }
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    super.onReceivedError(view, request, error);
                }
            });
            mWebView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    super.onProgressChanged(view, newProgress);
                    if (newProgress == 100) {
                        mProgressBar.setProgress(newProgress);
                        mWebView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mProgressBar.setVisibility(View.GONE);
                            }
                        }, 10);
                        Log.d(TAG, ">>>>>>WebViewContentProxy#onProgressChanged : " + view.getUrl());
                    } else {
                        if (mQuit) return;
                        if (mProgressBar.getVisibility() == View.GONE) {
                            mProgressBar.setVisibility(View.VISIBLE);
                        }
                        mProgressBar.setProgress(newProgress);
                    }
                }
            });
        }

        /**
         * judge some flag to jump a outer web view;
         *
         * @return
         */
        private boolean handleUrl(String url) {
            try {
                final Uri uri = Uri.parse(url);
                if (uri.getPathSegments().contains("openwebview")) {
                    return false;
                }
            } catch (Exception e) {
                Log.e(TAG, ">>>>>>WebViewContentProxy#handleUrl : ");
            }
            return true;
        }

        @Override
        public WebView getWebView() {
            return mWebView;
        }

        @Override
        public void loadUrl(String url) {
            if (mWebView != null) {
                mWebView.loadUrl(url);
            }
        }

        @Override
        public void showWebView() {
            checkInflater();
            if (mWebView != null) mWebView.onResume();
            mWebView.requestFocus();
            mQuit = false;
            mContainer.setVisibility(View.VISIBLE);
        }

        @Override
        public void hideWebView() {
            if (mWebView != null) mWebView.onPause();
            mProgressBar.setVisibility(View.GONE);
            mQuit = true;
            mContainer.setVisibility(View.GONE);
        }

        @Override
        public boolean goBack() {
            return false;
//            if(mWebView != null && mWebView.canGoBack()){
//                mWebView.goBack();
//                return true;
//            }
//            return false;
        }
    }
}
