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
import android.os.Build;
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
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
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
import java.util.Collections;
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
    boolean mHasResult;                      // lần gõ gần nhất có app khớp? (để bật/tắt khung Kết quả)

    // Khung KẾT QUẢ app khớp + list GỢI Ý DỌC (khi đang gõ).
    View mResultAppBox;                      // wrapper "Kết quả" (title + list)
    RecyclerView mResultAppList;             // list kết quả app khớp (dọc)
    RecyclerView mSuggestionVerticalList;    // list gợi ý dọc
    /** Số app hiển thị ở chế độ gợi ý MẶC ĐỊNH (bấm "Xem thêm" -> MAX_SEARCH_ITEM_SIZE). */
    private static final int SUGGESTION_DEFAULT_LIMIT = 4;

    // [KEYBOARD + STATUS BAR] THIẾT KẾ THỐNG NHẤT (đã duyệt):
    //   - Ẩn status bar NGAY khi mở search trên CẢ 2 máy, qua Launcher.setStatusBarHiddenForSearch (bên trong
    //     là setStatusBarHiddenForEdit: API>=30 dùng WindowInsetsController; API<30 ghi cờ legacy — hoạt động
    //     khi window đang full màn).
    //   - KHÔNG dựa "hệ thống co window" làm nguồn nâng ô nhập (không ổn định, xung đột ẩn-bar trên máy cũ).
    //     Thay vào đó TỰ đo chiều cao bàn phím rồi đặt search_box.bottomMargin = kb + GAP:
    //        * Máy mới (API >= 30): đọc WindowInsets.Type.ime() qua listener bám decorView (window bị ép
    //          edge-to-edge nên không tự co; insets được dispatch tới window).
    //        * Máy cũ (API < 30): window GIỮ NO_LIMITS (full, như edit — để ẩn bar được, không resize, tránh
    //          PAN) + đọc chiều cao bàn phím bằng InputMethodManager.getInputMethodWindowVisibleHeight()
    //          (hidden API — đã duyệt; dự án đã dùng HiddenApiBypass). Đo trong OnGlobalLayout.
    //   - Thanh search cách mép bàn phím một khoảng nhỏ SEARCH_BOTTOM_GAP_DP để không dính liền.
    private static final int SEARCH_BOTTOM_GAP_DP = 6;
    /** Đang giữ ẩn status bar (chỉ hiện lại khi chính search đã ẩn). */
    private boolean mSearchStatusBarHidden;
    // === Máy mới (API>=30): ime-inset qua decor ===
    private boolean mSearchImeListenerAttached;
    // CHÚ Ý: SearchViewLayout extends ConstraintLayout -> kế thừa nested type View.OnApplyWindowInsetsListener
    // (platform) nên tên NGẮN "OnApplyWindowInsetsListener" luôn resolve về platform (che import androidx) —
    // phải FULLY-QUALIFIED kiểu androidx.core.view.OnApplyWindowInsetsListener thì ViewCompat mới nhận.
    private androidx.core.view.OnApplyWindowInsetsListener mSearchImeListener;
    // === Máy cũ (API<30): hidden API InputMethodManager đo bàn phím trong OnGlobalLayout ===
    private ViewTreeObserver.OnGlobalLayoutListener mLegacyKbListener;
    private int mLastLegacyKb;
    /** Margin đáy GỐC của search_box (= navigationBarHeightPx, config đặt) để khôi phục khi đóng search. */
    private int mSearchBoxBaseBottomMargin = -1;
    /** Kiểm tra lại chiều cao bàn phím sau vài trăm ms (phòng insets/hidden chưa kịp báo). */
    private final Runnable mKbRecheckRunnable = new Runnable() {
        @Override public void run() { recheckKeyboardHeight(); }
    };
    /** Số lần đã poll lại chưa thấy bàn phím (chặn poll vô hạn khi máy không hiện IME). */
    private int mKbRecheckCount;
    /**
     * Top-inset đang áp (px) cho máy THẤP (API<30) khi KHÔNG ẩn được status bar: padding-top = chiều cao
     * status bar để bar không đè lên nội dung search (gợi ý...). Nếu ẩn được bar -> 0.
     */
    private int mSearchTopInsetPx;

    Launcher mLauncher;
    SearchViewAdapter mSearchViewAdapter;
    // Adapter riêng cho khung KẾT QUẢ app khớp (dọc) và list GỢI Ý DỌC (dọc) — tách khỏi adapter lưới gợi ý
    // mSearchViewAdapter để gõ có kết quả mà vẫn giữ gợi ý ở dưới (trước đây 1 adapter filter làm mất gợi ý).
    SearchViewAdapter mResultAdapter;
    SearchViewAdapter mSuggestionVerticalAdapter;
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
            // "Xem thêm": mở rộng gợi ý 4 -> 8 cho CẢ lưới (text rỗng) lẫn list dọc (đang gõ).
            if (mSearchViewAdapter != null) {
                mSearchViewAdapter.setSuggestionLimit(SearchViewAdapter.MAX_SEARCH_ITEM_SIZE);
            }
            if (mSuggestionVerticalAdapter != null) {
                mSuggestionVerticalAdapter.setSuggestionLimit(SearchViewAdapter.MAX_SEARCH_ITEM_SIZE);
            }
            if (mActionShowMore != null) mActionShowMore.setVisibility(View.GONE); // đã mở hết -> ẩn nút
        }
        else if (v == mActionClearHistory){
            // "Xóa": xoá toàn bộ lịch sử app mở-từ-search rồi làm mới UI.
//            SearchHistoryStore.clear(mLauncher);
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
        // KHÔNG dùng setFitsSystemWindows(true): trên máy mới (API>=30) insets có thể tới chính view này và
        // tự-padding trùng với việc ta chủ động đặt search_box.bottomMargin = ime (gây nâng 2 lần). Máy cũ
        // thì đường fitSystemWindows đã bị LauncherRootView nuốt nên thuộc tính này vốn vô tác dụng.
    }

    /**
     * Khi MỞ search: ẩn status bar ngay + bật cơ chế nâng ô nhập theo bàn phím cho đúng API.
     *
     * - Máy mới (API >= 30): bị ép edge-to-edge nên window KHÔNG tự co; bàn phím hiện = có
     *   {@code WindowInsets.Type.ime()}. Ẩn bar bằng controller + bám decor đọc ime-inset.
     * - Máy cũ (API < 30): muốn ẩn bar khi gõ thì window phải GIỮ full màn (NO_LIMITS, như edit — không
     *   resize để tránh bị PAN). Vì window không co nên hệ thống không báo bàn phím qua visible-frame; ta
     *   đọc chiều cao bàn phím bằng hidden API {@code InputMethodManager.getInputMethodWindowVisibleHeight()}
     *   trong OnGlobalLayout + poll.
     *   Cả hai máy đều tự đặt search_box.bottomMargin = kb + GAP; KHÔNG dựa hệ thống co window.
     */
    private void enterSearchSoftInputMode() {
        // Ẩn status bar NGAY khi mở search (mọi máy).
        if (!mSearchStatusBarHidden) {
            mLauncher.setStatusBarHiddenForSearch(true);
            mSearchStatusBarHidden = true;
        }
        // Ô nhập về sát đáy ngay khi mở; khi bàn phím lên sẽ được nâng lên (kb + gap).
        ConstraintLayout.LayoutParams lp0 =
                (ConstraintLayout.LayoutParams) mSearchBox.getLayoutParams();
        if (lp0.bottomMargin != 0) {
            lp0.bottomMargin = 0;
            mSearchBox.setLayoutParams(lp0);
            mSearchBox.requestLayout();
        }
        if (Build.VERSION.SDK_INT >= 30) {
            // Máy mới: clear NO_LIMITS (NO_LIMITS có thể chặn ime-inset dispatch) + bám decor đọc ime-inset.
            mLauncher.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            mLauncher.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                            | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            attachSearchImeListener();
        } else {
            // Máy cũ: GIỮ NO_LIMITS (window full để ẩn bar được, như edit — không resize tránh PAN).
            // Đo bàn phím bằng hidden API trong OnGlobalLayout + poll.
            mLauncher.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
                            | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            attachLegacyKbListener();
        }
        // Poll giới hạn để bắt bàn phím lên (trên máy cũ, IME hiện có thể không gây layout view đổi nên
        // OnGlobalLayout không bắn). recheckKeyboardHeight tự lặp khi chưa thấy IME.
        mKbRecheckCount = 0;
        removeCallbacks(mKbRecheckRunnable);
        postDelayed(mKbRecheckRunnable, 150L);
    }

    /**
     * Khi ĐÓNG search: hiện lại status bar (nếu ta đã ẩn), gỡ cơ chế nâng, trả margin ô nhập về gốc,
     * trả softInputMode về adjustPan như baseline. Điểm đóng DUY NHẤT: cả {@link #hideSearchView()} lẫn
     * {@code SearchPullDetector} đều gọi hàm này.
     */
    public void restoreSoftInputMode() {
        detachKbTracking(); // gỡ theo đúng API (decor ime-listener / legacy OnGlobalLayout)
        // Bỏ padding-top (nếu máy thấp chừa vì bar không ẩn được) và padding-bottom (chèn vì bàn phím).
        if (mSearchTopInsetPx != 0 || getPaddingBottom() != 0) {
            mSearchTopInsetPx = 0;
            setPadding(getPaddingLeft(), 0, getPaddingRight(), 0);
            requestLayout();
        }
        if (mSearchStatusBarHidden) {
            mLauncher.setStatusBarHiddenForSearch(false); // hiện lại status bar khi đóng search
            mSearchStatusBarHidden = false;
        }
        // Đưa margin ô nhập về gốc (navigationBarHeightPx) cho lần mở sau (lần mở sẽ tự set 0).
        if (mSearchBoxBaseBottomMargin >= 0) {
            ConstraintLayout.LayoutParams lp =
                    (ConstraintLayout.LayoutParams) mSearchBox.getLayoutParams();
            if (lp.bottomMargin != mSearchBoxBaseBottomMargin) {
                lp.bottomMargin = mSearchBoxBaseBottomMargin;
                mSearchBox.setLayoutParams(lp);
                mSearchBox.requestLayout();
            }
        }
        // Bật lại NO_LIMITS (máy mới đã clear khi mở) + trả softInputMode về adjustPan như baseline.
        mLauncher.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        mLauncher.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    }

    // ===== Nâng ô nhập theo bàn phím: máy mới dùng ime-inset, máy cũ dùng hidden API =====

    /** Poll lại chiều cao bàn phím; nếu chưa thấy IME thì tự post lại (giới hạn ~5s). */
    private void recheckKeyboardHeight() {
        if (mState != SearchViewState.OPENING && mState != SearchViewState.OPENED) return;
        boolean modern = Build.VERSION.SDK_INT >= 30;
        if (!modern) updateTopInsetForStatusBar(); // máy thấp: cập nhật top-inset nếu bar không ẩn được

        // Đã được nâng -> không cần poll tiếp.
        ConstraintLayout.LayoutParams cur =
                (ConstraintLayout.LayoutParams) mSearchBox.getLayoutParams();
        if (cur.bottomMargin > 0 || getPaddingBottom() > 0) return;

        int kb = modern
                ? measureKeyboardByVisibleFrame()   // máy mới: listener là chính, đây chỉ fallback
                : measureKeyboardHiddenApi();       // máy cũ: hidden API
        if (kb > 0) {
            if (modern) {
                applyKeyboardHeightToSearchBox(kb);
            } else {
                applySearchBottomInset(kb);
            }
            mKbRecheckCount = 0;
            return;
        }
        // Chưa thấy bàn phím -> thử lại (bàn phím bật trễ). Giới hạn để không poll vô hạn.
        if (mKbRecheckCount < 33) { // ~5s
            mKbRecheckCount++;
            removeCallbacks(mKbRecheckRunnable);
            postDelayed(mKbRecheckRunnable, 150L);
        }
    }

    /**
     * Áp chiều cao bàn phím (px) + GAP làm margin đáy ô nhập (dùng cho MÁY MỚI — đẩy riêng search_box).
     * kb=0 -> margin 0 (ô nhập về đáy).
     */
    private void applyKeyboardHeightToSearchBox(int kbPx) {
        if (mState != SearchViewState.OPENING && mState != SearchViewState.OPENED) return;
        int gap = Math.round(SEARCH_BOTTOM_GAP_DP * getResources().getDisplayMetrics().density);
        int target = kbPx > 0 ? kbPx + gap : 0;
        ConstraintLayout.LayoutParams lp =
                (ConstraintLayout.LayoutParams) mSearchBox.getLayoutParams();
        if (lp.bottomMargin != target) {
            lp.bottomMargin = target;
            mSearchBox.setLayoutParams(lp);
            mSearchBox.requestLayout();
            android.util.Log.e("SearchKB", "kb=" + kbPx + " gap=" + gap + " bottomMargin=" + target);
        }
    }

    /**
     * Máy THẤP: chèn BOTTOM-PADDING cho chính SearchViewLayout = kb + GAP để TOÀN BỘ màn search không bị
     * bàn phím đè (cả search_box lẫn danh sách co lên trên vùng kb). Giữ padding-top = top-inset (nếu bar
     * không ẩn được). kb=0 -> padding-bottom 0.
     */
    private void applySearchBottomInset(int kbPx) {
        if (mState != SearchViewState.OPENING && mState != SearchViewState.OPENED) return;
        int gap = Math.round(SEARCH_BOTTOM_GAP_DP * getResources().getDisplayMetrics().density);
        int bottom = kbPx > 0 ? kbPx + gap : 0;
        if (getPaddingBottom() != bottom) {
            setPadding(getPaddingLeft(), mSearchTopInsetPx, getPaddingRight(), bottom);
            requestLayout();
            android.util.Log.e("SearchKB", "searchBottomInset=" + bottom + " (kb=" + kbPx + ")");
        }
    }

    // ----- Máy mới (API>=30): ime-inset qua decor -----

    private void attachSearchImeListener() {
        if (mSearchImeListenerAttached) return;
        final View decor = mLauncher.getWindow().getDecorView();
        mSearchImeListener = (v, insets) -> {
            int ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            applyKeyboardHeightToSearchBox(ime);
            return insets; // không consume -> các view khác vẫn nhận insets như cũ
        };
        ViewCompat.setOnApplyWindowInsetsListener(decor, mSearchImeListener);
        mSearchImeListenerAttached = true;
        android.util.Log.e("SearchKB", "IME-listener attach decor");
    }

    private void detachSearchImeListener() {
        if (!mSearchImeListenerAttached) return;
        try {
            ViewCompat.setOnApplyWindowInsetsListener(mLauncher.getWindow().getDecorView(), null);
        } catch (Throwable t) {
            // decor có thể đã teardown (detach/recreate); bỏ qua.
        }
        mSearchImeListener = null;
        mSearchImeListenerAttached = false;
        android.util.Log.e("SearchKB", "IME-listener detach");
    }

    // ----- Máy cũ (API<30): hidden API InputMethodManager đo bàn phím -----

    /** Đo chiều cao bàn phím (px) bằng hidden API {@code getInputMethodWindowVisibleHeight}. */
    private int measureKeyboardHiddenApi() {
        try {
            java.lang.reflect.Method m = InputMethodManager.class.getMethod(
                    "getInputMethodWindowVisibleHeight");
            Object val = m.invoke(mInputMethodManager);
            if (val instanceof Integer) return (Integer) val;
        } catch (Throwable t) {
            android.util.Log.e("SearchKB", "hidden API kb fail", t);
        }
        return measureKeyboardByVisibleFrame();
    }

    /** Đo bàn phím qua visible-frame (fallback chung). */
    private int measureKeyboardByVisibleFrame() {
        try {
            View decor = mLauncher.getWindow().getDecorView();
            Rect r = new Rect();
            decor.getWindowVisibleDisplayFrame(r);
            int rootH = decor.getRootView().getHeight();
            int space = rootH - r.bottom;
            if (space > rootH * 0.15f) return space;
        } catch (Throwable t) {
            // bỏ qua
        }
        return 0;
    }

    /**
     * Máy thấp (API<30): nếu status bar KHÔNG ẩn được (mất cờ FULLSCREEN) thì áp padding-top = chiều cao
     * status bar để bar không đè lên nội dung search (gợi ý/tiêu đề). Nếu ẩn được -> top inset 0. Search_box
     * neo đáy nên không bị ảnh hưởng.
     */
    private void updateTopInsetForStatusBar() {
        if (Build.VERSION.SDK_INT >= 30) return;
        if (mState != SearchViewState.OPENING && mState != SearchViewState.OPENED) return;
        int top = 0;
        try {
            View decor = mLauncher.getWindow().getDecorView();
            boolean barHidden = (decor.getSystemUiVisibility()
                    & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0;
            if (!barHidden) {
                Rect r = new Rect();
                decor.getWindowVisibleDisplayFrame(r);
                if (r.top > 0 && r.top < 400) {
                    top = r.top;
                } else {
                    top = mLauncher.getDeviceProfile().statusBarHeightPx;
                }
            }
        } catch (Throwable t) {
            // bỏ qua
        }
        if (top != mSearchTopInsetPx) {
            mSearchTopInsetPx = top;
            setPadding(getPaddingLeft(), top, getPaddingRight(), getPaddingBottom());
            requestLayout();
            android.util.Log.e("SearchKB", "topInset=" + top);
        }
    }

    /** Máy cũ: nghe global-layout mỗi khi layout đổi để đo lại bàn phím. */
    private void attachLegacyKbListener() {
        if (mLegacyKbListener != null) return;
        mLegacyKbListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                updateTopInsetForStatusBar(); // bar ẩn/hiện lại -> cập nhật top-inset
                int kb = measureKeyboardHiddenApi();
                if (kb != mLastLegacyKb) {
                    mLastLegacyKb = kb;
                    applySearchBottomInset(kb); // kb=0 -> padding-bottom 0 (ô nhập về đáy)
                }
            }
        };
        mLauncher.getWindow().getDecorView().getViewTreeObserver()
                .addOnGlobalLayoutListener(mLegacyKbListener);
        android.util.Log.e("SearchKB", "legacy kb-listener attach");
    }

    /** Gỡ mọi cơ chế bám bàn phím theo đúng API. */
    private void detachKbTracking() {
        mKbRecheckCount = 0;
        removeCallbacks(mKbRecheckRunnable);
        if (Build.VERSION.SDK_INT >= 30) {
            detachSearchImeListener();
        } else {
            if (mLegacyKbListener != null) {
                try {
                    mLauncher.getWindow().getDecorView().getViewTreeObserver()
                            .removeOnGlobalLayoutListener(mLegacyKbListener);
                } catch (Throwable t) {
                    // decor có thể đã teardown; bỏ qua.
                }
                mLegacyKbListener = null;
            }
            mLastLegacyKb = 0;
            android.util.Log.e("SearchKB", "legacy kb-listener detach");
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
        mResultAppBox = findViewById(R.id.result_app_box);
        mResultAppList = findViewById(R.id.result_app_list);
        mSuggestionVerticalList = findViewById(R.id.suggestion_list_vertical);
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
        // ExpandableHeightRecyclerView nằm trong ScrollView (result_layout): BẮT BUỘC tắt nested scrolling
        // để RecyclerView tự đo theo nội dung (ExpandableHeightRecyclerView ép AT_MOST lớn). Nếu bật true,
        // RecyclerView trong ScrollView có thể đo cao 0 -> lưới gợi ý "có tiêu đề nhưng không hiện app".
        mSuggestionRV.setNestedScrollingEnabled(false);
        // Lưới lịch sử dùng cùng kiểu grid 4 cột như gợi ý.
        if (mHistoryList != null) {
            mHistoryList.setLayoutManager(new GridLayoutManager(mLauncher, 4));
            mHistoryList.setItemAnimator(new DefaultItemAnimator());
            mHistoryList.setNestedScrollingEnabled(false);
        }
        // Khung KẾT QUẢ + list GỢI Ý DỌC: 1 cột dọc (LinearLayoutManager) trong ScrollView -> tắt nested.
        if (mResultAppList != null) {
            mResultAppList.setLayoutManager(new LinearLayoutManager(mLauncher));
            mResultAppList.setItemAnimator(new DefaultItemAnimator());
            mResultAppList.setNestedScrollingEnabled(false);
        }
        if (mSuggestionVerticalList != null) {
            mSuggestionVerticalList.setLayoutManager(new LinearLayoutManager(mLauncher));
            mSuggestionVerticalList.setItemAnimator(new DefaultItemAnimator());
            mSuggestionVerticalList.setNestedScrollingEnabled(false);
        }
    }

    void config(){
        ((ConstraintLayout.LayoutParams) mSearchBox.getLayoutParams()).setMarginStart(margin);
        // Ô nhập nằm ở ĐÁY; theme full-bleed nav -> chừa margin đáy = chiều cao thanh điều hướng để
        // input không lọt dưới nav bar khi bàn phím chưa hiện. Khi IME bung, adjustPan pan tiếp lên
        // trên bàn phím. (mDeviceProfile chưa gán ở thời điểm config() -> lấy trực tiếp từ Launcher.)
        mSearchBoxBaseBottomMargin = mLauncher.getDeviceProfile().navigationBarHeightPx;
        ((ConstraintLayout.LayoutParams) mSearchBox.getLayoutParams()).bottomMargin =
                mSearchBoxBaseBottomMargin;
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
                String s = (String) msg.obj;
                if (TextUtils.isEmpty(s)) {
                    // Xoá hết text -> về lưới gợi ý; không có "kết quả".
                    mHasResult = false;
                    updateSectionsVisibility(false);
                    return true;
                }
                // Lọc KẾT QUẢ trên adapter riêng (mResultAdapter), không đụng adapter lưới gợi ý.
                if (mResultAdapter != null) {
                    mResultAdapter.getFilter().filter(s, new Filter.FilterListener() {
                        @Override
                        public void onFilterComplete(int count) {
                            // Chống lệch khi gõ nhanh: nếu text đã đổi thì lượt filter này đã cũ -> bỏ.
                            String cur = mSearchKeyEDT != null
                                    ? mSearchKeyEDT.getText().toString() : "";
                            if (!cur.equals(s)) return;
                            mHasResult = count > 0;
                            updateSectionsVisibility(true);
                        }
                    });
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

    /** Đã có danh sách app để hiện lưới Gợi ý hay chưa (Launcher gọi để nạp dự phòng khi rỗng). */
    public boolean hasApps() {
        return mApplicationInfoList != null && !mApplicationInfoList.isEmpty();
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
     * Sắp hiển thị các vùng theo trạng thái ô nhập:
     * - Không text: lưới gợi ý (suggestion_list) + header "Gợi ý"; ẩn kết quả & gợi ý dọc.
     * - Có text & có app khớp: khung KẾT QUẢ (result_app_box) trên + header "Gợi ý" + list gợi ý DỌC.
     * - Có text & không app khớp: ẩn khung Kết quả; hiện header "Gợi ý" + list gợi ý DỌC.
     * Lịch sử chỉ khi rỗng & có data; web/store/maps chỉ khi có text.
     */
    private void updateSectionsVisibility(boolean hasText) {
        boolean hasResult = mHasResult && hasText;
        // Header "Gợi ý"/"Xem thêm" luôn hiển thị (áp cho lưới lẫn gợi ý dọc).
        if (mSuggestionText != null) mSuggestionText.setVisibility(View.VISIBLE);
        if (mActionShowMore != null) mActionShowMore.setVisibility(View.VISIBLE);
        // Lịch sử: chỉ khi chưa gõ & có dữ liệu.
        if (mHistoryLayout != null) {
            mHistoryLayout.setVisibility((!hasText && mHistoryHasData) ? View.VISIBLE : View.GONE);
        }
        // web/store/maps: khi đang gõ.
        if (mOtherSearchLayout != null) {
            mOtherSearchLayout.setVisibility(hasText ? View.VISIBLE : View.GONE);
        }
        // Lưới gợi ý (text rỗng) / khung Kết quả + gợi ý dọc (đang gõ).
        if (mSuggestionRV != null) {
            mSuggestionRV.setVisibility(hasText ? View.GONE : View.VISIBLE);
        }
        if (mResultAppBox != null) {
            mResultAppBox.setVisibility(hasResult ? View.VISIBLE : View.GONE);
        }
        if (mSuggestionVerticalList != null) {
            mSuggestionVerticalList.setVisibility(hasText ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Nạp lại lưới LỊCH SỬ từ {@link SearchHistoryStore}: map componentName -> AppInfo bằng cách dò
     * trong {@link #mApplicationInfoList} (đã có toàn bộ app), bỏ app đã gỡ. Rỗng -> ẩn cả cụm.
     * Gọi khi mở search và khi text về rỗng.
     */
    private void refreshHistory() {
        ArrayList<AppInfo> historyApps = new ArrayList<>();
//        List<ComponentName> saved = SearchHistoryStore.load(mLauncher);
        List<ComponentName> saved = new ArrayList<>();
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
//            mHistoryAdapter.setSuggestionLimit(SearchHistoryStore.MAX); // hiển thị hết lịch sử
            mHistoryList.setAdapter(mHistoryAdapter);
        }
        boolean hasText = mSearchKeyEDT != null && !TextUtils.isEmpty(mSearchKeyEDT.getText());
        updateSectionsVisibility(hasText);
    }

    public void hideKeyboard(){

    }

    public void setData(ArrayList<AppInfo> arrayList){
        // Lưới gợi ý (text rỗng) — hành vi cũ.
        mSearchViewAdapter = new SearchViewAdapter(mLauncher,arrayList);
        mSearchViewAdapter.setSuggestionLimit(SUGGESTION_DEFAULT_LIMIT);
        mSuggestionRV.setAdapter(mSearchViewAdapter);
        // Gợi ý DỌC (khi đang gõ): cùng top-app, giới hạn 4 (đồng bộ "Xem thêm" với lưới).
        mSuggestionVerticalAdapter = new SearchViewAdapter(mLauncher, arrayList, R.layout.search_item_row);
        mSuggestionVerticalAdapter.setSuggestionLimit(SUGGESTION_DEFAULT_LIMIT);
        if (mSuggestionVerticalList != null) mSuggestionVerticalList.setAdapter(mSuggestionVerticalAdapter);
        // Kết quả app khớp (khi gõ): adapter dọc, bị filter theo từ khóa.
        mResultAdapter = new SearchViewAdapter(mLauncher, arrayList, R.layout.search_item_row);
        if (mResultAppList != null) mResultAppList.setAdapter(mResultAdapter);
    }

    @Override
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        return !isFocusOn && isFocusable() && mSearchKeyEDT.requestFocus(direction, previouslyFocusedRect);
    }

    @Override
    protected void onDetachedFromWindow() {
        // Phòng khi view bị gỡ khi search đang mở (recreate...): gỡ cơ chế bám bàn phím + hiện lại
        // status bar nếu chính search đang giữ ẩn, tránh kẹt trạng thái.
        detachKbTracking();
        if (mSearchStatusBarHidden) {
            try {
                mLauncher.setStatusBarHiddenForSearch(false);
            } catch (Throwable t) {
                // launcher có thể đã detached; bỏ qua.
            }
            mSearchStatusBarHidden = false;
        }
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
        // Nạp dự phòng danh sách app cho lưới Gợi ý (phòng bind bị bỏ lỡ -> lưới rỗng).
        mLauncher.ensureSearchViewApps();
        setVisibility(View.VISIBLE);
        mState = SearchViewState.OPENING;
        setAlpha(0.0f);
        mSuggestionRV.setVisibility(View.VISIBLE);
        mSearchKeyEDT.addTextChangedListener(mSearchKeywordTextWatcher);
        if (mSearchViewLayoutDelegate != null) {
            mSearchViewLayoutDelegate.onSearchViewOpened();
        }

        // Mỗi lần mở: đưa gợi ý về mặc định 4 (lưới + dọc), reset trạng thái kết quả, làm mới Lịch sử,
        // dựng đúng bố cục, rồi BẬT bàn phím ngay (không chờ spring-end).
        if (mSearchViewAdapter != null) {
            mSearchViewAdapter.setSuggestionLimit(SUGGESTION_DEFAULT_LIMIT);
        }
        if (mSuggestionVerticalAdapter != null) {
            mSuggestionVerticalAdapter.setSuggestionLimit(SUGGESTION_DEFAULT_LIMIT);
        }
        mHasResult = false;
        refreshHistory();
        updateSectionsVisibility(false); // về trạng thái "chưa gõ": lưới hiện, kết quả/dọc ẩn
        enterSearchSoftInputMode();
        showKeyboardCompat();
    }
}
