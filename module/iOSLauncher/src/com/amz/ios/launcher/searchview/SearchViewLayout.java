package com.amz.ios.launcher.searchview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amz.ios.database.HiddenAppManager;
import com.amz.ios.launcher.AppInfo;
import com.amz.ios.launcher.DeviceProfile;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.TextViewCustomFont;
import com.amz.ios.launcher.overscroll.OverScrollLayout;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class SearchViewLayout extends ConstraintLayout implements View.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    public static final int[] SearchViewLayout = {R.attr.voiceHintPrompt};

    SearchViewState mState;
    EditText mSearchKeyEDT;
    ConstraintLayout mSearchBox;
    TextViewCustomFont mActionBackTV;
    AppCompatImageView mActionVoiceIV;
    AppCompatImageView mActionClearIV;
    RecyclerView mSuggestionRV;
    LinearLayout mOtherSearchLayout;
    OverScrollLayout mOverScrollLayout;
    View mSearchWeb;
    View mSearchStore;
    View mSearchMaps;

    LinearLayout mSearchResult;

    Launcher mLauncher;
    SearchViewAdapter mSearchViewAdapter;
    InputMethodManager mInputMethodManager;
    SearchViewLayoutDelegate mSearchViewLayoutDelegate;
    DeviceProfile mDeviceProfile;
    TextWatcher mSearchKeywordTextWatcher;

    String mSearchHint;
    int margin;
    boolean isFocusOn;
    ArrayList<AppInfo> mApplicationInfoList;

    Handler mHandler;

    public interface SearchViewLayoutDelegate{
        void onSearchViewAlphaChanged(float f);
        void onSearchViewClosed();
        void onSearchViewOpened();
    }

    public enum SearchViewState {
        OPENING,
        OPENED,
        CLOSING,
        CLOSED
    }

    public SearchViewLayout(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public SearchViewLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = (Launcher) context;
        margin = mLauncher.getDeviceProfile().edgeMarginPx;
        TypedArray obtainStyledAttributes = mLauncher.obtainStyledAttributes(attrs, SearchViewLayout, 0, 0);
        if (obtainStyledAttributes.hasValue(0)) {
            setVoiceHintPrompt(obtainStyledAttributes.getString(0));
        }
        obtainStyledAttributes.recycle();
        LayoutInflater.from(context).inflate(R.layout.search_view,this,true);
    }

    private void setVoiceHintPrompt(String string) {
        if (TextUtils.isEmpty(string)) {
            mSearchHint = mLauncher.getString(R.string.hint_prompt);
            return;
        }
        mSearchHint = string;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    }

    @Override
    public void onClick(View v) {


        if (v == mActionBackTV){
            hideSearchView();
        }
        else if (v == mActionClearIV){
            if (mSearchKeyEDT != null)
                mSearchKeyEDT.setText("");
        }
        else if (v == mSearchMaps || v == mSearchStore || v== mSearchWeb) {
            String keyWord = mSearchKeyEDT.getText().toString();
            if (TextUtils.isEmpty(keyWord)) return;
            Intent intent;
            Intent secondIntent = null;
            if (v == mSearchMaps) {
                intent = new Intent("android.intent.action.VIEW", Uri.parse("geo:0,0?q=" + keyWord));
                intent.setPackage("com.google.android.apps.maps");
            }
            else if (v == mSearchStore) {
                intent = new Intent("android.intent.action.VIEW", Uri.parse("market://search").buildUpon().appendQueryParameter("c", "apps").appendQueryParameter("q", keyWord).build());
                secondIntent = new Intent("android.intent.action.VIEW", Uri.parse("http://play.google.com/store/search?q=" + keyWord));
            }
            else {
                intent = new Intent("android.intent.action.WEB_SEARCH");
                intent.putExtra("query", keyWord);
            }

            try {
                this.mLauncher.startActivity(intent);
            }
            catch (Exception e){
                try {
                    if (secondIntent != null)
                        this.mLauncher.startActivity(secondIntent);
                }
                catch (Exception e1){
                    Toast.makeText(mLauncher,R.string.application_not_found,Toast.LENGTH_SHORT).show();
                }
                Toast.makeText(mLauncher,R.string.application_not_found,Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setUpView();
        setListeners();
        setAdapter();
        config();
        init();
        animate().translationY((-mDeviceProfile.getCurrentHeight()) / 6.0f);
    }

    void init(){
        mDeviceProfile = mLauncher.getDeviceProfile();
        mState = SearchViewState.CLOSED;
        mApplicationInfoList = new ArrayList<>();
        mInputMethodManager = (InputMethodManager) mLauncher.getSystemService(Context.INPUT_METHOD_SERVICE);
        mHandler = new Handler(Looper.getMainLooper(),new HandlerCallback());
        mSearchKeywordTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                SearchViewLayout searchViewLayout = SearchViewLayout.this;
                if (searchViewLayout.mState == SearchViewState.OPENED && searchViewLayout.mHandler != null && s != null && s.length() != before) {
                    mOtherSearchLayout.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
                    mHandler.removeCallbacksAndMessages(null);
                    Message message = new Message();
                    message.obj = s.toString();
                    mHandler.sendMessage(message);
                }
                if (TextUtils.isEmpty(mSearchKeyEDT.getText())) {
                    setClearBtnVisible(false);
                    setVoiceBtnVisible(true);
                }
                else {
                    setVoiceBtnVisible(false);
                    setClearBtnVisible(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        setVoiceBtnVisible(true);
        setFitsSystemWindows(true);
    }

    void setUpView(){
        mActionBackTV = findViewById(R.id.action_back);
        mSearchKeyEDT = findViewById(R.id.et_search);
        mActionVoiceIV = findViewById(R.id.action_voice);
        mActionClearIV = findViewById(R.id.action_clear);
        mSearchBox = findViewById(R.id.search_box);
        mSuggestionRV = findViewById(R.id.suggestion_list);
        mOtherSearchLayout = findViewById(R.id.other_search_layout);
        mSearchWeb = findViewById(R.id.search_web);
        mSearchStore = findViewById(R.id.search_store);
        mSearchMaps = findViewById(R.id.search_maps);
        mSearchResult = findViewById(R.id.result_layout);
        mOverScrollLayout = findViewById(R.id.overscroll_layout);
    }

    void setListeners(){
        mSearchKeyEDT.setOnFocusChangeListener(null);
        mSearchKeyEDT.setOnEditorActionListener(null);
        mActionClearIV.setOnClickListener(this);
        mActionVoiceIV.setOnClickListener(this);
        mActionBackTV.setOnClickListener(this);
        mSearchResult.setOnClickListener(this);
        mSearchMaps.setOnClickListener(this);
        mSearchStore.setOnClickListener(this);
        mSearchWeb.setOnClickListener(this);
    }

    void setAdapter(){
        mSuggestionRV.setLayoutManager(new GridLayoutManager(mLauncher,4));
        mSuggestionRV.setItemAnimator(new DefaultItemAnimator());
        mSuggestionRV.setNestedScrollingEnabled(true);
    }

    void config(){
        ((ConstraintLayout.LayoutParams) mSearchBox.getLayoutParams()).setMarginStart(margin);
        ((ConstraintLayout.LayoutParams) mActionBackTV.getLayoutParams()).setMarginEnd(margin);
        ((ConstraintLayout.LayoutParams) ((OverScrollLayout) findViewById(R.id.overscroll_layout)).getLayoutParams()).setMargins(margin, 0, margin, 0);
        mSuggestionRV.setPadding(0, margin, 0, margin);
        mOtherSearchLayout.setPadding(margin, 0, margin, 0);
    }

    public void setSearchViewLayoutDelegate(SearchViewLayoutDelegate delegate){
        mSearchViewLayoutDelegate = delegate;
    }

    public class HandlerCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            try {
                if (mSearchViewAdapter != null) {
                    mSearchViewAdapter.getFilter().filter((String) msg.obj);
                    return true;
                }
                return true;
            } catch (Throwable th) {
                th.getMessage();
                return true;
            }
        }
    }

    @Override
    public void clearFocus() {
        isFocusOn = true;
        if (mInputMethodManager != null)
            mInputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
        super.clearFocus();
        mSearchKeyEDT.clearFocus();
        isFocusOn = false;
    }

    @Override
    public final boolean dispatchKeyEventPreIme(KeyEvent keyEvent) {
        if (keyEvent == null || keyEvent.getKeyCode() != 4) {
            return super.dispatchKeyEventPreIme(keyEvent);
        }
        hideSearchView();
        return true;
    }

    public void hideSearchView(){
        if (mState == SearchViewState.CLOSED) return;
        mState = SearchViewState.CLOSED;
        mSearchKeyEDT.removeTextChangedListener(mSearchKeywordTextWatcher);
        mSearchKeyEDT.setText("");

        setClearBtnVisible(false);
        setVoiceBtnVisible(true);

        if (mOtherSearchLayout != null && mSearchKeyEDT.length() != 0){
            mOtherSearchLayout.setVisibility(View.GONE);
        }

        int height = mDeviceProfile.getCurrentHeight();

        ObjectAnimator ofPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(this, PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, (-height) / 6.0f), PropertyValuesHolder.ofFloat(View.ALPHA, 0.0f));
        ofPropertyValuesHolder.setDuration(368L);
        ofPropertyValuesHolder.setInterpolator(new DecelerateInterpolator());
        ofPropertyValuesHolder.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setVisibility(GONE);
                if (mSearchViewAdapter != null)
                    mSearchViewAdapter.getFilter().filter(null);
                setDrawingCacheEnabled(false);
                clearFocus();
            }

            @Override
            public void onAnimationStart(Animator animation) {
                setDrawingCacheEnabled(true);
            }
        });
        ofPropertyValuesHolder.start();
        if (this.mSearchViewLayoutDelegate != null)
            mSearchViewLayoutDelegate.onSearchViewClosed();
    }

    public void showSearchView(){
        if (mState == SearchViewState.OPENED) return;
        setVisibility(VISIBLE);
        mState = SearchViewState.OPENING;
        setAlpha(0.0f);
        mSuggestionRV.setVisibility(VISIBLE);
        mSearchKeyEDT.addTextChangedListener(null);
    }

    public void setClearBtnVisible(boolean flag){
        mActionClearIV.setVisibility(
                flag ? VISIBLE : GONE
        );
    }

    @SuppressLint("QueryPermissionsNeeded")
    public void setVoiceBtnVisible(boolean flag){
        /*
        if (flag){
            if (mLauncher.getPackageManager().queryIntentActivities(new Intent("android.speech.action.RECOGNIZE_SPEECH"), 0).size() > 0){
                mActionVoiceIV.setVisibility(VISIBLE);
            }
        }
        else
            mActionVoiceIV.setVisibility(View.INVISIBLE);
         */
    }

    public boolean isOpened(){
        return mState == SearchViewState.OPENED;
    }

    public boolean isOpening(){
        return mState == SearchViewState.OPENING;
    }

    public SearchViewState getState(){
        return mState;
    }

    public void setApps(ArrayList<AppInfo> list){
        mApplicationInfoList.clear();
        mApplicationInfoList.addAll(list);
        setData(list);
    }

    public void showKeyboard(View view){
        view.requestLayout();
        if (mLauncher.getResources().getConfiguration().keyboard != 1) return;
        if (mInputMethodManager == null) return;
        mInputMethodManager.showSoftInput(view,0);
    }

    public void hideKeyboard(){

    }

    public void setData(ArrayList<AppInfo> arrayList){
        mSearchViewAdapter = new SearchViewAdapter(mLauncher,arrayList);
        mSuggestionRV.setAdapter(mSearchViewAdapter);
    }

    @Override
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        return !isFocusOn && isFocusable() && mSearchKeyEDT.requestFocus(direction, previouslyFocusedRect);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

    }

    @Override
    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        if (mSearchViewLayoutDelegate != null)
            mSearchViewLayoutDelegate.onSearchViewAlphaChanged(alpha);
    }

    public void setKeyword(String key){
        if (mSearchKeyEDT != null)
            mSearchKeyEDT.setText(key);
    }

    public void setState(SearchViewState state){
        mState = state;
    }

    public void setState(boolean z) {
        this.mState = z ? SearchViewState.OPENED : SearchViewState.CLOSED;
    }

    public final void startOpen() {
        if (mState == SearchViewState.OPENED) {
            return;
        }
        setVisibility(View.VISIBLE);
        mState = SearchViewState.OPENING;
        setAlpha(0.0f);
        mSuggestionRV.setVisibility(View.VISIBLE);
        mSearchKeyEDT.addTextChangedListener(mSearchKeywordTextWatcher);
        if (mSearchViewLayoutDelegate != null) {
            mSearchViewLayoutDelegate.onSearchViewOpened();
        }

    }
}
