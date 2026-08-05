package com.amz.ios.launcher.applibrary;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amz.ios.launcher.DeviceProfile;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.LauncherAnimUtils;
import com.amz.ios.launcher.LauncherRootView;

public class OpenedLibraryView extends OpenedView {

    public Launcher mLauncher;
    public RecyclerView mOpenItemsRV;
    public OpenLibraryItemAdapter mAdapter;

    public static int OpenedLibraryView_ID = 1002000134;

    public class HideAnimatorAdapter extends AnimatorListenerAdapter {

        @Override
        public final void onAnimationEnd(Animator animator) {
            setLayerType(LAYER_TYPE_NONE, null);
            LauncherRootView launcherRootView = (LauncherRootView) getParent();
            if (launcherRootView != null) {
                launcherRootView.removeView(OpenedLibraryView.this);
            }
        }

        @Override
        public final void onAnimationStart(Animator animator) {
            mLauncher.showAppsLibrary();
        }
    }

    public OpenedLibraryView(Context context) {
        super(context);
        setUpView();
        config();
        setListeners();
        setUpActions();
        setAdapter();
    }

    void setUpView(){
        mLauncher = (Launcher) getContext();
        mOpenItemsRV = new RecyclerView(getContext(),null);
        int i = mLauncher.getDeviceProfile().edgeMarginPx;
        int width = mLauncher.getDeviceProfile().getCurrentWidth();
        setPadding(i, 0, i, 0);
        LayoutParams layoutParams = new LayoutParams(width - (i * 2), -2);
        ((FrameLayout.LayoutParams) layoutParams).gravity = 17;
        addView(mOpenItemsRV,layoutParams);
    }

    void setListeners(){

    }

    void setUpActions(){

    }

    void setAdapter(){
        final OpenLibraryItemAdapter adapter = new OpenLibraryItemAdapter(mLauncher);

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(),4);
        layoutManager.setSpanSizeLookup(
                new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        // Header và ô quảng cáo chiếm trọn 1 hàng (4 cột); app = 1 ô.
                        if (position == 0 || adapter.isAdPosition(position)) return 4;
                        return 1;
                    }
                }
        );

        mOpenItemsRV.setLayoutManager(layoutManager);
        mAdapter = adapter;
        mOpenItemsRV.setAdapter(mAdapter);
        mOpenItemsRV.setOverScrollMode(OVER_SCROLL_NEVER);
    }

    void config(){
        DeviceProfile deviceProfile = mLauncher.getDeviceProfile();

        setId(OpenedLibraryView_ID);
        setX(0);
        setY(0);
        setClipChildren(false);
        setClipToPadding(false);
        setClickable(true);
        setFocusable(true);
        setPadding(deviceProfile.edgeMarginPx,0,deviceProfile.edgeMarginPx,0);

        LayoutParams params = new LayoutParams(deviceProfile.getCurrentWidth(), deviceProfile.getCurrentHeight());
        ((FrameLayout.LayoutParams) params).gravity = Gravity.AXIS_SPECIFIED;
        setLayoutParams(params);

    }

    public void hide(){
        if (getParent() instanceof LauncherRootView) {
            setLayerType(LAYER_TYPE_HARDWARE, null);
            ObjectAnimator animator = LauncherAnimUtils.ofPropertyValuesHolder(this, PropertyValuesHolder.ofFloat("alpha", 0.0f), PropertyValuesHolder.ofFloat("scaleX", 0.3f), PropertyValuesHolder.ofFloat("scaleY", 0.3f));
            animator.setInterpolator(Launcher.initInterpolator(0.33f, 0.89f, 0.55f, 1.0f));
            animator.addListener(new HideAnimatorAdapter());
            animator.setDuration(368L);
            animator.start();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event == null || event.getKeyCode() != 4) {
            return super.dispatchKeyEventPreIme(event);
        }
        hide();
        return true;
    }

    @Override
    public final boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN && this.mOpenItemsRV != null) {
            Rect rect = new Rect();
            this.mOpenItemsRV.getGlobalVisibleRect(rect);
            if (!rect.contains((int) motionEvent.getRawX(), (int) motionEvent.getRawY())) {
                hide();
            }
        }
        return super.dispatchTouchEvent(motionEvent);
    }
}
