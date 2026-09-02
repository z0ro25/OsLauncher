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
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amz.ios.database.HiddenAppManager;
import com.amz.ios.launcher.AppInfo;
import com.amz.ios.launcher.DeviceProfile;
import com.amz.ios.launcher.ExtendedEditText;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.TextViewCustomFont;
import com.amz.ios.launcher.overscroll.OverScrollLayout;

import org.w3c.dom.Text;

import android.content.ComponentName;
import java.util.ArrayList;
import java.util.List;

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

    // Các view/adapter thêm cho bố cục mới (Gợi ý có "Xem thêm" + mục Lịch sử).
    TextViewCustomFont mSuggestionText;      // tiêu đề "Gợi ý"
    TextViewCustomFont mActionShowMore;      // nút "Xem thêm" (4 -> 8)
    View mHistoryLayout;                     // cả cụm Lịch sử (ẩn khi rỗng)
    TextViewCustomFont mActionClearHistory;  // nút "Xóa" lịch sử
    RecyclerView mHistoryList;               // lưới app lịch sử
    SearchViewAdapter mHistoryAdapter;
    boolean mHistoryHasData;                 // có lịch sử để hiển thị hay không
    /** Số app hiển thị ở chế độ gợi ý MẶC ĐỊNH (bấm "Xem thêm" -> MAX_SEARCH_ITEM_SIZE). */
    private static final int SUGGESTION_DEFAULT_LIMIT = 4;

    // [FIX bàn phím đè] LauncherRootView.fitSystemWindows() nuốt insets + ép bottom=0 nên adjustPan
    // KHÔNG đẩy search lên trên bàn phím. Ta TỰ đo chiều cao bàn phím (getWindowVisibleDisplayFrame)
    // rồi nâng ô nhập + chừa đáy danh sách. Lưu giá trị cuối để tránh layout lặp.
    private ViewTreeObserver.OnGlobalLayoutListener mKeyboardListener;
    private int mLastKeyboardH = -1;

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
        else if (v == mActionShowMore){
            // "Xem thêm": mở rộng lưới gợi ý 4 -> 8 (chỉ ở chế độ gợi ý). Không đụng hằng số dùng chung.
            if (mSearchViewAdapter != null) {
                mSearchViewAdapter.setSuggestionLimit(SearchViewAdapter.MAX_SEARCH_ITEM_SIZE);
            }
            if (mActionShowMore != null) mActionShowMore.setVisibility(View.GONE); // đã mở hết -> ẩn nút
        }
        else if (v == mActionClearHistory){
            // "Xóa": xoá toàn bộ lịch sử app mở-từ-search rồi làm mới UI.
            SearchHistoryStore.clear(mLauncher);
            refreshHistory();
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
                    mHandler.removeCallbacksAndMessages(null);
                    Message message = new Message();
                    message.obj = s.toString();
                    mHandler.sendMessage(message);
                }
                // Gõ text -> hiện KẾT QUẢ lọc + nhóm web/store/maps, ẩn tiêu đề Gợi ý + "Xem thêm" +
                // Lịch sử. Xóa hết -> khôi phục Gợi ý + Lịch sử. (Gộp toggle vào 1 hàm cho gọn.)
                boolean hasText = !TextUtils.isEmpty(mSearchKeyEDT.getText());
                updateSectionsVisibility(hasText);
                if (!hasText) {
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

        // Nghe global-layout để tự bám bàn phím (xem mKeyboardListener). Chỉ tác động khi search hiển thị.
        mKeyboardListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                adjustForKeyboard();
            }
        };
        getViewTreeObserver().addOnGlobalLayoutListener(mKeyboardListener);
    }

    /**
     * Tự đo chiều cao bàn phím rồi NÂNG ô nhập lên trên bàn phím + chừa padding đáy cho danh sách kết
     * quả (để item cuối cuộn lên trên bàn phím). Cần vì LauncherRootView nuốt insets (bottom=0) khiến
     * adjustPan/adjustResize không đẩy được. Chỉ chạy khi search đang hiển thị; đóng -> trả về 0.
     */
    private void adjustForKeyboard() {
        int kb = 0;
        Rect r = new Rect();
        getWindowVisibleDisplayFrame(r);
        int rootH = getRootView().getHeight();
        if (getVisibility() == View.VISIBLE) {
            int h = rootH - r.bottom;
            if (h > rootH * 0.15f) kb = h; // đủ lớn -> coi như bàn phím đang hiện (loại nav bar nhỏ)
        }
        // [LOG TẠM - gỡ sau khi chẩn đoán xong] đo bàn phím + kích thước thật.
        int imeInset = -1;
        androidx.core.view.WindowInsetsCompat wi = androidx.core.view.ViewCompat.getRootWindowInsets(this);
        if (wi != null) imeInset = wi.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom;
        android.util.Log.e("SearchKB", "adjust vis=" + getVisibility() + " rootH=" + rootH
                + " rBottom=" + r.bottom + " rTop=" + r.top + " kb=" + kb + " viewH=" + getHeight()
                + " imeInset=" + imeInset);
        if (kb == mLastKeyboardH) return; // không đổi -> bỏ qua, tránh layout lặp vô hạn
        mLastKeyboardH = kb;
        if (mSearchBox != null) mSearchBox.setTranslationY(-kb);
        if (mSearchResult != null) {
            mSearchResult.setPadding(mSearchResult.getPaddingLeft(), mSearchResult.getPaddingTop(),
                    mSearchResult.getPaddingRight(), kb);
        }
    }

    /** Khi MỞ search: dùng adjustNothing (không pan bất định) + luôn hiện bàn phím; tự bám bằng đo. */
    private void enterSearchSoftInputMode() {
        // adjustResize để getWindowVisibleDisplayFrame CO theo bàn phím (adjustNothing không co -> đo
        // không ra). Root nuốt insets nên layout con không tự co -> ta vẫn đẩy thủ công theo số đo.
        mLauncher.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                        | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        setStatusBarHiddenForSearch(true); // theo yêu cầu: ẩn status bar khi mở search
        android.util.Log.e("SearchKB", "enterSearchSoftInputMode + hide status");
    }

    /** Khi ĐÓNG search: trả softInputMode về adjustPan như baseline (các màn khác không đổi hành vi). */
    public void restoreSoftInputMode() {
        mLauncher.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setStatusBarHiddenForSearch(false); // hiện lại status bar khi đóng search
    }

    /**
     * Ẩn/hiện status bar RIÊNG cho màn search bằng {@link WindowInsetsControllerCompat} — CÙNG cơ chế
     * dự án đang dùng cho edit mode (cửa sổ ở "chế độ controller" từ onCreate nên cờ legacy bị bỏ qua,
     * chỉ controller mới ăn). TÁCH RIÊNG: KHÔNG gọi setStatusBarHiddenForEdit() và KHÔNG đụng cache
     * mStatusBarHiddenForEdit của edit -> hai luồng độc lập. Cutout mode [0,H] đã ép sẵn từ onCreate
     * nên ẩn status không làm khung tụt.
     */
    private void setStatusBarHiddenForSearch(boolean hidden) {
        try {
            android.view.Window w = mLauncher.getWindow();
            WindowInsetsControllerCompat c = new WindowInsetsControllerCompat(w, w.getDecorView());
            c.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            if (hidden) {
                c.hide(WindowInsetsCompat.Type.statusBars());
            } else {
                c.show(WindowInsetsCompat.Type.statusBars());
            }
            android.util.Log.e("SearchKB", "setStatusBarHiddenForSearch=" + hidden);
        } catch (Throwable t) {
            android.util.Log.e("SearchKB", "setStatusBarHiddenForSearch FAIL", t);
        }
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
        mSuggestionText = findViewById(R.id.suggestion_text);
        mActionShowMore = findViewById(R.id.action_show_more);
        mHistoryLayout = findViewById(R.id.history_layout);
        mActionClearHistory = findViewById(R.id.action_clear_history);
        mHistoryList = findViewById(R.id.history_list);
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
        if (mActionShowMore != null) mActionShowMore.setOnClickListener(this);
        if (mActionClearHistory != null) mActionClearHistory.setOnClickListener(this);
    }

    void setAdapter(){
        mSuggestionRV.setLayoutManager(new GridLayoutManager(mLauncher,4));
        mSuggestionRV.setItemAnimator(new DefaultItemAnimator());
        mSuggestionRV.setNestedScrollingEnabled(true);
        // Lưới lịch sử dùng cùng kiểu grid 4 cột như gợi ý.
        if (mHistoryList != null) {
            mHistoryList.setLayoutManager(new GridLayoutManager(mLauncher, 4));
            mHistoryList.setItemAnimator(new DefaultItemAnimator());
            mHistoryList.setNestedScrollingEnabled(true);
        }
    }

    void config(){
        ((ConstraintLayout.LayoutParams) mSearchBox.getLayoutParams()).setMarginStart(margin);
        // Ô nhập nằm ở ĐÁY; theme full-bleed nav -> chừa margin đáy = chiều cao thanh điều hướng để
        // input không lọt dưới nav bar khi bàn phím chưa hiện. Khi IME bung, adjustPan pan tiếp lên
        // trên bàn phím. (mDeviceProfile chưa gán ở thời điểm config() -> lấy trực tiếp từ Launcher.)
        ((ConstraintLayout.LayoutParams) mSearchBox.getLayoutParams()).bottomMargin =
                mLauncher.getDeviceProfile().navigationBarHeightPx;
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
        restoreSoftInputMode(); // trả adjustPan cho các màn khác
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

    /**
     * Bật bàn phím TIN CẬY khi mở search. Dùng cơ chế của {@link ExtendedEditText#showKeyboard()}
     * (requestFocus + SHOW_IMPLICIT + retry sau layout), KHÔNG dính guard {@code configuration.keyboard
     * != 1} của {@link #showKeyboard(View)} cũ — guard đó khiến máy KHÔNG có bàn phím cứng không bật
     * được IME. Giữ nguyên hàm cũ để không phá nơi khác nếu còn tham chiếu.
     */
    public void showKeyboardCompat() {
        if (mSearchKeyEDT instanceof ExtendedEditText) {
            ((ExtendedEditText) mSearchKeyEDT).showKeyboard();
        } else if (mSearchKeyEDT != null) {
            showKeyboard(mSearchKeyEDT);
        }
    }

    /**
     * Toggle các mục theo trạng thái ô nhập:
     * - Có text: ẩn tiêu đề Gợi ý + "Xem thêm" + Lịch sử; hiện nhóm web/store/maps (kết quả filter nằm
     *   ở suggestion_list dùng chung, tự đổi nội dung theo filter).
     * - Text rỗng: hiện Gợi ý (tiêu đề + "Xem thêm") + Lịch sử (nếu có); ẩn nhóm web/store/maps.
     */
    private void updateSectionsVisibility(boolean hasText) {
        if (mSuggestionText != null) mSuggestionText.setVisibility(hasText ? View.GONE : View.VISIBLE);
        if (mActionShowMore != null) mActionShowMore.setVisibility(hasText ? View.GONE : View.VISIBLE);
        if (mHistoryLayout != null) {
            mHistoryLayout.setVisibility((!hasText && mHistoryHasData) ? View.VISIBLE : View.GONE);
        }
        if (mOtherSearchLayout != null) {
            mOtherSearchLayout.setVisibility(hasText ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Nạp lại lưới LỊCH SỬ từ {@link SearchHistoryStore}: map componentName -> AppInfo bằng cách dò
     * trong {@link #mApplicationInfoList} (đã có toàn bộ app), bỏ app đã gỡ. Rỗng -> ẩn cả cụm.
     * Gọi khi mở search và khi text về rỗng.
     */
    private void refreshHistory() {
        ArrayList<AppInfo> historyApps = new ArrayList<>();
        List<ComponentName> saved = SearchHistoryStore.load(mLauncher);
        for (ComponentName cn : saved) {
            for (AppInfo app : mApplicationInfoList) {
                if (app != null && app.componentName != null && app.componentName.equals(cn)) {
                    historyApps.add(app);
                    break;
                }
            }
        }
        mHistoryHasData = !historyApps.isEmpty();
        if (mHistoryList != null) {
            mHistoryAdapter = new SearchViewAdapter(mLauncher, historyApps);
            mHistoryAdapter.setSuggestionLimit(SearchHistoryStore.MAX); // hiển thị hết lịch sử
            mHistoryList.setAdapter(mHistoryAdapter);
        }
        boolean hasText = mSearchKeyEDT != null && !TextUtils.isEmpty(mSearchKeyEDT.getText());
        updateSectionsVisibility(hasText);
    }

    public void hideKeyboard(){

    }

    public void setData(ArrayList<AppInfo> arrayList){
        mSearchViewAdapter = new SearchViewAdapter(mLauncher,arrayList);
        // Chế độ gợi ý mặc định hiển thị 4 app; "Xem thêm" mở rộng thành 8.
        mSearchViewAdapter.setSuggestionLimit(SUGGESTION_DEFAULT_LIMIT);
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

        // Mỗi lần mở: đưa gợi ý về mặc định 4, làm mới Lịch sử, dựng đúng bố cục Gợi ý/Lịch sử, và
        // BẬT bàn phím ngay (không chờ spring-end) để tránh trường hợp IME không tự lên.
        if (mSearchViewAdapter != null) {
            mSearchViewAdapter.setSuggestionLimit(SUGGESTION_DEFAULT_LIMIT);
        }
        refreshHistory();
        enterSearchSoftInputMode();
        showKeyboardCompat();
        // Bàn phím bung trễ vài trăm ms -> đo lại vài lần cho chắc (global-layout có thể không bắn đủ).
        Runnable recheck = new Runnable() {
            @Override public void run() { adjustForKeyboard(); }
        };
        postDelayed(recheck, 300L);
        postDelayed(recheck, 650L);
    }
}
