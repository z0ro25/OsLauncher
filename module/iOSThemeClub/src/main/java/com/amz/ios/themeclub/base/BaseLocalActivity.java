package com.amz.ios.themeclub.base;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.amz.ios.ioslite.common.CommonAppCompatActivity;
import com.amz.ios.ioslite.common.analytics.AnalyticsDelegate;
import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.ioslite.common.launcher.LauncherRouter;
import com.amz.ios.ioslite.common.launcher.LauncherSettingCallback;
import com.amz.ios.ioslite.common.launcher.LauncherStateManager;
import com.amz.ios.ioslite.common.launcher.LauncherWallpaperManager;
import com.amz.ios.themeclub.R;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.themeclub.adapter.MyCoverFlowAdapter;
import com.amz.ios.themeclub.bean.ThemeInfo;
import com.amz.ios.themeclub.ui.activity.FullThemeActivity;
import com.amz.ios.themeclub.util.Utilities;
import com.amz.ios.themeclub.view.CoverFlowView;
import com.amz.ios.themeclub.view.ThemePagedView;

import java.util.List;

import static com.amz.ios.themeclub.effect.ThemeScrollEffect.SCROLL_EFFECT_NONE;
import static com.amz.ios.themeclub.effect.ThemeScrollEffect.SCROLL_EFFECT_STACK;

public abstract class BaseLocalActivity<W> extends CommonAppCompatActivity implements  View.OnClickListener,
        View.OnTouchListener, ThemePagedView.PageSwitchListener, LauncherRouter.LauncherDelegate, LauncherSettingCallback {
    public RelativeLayout top_layout;
    public CustomTextView mNameDetail1;
    public CustomTextView mIntroduceDetail;
    //    public GalleryAdapter mGalleryAdapter;
//    public RecyclerView mRecyclerView;
    public ThemePagedView mPagedView;
    public ThemePagedView mFullThemePagedView;

    public CustomTextView mApplyTheme;
    public CustomTextView mTitleText;
    public LinearLayout mDeleteImage;
    public LinearLayout mBack;
    public RelativeLayout mProgress;
    public ImageView mThemeImageView;
    public RelativeLayout mFullThemeLayout;
    public final String TAG = "BaseLocalActivity";
    private W mData;

    private View mPageIndicators;
    private int mPageIndex = -1;

    private boolean m_bShowThemeImage;

    public List<Bitmap> listViews;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme_detail);
        mProgress = (RelativeLayout) findViewById(R.id.progress_set);
        final Intent intent = getIntent();
        mData = handleIntent(intent);

        findViewById();
        initCommonConfig();
    }

    private void initCommonConfig() {
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.themeclub_gallery_space);
//        mRecyclerView.addItemDecoration(new SpaceItemUtils(spacingInPixels));
        mIntroduceDetail.setLines(5);
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleIntentData(mData);
        setupView(mData);
        addClick();
        fullThemeProc();
    }

    private void addClick() {
        mDeleteImage.setOnClickListener(this);
        mBack.setOnClickListener(this);
        mApplyTheme.setOnClickListener(this);
    }

    private void findViewById() {
        mNameDetail1 = (CustomTextView) findViewById(R.id.name_detail);
        mIntroduceDetail = (CustomTextView) findViewById(R.id.introduce_detail);
        mApplyTheme = (CustomTextView) findViewById(R.id.apply_theme);
        mTitleText = (CustomTextView) findViewById(R.id.theme_name);
        mDeleteImage = (LinearLayout) findViewById(R.id.delete_detail);
        mBack = (LinearLayout) findViewById(R.id.back);
        RelativeLayout themeLayout = (RelativeLayout) findViewById(R.id.particular_relative);

        RelativeLayout.LayoutParams lp;
        final boolean isLayoutRtl = Utilities.isRtl(getResources());

        mPagedView = (ThemePagedView) findViewById(R.id.theme_preview);
        mPagedView.setPageSwitchListener(this);
        lp = (RelativeLayout.LayoutParams) mPagedView.getLayoutParams();
//        lp.gravity = Gravity.CENTER;
        Rect padding = getWorkspacePadding(isLayoutRtl);
        mPagedView.setLayoutParams(lp);
        mPagedView.setPadding(padding.left, padding.top, padding.right, padding.bottom);
        mPagedView.setPageSpacing(getWorkspacePageSpacing(isLayoutRtl));
        mPagedView.setScrollEffectFromString(SCROLL_EFFECT_NONE);
        mPagedView.showScrollEffectAnimation();

        mPageIndicators = findViewById(R.id.indicator);

        top_layout = (RelativeLayout) findViewById(R.id.top_title);

        mPagedView.setOnThemeImageListener(new ThemePagedView.ThemeImageListener() {
            @Override
            public void image(final int currentIdx) {
                hideStatusBar();
                mFullThemeLayout.setVisibility(View.VISIBLE);
                top_layout.setVisibility(View.INVISIBLE);
                mFullThemePagedView.setCurrentPage(currentIdx);

//                showFullTheme(mData, currentIdx, BaseLocalActivity.this);

            }
        });


    }

    public void  hideStatusBar() {
////        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
//
////        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        Window window = getWindow();
        WindowManager.LayoutParams attrs = window.getAttributes();
//        attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
//        attrs.flags &= (~WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

        attrs.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        attrs.flags &= (~WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

        window.setAttributes(attrs);
    }

    public void showStatusBar(){
//        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);

        Window window = getWindow();
        WindowManager.LayoutParams attrs = window.getAttributes();
        attrs.flags |= WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
        attrs.flags &= (~WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        window.setAttributes(attrs);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void fullThemeProc(){
        mFullThemeLayout = (RelativeLayout) findViewById(R.id.full_theme_layout);
        mFullThemeLayout.setVisibility(View.GONE);

        mFullThemePagedView = (ThemePagedView) findViewById(R.id.full_theme_pagedview);
        mFullThemePagedView.setViews(listViews, 1);
        mFullThemePagedView.setScrollEffectFromString(SCROLL_EFFECT_STACK);
        mFullThemePagedView.showScrollEffectAnimation();
        mFullThemePagedView.setOnThemeImageListener(new ThemePagedView.ThemeImageListener() {
            @Override
            public void image(int currentIdx) {
                hideFullThemeLayout();
            }
        });
    }
    Rect getWorkspacePadding(boolean isLayoutRtl) {
//        Rect searchBarBounds = getSearchBarBounds(isLayoutRtl);
        Rect padding = new Rect();
//        if (isLandscape && transposeLayoutWithOrientation) {
//            // Pad the left and right of the workspace with search/hotseat bar sizes
//            if (isLayoutRtl) {
//                padding.set(hotseatBarHeightPx, edgeMarginTop,
//                        searchBarBounds.width(), edgeMarginTop);
//            } else {
//                padding.set(searchBarBounds.width(), edgeMarginTop,
//                        hotseatBarHeightPx, edgeMarginTop);
//            }
//        } else {
//            if (isTablet) {
//                // Pad the left and right of the workspace to ensure consistent spacing
//                // between all icons
//                float gapScale = 1f + (dragViewScale - 1f) / 2f;
//                int width = getR;
//                int height = getCurrentHeight();
//                int paddingTop = searchBarBounds.bottom;
//                int paddingBottom = hotseatBarHeightPx + pageIndicatorHeightPx;
//                int availableWidth = Math.max(0, width - (int) ((inv.numColumns * cellWidthPx) +
//                        (inv.numColumns * gapScale * cellWidthPx)));
//                int availableHeight = Math.max(0, height - paddingTop - paddingBottom
//                        - 2 * inv.numRows * cellHeightPx);
//                padding.set(availableWidth / 2, paddingTop + availableHeight / 2,
//                        availableWidth / 2, paddingBottom + availableHeight / 2);
//            } else {
        // Pad the top and bottom of the workspace with search/hotseat bar sizes
        padding.set(12,
                0,
                12,
                12);
//            }
//        }
        return padding;
    }

    private int getWorkspacePageSpacing(boolean isLayoutRtl) {

        // In portrait, we want the pages spaced such that there is no
        // overhang of the previous / next page into the current page viewport.
        // We assume symmetrical padding in portrait mode.
        return Math.max(24, 2 * getWorkspacePadding(isLayoutRtl).left);

    }

    @Override
    public void onPageSwitch(View newPage, int newPageIndex) {

    }

    @Override
    public void onPageBeginMoving() {

    }

    @Override
    public void onPageEndMoving() {
        if (mPageIndex != mPagedView.getCurrentPage()) {
            mPageIndex = mPagedView.getCurrentPage();
            LauncherWallpaperManager.setScreenCount(mPagedView.hasCustomContent() ? mPagedView.getPageCount() - 1 : mPagedView.getPageCount());
            LauncherWallpaperManager.setScreenCurrentPosition(mPagedView.hasCustomContent() ? mPageIndex - 1 : mPageIndex);
            LauncherStateManager.notifyPageSwitch();

//            if (mPagedView.isInOverviewMode()) {
//                mOverviewPanel.synchronizeHomeBtn();
//            }
        }
    }

    private void hideFullThemeLayout(){
        showStatusBar();
        mFullThemeLayout.setVisibility(View.GONE);
        top_layout.setVisibility(View.VISIBLE);
        mPagedView.setCurrentPage(mFullThemePagedView.getCurrentPage());
    }
    @Override
    public void onBackPressed() {
        if (mFullThemeLayout.getVisibility() == View.VISIBLE){
            hideFullThemeLayout();
            return;
        }
        super.onBackPressed();

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        DebugLog.w(TAG, "=================onRestart");
    }

    @Override
    protected void onStart() {
        super.onStart();
        DebugLog.w(TAG, "================onStart");
        handleThemeUninstalled(mData);
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        DebugLog.w(TAG, "================onNewIntent");
        mData = handleIntent(intent);
        handleIntentData(mData);
        setupView(mData);
    }

    @Override
    public void onClick(View v) {
        final int id = v.getId();
        if (id == R.id.back) {
            finish();
        } else if (id == R.id.delete_detail) {
            unInstallApk(mData);
            finish();
        } else if (id == R.id.apply_theme) {
            apply(mData);
        }
    }

    protected abstract W handleIntent(Intent intent);

    protected abstract void handleThemeUninstalled(W w);

    protected abstract void showFullTheme(W w, int curIdx, Context context);

    protected abstract void setupView(W w);

    protected abstract void apply(W w);

    protected abstract void unInstallApk(W w);

    protected abstract void handleIntentData(W w);
}
