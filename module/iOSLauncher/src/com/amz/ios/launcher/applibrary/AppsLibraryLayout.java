package com.amz.ios.launcher.applibrary;


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
import com.amz.ios.launcher.AppInfo;
import com.amz.ios.launcher.DeviceProfile;
import com.amz.ios.launcher.ExtendedEditText;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.bounce.BouncyRecyclerView;
import com.amz.ios.launcher.bounce.OnOverPullListener;
import com.amz.ios.launcher.model.AppNameComparator;

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
    public ArrayList<AppCategory> mCategories = new ArrayList<>();
    public AppLibraryAdapter mAppLibraryAdapter = new AppLibraryAdapter();

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
    }

    void setUpAdapter(){
        mTotalLibraryRV.setLayoutManager(new GridLayoutManager(this.getContext(),2));
        mTotalLibraryRV.setAdapter(null);
        mSearchResultRV.setLayoutManager(new LinearLayoutManager(this.getContext()));
    }

    public void setApps(ArrayList<AppInfo> apps){
        mAppLibraryAdapter.mCategories = new SortAppsCallable(this,apps,0).call();
        mAppLibraryAdapter.notifyDataSetChanged();
        mTotalLibraryRV.setAdapter(mAppLibraryAdapter);
    }

    /**
     * Đảm bảo App Library luôn sẵn sàng hiển thị app mỗi khi được mở (vuốt qua page cuối).
     *
     * Gọi từ {@link com.amz.ios.launcher.Launcher#onAppsLibraryOpened()}. Xử lý các trường hợp
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
        if ((categoriesEmpty || adapterMissing) && apps != null && !apps.isEmpty()) {
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
        this.mSearchResultRV.setPadding(mDeviceProfile.edgeMarginPx, paddingTop, mDeviceProfile.edgeMarginPx, paddingBottom);
        LayoutParams params = (LayoutParams) this.mSearchAppBoxLibrary.getLayoutParams();
        ((MarginLayoutParams) params).rightMargin = margin;
        ((MarginLayoutParams) params).leftMargin = margin;

    }

    @Override
    public void onTransitionStarted(MotionLayout motionLayout, int startId, int endId) {
        if (endId == R.id.apps_library_end) {
            this.mSearchResultRV.setVisibility(View.VISIBLE);
        }
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
