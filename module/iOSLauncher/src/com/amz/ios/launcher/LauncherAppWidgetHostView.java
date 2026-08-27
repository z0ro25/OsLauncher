/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amz.ios.launcher;

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;

import com.amz.ios.launcher.DragLayer.TouchCompleteListener;
import com.amz.ios.launcher.popup.PopupPopulator;

import java.util.ArrayList;

/**
 * {@inheritDoc}
 */
public class LauncherAppWidgetHostView extends AppWidgetHostView implements TouchCompleteListener, IShakeInterface, PopUpInterface {

    LayoutInflater mInflater;

    private BubbleTextView.ReorderHintAnimation mShakeAnimators;

    private CheckLongPressHelper mLongPressHelper;
    private StylusEventHelper mStylusEventHelper;
    private Context mContext;
    private int mPreviousOrientation;
    private DragLayer mDragLayer;

    private float mSlop;

    DeviceProfile mDeviceProfile;

    Paint mPaint;
    Path mPath;
    RectF mRectF;

//    TextViewCustomFont mTextViewCustomFont;

    public int[] location;
    private boolean s;
    private boolean z;
    String key;
    int mTopMargin;
    int mLeftMargin;
    int mRightMargin;
    int mBottomMargin;
    int mLabelHeight;

    Runnable mStartAnimRunnable;
    Runnable mWidgetReloadRunnable;

    /**
     * The scaleX and scaleY value such that the widget fits within its cellspans, scaleX = scaleY.
     */
    private float mScaleToFit = 1f;

    // Dấu trừ (gỡ widget) khi ở chế độ edit.
    // Đã thử vẽ badge trong draw()/overlay của host nhưng THẤT BẠI: (1) lúc jiggle, draw() của host
    // KHÔNG được gọi lại (host tái dùng RenderNode, invalidate() không kích hoạt redraw) và (2) nội
    // dung widget composite ĐÈ lên do elevation. Vì vậy dùng VIEW CON thật (ImageView): bật/tắt qua 2
    // hook lifecycle đáng tin cậy (beginOrAdjustHintAnimations / completeAndClearReorderHintAnimations),
    // elevation cao để nổi trên nội dung, có OnClickListener riêng -> không phụ thuộc draw() hay Z của
    // canvas. Bản RIÊNG cho widget, không đụng dấu trừ của app (memory isolate-resources).
    private int mDelIconSize;   // kích thước dấu trừ (45% icon, giống app)
    private int mDelInset;       // chừa 4dp từ mép trên-trái host
    private ImageView mDelBadgeView;

    private void ensureDelBadge() {
        if (mDelBadgeView != null) return;
        mDelBadgeView = new ImageView(mContext);
        mDelBadgeView.setImageResource(R.drawable.delete_button);
        mDelBadgeView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mDelBadgeView.setVisibility(View.GONE);
        // Nổi trên nội dung widget (nội dung có thể có elevation) + luôn vẽ sau cùng.
        mDelBadgeView.setElevation(Float.MAX_VALUE / 4);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(mDelIconSize, mDelIconSize);
        lp.gravity = android.view.Gravity.TOP | android.view.Gravity.LEFT;
        // Margin ÂM nhỏ: badge nhô nhẹ ra ngoài mép trên-trái widget (không đè nội dung),
        // nhưng vẫn nằm gọn trong khe giữa các ô nên KHÔNG bị mép lưới/Workspace cắt.
        lp.leftMargin = -mDelIconSize / 4;
        lp.topMargin = -mDelIconSize / 4;
        mDelBadgeView.setLayoutParams(lp);
        mDelBadgeView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((Launcher) mContext).showRemoveWidgetDialog(LauncherAppWidgetHostView.this);
            }
        });
        addView(mDelBadgeView);
        // Cho badge tràn ra ngoài bounds host: tắt clip trên host + chuỗi container cha
        // (ShortcutAndWidgetContainer -> CellLayout). Tắt lazy tại đây để không đụng
        // hành vi mặc định của container ở nơi khác (isolate-resources); icon app vẫn vẽ
        // trong bounds nên không bị ảnh hưởng.
        disableClipForBadge();
    }

    private void disableClipForBadge() {
        setClipChildren(false);
        setClipToPadding(false);
        android.view.ViewParent p = getParent();
        for (int i = 0; i < 2 && p instanceof ViewGroup; i++) {
            ViewGroup vg = (ViewGroup) p;
            vg.setClipChildren(false);
            vg.setClipToPadding(false);
            p = vg.getParent();
        }
    }

    private void showDelBadge(boolean show) {
        ensureDelBadge();
        mDelBadgeView.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            mDelBadgeView.bringToFront();
        }
    }

    public LauncherAppWidgetHostView(Context context) {
        super(context);

        mContext = context;
        // Route long-press THẲNG tới Launcher.onLongClick(this) thay vì dựa vào
        // performLongClick() (cần OnLongClickListener được Workspace gắn khi add child).
        // Nếu widget được add qua đường KHÔNG gắn listener, performLongClick() trả false ->
        // long-press "không handled" -> gesture lọt lên long-press vùng trống của Workspace ->
        // popWorkspace() -> startTidyUp() = BẬT EDIT nhầm. Gắn listener tường minh ở đây bảo đảm
        // luôn mở popup widget (chưa edit) / nhấc kéo (đang edit) và host tự "nuốt" gesture.
        mLongPressHelper = new CheckLongPressHelper(this, (Launcher) context);
        mStylusEventHelper = new StylusEventHelper(this);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mDragLayer = ((Launcher) context).getDragLayer();
        setAccessibilityDelegate(LauncherAppState.getInstance().getAccessibilityDelegate());
//        mTextViewCustomFont = new TextViewCustomFont(context, null);

        setUp();
        config();
    }

    void setUp(){
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPath = new Path();
        mRectF = new RectF();
        mDeviceProfile = ((Launcher)mContext).getDeviceProfile();
        mDelIconSize = (int) (mDeviceProfile.iconSizePx * 0.45f);
        mDelInset = (int) (getResources().getDisplayMetrics().density * 4);
    }

    void config(){
        /*
        mTextViewCustomFont.setTextSize(1, 11.0f);
        mTextViewCustomFont.setTextAlignment(TEXT_ALIGNMENT_CENTER);
        mTextViewCustomFont.setTextColor(-1);
        mTextViewCustomFont.setSingleLine();
        mTextViewCustomFont.setEllipsize(TextUtils.TruncateAt.END);


         */
        DragLayer.LayoutParams layoutParams = new DragLayer.LayoutParams(-1, -2);
        ((FrameLayout.LayoutParams) layoutParams).gravity = 80;

        /*
        addView(
                mTextViewCustomFont,
                layoutParams
        );

         */
    }

    @Override
    protected View getErrorView() {
        return mInflater.inflate(R.layout.appwidget_error, this, false);
    }

    public void updateLastInflationOrientation() {
        mPreviousOrientation = mContext.getResources().getConfiguration().orientation;
    }

    @Override
    public void updateAppWidget(RemoteViews remoteViews) {
        // Store the orientation in which the widget was inflated
        updateLastInflationOrientation();
        super.updateAppWidget(remoteViews);
    }

    public boolean isReinflateRequired() {
        // Re-inflate is required if the orientation has changed since last inflated.
        int orientation = mContext.getResources().getConfiguration().orientation;
        return mPreviousOrientation != orientation;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Just in case the previous long press hasn't been cleared, we make sure to start fresh
        // on touch down.
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mLongPressHelper.cancelLongPress();
        }

        // Consume any touch events for ourselves after longpress is triggered
        if (mLongPressHelper.hasPerformedLongPress()) {
            mLongPressHelper.cancelLongPress();
            return true;
        }

        // Watch for longpress or stylus button press events at this level to
        // make sure users can always pick up this widget
        if (mStylusEventHelper.checkAndPerformStylusEvent(ev)) {
            mLongPressHelper.cancelLongPress();
            return true;
        }
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                if (!mStylusEventHelper.inStylusButtonPressed()) {
                    mLongPressHelper.postCheckForLongPress();
                }
                mDragLayer.setTouchCompleteListener(this);
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mLongPressHelper.cancelLongPress();
                break;
            case MotionEvent.ACTION_MOVE:
                if (!Utilities.pointInView(this, ev.getX(), ev.getY(), mSlop)) {
                    mLongPressHelper.cancelLongPress();
                }
                break;
        }

        // Otherwise continue letting touch events fall through to children
        return false;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        // If the widget does not handle touch, then cancel
        // long press when we release the touch
        switch (ev.getAction()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mLongPressHelper.cancelLongPress();
                break;
            case MotionEvent.ACTION_MOVE:
                if (!Utilities.pointInView(this, ev.getX(), ev.getY(), mSlop)) {
                    mLongPressHelper.cancelLongPress();
                }
                break;
        }
        return false;
    }

    @Override
    public void setAppWidget(int appWidgetId, AppWidgetProviderInfo info) {
        super.setAppWidget(appWidgetId, info);
        String key = "widget_" + appWidgetId;
        String label = info.label;
        /*
        if (label == null){
            label = info.loadLabel(mContext.getPackageManager());
        }
         */

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        String customLabel = sharedPreferences.getString(key, null);
        if (customLabel != null)
            label = customLabel;

//        mTextViewCustomFont.setText(label);

//        mTextViewCustomFont.setVisibility(View.GONE);
//        mLabelHeight = mTextViewCustomFont.getMeasuredHeight();

        Launcher launcher = (Launcher) mContext;
        DeviceProfile deviceProfile = launcher.getDeviceProfile();
        int margin = deviceProfile.edgeMarginPx;

    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        setSelected(this.s && focused != null);
        if (focused != null) {
            focused.setFocusableInTouchMode(false);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        mLongPressHelper.cancelLongPress();
    }

    @Override
    public AppWidgetProviderInfo getAppWidgetInfo() {
        AppWidgetProviderInfo info = super.getAppWidgetInfo();
        if (info != null && !(info instanceof LauncherAppWidgetProviderInfo)) {
            throw new IllegalStateException("Launcher widget must have"
                    + " LauncherAppWidgetProviderInfo");
        }
        return info;
    }

    public LauncherAppWidgetProviderInfo getLauncherAppWidgetProviderInfo() {
        return (LauncherAppWidgetProviderInfo) getAppWidgetInfo();
    }

    @Override
    public void onTouchComplete() {
        if (!mLongPressHelper.hasPerformedLongPress()) {
            // If a long press has been performed, we don't want to clear the record of that since
            // we still may be receiving a touch up which we want to intercept
            mLongPressHelper.cancelLongPress();
        }
    }

    @Override
    public int getDescendantFocusability() {
        return ViewGroup.FOCUS_BLOCK_DESCENDANTS;
    }

    public void setScaleToFit(float scale) {
        mScaleToFit = scale;
        setScaleX(scale);
        setScaleY(scale);
    }

    @Override
    public void clearChildFocus(View child) {
        super.clearChildFocus(child);
        setSelected(false);
    }

    public float getScaleToFit() {
        return mScaleToFit;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (action == MotionEvent.ACTION_DOWN){
            if (mContext instanceof Launcher) {
                Launcher launcher = (Launcher) mContext;
                this.z = false;
                location = getLocationIconViewOnScreen();
//                postDelayed(this.C, ViewConfiguration.getTapTimeout() + 200);
            }
        }
        else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL){

        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (this.s && event.getKeyCode() == 111 && event.getAction() == 1) {
            this.s = false;
            requestFocus();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchUnhandledMove(View focused, int direction) {
        return this.s;
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if ((child instanceof TextViewCustomFont) || child == mDelBadgeView || child != getChildAt(1)) {
            return super.drawChild(canvas, child, drawingTime);
        }
        int save = canvas.save();
        if (Build.VERSION.SDK_INT >= 26){
            canvas.clipOutPath(mPath);
        }
        else {
            canvas.clipPath(mPath, Region.Op.DIFFERENCE);
        }
        boolean drawChild = super.drawChild(canvas, child, drawingTime);
        canvas.restoreToCount(save);
        return drawChild;
    }

    public String getLabel(){
//        return mTextViewCustomFont.getText().toString();
        return "";
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (this.s || keyCode != 66){
            return super.onKeyDown(keyCode,event);
        }
        event.startTracking();
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (event.isTracking() && !this.s && keyCode == 66){
            this.s = true;
            ArrayList<View> focusables = getFocusables(View.FOCUS_FORWARD);
            int size = focusables.size();
            if (size == 1 && getTag() instanceof ItemInfo){
                ItemInfo itemInfo = (ItemInfo) getTag();
                if (itemInfo.spanX == 1 && itemInfo.spanY == 1){
                    focusables.get(0).performClick();
                    this.s = false;
                    return true;
                }
                focusables.get(0).requestFocus();
                return true;
            }
            this.s = false;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        try {
            super.onLayout(changed, left, top, right, bottom);
        }
        catch (RuntimeException e){

        }
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        if (z){
            this.s = false;
            setSelected(false);
        }
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // View nhãn widget đã bị gỡ (đợt re-skin iOS) nên nội dung widget nằm ở child[0], không phải
        // child[1]. Trước đây getChildAt(1) trả null -> khối chuẩn hoá padding ném NPE bị catch rỗng
        // nuốt -> widget giữ padding mặc định của framework -> hiển thị thụt/không đẹp.
        final View childAt = getChildAt(0);
        if (childAt == null) {
            return;
        }
        try {
            AppWidgetProviderInfo appWidgetInfo = getAppWidgetInfo();
            if (appWidgetInfo.provider != null || appWidgetInfo.provider.getPackageName() == null || !appWidgetInfo.provider.getPackageName().equals(mContext.getPackageName())){
                childAt.setPadding(0,0,0,0);
                Drawable background = childAt.getBackground();
                if (background == null && childAt instanceof ViewGroup){
                    View childAt2 = ((ViewGroup) childAt).getChildAt(0);
                    if (childAt2 != null && (background = childAt2.getBackground()) == null && (childAt2 instanceof ImageView)) {
                        background = ((ImageView) childAt2).getDrawable();
                    }
                    if (background instanceof ColorDrawable){
                        childAt.setBackgroundColor(((ColorDrawable) background).getColor());
                    }
                    else if (background instanceof GradientDrawable){
                        Bitmap bitmap = Utilities.createIconBitmap(background,getContext());
                        if (bitmap != null){

                        }
                    }
                    else if (background instanceof BitmapDrawable){

                    }
                }
            }
        }
        catch (Exception e){

        }

    }

    @Override
    protected void prepareView(View view) {
        super.prepareView(view);
        if (view != null) {
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
            if (layoutParams == null) {
                layoutParams = new FrameLayout.LayoutParams(-1, -1);
            }
            int top = 0;
            int left = 0;
            int right = 0;
            int bottom = 0;
            layoutParams.setMargins(left, top, right, bottom);
            layoutParams.gravity = 119;
            view.setLayoutParams(layoutParams);
        }
    }

    public int[] getLocationWidget() {
        int[] iArr = this.location;
        return iArr != null ? iArr : getLocationIconViewOnScreen();
    }

    private int[] getLocationIconViewOnScreen() {
        int[] location = new int[2];
        getLocationOnScreen(location);
        return location;
    }

    /** Implementation of IShake **/

    @Override
    public void beginOrAdjustHintAnimations() {
        if (this.mShakeAnimators == null) {
            this.mShakeAnimators = new BubbleTextView.ReorderHintAnimation(this);
            this.mShakeAnimators.animate();
        }
        // Vào edit (shake bắt đầu) -> hiện dấu trừ.
        showDelBadge(true);
    }

    @Override
    public void beginOrAdjustHintAnimations(int i) {
        if (this.mShakeAnimators == null) {
            this.mShakeAnimators = new BubbleTextView.ReorderHintAnimation(this);
            this.mShakeAnimators.animate(i);
        }
        showDelBadge(true);
    }

    @Override
    public void completeAndClearReorderHintAnimations() {
        if (this.mShakeAnimators != null) {
            this.mShakeAnimators.completeAnimationImmediately();
            this.mShakeAnimators = null;
        }
        // Thoát edit -> ẩn dấu trừ.
        showDelBadge(false);
    }

    @Override
    public void joinAnimations(View view) {
        if (this.mShakeAnimators != null && (view instanceof IShakeInterface)) {
            view.clearAnimation();
            view.setScaleX(1.0f);
            view.setScaleY(1.0f);
            view.setTranslationX(0.0f);
            view.setTranslationY(0.0f);
            this.mShakeAnimators.join(view);
        }
    }

    /* Implementation Of PopUpInterface */

    @Override
    public void showLabel(boolean flag) {
        int visibility = flag ? View.VISIBLE : View.GONE;
        //mTextViewCustomFont.setVisibility(visibility);
    }

    @Override
    public int getContentHeight() {
        return getHeight();
    }

    @Override
    public int getContentWidth() {
        return getWidth();
    }

    @Override
    public int getContentPaddingTop() {
        return 0;
    }

    @Override
    public int getContentPaddingBottom() {
        return 0;
    }

    @Override
    public int getContentPaddingRight() {
        return 0;
    }

    @Override
    public int getContentPaddingLeft() {
        return 0;
    }
}
