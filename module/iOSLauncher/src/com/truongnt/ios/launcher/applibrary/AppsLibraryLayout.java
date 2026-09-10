package com.truongnt.ios.launcher.applibrary;


import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.mmin18.widget.RealtimeBlurView;
import com.truongnt.ios.launcher.AppInfo;
import com.truongnt.ios.launcher.DeviceProfile;
import com.truongnt.ios.launcher.ExtendedEditText;
import com.truongnt.ios.launcher.Launcher;
import com.truongnt.ios.launcher.R;
import androidx.recyclerview.widget.RecyclerView;
import com.truongnt.ios.launcher.bounce.BouncyRecyclerView;
import com.truongnt.ios.launcher.bounce.OnOverPullListener;
import com.truongnt.ios.launcher.model.AppNameComparator;

import java.util.ArrayList;

public class AppsLibraryLayout extends MotionLayout implements MotionLayout.TransitionListener {

    public static final int CATEGORY_APPS_GAME = 1;
    public static final int CATEGORY_APPS_AUDIO = 2;
    public static final int CATEGORY_APPS_VIDEO = 3;
    public static final int CATEGORY_APPS_PHOTO = 4;
    public static final int CATEGORY_APPS_SOCIAL = 5;
    public static final int CATEGORY_APPS_NEWS = 6;
    public static final int CATEGORY_APPS_MAPS = 7;
    public static final int CATEGORY_APPS_PRODUCTIVITY = 8;
    public static final int CATEGORY_APPS_OTHERS = 9;

    public Launcher mLauncher;
    public DeviceProfile mDeviceProfile;
    public BouncyRecyclerView mTotalLibraryRV;
    public BouncyRecyclerView mSearchResultRV;
    public RealtimeBlurView mSearchBlurView;
    public FrameLayout mLayoutAppsLibrary;
    public View mSearchAppBoxLibrary;
    public Handler mHandler;
    public boolean flag;
    public InputMethodManager mInputMethodManager;
    public Handler.Callback mSearchCallback;
    public TextWatcher mSearchWordWatcher;
    public ExtendedEditText mSearchWordET;
    public AppNameComparator mAppNameComparator;
    public SearchResultAdapter mSearchResultAdapter;
    /** Thanh chữ cái A-Z mép phải list kết quả search (chạm/vuốt để nhảy nhóm). */
    public AlphabetIndexBar mIndexBar;
    public ArrayList<AppCategory> mCategories = new ArrayList<>();
    public AppLibraryAdapter mAppLibraryAdapter = new AppLibraryAdapter();
    // Số app của lần build category gần nhất — để ensureReady biết cần rebuild khi nội dung đổi
    // (không chỉ dựa vào size()==10, vì category rỗng vẫn giữ size()==10 -> bỏ sót rebuild).
    private int mLastBuiltAppCount = -1;

    public AppsLibraryLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public AppsLibraryLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        config();
        setUpView();
        setSubViewsLayoutParams();
        setUpListeners();
        setUpAdapter();
    }

    void config(){
        setX(0.0f);
        setY(0.0f);
        setTranslationX(0.0f);
        setTranslationY(0.0f);
        setPadding(0, 0, 0, 0);
        mAppNameComparator = new AppNameComparator(getContext());
    }

    void setUpView(){

        Context context = getContext();
        mLauncher = Launcher.getLauncher(context);
        mDeviceProfile = mLauncher.getDeviceProfile();

        LayoutInflater.from(context).inflate(R.layout.apps_library_layout, (ViewGroup) this, true);
        mSearchAppBoxLibrary = findViewById(R.id.search_box_apps_library);
        mLayoutAppsLibrary = findViewById(R.id.layout_list_apps_library);
        mSearchBlurView = findViewById(R.id.realtime_blur_search_box);
        mSearchBlurView.setOverlayColor(Color.TRANSPARENT);
        mSearchBlurView.setDownsampleFactor(8);
        mSearchBlurView.setBlurRadius(25);

        mSearchResultRV = findViewById(R.id.apps_library_search_view);
        mTotalLibraryRV = findViewById(R.id.list_apps_library);
        mIndexBar = findViewById(R.id.apps_library_index_bar);
        setUpIndexBar();
        mSearchWordET = findViewById(R.id.et_search);
        mInputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    void setUpListeners(){

        mSearchCallback = new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                try {
                    if (mSearchResultAdapter != null) {
                        mSearchResultAdapter.mFilter.filter((String) msg.obj);
                        return true;
                    }
                    return true;
                } catch (Throwable th) {
                    th.getMessage();
                    return true;
                }
            }
        };

        mHandler = new Handler(Looper.getMainLooper(), mSearchCallback);

        mSearchWordWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                AppsLibraryLayout appsLibraryLayout = AppsLibraryLayout.this;
                if (appsLibraryLayout.mHandler != null) {
                    appsLibraryLayout.mHandler.removeCallbacksAndMessages(null);
                    Message message = new Message();
                    message.obj = s.toString();
                    AppsLibraryLayout.this.mHandler.sendMessage(message);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        setTransitionListener(this);

        mTotalLibraryRV.setOnOverPullListener(new OnOverPullListener() {
            @Override
            public void onOverPulledTop(float deltaDistance) {
                mSearchBlurView.invalidate();
            }

            @Override
            public void onOverPulledBottom(float deltaDistance) {
                mSearchBlurView.invalidate();
            }

            @Override
            public void onRelease() {
                mSearchBlurView.invalidate();
            }
        });

        // RealtimeBlurView không tự vẽ lại khi list con cuộn (chỉ invalidate lúc overpull). Vì vậy khi
        // cuộn list search, dải kính giữ khung cũ -> icon app trượt dưới thanh search hiện ra "xuyên
        // thấy". Ép blur vẽ lại theo từng frame cuộn để luôn frost đúng nội dung -> hết xuyên thấy,
        // KHÔNG cần đổi màu nền.
        RecyclerView.OnScrollListener blurInvalidator = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                mSearchBlurView.invalidate();
            }
        };
        mTotalLibraryRV.addOnScrollListener(blurInvalidator);
        mSearchResultRV.addOnScrollListener(blurInvalidator);

        mSearchWordET.setOnTouchListener(
            new OnTouchListener(){
                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    AppsLibraryLayout appsLibraryLayout = AppsLibraryLayout.this;
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (appsLibraryLayout.flag) {
                            appsLibraryLayout.transitionToStart();
                        } else {
                            appsLibraryLayout.transitionToEnd();
                        }
                    }
                    return true;
                }
            }
        );

        mSearchWordET.addTextChangedListener(mSearchWordWatcher);

        // Bấm vào CẢ KHUNG search (nền ô) cũng mở search, không chỉ mỗi chữ hint "App Library".
        // et_search ở start là wrap_content nên vùng chạm cũ chỉ ôm sát chữ; khung nền phủ hết bề rộng.
        mSearchAppBoxLibrary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!flag) {
                    transitionToEnd();
                }
            }
        });

        setUpPullDownToSearch();
    }

    /**
     * Quãng vuốt tối thiểu (px) để coi là "kéo xuống mở search".
     * [CĂN CHỈNH] 120 -> 40: mức cũ phải kéo khá dài mới ăn, cảm giác ì. 40px đủ để phân biệt với
     * chạm nhẹ/rung tay nhưng phản hồi gần như tức thì.
     */
    private static final int PULL_DOWN_TO_SEARCH_THRESHOLD_PX = 40;

    /**
     * Kéo xuống ở ĐỈNH lưới App Library -> mở ô search (kiểu iOS).
     *
     * Chỉ nhận khi list ĐANG Ở ĐỈNH (không cuộn được lên nữa) để không cướp thao tác cuộn thường:
     * đang xem giữa list mà vuốt xuống thì vẫn là cuộn lên bình thường.
     *
     * Dùng OnTouchListener chứ không phải OnScrollListener: khi list đã ở đỉnh, RecyclerView
     * không sinh sự kiện cuộn nữa (chỉ có hiệu ứng nảy của BouncyRecyclerView), nên phải đọc
     * thẳng quãng di chuyển của ngón tay. Trả về false ở mọi nhánh để KHÔNG nuốt sự kiện —
     * cuộn/bấm item vẫn hoạt động như cũ.
     */
    private void setUpPullDownToSearch() {
        if (mTotalLibraryRV == null) {
            return;
        }
        mTotalLibraryRV.setOnTouchListener(new OnTouchListener() {
            private float mDownY = -1f;
            private boolean mTriggered;

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        mDownY = event.getY();
                        mTriggered = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (mTriggered || flag) {
                            break;
                        }
                        // canScrollVertically(-1) = còn cuộn LÊN được -> chưa ở đỉnh.
                        if (mTotalLibraryRV.canScrollVertically(-1)) {
                            // Chưa tới đỉnh: xoá mốc đo. Nhờ vậy khi list vừa CHẠM đỉnh, quãng kéo
                            // được tính LẠI từ đúng thời điểm đó — không cộng dồn phần đã cuộn
                            // trước đó (trước đây phải kéo thêm rất dài mới ăn, cảm giác ì).
                            mDownY = -1f;
                            break;
                        }
                        if (mDownY < 0f) {
                            mDownY = event.getY();   // vừa tới đỉnh -> đặt mốc đo tại đây
                            break;
                        }
                        if (event.getY() - mDownY >= PULL_DOWN_TO_SEARCH_THRESHOLD_PX) {
                            mTriggered = true;   // chỉ mở MỘT lần cho mỗi lần chạm
                            transitionToEnd();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        mDownY = -1f;
                        mTriggered = false;
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
    }

    void setUpAdapter(){
        mTotalLibraryRV.setLayoutManager(new GridLayoutManager(this.getContext(),2));
        mTotalLibraryRV.setAdapter(null);
        mSearchResultRV.setLayoutManager(new LinearLayoutManager(this.getContext()));
    }

    /**
     * Gắn xử lý chạm cho thanh chữ cái: chọn chữ nào thì cuộn list kết quả tới đúng nhóm đó.
     * Dùng scrollToPositionWithOffset để header chữ cái nằm SÁT ĐỈNH vùng nhìn thấy.
     */
    void setUpIndexBar() {
        if (mIndexBar == null) {
            return;
        }
        mIndexBar.setOnLetterSelectedListener(new AlphabetIndexBar.OnLetterSelectedListener() {
            @Override
            public void onLetterSelected(String letter) {
                if (mSearchResultAdapter == null || mSearchResultRV == null) {
                    return;
                }
                int pos = mSearchResultAdapter.findPositionForLetter(letter);
                if (pos < 0) {
                    return;
                }
                RecyclerView.LayoutManager lm = mSearchResultRV.getLayoutManager();
                if (lm instanceof LinearLayoutManager) {
                    ((LinearLayoutManager) lm).scrollToPositionWithOffset(pos, 0);
                } else {
                    mSearchResultRV.scrollToPosition(pos);
                }
            }
        });
    }

    /**
     * Nạp danh sách chữ cái cho thanh A-Z theo adapter hiện tại, và đăng ký cập nhật lại mỗi khi
     * người dùng gõ tìm kiếm (kết quả lọc đổi -> số nhóm chữ đổi theo).
     */
    public void bindIndexBarToAdapter() {
        if (mIndexBar == null || mSearchResultAdapter == null) {
            return;
        }
        mIndexBar.setLetters(mSearchResultAdapter.getSectionLetters());
        mSearchResultAdapter.setOnResultsChangedListener(
                new SearchResultAdapter.OnResultsChangedListener() {
                    @Override
                    public void onResultsChanged() {
                        // LUÔN cập nhật chữ (không phụ thuộc flag): kết quả có thể đổi trước khi
                        // hiệu ứng vào màn search chạy xong, chờ flag=true sẽ bỏ lỡ lần nạp đó.
                        if (mIndexBar == null || mSearchResultAdapter == null) {
                            return;
                        }
                        mIndexBar.setLetters(mSearchResultAdapter.getSectionLetters());
                        // Đang ở màn search thì đồng bộ luôn ẩn/hiện theo việc còn chữ hay không
                        // (gõ lọc tới mức không còn nhóm nào -> thanh tự ẩn).
                        if (flag) {
                            mIndexBar.setVisibility(
                                    mIndexBar.hasLetters() ? View.VISIBLE : View.GONE);
                        }
                    }
                });
    }

    public void setApps(ArrayList<AppInfo> apps){
        // SortAppsCallable.call() build lại mCategories (10 mục cố định) và trả về chính nó.
        // Đưa qua setCategories() để adapter lọc ẩn folder rỗng trước khi render.
        mAppLibraryAdapter.setCategories(new SortAppsCallable(this,apps,0).call());
        mLastBuiltAppCount = (apps != null) ? apps.size() : 0;
        mAppLibraryAdapter.notifyDataSetChanged();
        mTotalLibraryRV.setAdapter(mAppLibraryAdapter);
    }

    /**
     * Đảm bảo App Library luôn sẵn sàng hiển thị app mỗi khi được mở (vuốt qua page cuối).
     *
     * Gọi từ {@link com.truongnt.ios.launcher.Launcher#onAppsLibraryOpened()}. Xử lý các trường hợp
     * còn sót có thể khiến màn trống trơn:
     *   1. setApps() chưa từng chạy (bind bị defer/clear lúc pause) -> adapter chưa gắn / category
     *      rỗng: dựng lại từ danh sách app đã bind (apps).
     *   2. MotionLayout kẹt ở trạng thái search -> danh sách category bị alpha=0 / RV search che:
     *      ép về start và khôi phục alpha/visibility list.
     *
     * @param apps danh sách app đã bind gần nhất (Launcher.allApp); có thể null nếu chưa bind.
     */
    public void ensureReady(ArrayList<AppInfo> apps){
        boolean categoriesEmpty = (mCategories == null || mCategories.size() != 10);
        boolean adapterMissing = (mTotalLibraryRV.getAdapter() == null);
        // Rebuild cả khi số app đã đổi so với lần build trước: lần build đầu có thể chạy với danh
        // sách app thiếu (cold-load qua bindAppsAdded) -> mCategories vẫn size()==10 nhưng thiếu app,
        // trước đây guard cũ bỏ qua khiến folder trống dù model đã đủ app.
        boolean countChanged = (apps != null && apps.size() != mLastBuiltAppCount);
        if ((categoriesEmpty || adapterMissing || countChanged) && apps != null && !apps.isEmpty()) {
            setApps(apps);
        }
        // Khôi phục hiển thị danh sách category (phòng khi transition search để lại alpha=0).
        mTotalLibraryRV.setVisibility(View.VISIBLE);
        mTotalLibraryRV.setAlpha(1.0f);
    }

    public void setSubViewsLayoutParams(){
        int margin = mDeviceProfile.edgeMarginPx * 2;

        // paddingTop trong XML (= realtime_blur_height_search_view) đã chừa sẵn vùng dải kính blur phía
        // trên để nội dung nằm dưới dải kính. KHÔNG cộng thêm status_bar_heightex như fix cũ (7ba77952):
        // fix đó từng cần vì khi ấy 2 RecyclerView còn bật fitsSystemWindows và framework GHI ĐÈ padding
        // về 0 (cửa sổ bật FLAG_LAYOUT_NO_LIMITS -> inset top báo 0). Giờ fitsSystemWindows đã bỏ nên
        // padding XML có hiệu lực (102dp >= chiều cao status bar) — cộng thêm 32dp nữa chỉ tạo khoảng
        // trống giữa ô search và ô app/category đầu (cách ~36dp). Bỏ phần cộng để list bám sát dải kính.
        int paddingTop = this.mTotalLibraryRV.getPaddingTop();
        int paddingBottom = this.mTotalLibraryRV.getPaddingBottom();
        this.mLayoutAppsLibrary.setX(0.0f);
        this.mLayoutAppsLibrary.setY(0.0f);
        this.mLayoutAppsLibrary.setTranslationX(0.0f);
        this.mLayoutAppsLibrary.setTranslationY(0.0f);
        this.mLayoutAppsLibrary.setPadding(0, 0, 0, 0);
        ((MarginLayoutParams) ((LayoutParams) this.mLayoutAppsLibrary.getLayoutParams())).height = mDeviceProfile.getCurrentHeight();
        this.mTotalLibraryRV.setPadding(mDeviceProfile.edgeMarginPx, paddingTop, mDeviceProfile.edgeMarginPx, paddingBottom);
        // List kết quả chừa thêm chỗ bên PHẢI cho thanh chữ cái A-Z (đè lên mép phải), để tên app
        // dài không chạy xuống dưới thanh.
        // paddingBottom = 0: list kết quả KHÔNG cần chừa đáy (khoảng 60dp thừa hưởng từ layout
        // vốn dành cho lưới category) -> bỏ để không hở một dải trống dưới app cuối.
        int indexBarWidth = getResources().getDimensionPixelSize(R.dimen.apps_library_index_bar_width);
        this.mSearchResultRV.setPadding(mDeviceProfile.edgeMarginPx, paddingTop,
                mDeviceProfile.edgeMarginPx + indexBarWidth, 0);
        LayoutParams params = (LayoutParams) this.mSearchAppBoxLibrary.getLayoutParams();
        ((MarginLayoutParams) params).rightMargin = margin;
        ((MarginLayoutParams) params).leftMargin = margin;

    }

    @Override
    public void onTransitionStarted(MotionLayout motionLayout, int startId, int endId) {
        if (endId == R.id.apps_library_end) {
            this.mSearchResultRV.setVisibility(View.VISIBLE);
            // Vào màn search -> hiện thanh chữ cái ngay từ đầu hiệu ứng cho mượt.
            showIndexBarIfSearching();
        }
    }

    /**
     * Nạp chữ cái mới nhất rồi hiện thanh A-Z (ẩn nếu không có nhóm chữ nào).
     *
     * Gọi ở CẢ HAI mốc vào màn search (onTransitionStarted + onTransitionCompleted): mốc đầu cho
     * thanh xuất hiện sớm, mốc sau chốt lại phòng khi lúc đó adapter chưa sẵn sàng.
     * Gọi lặp vô hại vì chỉ đọc dữ liệu và set visibility.
     */
    private void showIndexBarIfSearching() {
        if (mIndexBar == null) {
            return;
        }
        if (mSearchResultAdapter != null) {
            mIndexBar.setLetters(mSearchResultAdapter.getSectionLetters());
        }
        mIndexBar.setVisibility(mIndexBar.hasLetters() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onTransitionChange(MotionLayout motionLayout, int startId, int endId, float progress) {
        if (endId == R.id.apps_library_start) {
            this.mTotalLibraryRV.setAlpha(progress);
            this.mSearchResultRV.setAlpha(1.0f - progress);
        } else if (endId == R.id.apps_library_end) {
            this.mTotalLibraryRV.setAlpha(1.0f - progress);
            this.mSearchResultRV.setAlpha(progress);
        }
    }

    @Override
    public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {

        if (currentId == R.id.apps_library_end) {
            this.flag = true;
            // Chốt lại việc hiện thanh chữ cái tại mốc CHẮC CHẮN này (onTransitionStarted có thể
            // chạy khi adapter chưa sẵn sàng, hoặc không nổ nếu vào màn search không qua hiệu ứng).
            showIndexBarIfSearching();
            mSearchWordET.requestFocus();
            if (this.mSearchWordET != null) {
                if (mInputMethodManager != null) {
                    mInputMethodManager.showSoftInput(mSearchWordET, InputMethodManager.SHOW_IMPLICIT);
                }

            }
        } else if (currentId == R.id.apps_library_start) {
            this.flag = false;
            mSearchResultRV.setVisibility(View.INVISIBLE);
            mSearchResultRV.setAlpha(0.0f);
            // Rời màn search -> ẩn thanh chữ cái (thanh chỉ dùng cho list kết quả).
            if (mIndexBar != null) {
                mIndexBar.setVisibility(View.GONE);
            }
            mSearchWordET.setText("");
            mSearchWordET.clearFocus();
            if (mSearchWordET == null || this.mInputMethodManager == null) {
                return;
            }
            mInputMethodManager.hideSoftInputFromWindow(this.mSearchWordET.getWindowToken(), 0);
        }
    }

    @Override
    public void onTransitionTrigger(MotionLayout motionLayout, int triggerId, boolean positive, float progress) {
    }

    public int getAppType(AppInfo info){

        if (info == null)
            return CATEGORY_APPS_OTHERS;

        ApplicationInfo appInfo = info.mApplicationInfo;
        ComponentName componentName = info.componentName;

        if (componentName == null)
            return CATEGORY_APPS_OTHERS;

        int customized = mLauncher.getIconCache().getCustomizedCategory(componentName);
        if (customized != 0)
            return customized;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int category = appInfo.category;
            return category + 1;
        }

        return CATEGORY_APPS_OTHERS;
    }

}
