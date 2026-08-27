package com.amz.ios.launcher.leftpage.views;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.amz.ios.launcher.DeviceProfile;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.bounce.BouncyRecyclerView;
import com.amz.ios.launcher.bounce.OnOverPullListener;
import com.amz.ios.launcher.leftpage.custom.CustomZoomButton;
import com.amz.ios.launcher.leftpage.widgets.AppSuggestionWidget;
import com.amz.ios.launcher.leftpage.widgets.WidgetBaseLayout;
import com.amz.ios.launcher.leftpage.adapter.CustomContentWidgetAdapter;
import com.amz.ios.launcher.leftpage.adapter.WrapStaggeredGridLayoutManager;
import com.amz.ios.launcher.leftpage.database.WidgetInfo;
import com.amz.ios.launcher.slideup.SlidingUpPanelLayout;
import com.github.mmin18.widget.RealtimeBlurView;

import java.util.ArrayList;
import java.util.Arrays;

public class CustomContentView extends ConstraintLayout implements View.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    public CustomZoomButton mAppWidgetsAddBtn;
    public int mMargin;
    public CustomZoomButton mAppWidgetEditDoneBtn;
    public CustomZoomButton mEditDoneTopBtn;
    public BouncyRecyclerView mListWidgetRV;
    public SlidingUpWidgetsAppStyle mSlidingUpWidgetsAppStyle;
    public SlidingUpWidgetsList mSlidingUpWidgetsList;
    public View mSearchBoxLeftPage;
    public RealtimeBlurView mSearchBoxRealtimeBlurView;
    public Launcher mLauncher;
    public ArrayList<WidgetInfo> mWidgetInfoList = new ArrayList<>();
    public CustomContentWidgetAdapter mWidgetListAdapter;
    public boolean t;

    public class WidgetsAppStyleSlideListener implements SlidingUpPanelLayout.PanelSlideListener {
        @Override
        public void onPanelSlide(View panel, float slideOffset) {
            if(mSlidingUpWidgetsList == null) return;
            View dragView = mSlidingUpWidgetsList.getDragView();
            if (dragView == null) {return;}
            float f = 1.0f - (slideOffset * 0.1f);
            dragView.animate().scaleX(f).scaleY(f).setDuration(0L).setInterpolator(new DecelerateInterpolator()).start();
        }

        @Override
        public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
            if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED){
                collapseAppsStyle();
            }
        }
    }

    public class WidgetsListSlideListener extends SlidingUpPanelLayout.SimplePanelSlideListener {

        @Override
        public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
            if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED){
                collapseWidgetList();
            }
            else if (newState == SlidingUpPanelLayout.PanelState.EXPANDED){
                stopShakingWidgets();
                disableButtons();
            }
            else if (newState == SlidingUpPanelLayout.PanelState.DRAGGING){
                startShaking();
                enableButtons();
            }
        }
    }

    public CustomContentView(Context context) {
        super(context);
    }

    public CustomContentView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0 );
    }

    public CustomContentView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (getContext() instanceof Launcher){
            mLauncher = (Launcher) getContext();
        }
        this.mMargin = this.mLauncher.getDeviceProfile().edgeMarginPx * 2;

        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.layout_custom_content,(ViewGroup)this,true);

    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // Màn trái TRONG SUỐT: nền frosted do mSliderBlurBg (blur_apps_library_background)
        // lo — nó fade dần theo changeBlur() khi vuốt, GIỐNG App Library. Nếu CustomContentView
        // tự tô nền đặc thì lúc kéo sẽ trượt/pop đè lên -> mất animation fade. Để trong suốt.
        setBackground(null);

        this.mSearchBoxLeftPage = findViewById(R.id.search_box_left_page);
        this.mListWidgetRV = findViewById(R.id.recycler_view_list_widget);
        this.mSlidingUpWidgetsAppStyle = findViewById(R.id.left_page_sliding_up_widgets_app_style);
        this.mSlidingUpWidgetsList = findViewById(R.id.left_page_sliding_up_widgets_list);
        this.mSearchBoxRealtimeBlurView = findViewById(R.id.realtime_blur_search_box_custom_content);
        this.mAppWidgetEditDoneBtn = findViewById(R.id.add_widgets_done);
        this.mAppWidgetsAddBtn = findViewById(R.id.add_widgets);
        this.mEditDoneTopBtn = findViewById(R.id.edit_done_top);

        setUpView(mMargin / 2);
        setupListeners();
        setAdapter();
        getWidgetsFromDB();
        if (mWidgetInfoList == null || mWidgetInfoList.size() == 0)
            getSampleWidgets();
    }

    void setUpView(int i){

        DeviceProfile deviceProfile = mLauncher.getDeviceProfile();
        int height = deviceProfile.getCurrentHeight();
        int paddingTop = this.mListWidgetRV.getPaddingTop();
        ((ViewGroup.MarginLayoutParams) ((ConstraintLayout.LayoutParams) this.mListWidgetRV.getLayoutParams())).height = height;
        ((ViewGroup.MarginLayoutParams) ((ConstraintLayout.LayoutParams) this.mSlidingUpWidgetsList.getLayoutParams())).height = height;
        ((ViewGroup.MarginLayoutParams) ((ConstraintLayout.LayoutParams) this.mSlidingUpWidgetsAppStyle.getLayoutParams())).height = height;

        int padding = i / 2;
        // [BUG FIX] Màn TRÁI (negative page) bị dán SÁT MÉP TRÊN, widget đầu đè lên status bar.
        // Nguyên nhân: layout dựa vào android:fitsSystemWindows="true" để framework chừa status bar,
        // NHƯNG cửa sổ launcher bật FLAG_LAYOUT_NO_LIMITS (Launcher.hideNavigationBar) -> cửa sổ tràn
        // ra ngoài mọi system bar -> framework báo inset top = 0 (đo trên Samsung A50 Android 9) ->
        // fitsSystemWindows không chừa gì cả. Desktop KHÔNG dính vì nó tự chừa bằng
        // layout_marginTop="@dimen/status_bar_heightex" thay vì dựa vào inset sống.
        //
        // Sửa cùng cách với desktop: cộng chiều cao status bar CỐ ĐỊNH vào padding đỉnh.
        // Đặt TẠI ĐÂY (không phải nơi khác) vì setUpView() là chỗ CUỐI ghi padding cho recycler —
        // set ở chỗ khác sẽ bị dòng này ghi đè. Chỉ đụng recycler màn trái -> không ảnh hưởng màn khác.
        int statusBarPadding = getResources().getDimensionPixelSize(R.dimen.status_bar_heightex);
        this.mListWidgetRV.setPadding(padding, paddingTop + padding + statusBarPadding, padding, padding);
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) this.mSearchBoxLeftPage.getLayoutParams();
        ((ViewGroup.MarginLayoutParams) params).rightMargin = i;
        ((ViewGroup.MarginLayoutParams) params).leftMargin = i;
    }

    void setupListeners(){
        mListWidgetRV.setOnOverPullListener(
                new OnOverPullListener() {
                    @Override
                    public void onOverPulledTop(float deltaDistance) {
                        if (mSearchBoxRealtimeBlurView != null)
                            mSearchBoxRealtimeBlurView.invalidate();
                    }

                    @Override
                    public void onOverPulledBottom(float deltaDistance) {
                        if (mSearchBoxRealtimeBlurView != null)
                            mSearchBoxRealtimeBlurView.invalidate();
                    }

                    @Override
                    public void onRelease() {

                    }
                }
        );

        mSlidingUpWidgetsList.addPanelSlideListener(new WidgetsListSlideListener());

        // Chạm vào vùng TRỐNG của list (không trúng widget nào) khi đang ở chế độ edit
        // -> thoát edit, giống bấm "Done" ở màn app. Dùng OnItemTouchListener để không
        // ảnh hưởng tới cuộn / kéo-thả (chỉ bắt tap ngắn, ít di chuyển, không trúng child).
        mListWidgetRV.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            float downX, downY;

            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull android.view.MotionEvent e) {
                switch (e.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        downX = e.getX();
                        downY = e.getY();
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                        if (t) {
                            float slop = android.view.ViewConfiguration.get(getContext()).getScaledTouchSlop();
                            boolean isTap = Math.abs(e.getX() - downX) < slop
                                    && Math.abs(e.getY() - downY) < slop;
                            if (isTap && rv.findChildViewUnder(e.getX(), e.getY()) == null) {
                                exitEditMode();
                            }
                        }
                        break;
                }
                return false;
            }
        });

        mAppWidgetsAddBtn.setOnClickListener(this);
        mAppWidgetEditDoneBtn.setOnClickListener(this);
        mEditDoneTopBtn.setOnClickListener(this);
    }

    // Thoát chế độ edit: dừng rung + ẩn badge xoá + ẩn nút Add/Done (giống bấm Done).
    public void exitEditMode(){
        stopShakingWidgets();
        disableButtons();
    }

    void setAdapter(){

        mWidgetInfoList = new ArrayList<>();

        mWidgetListAdapter = new CustomContentWidgetAdapter(this,mWidgetInfoList);
        WrapStaggeredGridLayoutManager manager = new WrapStaggeredGridLayoutManager(
                2,StaggeredGridLayoutManager.VERTICAL
        );
        manager.assertNotInLayoutOrScroll(null);
        mListWidgetRV.setLayoutManager(manager);
        mListWidgetRV.setAdapter(mWidgetListAdapter);

        mListWidgetRV.addItemDecoration(
                new RecyclerView.ItemDecoration() {
                    @Override
                    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                        super.getItemOffsets(outRect, view, parent, state);
                        outRect.right = mMargin / 2;
                        outRect.top = mMargin / 2;
                        outRect.bottom = mMargin / 2;
                        outRect.left = mMargin / 2;
                    }
                }
        );
        manager.requestLayout();
    }

    public void getWidgetsFromDB(){
        mWidgetInfoList.clear();
        mWidgetInfoList.addAll(WidgetInfo.getWidgets());
        mWidgetListAdapter.notifyDataSetChanged();
    }

    public void getSampleWidgets(){
        ArrayList<String > arrayList = new ArrayList<>();
        int i = -1;
        SharedPreferences sharedPreferences = mLauncher.getSharedPreferences(
                "list_widget",
                Context.MODE_PRIVATE
        );
        try {
            String h = sharedPreferences.getString("list_choose_widget", null);
            arrayList = h != null ? new ArrayList<>(Arrays.asList(TextUtils.split(h, "‚‗‚"))) : new ArrayList<>(Arrays.asList("widget_calendar", "widget_battery", "widget_suggestion"));
        } catch (Throwable unused) {
            arrayList = new ArrayList<>(Arrays.asList("widget_weather", "widget_battery", "widget_suggestion"));
        }
        for (String next : arrayList) {
            next.getClass();
            char c = 65535;
            switch (next.hashCode()) {
                case -1005835502:
                    if (next.equals("widget_battery")) {
                        c = 0;
                        break;
                    }
                    break;
                case 384048857:
                    if (next.equals("widget_calendar")) {
                        c = 1;
                        break;
                    }
                    break;
                case 472806111:
                    if (next.equals("widget_suggestion")) {
                        c = 2;
                        break;
                    }
                    break;
                case 548844793:
                    if (next.equals("widget_weather")) {
                        c = 3;
                        break;
                    }
                    break;
                case 1613163831:
                    if (next.equals("widget_favorite")) {
                        c = 4;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    // battery: dùng loại 2x2 (half-span) thay vì 60 (full-span) để
                    // không bị phóng thành khối vuông full-width khổng lồ.
                    i = 61;
                    break;
                case 1:
                    // Mặc định "widget_calendar" = Lịch LƯỚI THÁNG (đủ ngày) 2x2, không phải bản
                    // chỉ hiện ngày (40). Đồng bộ với thẻ Lịch nổi bật ở khay widget.
                    i = 42;
                    break;
                case 2:
                    i = 71;
                    break;
                case 4:
                    i = 51;
                    break;
                default:
            }
            if (i != -1)
                addWidget(i);
        }
        arrayList.clear();
    }

    public final void addWidget(int i) {
        final WidgetInfo info = new WidgetInfo();
        info.order = mWidgetInfoList.size() + 1;
        info.type = i;
        this.mWidgetInfoList.add(info);
        info.save();
        // Item vừa được thêm vào cuối list => vị trí insert là (size - 1), KHÔNG phải order+1
        // (order+1 vượt quá số item, gây RecyclerView "Inconsistency detected" -> crash).
        mWidgetListAdapter.notifyItemInserted(mWidgetInfoList.size() - 1);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ic_search || id == R.id.search_box_left_page) {
            ((Activity) this.mLauncher).startSearch("", false, null, true);
        }
        if (v == mEditDoneTopBtn){
            // Nút Done góc trên phải: chỉ thoát edit (không đụng tới widget list panel).
            exitEditMode();
        }
        else if (v == mAppWidgetEditDoneBtn){
            stopShakingWidgets();
            disableButtons();
            closeWidgetList();
        }
        else if (v == mAppWidgetsAddBtn){
            expandWidgetList();
        }
    }

    public final void stopShakingWidgets() {
        if (this.mWidgetListAdapter != null) {
            this.t = false;
            for (int i = 0; i < this.mListWidgetRV.getChildCount(); i++) {
                View childAt = this.mListWidgetRV.getChildAt(i);
                if (childAt != null && this.mWidgetListAdapter != null) {
                    RecyclerView.ViewHolder viewHolder = this.mListWidgetRV.getChildViewHolder(childAt);
                    if (viewHolder instanceof CustomContentWidgetAdapter.LauncherWidgetListViewHolder) {
                        ((CustomContentWidgetAdapter.LauncherWidgetListViewHolder) viewHolder).stopShaking();
                    }
                }
            }
            this.mListWidgetRV.setLongPressDragEnabled(false);
        }
    }

    public final void disableButtons(){
        this.mAppWidgetEditDoneBtn.setVisibility(GONE);
        this.mAppWidgetsAddBtn.setVisibility(GONE);
        if (this.mEditDoneTopBtn != null) this.mEditDoneTopBtn.setVisibility(GONE);
    }

    public final void enableButtons() {
        this.mAppWidgetEditDoneBtn.setVisibility(VISIBLE);
        this.mAppWidgetsAddBtn.setVisibility(VISIBLE);
        if (this.mEditDoneTopBtn != null) this.mEditDoneTopBtn.setVisibility(VISIBLE);
    }

    public final void startShaking() {
        if (this.mWidgetListAdapter != null) {
            this.t = true;
            for (int i = 0; i < this.mListWidgetRV.getChildCount(); i++) {
                View childView = this.mListWidgetRV.getChildAt(i);
                if (childView != null) {
                    RecyclerView.ViewHolder viewHolder = this.mListWidgetRV.getChildViewHolder(childView);
                    if (viewHolder instanceof CustomContentWidgetAdapter.LauncherWidgetListViewHolder) {
                        ((CustomContentWidgetAdapter.LauncherWidgetListViewHolder) viewHolder).startShaking();
                    }
                }
            }
            this.mListWidgetRV.setLongPressDragEnabled(true);
        }
    }

    public void onOpenPage(){
        // KHÔNG tự tô nền ở đây: nền frosted do mSliderBlurBg lo (fade khi vuốt, giống
        // App Library). Giữ màn trái trong suốt để không "pop" đè lên lúc mở hẳn.
        setBackground(null);
        if(mListWidgetRV == null) return;
        int childCount = mListWidgetRV.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = mListWidgetRV.getChildAt(i);
            if (childAt instanceof WidgetBaseLayout) {
                ((WidgetBaseLayout) childAt).setAppSuggestionViewMargin();
            }
        }
    }

    /**
     * Nạp lại khung app gợi ý (app mở gần nhất).
     *
     * Gọi từ Launcher.onRecentChange() — AppUsagesModel phát callback đó sau mỗi lần người dùng mở
     * app, nên danh sách luôn khớp thứ tự dùng thật mà không cần cơ chế theo dõi riêng.
     * Cùng khuôn duyệt con với onOpenPage().
     */
    public void reloadAppSuggestions() {
        if (mListWidgetRV == null) return;
        int childCount = mListWidgetRV.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = mListWidgetRV.getChildAt(i);
            if (childAt instanceof AppSuggestionWidget) {
                AppSuggestionWidget.reloadSuggestionApps((AppSuggestionWidget) childAt);
            }
        }
    }

    public void onClosePage(){
        if(mListWidgetRV == null) return;
        int childCount = mListWidgetRV.getChildCount();
        for (int i = 0 ; i < childCount; i++){
            View childAt = mListWidgetRV.getChildAt(i);
            if (childAt instanceof WidgetBaseLayout){
                ((WidgetBaseLayout) childAt).r();
            }
        }
        mListWidgetRV.smoothScrollToPosition(0);
        disableButtons();
        stopShakingWidgets();
        collapseWidgetList();
    }

    public void collapseAppsStyle(){
        if (mSlidingUpWidgetsAppStyle == null) return;
        mSlidingUpWidgetsAppStyle.postOnAnimation(
                new Runnable() {
                    @Override
                    public void run() {
                        closeAppsStyle();
                    }
                }
        );
    }

    public void collapseWidgetList(){
        if (mSlidingUpWidgetsList != null) {
            mSlidingUpWidgetsList.postOnAnimation(
                    new Runnable() {
                        @Override
                        public void run() {
                            closeWidgetList();
                        }
                    }
            );
        }
    }

    public void closeAppsStyle(){
        mSlidingUpWidgetsAppStyle.clearFocus();
        mSlidingUpWidgetsAppStyle.setVisibility(View.GONE);
        mSlidingUpWidgetsAppStyle.setPanelStateInternal(SlidingUpPanelLayout.PanelState.COLLAPSED);
        mSlidingUpWidgetsAppStyle.close();
    }

    public void closeWidgetList(){
        if (mSlidingUpWidgetsAppStyle.isShown()) {
            collapseAppsStyle();
        }
        mSlidingUpWidgetsList.clearFocus();
        mSlidingUpWidgetsList.setVisibility(View.GONE);
        mSlidingUpWidgetsList.setPanelStateInternal(SlidingUpPanelLayout.PanelState.COLLAPSED);
        mSlidingUpWidgetsList.close();
        if (mSlidingUpWidgetsList == null) {
            return;
        }

        View dragView = mSlidingUpWidgetsList.getDragView();
        if (dragView == null) return;

        dragView.setScaleX(1.0f);
        dragView.setScaleY(1.0f);
    }

    public void expandWidgetList(){
        if (mSlidingUpWidgetsList == null) return;
        mSlidingUpWidgetsList.postOnAnimation(
                new Runnable() {
                    @Override
                    public void run() {
                        openWidgetList();
                    }
                }
        );
    }

    public void openWidgetList(){
        mSlidingUpWidgetsList.requestFocus();
        mSlidingUpWidgetsList.setVisibility(VISIBLE);
        mSlidingUpWidgetsList.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
        stopShakingWidgets();
        disableButtons();
    }

}
