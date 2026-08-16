package com.amz.ios.launcher.widget;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.amz.ios.ioslite.common.launcher.Insettable;
import com.amz.ios.launcher.CellLayout;
import com.amz.ios.launcher.DeleteDropTarget;
import com.amz.ios.launcher.DragController;
import com.amz.ios.launcher.DragLayer;
import com.amz.ios.launcher.DragSource;
import com.amz.ios.launcher.DropTarget;
import com.amz.ios.launcher.ExtendedEditText;
import com.amz.ios.launcher.folder.Folder;
import com.amz.ios.launcher.IconCache;
import com.amz.ios.launcher.ItemInfo;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.LauncherAppWidgetProviderInfo;
import com.amz.ios.launcher.PendingAddItemInfo;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.Utilities;
import com.amz.ios.launcher.WidgetPreviewLoader;
import com.amz.ios.launcher.Workspace;
import com.amz.ios.launcher.compat.AppWidgetManagerCompat;
import com.amz.ios.launcher.leftpage.widgets.WidgetBaseLayout;
import com.amz.ios.launcher.model.PackageItemInfo;
import com.amz.ios.launcher.model.WidgetsModel;
import com.amz.ios.launcher.slideup.SlidingUpPanelLayout;
import com.amz.ios.launcher.util.SpeedLinearLayoutManager;

import java.util.ArrayList;
import java.util.concurrent.Callable;

public class WidgetsContainerView extends SlidingUpPanelLayout implements
        DragSource,
        Insettable,
        View.OnLongClickListener,
        View.OnClickListener,
        WidgetAppListAdapter.IWidgetListAdapter
{

    DragLayer mDragLayer;
    Launcher mLauncher;
    WidgetPreviewLoader mWidgetPreviewLoader;
    RecyclerView mWidgetListRV;
    public WidgetAppListAdapter mWidgetListAdapter;
    AppCompatImageView mActionClearBtn;
    ExtendedEditText mSearchWidgetEDT;
    SpeedLinearLayoutManager mLayoutManager;
    Rect mRect;
    IconCache mIconCache;
    DragController mDragController;
    WidgetsModel mWidgetsModel;
    InputMethodManager mInputMethodManager;

    public class WidgetContainerViewListener extends SlidingUpPanelLayout.SimplePanelSlideListener {

        @Override
        public void onPanelStateChanged(View panel, PanelState previousState, PanelState newState) {
            if (newState == PanelState.COLLAPSED){
                if (mLauncher.mWidgetsAppStyle.isShown()) {
                    collapseAppStyle();
                }
                mLauncher.closeWidgetView(true);
                View dragView = getDragView();
                if (dragView != null) {
                    dragView.setScaleX(1.0f);
                    dragView.setScaleY(1.0f);
                }
            }
            else if (newState == PanelState.EXPANDED){
                mLauncher.cancelShakingAnimation();
            }
            else if (newState == PanelState.DRAGGING){
                mLauncher.onShakingAllApps();
            }
        }
    }

    public static class WidgetsAppCellStyleSlideListener implements SlidingUpPanelLayout.PanelSlideListener {

        Launcher launcher;

        public WidgetsAppCellStyleSlideListener(Launcher launcher){
            this.launcher = launcher;
        }

        @Override
        public void onPanelSlide(View panel, float slideOffset) {
            WidgetsContainerView containerView = launcher.mWidgetsView;
            if (containerView == null) return;
            View dragView = containerView.getDragView();
            if (dragView == null) return;
            float f2 = 1.0f - (slideOffset * 0.1f);
            dragView.animate().scaleX(f2).scaleY(f2).setDuration(0L).setInterpolator(new DecelerateInterpolator()).start();
        }

        @Override
        public void onPanelStateChanged(View panel, PanelState previousState, PanelState newState) {
            if (newState == PanelState.COLLAPSED) {
                launcher.mWidgetsView.collapseAppStyle();
            }
        }
    }

    public class FilterWidgetCallable implements Callable<WidgetsModel> {

        String mKeyWord = null;

        public FilterWidgetCallable(CharSequence keyWord) {
            if (keyWord != null)
                mKeyWord = keyWord.toString();
        }

        @Override
        public WidgetsModel call() throws Exception {
            if (mWidgetsModel == null) return null;
            if (TextUtils.isEmpty(mKeyWord)){
                mWidgetsModel.setFilterNull();
            }
            else {
                mWidgetsModel.mFilteredPackageInfo.clear();
                mWidgetsModel.mFilteredWidgetList.clear();
                int size = mWidgetsModel.getPackageSize();
                for (int i = 0 ; i < size ; i++){
                    PackageItemInfo packageInfo = mWidgetsModel.getPackageItemInfo(i);
                    String itemToString = packageInfo.title.toString().toLowerCase();
                    boolean isContain = itemToString.contains(mKeyWord.toLowerCase());
                    if (isContain){
                        mWidgetsModel.mFilteredPackageInfo.add(
                                packageInfo
                        );
                        mWidgetsModel.mFilteredWidgetList.put(
                                packageInfo,
                                (ArrayList<Object>) mWidgetsModel.getSortedWidgets(packageInfo)
                        );
                    }
                }
            }
            return mWidgetsModel;
        }
    }

    public WidgetsContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLauncher = (Launcher) context;
        mDragController = mLauncher.getDragController();
        mDragLayer = mLauncher.getDragLayer();
        mRect = new Rect();
        mIconCache = mLauncher.getIconCache();
        mWidgetPreviewLoader = LauncherAppState.getInstance().getWidgetCache();
        mInputMethodManager = (InputMethodManager) mLauncher.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setUpView();
        setListeners();
        setAdapter();
    }

    public void setUpView(){
        mWidgetListRV = findViewById(R.id.widgets_list_view);
        mSearchWidgetEDT = findViewById(R.id.search_widget);
        mActionClearBtn = findViewById(R.id.action_clear);
    }

    public void setAdapter(){
        mWidgetListAdapter = new WidgetAppListAdapter(
                mLauncher,
                this,
                this,
                this
        );
        mLayoutManager = new SpeedLinearLayoutManager(this.mLauncher);
        mWidgetListRV.setLayoutManager(mLayoutManager);
        mWidgetListRV.setAdapter(mWidgetListAdapter);
    }

    public void setListeners(){
        addPanelSlideListener(new WidgetContainerViewListener());
        mActionClearBtn.setOnClickListener(this);
        setFadeOnClickListener(
            new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                setPanelState(PanelState.COLLAPSED);
            }
        }
        );
        mSearchWidgetEDT.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (TextUtils.isEmpty(s)){
                            mActionClearBtn.setVisibility(View.GONE);
                        }
                        else mActionClearBtn.setVisibility(View.VISIBLE);
                        filterWidget(s);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                }
        );
    }

    public void filterWidget(CharSequence keyword){
        try {
            new FilterWidgetCallable(keyword).call();
            WidgetsContainerView.this.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (!mWidgetListRV.isComputingLayout()){
                                mWidgetListAdapter.notifyDataSetChanged();
                            }
                        }
                    }
            );
        }
        catch (Throwable th){
            th.getMessage();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    public void scrollToTop() {
        mWidgetListRV.scrollToPosition(0);
    }

    @Override
    public boolean supportsFlingToDelete() {
        return false;
    }

    @Override
    public boolean supportsAppInfoDropTarget() {
        return false;
    }

    @Override
    public boolean supportsDeleteDropTarget() {
        return false;
    }

    @Override
    public float getIntrinsicIconScaleFactor() {
        return 0;
    }

    @Override
    public void onFlingToDeleteCompleted() {

    }

    @Override
    public void onDropCompleted(View target, DropTarget.DragObject d, boolean isFlingToDelete, boolean success) {

        if (!success || (target != mLauncher.getWorkspace() && !(target instanceof DeleteDropTarget) && !(target instanceof Folder))) {
            this.mLauncher.exitSpringLoadedDragModeDelayed(true, 300, (Runnable) null);
        }

        this.mLauncher.unlockScreenOrientation(false);
        if (success) return;

        boolean flag = false;

        if (target instanceof Workspace){
            CellLayout childAt = (CellLayout) ((Workspace) target).getChildAt(this.mLauncher.getCurrentWorkspaceScreen());
            ItemInfo itemInfo = (ItemInfo) d.dragInfo;
            if (childAt != null) {
                flag = !childAt.findCellForSpan((int[]) null, itemInfo.spanX, itemInfo.spanY);
                if (flag) {
                    mLauncher.showOutOfSpaceMessage(false);
                }
                d.deferDragViewCleanupPostAnimation = false;
            }
        }
        else {
            d.deferDragViewCleanupPostAnimation = false;
        }
    }

    @Override
    public void setInsets(Rect insets) {
        r();
    }

    @Override
    public boolean onLongClick(View v) {
        if (!v.isInTouchMode() || !mLauncher.isWidgetsViewVisible()) return false;
        if (!mLauncher.isDraggingEnabled()) return false;

        Object tag = v.getTag();
        boolean isPendingItem = tag instanceof PendingAddItemInfo;
        boolean isPendingWidgetItem = tag instanceof PendingAddWidgetInfo;
        boolean isPendingShortcutItem = tag instanceof PendingAddShortcutInfo;
        if (!isPendingItem) return false;
        PendingAddItemInfo pendingAddItemInfo = (PendingAddItemInfo) tag;
        Bitmap previewBmp;
        float ratio;
        Rect bound;

        if (v instanceof WidgetAppStyleCell) {
            WidgetAppStyleCell widget = (WidgetAppStyleCell) v;
            WidgetImageView widgetImageView = widget.mWidgetPreview;
            Bitmap widgetBmp = widgetImageView.getBitmap();
            if (widgetBmp == null) return false;
            bound = widgetImageView.getBitmapBounds();
            int width;
            if (pendingAddItemInfo instanceof PendingAddWidgetInfo) {
                PendingAddWidgetInfo widgetInfo = (PendingAddWidgetInfo) pendingAddItemInfo;
                int[] itemSize = mLauncher.getWorkspace().estimateItemSize(widgetInfo, true);
                int[] preScaledWidthOut = new int[1];
                previewBmp = mWidgetPreviewLoader.generateWidgetPreview(
                        mLauncher,
                        widgetInfo.info,
                        Math.min((int) (widgetBmp.getWidth() * 1.25f), itemSize[0]),
                        Math.min((int) (widgetBmp.getWidth() * 1.25f), itemSize[0]),
                        null,
                        preScaledWidthOut
                );
                if (preScaledWidthOut[0] < widgetBmp.getWidth()) {
                    int diff = (widgetBmp.getWidth() - preScaledWidthOut[0]) / 2;
                    if (widgetBmp.getWidth() > widgetImageView.getWidth()) {
                        diff = (widgetImageView.getWidth() * diff) / widgetBmp.getWidth();
                    }
                    bound.left += diff;
                    bound.right -= diff;
                }
                width = bound.width();
            } else if (isPendingShortcutItem){
                PendingAddShortcutInfo info = (PendingAddShortcutInfo) pendingAddItemInfo;
                previewBmp = Utilities.createIconBitmap(
                        mIconCache.getFullResIcon(info.activityInfo),
                        mLauncher
                );
                info.spanY = 1;
                info.spanX = 1;
                width = mLauncher.getDeviceProfile().cellWidthPx;
            }
            else return false;
            float bmpWidth = previewBmp.getWidth();
            ratio = bmpWidth / width;
        }
        else if (v instanceof WidgetBaseLayout) {
            WidgetBaseLayout widget = (WidgetBaseLayout) v;
            int[] locations = new int[2];
            widget.getLocationOnScreen(locations);
            int width = widget.getWidth();
            int height = widget.getHeight();
            bound = new Rect(
                locations[0],locations[1] - height, locations[0] + width, locations[1]
            );
            if (isPendingWidgetItem){
                PendingAddWidgetInfo pendingAddWidgetInfo = (PendingAddWidgetInfo) pendingAddItemInfo;
                int[] estimateItemSize = mLauncher.getWorkspace().estimateItemSize(pendingAddWidgetInfo,true);
                previewBmp = Utilities.captureView(widget);
                if (previewBmp == null){
                    int[] preWidthScaleOut = new int[1];
                    previewBmp = mWidgetPreviewLoader.generateWidgetPreview(
                            mLauncher,
                            ((PendingAddWidgetInfo) pendingAddItemInfo).info,
                            Math.min((int) (bound.width() * 1.25f), estimateItemSize[0]),
                            Math.min((int) (bound.width() * 1.25f), estimateItemSize[0]),
                            null,
                            preWidthScaleOut
                    );
                    if (preWidthScaleOut[0] < bound.width()) {
                        int diff = (bound.width() - preWidthScaleOut[0]) / 2;
                        if (bound.width() > widget.getWidth()) {
                            diff = (widget.getWidth() * diff) / bound.width();
                        }
                        bound.left += diff;
                        bound.right -= diff;
                    }
                    ratio = bound.width() * 1.0f / previewBmp.getWidth();
                }
                else ratio = 1.0f;
            }
            else if (isPendingShortcutItem){
                PendingAddShortcutInfo pendingAddShortcutInfo = (PendingAddShortcutInfo) pendingAddItemInfo;
                previewBmp = Utilities.createIconBitmap(
                        mIconCache.getFullResIcon(pendingAddShortcutInfo.activityInfo),
                        mLauncher
                );
                pendingAddItemInfo.spanX = 1;
                pendingAddItemInfo.spanY = 1;
                ratio = mLauncher.getDeviceProfile().cellWidthPx * 1.0f / previewBmp.getWidth();
            }
            else return false;
            bound.set(0,0,0,0);
        }
        else
            return false;

        if (previewBmp == null || bound == null){
            return false;
        }

        mLauncher.lockScreenOrientation();
        Workspace workspace2 = mLauncher.getWorkspace();
        workspace2.estimateItemSize(pendingAddItemInfo, false);
        mDragController.startDrag(
                v,
                previewBmp,
                this,
                pendingAddItemInfo,
                bound,
                0,
                ratio
        );
        previewBmp.recycle();

        if (mLauncher.getDragController().mDragging){
            mLauncher.enterSpringLoadedDragMode();
        }

        if (isPendingWidgetItem){
            WidgetHostViewLoader loader = new WidgetHostViewLoader(mLauncher,v);
            LauncherAppWidgetProviderInfo providerInfo = loader.mInfo.info;
            Bundle bundle = WidgetHostViewLoader.getDefaultOptionsForWidget(
                    mLauncher,loader.mInfo
            );

            ComponentName configure = loader.mInfo.info.configure;
            if (configure != null){
                loader.mInfo.bindOptions = bundle;
            }
            else {
                loader.mInflateWidgetRunnable = new WidgetInflaterRunnable(loader,providerInfo);
                loader.mBindWidgetRunnable = new WidgetBindRunnable(loader,providerInfo,bundle);
            }
            loader.mHandler.post(loader.mBindWidgetRunnable);

            mLauncher.getDragController().addDragListener(
                    loader
            );
        }

        return true;
    }

    @Override
    public void onClick(View v) {
        if (v == null) {
            return;
        }
        if (v == mActionClearBtn){
            mSearchWidgetEDT.setText("");
            return;
        }

        // Bấm thẳng vào ô preview widget/shortcut trong khay chọn -> đặt ngay vào ô trống
        // + đóng khay (đóng khay sẽ post onShakingAllApps -> vào chế độ edit/jiggle). Cả khay
        // grid chính lẫn panel app-style đều dùng listener này với tag Pending*Info nên chỉ cần
        // xử lý ở đây là phủ cả hai. Nút "Add" của panel app-style vẫn hoạt động độc lập.
        Object tag = v.getTag();
        if (tag instanceof PendingAddWidgetInfo) {
            mLauncher.closeWidgetViewWithAnimation();
            mLauncher.addAppWidgetFromScreenEditView((PendingAddWidgetInfo) tag);
        }
        else if (tag instanceof PendingAddShortcutInfo) {
            mLauncher.closeWidgetViewWithAnimation();
            mLauncher.addAppShortcutFromScreenEditView((PendingAddShortcutInfo) tag);
        }
        else if (mLauncher.isWidgetsViewVisible()){
            showToast();
        }
    }

    public void showToast(){
        Toast.makeText(
                mLauncher,
//                Utilities.getSpanText(
                    mLauncher.getResources().getText(R.string.long_press_widget_to_add),
//                "",
//                ),
                Toast.LENGTH_SHORT
        ).show();
    }

    public final void r() {
        Rect rect = new Rect(0, 0, 0, 0);
        if (rect.equals(this.mRect)) {
            return;
        }
        this.mRect.set(rect);
        setPadding(0, rect.top, 0, rect.bottom);
        setPadding(0, 0, 0, 0);
    }

    @Override
    public void setVisibility(int visibility) {
        if (visibility == VISIBLE){
            setAlpha(1.0f);
        }
        else {
            mActionClearBtn.setVisibility(View.GONE);
        }
        super.setVisibility(visibility);

    }

    public void collapseWidgetList(){
        if (mLauncher.mWidgetsAppStyle.isShown()) {
            collapseAppStyle();
        }
        mLauncher.closeWidgetView(true);
        View dragView = getDragView();
        if (dragView != null) {
            dragView.setScaleX(1.0f);
            dragView.setScaleY(1.0f);
        }
    }
    
    public void closeAppStyle(){
        mLauncher.mWidgetsAppStyle.clearFocus();
        mLauncher.mWidgetsAppStyle.setVisibility(View.GONE);
        mLauncher.mWidgetsAppStyle.mWidgetViewPager.setAdapter(null);
        mLauncher.mWidgetsAppStyle.setPanelStateInternal(PanelState.COLLAPSED);
        mLauncher.mWidgetsAppStyle.close();
    }

    public final void collapseAppStyle() {
        SlidingUpWidgetsCellAppStyle slidingUpWidgetsCellAppStyle = mLauncher.mWidgetsAppStyle;
        if (slidingUpWidgetsCellAppStyle != null) {
            slidingUpWidgetsCellAppStyle.postOnAnimation(new Runnable() {
                @Override
                public void run() {
                    closeAppStyle();
                }
            });
        }
    }

    public void setWidgetModel(WidgetsModel widgetModel){
        mWidgetsModel = widgetModel;
        mWidgetListAdapter.notifyDataSetChanged();
    }



    @Override
    public void clearFocus() {
        if (mInputMethodManager != null){
            mInputMethodManager.hideSoftInputFromWindow(getWindowToken(),0);
        }
        super.clearFocus();
        mSearchWidgetEDT.setText("");
        if (mWidgetsModel != null){
            mWidgetsModel.setFilterNull();
            mWidgetListAdapter.setWidgetModel(mWidgetsModel);
        }
    }

    public void expandAppStyleView(){
        mLauncher.mWidgetsAppStyle.requestFocus();
        mLauncher.mWidgetsAppStyle.setVisibility(View.VISIBLE);
        mLauncher.mWidgetsAppStyle.setPanelState(PanelState.EXPANDED);
    }

    public static class WidgetBindRunnable implements Runnable {

        WidgetHostViewLoader mLoader;
        LauncherAppWidgetProviderInfo mProviderInfo;
        Bundle mWidgetBundle;

        public WidgetBindRunnable(WidgetHostViewLoader loader, LauncherAppWidgetProviderInfo info, Bundle bundle) {
            this.mLoader = loader;
            this.mProviderInfo = info;
            this.mWidgetBundle = bundle;
        }

        @Override
        public void run() {
            mLoader.mWidgetLoadingId = mLoader.mLauncher.getAppWidgetHost().allocateAppWidgetId();

            if (AppWidgetManagerCompat.getInstance(mLoader.mLauncher).bindAppWidgetIdIfAllowed(
                    mLoader.mWidgetLoadingId,
                    mLoader.mInfo.info,
                    mLoader.mInfo.bindOptions
            )){
                mLoader.mHandler.post(mLoader.mInflateWidgetRunnable);
            }
        }
    }

    public static class WidgetInflaterRunnable implements Runnable {

        WidgetHostViewLoader mLoader;
        LauncherAppWidgetProviderInfo mProviderInfo;

        public WidgetInflaterRunnable(WidgetHostViewLoader loader, LauncherAppWidgetProviderInfo info) {
            this.mLoader = loader;
            this.mProviderInfo = info;
        }

        @Override
        public void run() {
            if (mLoader.mWidgetLoadingId == -1) return;
            Launcher launcher = mLoader.mLauncher;
            AppWidgetHost appWidgetHost = launcher.getAppWidgetHost();
            AppWidgetHostView appWidgetHostView = appWidgetHost.createView(
                launcher,mLoader.mWidgetLoadingId,mProviderInfo
            );
            mLoader.mInfo.boundWidget = appWidgetHostView;
            mLoader.mWidgetLoadingId = -1;
            appWidgetHostView.setVisibility(View.INVISIBLE);
            int[] estimateSize = launcher.getWorkspace().estimateItemSize(
                mLoader.mInfo,false
            );
            DragLayer.LayoutParams layoutParams = new DragLayer.LayoutParams(
                    estimateSize[0],
                    estimateSize[1]
            );
            layoutParams.customPosition = true;
            appWidgetHostView.setLayoutParams(layoutParams);
            launcher.getDragLayer().addView(appWidgetHostView);
            mLoader.mView.setTag(mLoader.mInfo);
        }
    }
}
