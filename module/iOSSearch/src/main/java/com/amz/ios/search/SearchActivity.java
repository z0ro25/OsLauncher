package com.amz.ios.search;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.transition.Explode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewStub;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.amz.ios.ioslite.common.analytics.AnalyticsDelegate;
import com.amz.ios.ioslite.common.analytics.UMEventConstants;
import com.amz.ios.ioslite.common.util.ToastUtil;
import com.amz.ios.http.Internal.CancelableCallBack;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.LauncherModel;
import com.amz.ios.search.config.MSCConfiguration;
import com.amz.ios.search.config.SearchConfiguration;
import com.amz.ios.search.entities.BaseCardItemInfo;
import com.amz.ios.search.entities.IGroupItemInfoImplAdapter;
import com.amz.ios.search.provider.SearchDataProvider;
import com.amz.ios.search.view.IHeaderSearchView;
import com.amz.ios.search.view.ISearchContent;
import com.amz.ios.search.view.IWebViewContent;
import com.amz.ios.search.widget.ScrollViewExtend;
import com.amz.ioslauncher.iossearch.R;

import java.util.List;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-09 下午7:55
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class SearchActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = SearchActivity.class.getSimpleName();

    private static final boolean DEBUG = MSCConfiguration.DEBUG;

    public static final int PERMISSION_REQUEST_CODE = 200;

    //data handler
    private SearchDataProvider mSearchDataPresenter;

    //view proxy
    //content webview
    private IWebViewContent mContentWebViewProxy;

    //content view
    private DataFlowAdapter mDataFlowAdapter;
    private ISearchContent mSearchContentProxy;

    //head
    private IHeaderSearchView mSearchInputViewProxy;
    //bottom
//    private IBottomView mBottomViewProxy;


    //configuration
    private SearchConfiguration mConfiguration;
    private int mTouchSlop;
    /**
     * RESET IDLE MODE {@link #resetToPreloadState()}
     */
    public static final int SHOW_IDE_MODE = 0x1;
    /**
     * SHOW WEBVIEW MODE {@link #doSearch(int)}
     */
    public static final int SHOW_WEBVIEW_MODE = 0x2;
    /**
     * Main2Activity
     * START AUTO SEARCH AFTER DO {@link #doAutoSearch(String)}
     */
    public static final int SHOW_AUTO_SEARCH_MODE = 0X3;

    private int mShowMode = SHOW_IDE_MODE;

    public static void startSearchActivty(Context context, String word) {
        startSearchActivty(context, SearchConfiguration.TYPE_DEFAULT_SEARCH, word);
    }

    public static void startSearchActivty(Context context, @SearchConfiguration.SearchType int type, String word) {
        Intent intent = new Intent();
        StringBuilder sb = new StringBuilder();
        sb.append(SearchConfiguration.AUTHORITY)
                .append(type)
                .append("/")
                .append(word);
        intent.setData(Uri.parse(sb.toString()));
        if (context instanceof Activity) {
            Activity target = (Activity) context;
            target.startActivity(new Intent(context, SearchActivity.class), ActivityOptionsCompat.makeSceneTransitionAnimation(target).toBundle());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                target.getWindow().setEnterTransition(new Explode().setDuration(500));
                target.getWindow().setExitTransition(new Explode().setDuration(500));
            }
        } else {
            context.startActivity(intent);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setExitTransition(null);
        getWindow().setEnterTransition(null);
        getWindow().setAllowReturnTransitionOverlap(false);
        getWindow().setAllowEnterTransitionOverlap(false);
        getWindow().setTransitionBackgroundFadeDuration(0L);
        hideNavigationBar(getWindow());
        getWindow().setStatusBarColor(0);
        getWindow().setNavigationBarColor(Color.parseColor("#01ffffff"));

        setContentView(R.layout.fmsearch_layout_search_main);
        initConfig();
        initView();
        initData();
    }



    public static void hideNavigationBar(Window window) {
        View decorView = window.getDecorView();
        WindowInsetsControllerCompat windowInsetsController;
        windowInsetsController = Build.VERSION.SDK_INT >= 30
                ? ViewCompat.getWindowInsetsController(decorView) : new WindowInsetsControllerCompat(window, decorView);

        if (windowInsetsController == null) {
            return;
        }
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars());

        decorView.setOnSystemUiVisibilityChangeListener(it -> {
            if (it == 0) {
                Log.e("alshdflkasdf", "on show nav");
                new Handler().postDelayed(() -> {
                    WindowInsetsControllerCompat windowInsetsController1;
                    windowInsetsController1 = Build.VERSION.SDK_INT >= 30 ?
                            ViewCompat.getWindowInsetsController(decorView) : new WindowInsetsControllerCompat(window, decorView);
                    windowInsetsController1.hide(WindowInsetsCompat.Type.navigationBars());
                }, 3000);
            }
        });
    }

    private void searchKeywordFromIntent(Intent intent) {
        String key = intent.getStringExtra(SEARCH_INTENT_KEYWORD);
        if (!TextUtils.isEmpty(key)) {
            mSearchInputViewProxy.setCurrentEditText(key);
        }
    }

    public static final String SEARCH_INTENT_KEYWORD = "keyword";

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, ">>>>>>SearchActivity#onNewIntent : ");
        initConfig();
        if (!mConfiguration.shouldSkipConfig()) {
            //do search
            doSearch(mConfiguration.type, mConfiguration.keyword);
        }
    }

    private void initConfig() {
        //init by intent
        Intent intent = getIntent();
        mConfiguration = SearchConfiguration.filterConfig(intent.getData());

        ViewConfiguration viewConfiguration = ViewConfiguration.get(this);
        mTouchSlop = viewConfiguration.getScaledTouchSlop();

        searchKeywordFromIntent(intent);
    }

    private void initView() {
        if (DEBUG) Log.d(TAG, ">>>>>>SearchActivity#initView :");
        //head
        final View searchBlock = findViewById(R.id.search_head);
        searchBlock.setBackground(getResources().getDrawable(R.drawable.fmsearch_dw_search_widget_bg));
//        searchBlock.setBackgroundColor(Color.TRANSPARENT);//changed by Hong
        mSearchInputViewProxy = new IHeaderSearchView.SearchViewProxy(searchBlock);
        mSearchInputViewProxy.addTextChangedListener(mTextWatcher);
        mSearchInputViewProxy.setOnFocusChangeListener(mFocusChangeListener);
        mSearchInputViewProxy.setOnClickListener(this);
        mSearchInputViewProxy.setOnKeyListener(mOnKeyListener);
        setShowSoftInputView(mSearchInputViewProxy.getIuputView());

        //content web
        final ViewStub mWebContainer = (ViewStub) findViewById(R.id.web_container);
        final ProgressBar progress = (ProgressBar) findViewById(R.id.progressbar);
        mContentWebViewProxy = new IWebViewContent.WebViewContentProxy(mWebContainer, progress);

        //content search result
        final View searchContent = findViewById(R.id.search_container);
        mDataFlowAdapter = new DataFlowAdapter(this);
        mSearchContentProxy = new ISearchContent.SearchContentProxy(this, searchContent, mDataFlowAdapter);
        mSearchContentProxy.setOnScrollChangeLintener(mOnScrollChangeLintener);


        LauncherAppState app = LauncherAppState.getInstance();
        LauncherModel mModel = app.getModel();
        BitmapDrawable gaussWallpaper = mModel.getGaussWallpaperDrawable();

        ImageView bgView = (ImageView) findViewById(R.id.background);
        bgView.setBackground(gaussWallpaper);
        //bottom
//        final ViewStub bottomViewContainer = (ViewStub) findViewById(R.id.search_bottom_container);
//        mBottomViewProxy = new IBottomView.BottomViewProxy(bottomViewContainer, mConfiguration);
//        mBottomViewProxy.setOnClickListener(this);
        RelativeLayout seperator = (RelativeLayout) findViewById(R.id.seperator);
        seperator.setBackgroundColor(getResources().getColor(com.amz.ios.ioslite.common.R.color.white50percent));
    }


    private void initData() {
        if (DEBUG) Log.d(TAG, ">>>>>>SearchActivity#initData ");
        //init search logic data handler
        if (mSearchDataPresenter == null)
            mSearchDataPresenter = new SearchDataProvider(this, mContentWebViewProxy);

        //if open search from newIntent will should do search
        if (!mConfiguration.shouldSkipConfig()) {
            //do search
            doSearch(mConfiguration.type, mConfiguration.keyword);
        } else {
            //init load
            doPreLoad();
        }
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, ">>>>>>SearchActivity#onConfigurationChanged : " + newConfig);
    }

    @Override
    protected void onDestroy() {
        //release request and memery
        //mDataFlowPresenter
        mSearchDataPresenter.destroy();
        mDataFlowAdapter.destory();
        super.onDestroy();


    }

    private void resetToPreloadState() {
        mShowMode = SHOW_IDE_MODE;
        mDataFlowAdapter.clear().notifyChanged(); //changed by Hong
//        mBottomViewProxy.hideBottom();
        mContentWebViewProxy.hideWebView();
        mSearchContentProxy.showContent();
        doPreLoad();
    }

    private UiHandler uiHandler = new UiHandler();

    /**
     * when first openSearch page do this
     */
    private void doPreLoad() {
        //delay load after ui updated
        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                mSearchDataPresenter.cancelAllTask();
                //load recommod app
                mSearchDataPresenter.loadRecommondApp().asynLoop().observer(new UiHandler());
                //load contact MOVE TO permission request logic
                //mDataFlowPresenter.loadContact().observer(mUihandler)
                checkPermissionAndLoadContacts();
                //load ad
                mSearchDataPresenter.loadAd().asynLoop().delay(0).observer(new UiHandler());
                //preload all app
                mSearchDataPresenter.loadLocalApp("").asynLoop().observer(null);
                //load music,Don't worry it  just to check permission
                checkPermissionAndLoadMusic();
            }
        });
    }

    /**
     * work when user press search button
     */
    private void doSearch(int action) {
        if (mSearchInputViewProxy.checkInputViewAndShake()) {
            doSearch(action, "");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        final View inputView = mSearchInputViewProxy.getIuputView();
        if (inputView != null && inputView.hasFocus()) {
            showSoftInput(true);
            mSearchInputViewProxy.inputViewState(inputView.hasFocus());

        }
        if (mSearchInputViewProxy != null) {
            mSearchInputViewProxy.switchHitTip();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        onBackPressed();
    }

    /**
     * work when user press search button
     */
    private void doSearch(int action, String word) {
        //open webview to load keyword
        String finalWord = word;
        if (TextUtils.isEmpty(finalWord)) {
            finalWord = mSearchInputViewProxy.getSearchWord();
        }
        if (TextUtils.isEmpty(finalWord)) return;
        if (DEBUG)
            Log.e(TAG, ">>>>>>SearchActivity#doSearch : doSearch with action = " + action + " , keyword = " + finalWord);
        AnalyticsDelegate.onSearchEvent(this,UMEventConstants.SEARCH_ACTIVITY_SEARCH_BUTTON_CLICK);
        hideSoftInput();
        mShowMode = SHOW_WEBVIEW_MODE;
        mSearchContentProxy.hideContent();
        mContentWebViewProxy.showWebView();
//        mBottomViewProxy.hideBottom();
        mSearchDataPresenter.loadUrl(action, finalWord);
    }

    /**
     * work when text change
     */
    private void doAutoSearch(String word) {
        if (DEBUG) {
            Log.e(TAG, ">>>>>>SearchActivity#doAutoSearch : with word = " + word + ",mode = " + mShowMode);
        }
        //if state come from webview mode
        //we should show our content view fisrt then load
        if (mShowMode == SHOW_WEBVIEW_MODE) {
            mContentWebViewProxy.hideWebView();
            mSearchContentProxy.showContent();
//            mBottomViewProxy.showBottom();
        } else {
            //if keyword is space
            //then skip
            if (TextUtils.isEmpty(word)) {
                Log.e(TAG, ">>>>>>SearchActivity#doAutoSearch : empty word jump skip");
                return;
            }
        }
        word = word.trim();
        mShowMode = SHOW_AUTO_SEARCH_MODE;
        //cancel last actions
        mSearchDataPresenter.cancelAllTask();
        mDataFlowAdapter.clear().notifyChanged();
        //start doauto search
//        mBottomViewProxy.showBottom();
        //search local app
        mSearchDataPresenter.loadLocalApp(word).asynLoop().observer(new UiHandler());
        //search contact
        mSearchDataPresenter.loadContact(word).asynLoop().observer(new UiHandler());
        //search local files
        mSearchDataPresenter.loadLocalFile(word).asynLoop().observer(new UiHandler());
//        search music
//        mSearchDataPresenter.loadMusic(word).asynLoop().observer(new UiHandler()); changed by Hong
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //cancel it because.
        /*if (requestCode == PERMISSION_REQUEST_CODE && resultCode == RESULT_OK) {
            mSearchDataPresenter.loadContact("").asynLoop().observer(new UiHandler());
        }*/
    }

    private void checkPermissionAndLoadContacts() {
        checkPermissionAndLoad(mSearchDataPresenter.loadContact("").addInPool().asynLoop(), PERMISSION_REQUEST_CODE, new UiHandler(), Manifest.permission.READ_CONTACTS);
    }

    private void checkPermissionAndLoadMusic() {
        checkPermissionAndLoad(mSearchDataPresenter.loadMusic("").addInPool(), 0, new UiHandler(), Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    @Override
    public void onClick(View v) {
        //when button click
        final int id = v.getId();
        if (id == R.id.btn_search_pic) {//search picture
            doSearch(SearchConfiguration.TYPE_PIC_SEARCH);
        } else if (id == R.id.btn_search_video) {//search video
            doSearch(SearchConfiguration.TYPE_VIDEO_SERACH);
        } else if (id == R.id.btn_search_websit) {//search websit
            doSearch(SearchConfiguration.TYPE_DEFAULT_SEARCH);
        } else if (id == R.id.btn_search_container) {//search with keyword
            mSearchInputViewProxy.showRealSearchView();
        } else if (id == R.id.btn_cancel) {//cancel search
            mSearchInputViewProxy.clearSearchWord();
            showSoftInput();
        } else if (id == R.id.btn_real_search) {//press search
//            doSearch(SearchConfiguration.TYPE_DEFAULT_SEARCH);
            mSearchInputViewProxy.getIuputView().requestFocus();
        } else if (id == R.id.real_search_container) {
            showSoftInput();
            mSearchInputViewProxy.getIuputView().requestFocus();
        }
    }

    private ScrollViewExtend.OnScrollChangeLintener mOnScrollChangeLintener = new ScrollViewExtend.OnScrollChangeLintener() {
        @Override
        public void onScrollChange(int l, int t, int oldl, int oldt) {
            if (Math.abs(t - oldt) >= mTouchSlop) {
                hideSoftInput();
            }
        }
    };

    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            Log.d("aaaa", "aaaa");
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            Log.d("fff", "ff");
        }

        @Override
        public void afterTextChanged(Editable s) {
            String word = s.toString();
            if (word.contains("\n")){
                word = word.replace("\n", "");
                mSearchInputViewProxy.setCurrentEditText(word);
                mSearchInputViewProxy.clearFocus();
                return;
            }

            mSearchInputViewProxy.afterTextchanged(word.toString());
            if (word.length() == 0) {
                resetToPreloadState();
            } else {
                //if we now in webview mode just do nothing
                if (mShowMode != SHOW_WEBVIEW_MODE) {
                    //filter space text change.
                    word = mSearchInputViewProxy.getSearchWord();
                    if (TextUtils.isEmpty(word)) {
                        //do nothing
                        return;
                    }
//                    doAutoSearch(s.toString());
                    doAutoSearch(word);

                }
            }

        }
    };

    private View.OnFocusChangeListener mFocusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (v == mSearchInputViewProxy.getIuputView()) {
                if (hasFocus) {
                    String s = mSearchInputViewProxy.getSearchWord();
                    showSoftInput(true);
                } else {
                    hideSoftInput();
                }

                mSearchInputViewProxy.inputViewState(hasFocus);
            }
        }
    };

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (handleKeyEvent(keyCode, event)) return true;
        return super.onKeyUp(keyCode, event);
    }


    private View.OnKeyListener mOnKeyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (handleKeyEvent(keyCode, event)) return true;
            return false;
        }
    };


    protected boolean handleKeyEvent(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if (isShowSoftInput()) {//check soft input
                hideSoftInput();
                return true;
            } else if (mContentWebViewProxy.goBack()) {//check webview
                return true;
            } else if (mShowMode == SHOW_WEBVIEW_MODE) {
                if (mSearchInputViewProxy.isInputTextEmpty()) {
                    resetToPreloadState();
                } else {
                    doAutoSearch(mSearchInputViewProxy.getSearchWord());
                }
                return true;
            } else if (mShowMode == SHOW_AUTO_SEARCH_MODE) {//check input textview
                if (!mSearchInputViewProxy.isInputTextEmpty()) {
                    mSearchInputViewProxy.getIuputView().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mSearchInputViewProxy.clearSearchWord();
                        }
                    }, 100);
                    return true;
                }
            } else if (mShowMode == SHOW_IDE_MODE) {
                onBackPressed();
            }

        } else if (keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_ENTER) {
//            doSearch(SearchConfiguration.TYPE_DEFAULT_SEARCH);
            return true;
        }
        return false;
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }


    private class UiHandler extends CancelableCallBack<List<BaseCardItemInfo>> {

        @Override
        public void onSucess(final List<BaseCardItemInfo> result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (DEBUG)
                        Log.e(TAG, ">>>>>>UiHandler#onSucess :\n\t" + getName() + " load finished");

                    //then filter emtpry data
                    //filter start
                    if (!(result != null && !result.isEmpty())) {
                        if (DEBUG)
                            Log.d(TAG, ">>>>>>UiHandler#onSucess :Emma~,Task " + getName() + " has  empty data!");
                        return;
                    }
                    final BaseCardItemInfo itemInfo = result.get(0);
                    if (itemInfo.mInitTYpe == BaseCardItemInfo.TYPE_VIEW) {
                        if (itemInfo.isViewEmpty()) {
                            Log.d(TAG, ">>>>>>UiHandler#run : Emma~,Task " + getName() + " has  empty View!");
                            return;
                        }
                    }

                    //filter end
                    if (IGroupItemInfoImplAdapter.instanceOfGroupItem(result) && !isCanceled()) {
                        //cause all app is cached and the first register carditeminfo alse been cached
                        //so instance of AppCardItemInfo's item group pool will be reuse
                        //before use we should clean the group pool
                        final IGroupItemInfoImplAdapter item = (IGroupItemInfoImplAdapter) result.get(0);
                        item.clear();
                        item.addAllToGroup(result);
                        mDataFlowAdapter.add(item).notifyChanged();
                        return;
                    }
                    if (!isCanceled()) {
                        mDataFlowAdapter.addAll(result).notifyChanged();
                    }
                }
            });
        }

        @Override
        public void onFalure(String message, int type) {
            if (DEBUG) Log.e(TAG, ">>>>>>UiHandler#onFalure : \t\n" + getName() + " load finished");
            Log.e(TAG, message + "");
            if (!TextUtils.isEmpty(message) && shouldShowTips()) {
                ToastUtil.show(SearchActivity.this, message);
            }
        }
    }

}
