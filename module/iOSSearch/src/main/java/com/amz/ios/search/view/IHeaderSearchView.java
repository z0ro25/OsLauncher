package com.amz.ios.search.view;

import android.content.Context;
import android.graphics.Color;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.CycleInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amz.ios.ioslite.common.util.PreferencesUtil;
import com.amz.ios.launcher.util.Themes;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.search.config.MSCConfiguration;
import com.amz.ioslauncher.iossearch.R;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-22 下午3:17
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public interface IHeaderSearchView {

    String getSearchWord();

    void showRealSearchView();

    void showSearchButton();

    void clearFocus();

    void clearSearchWord();

    boolean checkInputViewAndShake();

    View getIuputView();

    void inputViewState(boolean bFocus); //changed by Hong

    void switchHitTip();

    boolean isInputTextEmpty();

    void setOnKeyListener(View.OnKeyListener l);

    void afterTextchanged(String s);

    void setCurrentEditText(String s);

    void setOnFocusChangeListener(View.OnFocusChangeListener l);

    void addTextChangedListener(TextWatcher watcher);

    void setOnClickListener(View.OnClickListener l);

    class SearchViewProxy implements IHeaderSearchView {

        private static final String TAG = SearchViewProxy.class.getSimpleName();
        public static final boolean DEBUG = MSCConfiguration.DEBUG;

        public static final String KEY_CURRENT_SHOW_INDEX = "key_index";
        private Context mContext;

        private View mSearchContainer;
        //search input view
        private EditText mSearchInputView;
        //cancel to clear current inputed text.
        private ImageView mCancelView;
        //real search box
        private RelativeLayout mRealSearchContainer;
        //press this button , enter into real search box
        //search box
        private CustomTextView mSearchBtnContainer;
        //real search button.
        private ImageView mRealSearchBtn;

        private Animation mShakeAni;

        public SearchViewProxy(View searchBlock) {
            mSearchContainer = searchBlock;
            mContext = searchBlock.getContext();

            mRealSearchContainer = (RelativeLayout) searchBlock.findViewById(R.id.real_search_container);

            mCancelView = (ImageView) searchBlock.findViewById(R.id.btn_cancel);
            mSearchInputView = (EditText) searchBlock.findViewById(R.id.edit_search);
            mSearchInputView.setHintTextColor(mContext.getResources().getColor(com.amz.ios.ioslite.common.R.color.white70percent));

            mSearchBtnContainer = (CustomTextView) searchBlock.findViewById(R.id.btn_search_container);
            mSearchBtnContainer.setVisibility(View.GONE);
            mRealSearchBtn = (ImageView) searchBlock.findViewById(R.id.btn_real_search);
            mRealSearchBtn.setAlpha(0.7f);
//            mRealSearchBtn.setVisibility(View.GONE);


            mSearchInputView.setImeOptions(EditorInfo.IME_ACTION_NONE);
        }

        @Override
        public String getSearchWord() {
            return mSearchInputView.getText().toString().trim();
        }

        @Override
        public void showRealSearchView() {
            mRealSearchContainer.setVisibility(View.VISIBLE);
//            mSearchBtnContainer.setVisibility(View.GONE);
            mRealSearchBtn.setVisibility(View.GONE);
            mSearchInputView.post(new Runnable() {
                @Override
                public void run() {
                    mSearchInputView.requestFocus();
                }
            });
        }

        @Override
        public void inputViewState(boolean bFocus) //changed by Hong
        {
//            if (bFocus){
//                mRealSearchBtn.setVisibility(View.GONE);
//            }
//            else{
                if (mSearchInputView.getText().toString().isEmpty()) {
                    mRealSearchBtn.setVisibility(View.VISIBLE);
                }
                else {
                    mRealSearchBtn.setVisibility(View.GONE);
                }
//            }
            mSearchInputView.setHint(R.string.fmsearch_search_tip_string);

        }

        @Override
        public void showSearchButton() {
            mRealSearchContainer.setVisibility(View.GONE);
//            mSearchBtnContainer.setVisibility(View.VISIBLE);
        }

        @Override
        public void clearFocus(){
            mSearchInputView.clearFocus();
        }

        @Override
        public void clearSearchWord() {
            mSearchInputView.setText("");
            mSearchInputView.requestFocus();
        }

        @Override
        public void setOnFocusChangeListener(View.OnFocusChangeListener l) {
            mSearchInputView.setOnFocusChangeListener(l);
        }

        @Override
        public void addTextChangedListener(TextWatcher watcher) {
            mSearchInputView.addTextChangedListener(watcher);
        }

        @Override
        public void setOnClickListener(View.OnClickListener l) {
//            mSearchBtnContainer.setOnClickListener(l);
            mRealSearchBtn.setOnClickListener(l);
            mCancelView.setOnClickListener(l);
            mRealSearchContainer.setOnClickListener(l);
        }

        @Override
        public View getIuputView() {
            return mSearchInputView;
        }

        @Override
        public boolean isInputTextEmpty() {
            return mSearchInputView.getText().toString().isEmpty();
        }

        @Override
        public void setOnKeyListener(View.OnKeyListener l) {
            mSearchInputView.setOnKeyListener(l);
        }

        @Override
        public void afterTextchanged(String s) {
            if (s.length() == 0) {
                mCancelView.setVisibility(View.GONE);
                //add shake animation
                // cancel this logic
                //mRealSearchBtn.setEnabled(false);
            } else {
                mCancelView.setVisibility(View.VISIBLE);
            }

            if (s.isEmpty()) {
                mRealSearchBtn.setVisibility(View.VISIBLE);
            }
            else {
                mRealSearchBtn.setVisibility(View.GONE);
            }

        }

        @Override
        public void setCurrentEditText(String s) {
            mSearchInputView.setText(s);
        }

        @Override
        public void switchHitTip() {
//            final String[] showStr = mContext.getResources().getStringArray(R.array.fmsearch_search_show_strings);
//            final int index = PreferencesUtil.getInt(mContext, KEY_CURRENT_SHOW_INDEX);
//            String tips = showStr[index == -1 ? 0 : index % showStr.length];

//            mSearchInputView.setHint(R.string.fmsearch_search_tip_string);
//            Themes.setEditTextTypeFace(mContext, mSearchInputView);
//            mSearchBtnContainer.setText(R.string.fmsearch_search_tip_string);
//            PreferencesUtil.putInt(mContext, KEY_CURRENT_SHOW_INDEX, (index + 1) % showStr.length);
        }

        @Override
        public boolean checkInputViewAndShake() {
            if (mShakeAni == null) {
                mShakeAni = new TranslateAnimation(0, 10, 0, 0);
                mShakeAni.setInterpolator(new CycleInterpolator(7f));
                mShakeAni.setDuration(500);
            }
            if (isInputTextEmpty()) {
                if (DEBUG) Log.d(TAG, ">>>>>>SearchViewProxy#checkInputViewAndShake : ");
                mSearchContainer.startAnimation(mShakeAni);
                return false;
            }
            return true;
        }
    }

}
