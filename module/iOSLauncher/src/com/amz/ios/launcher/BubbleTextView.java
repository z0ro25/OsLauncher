/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Build;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.Property;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;

import com.amz.ios.launcher.IconCache.IconLoadRequest;
import com.amz.ios.launcher.awareness.UnreadLoaderCompact;
import com.amz.ios.launcher.badge.BadgeInfo;
import com.amz.ios.launcher.badge.BadgeRenderer;
import com.amz.ios.launcher.graphics.IconPalette;
import com.amz.ios.launcher.leftpage.drawables.CalendarDrawable;
import com.amz.ios.launcher.leftpage.drawables.ClockDrawable;
import com.amz.ios.launcher.model.PackageItemInfo;
import com.amz.ios.launcher.theme.ThemeManager;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.launcher.provider.AppTypeProvider;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;

import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import android.Manifest;
import android.util.Log;
import android.view.animation.LinearInterpolator;

import java.util.Calendar;
import java.util.Random;

import static com.amz.ios.launcher.LauncherSettings.Favorites.ITEM_TYPE_VIRTUAL_APP;

/**
 * TextView that draws a bubble behind the text. We cannot use a LineBackgroundSpan
 * because we want to make the bubble taller than the text and TextView's clip is
 * too aggressive.
 */
public class BubbleTextView extends CustomTextView implements IShakeInterface, PopUpInterface{

    private static SparseArray<Theme> sPreloaderThemes = new SparseArray<Theme>(2);
    private static final int DISPLAY_WORKSPACE = 0;
    private static final int DISPLAY_ALL_APPS = 1;
    private static final int DISPLAY_WIDGET_SECTION = 2;

    private static final float REORDER_HINT_MAGNITUDE = 0.05f;
    private static final float SCALE_FACTOR_PRESS = 0.92f;
    private final Launcher mLauncher;
    private Drawable mIcon;
    private final CheckLongPressHelper mLongPressHelper;
    private final boolean mLayoutHorizontal;
    private int mIconSize;
    private int mTextColor;
    private int mTextSize;
    private boolean mIsIconVisible = true;

    private BadgeInfo mBadgeInfo;
    private BadgeRenderer mBadgeRenderer;
    private IconPalette mBadgePalette;
    private float mBadgeScale;
    private boolean mForceHideBadge;
    private Point mTempSpaceForBadgeOffset = new Point();
    private Rect mTempIconBounds = new Rect();

    private String mComponentName;

    //*/wangqingsong add show miss number 20190330
    private static String BadgeList;
    private static boolean ShowBadgeNumber = false;

    private static final Property<BubbleTextView, Float> BADGE_SCALE_PROPERTY = new Property<BubbleTextView, Float>(Float.TYPE, "badgeScale") {
        @Override
        public Float get(BubbleTextView bubbleTextView) {
            return bubbleTextView.mBadgeScale;
        }

        @Override
        public void set(BubbleTextView bubbleTextView, Float value) {
            bubbleTextView.mBadgeScale = value;
            bubbleTextView.invalidate();
        }
    };

    private boolean mIgnorePressedStateChange;
    private boolean mDisableRelayout = false;
    private boolean mIsFolderName = false;

    private static Drawable sNewInstallPrefix;

    private IconLoadRequest mIconLoadRequest;

    private static ClockDrawable mClockDrawable;

    private boolean isClock;

    private static float mReorderHintAnimationMagnitude = 0.0f;

    private ReorderHintAnimation mShakeAnimators;

    public BubbleTextView(Context context) {
        this(context, null, 0);
    }

    public BubbleTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mLauncher = (Launcher) context;
        this.mShakeAnimators = null;
        this.mNormalSize = false;
        this.mLeftPaint = new Paint();
        this.mLeftPaint.setFilterBitmap(true);

        this.mNewInstallPrefix = Utilities.getNewInstallPrefix(context);
        DeviceProfile grid = mLauncher.getDeviceProfile();

        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attrs, R.styleable.BubbleTextView, defStyle, 0);
        mLayoutHorizontal = obtainStyledAttributes.getBoolean(R.styleable.BubbleTextView_layoutHorizontal, false);

        int display = obtainStyledAttributes.getInteger(R.styleable.BubbleTextView_iconDisplay, DISPLAY_WORKSPACE);
        int defaultIconSize = grid.iconSizePx;
        if (display == DISPLAY_WORKSPACE) {
            mTextSize = grid.iconTextSizePx;
            mTextColor = grid.workspaceIconTextColor;
            setTextColor(mTextColor);
        } else if (display == DISPLAY_WIDGET_SECTION) {
            mTextSize = grid.originalIconTextSizePx;
            defaultIconSize = grid.originalIconSizePx;
        } else if (display == DISPLAY_ALL_APPS) {
            mTextSize = grid.allAppsIconTextSizePx;
            defaultIconSize = grid.allAppsIconSizePx;
        } else {
            mTextSize = grid.iconTextSizePx;
        }
        mIconSize = defaultIconSize;
        mDelIconSize = (int)(mIconSize * 0.45f);
        this.mDelIconLeftPadding = (Launcher.getCellWidth() - defaultIconSize - mDelIconSize) / 2;
        this.mDelIconTopPadding = obtainStyledAttributes.getDimensionPixelSize(R.styleable.BubbleTextView_iconSizeOverride, 0);
        setTextSizePx(mTextSize);
        obtainStyledAttributes.recycle();

        //*/wangqingsong add show miss number 20190330
        BadgeList = getContext().getString(R.string.show_badge_list);
        ShowBadgeNumber = getContext().getResources().getBoolean(R.bool.hct_show_badge_number);

        mLongPressHelper = new CheckLongPressHelper(this);
        setAccessibilityDelegate(LauncherAppState.getInstance().getAccessibilityDelegate());
        getNewInstallPrefix(context);
        Resources resources = getContext().getResources();
        mReorderHintAnimationMagnitude = REORDER_HINT_MAGNITUDE * ((float) resources.getDimensionPixelSize(R.dimen.app_icon_size));
    }

    private static Drawable getNewInstallPrefix(Context context) {
        if (sNewInstallPrefix == null) {
            sNewInstallPrefix = context.getResources().getDrawable(R.drawable.new_install_prefix);
            sNewInstallPrefix.setBounds(0, 0, sNewInstallPrefix.getIntrinsicWidth(), sNewInstallPrefix.getIntrinsicHeight());
        }

        return sNewInstallPrefix;
    }

    public void applyFromShortcutInfo(ShortcutInfo shortcutInfo) {
        this.mShortcutInfo = shortcutInfo;
        setText(shortcutInfo.title);
        setTag(shortcutInfo);
    }

    public void applyFromShortcutInfo(ShortcutInfo info, IconCache iconCache) {
        this.mShortcutInfo = info;
        applyFromShortcutInfo(info, iconCache, false);
    }

    public void applyFromShortcutInfo(ShortcutInfo info, IconCache iconCache,
                                      boolean promiseStateChanged) {
        this.mShortcutInfo = info;

        if (info.getTargetComponent() != null){
            this.mComponentName = info.getTargetComponent().flattenToString();
        }

        Bitmap b = info.getIcon(iconCache);

        FastBitmapDrawable iconDrawable = mLauncher.createIconDrawable(b);
        iconDrawable.setGhostModeEnabled(info.isDisabled != 0);

        if (info.isDisabled()) {
            iconDrawable.setState(FastBitmapDrawable.State.DISABLED);
        }

        initClock(info.getTargetComponent());

        setIcon(iconDrawable, mIconSize);
        if (info.contentDescription != null) {
            setContentDescription(info.contentDescription);
        }

        if (info.newInstalled) {
            SpannableString spannableString = new SpannableString(" " + info.title);
            spannableString.setSpan(new ImageSpan(getNewInstallPrefix(mLauncher), ImageSpan.ALIGN_BASELINE), 0, 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            setText(spannableString);
        } else {
            setText(info.title);
        }
        setTag(info);

        if (promiseStateChanged || info.isPromise()) {
            applyState(promiseStateChanged);
        }

        applyBadgeState(info, false /* animate */);
    }

    public void applyFromApplicationInfo(AppInfo info) {

        if (info != null){
            ComponentName componentName = info.componentName;
            if (componentName != null)
                mComponentName = componentName.flattenToString();
        }

        FastBitmapDrawable iconDrawable = mLauncher.createIconDrawable(info.iconBitmap);
        iconDrawable.setGhostModeEnabled(info.isDisabled != 0);

        if (info.isDisabled()) {
            iconDrawable.setState(FastBitmapDrawable.State.DISABLED);
        }

        initClock(info.componentName);

        setIcon(iconDrawable, mIconSize);

        if (info.newInstalled) {
            SpannableString spannableString = new SpannableString(" " + info.title);
            spannableString.setSpan(new ImageSpan(getNewInstallPrefix(mLauncher), ImageSpan.ALIGN_BASELINE), 0, 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            setText(spannableString);
        } else {
            setText(info.title);
        }
        if (info.contentDescription != null) {
            setContentDescription(info.contentDescription);
        }
        // We don't need to check the info since it's not a ShortcutInfo
        super.setTag(info);

        // Verify high res immediately
        verifyHighRes();

        applyBadgeState(info, false /* animate */);
    }

    public void applyFromPackageItemInfo(PackageItemInfo info) {
        mComponentName = info.packageName;

        setIcon(mLauncher.createIconDrawable(info.iconBitmap), mIconSize);
        setText(info.title);

        if (info.contentDescription != null) {
            setContentDescription(info.contentDescription);
        }
        // We don't need to check the info since it's not a ShortcutInfo
        super.setTag(info);

        // Verify high res immediately
        verifyHighRes();
    }

    /**
     * Overrides the default long press timeout.
     */
    public void setLongPressTimeout(int longPressTimeout) {
        mLongPressHelper.setLongPressTimeout(longPressTimeout);
    }

    public void setIsFolderName(boolean isFolderName) {
        mIsFolderName = isFolderName;
    }

    @Override
    public void setTag(Object tag) {
        if (tag != null) {
            LauncherModel.checkItemInfo((ItemInfo) tag);
        }
        super.setTag(tag);
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);

        if (!mIgnorePressedStateChange && !mIsFolderName) {
            updateIconState();
        }
    }

    /**
     * Returns the icon for this view.
     */
    public Drawable getIcon() {
        return mIcon;
    }

    /**
     * Returns whether the layout is horizontal.
     */
    public boolean isLayoutHorizontal() {
        return mLayoutHorizontal;
    }

    private void updateIconState() {
        if (mIcon instanceof FastBitmapDrawable) {
            FastBitmapDrawable d = (FastBitmapDrawable) mIcon;
            if (getTag() instanceof ItemInfo
                    && ((ItemInfo) getTag()).isDisabled()) {
                d.animateState(FastBitmapDrawable.State.DISABLED);
            } else if (isPressed()) {
                d.animateState(FastBitmapDrawable.State.PRESSED);
            } else {
                d.animateState(FastBitmapDrawable.State.NORMAL);
            }
        }
    }

    private void scaleSelf(float scale) {
        setScaleX(scale);
        setScaleY(scale);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() != MotionEvent.ACTION_DOWN || checkTouchArea(motionEvent)) {
            // Luôn ghi toạ độ điểm chạm ở ACTION_DOWN, kể cả khi chưa vào edit mode.
            // Cần cho trường hợp long-press (đang ở not-tidy-up) rồi mode chuyển sang
            // tidy-up giữa chừng: ACTION_MOVE sau đó phải đo khoảng cách từ điểm chạm
            // thật, tránh dùng giá trị cũ khiến drag khởi động sớm và đóng luôn dialog.
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                this.mTidyUpDownX = motionEvent.getX();
                this.mTidyUpDownY = motionEvent.getY();
            }
            if (!DragLayer.sTidyUping) {
                return handEventWhenNotTidyUp(motionEvent);
            } else
            {
                return handEventWhenTidyUp(motionEvent);
            }
        }

        return true;
    }

    public void clearPressedBackground() {
        setPressed(false);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (super.onKeyDown(keyCode, event)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Unlike touch events, keypress event propagate pressed state change immediately,
        // without waiting for onClickHandler to execute. Disable pressed state changes here
        // to avoid flickering.
        mIgnorePressedStateChange = true;
        boolean result = super.onKeyUp(keyCode, event);

        mIgnorePressedStateChange = false;
        updateIconState();
        return result;
    }

    @Override
    public void draw(Canvas canvas) {
        if (getCurrentTextColor() == Color.TRANSPARENT) {
            getPaint().clearShadowLayer();
        }
        super.draw(canvas);
        drawClock(canvas);
        drawUnreadEvent(canvas);
        drawBadgeIfNecessary(canvas);
        drawDelIcon(canvas);
        drawRecommendIcon(canvas);
    }

    /**
     * Draws the icon badge in the top right corner of the icon bounds.
     * @param canvas The canvas to draw to.
     */
    private void drawBadgeIfNecessary(Canvas canvas) {
        if (!mForceHideBadge && (hasBadge() || mBadgeScale > 0)) {
            getIconBounds(mTempIconBounds);
            mTempSpaceForBadgeOffset.set((getWidth() - mIconSize) / 2, getPaddingTop());
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();
            canvas.translate(scrollX, scrollY);
            //*/ wangqingsong add show miss number
            if(ShowBadgeNumber && mBadgeInfo !=null/* && BadgeList.contains(mBadgeInfo.getPackageUserKey().mPackageName)*/){
                if(mBadgeInfo.getPackageUserKey().mPackageName.contains("dialer")){
                    if(startCheckPermission()){
                        if (getMissCallCount() > 0) {
                            mBadgeRenderer.drawIconBadge(canvas,
                                    mBadgePalette,
                                    mBadgeInfo,
                                    mTempIconBounds,
                                    mBadgeScale,
                                    mTempSpaceForBadgeOffset,
                                    getMissCallCount());
                            Log.d("hct--->xiaopeng==401","getMissCallCount  ");
                        }
                    }
                    else {
                        if (mBadgeInfo.getNotificationCount() > 0) {
                            mBadgeRenderer.drawIconBadge(canvas,
                                    mBadgePalette,
                                    mBadgeInfo,
                                    mTempIconBounds,
                                    mBadgeScale,
                                    mTempSpaceForBadgeOffset,
                                    mBadgeInfo.getNotificationCount());
                            Log.d("hct--->xiaopeng==406","null......  ");
                        }
                    }
                }
                else {
                    if (mBadgeInfo.getNotificationCount() > 0) {
                        mBadgeRenderer.drawIconBadge(canvas,
                                mBadgePalette,
                                mBadgeInfo,
                                mTempIconBounds,
                                mBadgeScale,
                                mTempSpaceForBadgeOffset,
                                mBadgeInfo.getNotificationCount());
                        Log.d("hct--->xiaopeng==411","getNotificationCount  ");
                    }

                }
            }
//            else
//            {
//                mBadgeRenderer.draw(canvas, mBadgePalette, mBadgeInfo, mTempIconBounds, mBadgeScale,
//                    mTempSpaceForBadgeOffset);
//                    Log.d("hct--->xiaopeng==416","null......  ");
//            }

            canvas.translate(-scrollX, -scrollY);
        }
    }

    public void forceHideBadge(boolean forceHideBadge) {
        if (mForceHideBadge == forceHideBadge) {
            return;
        }
        mForceHideBadge = forceHideBadge;

        if (forceHideBadge) {
            invalidate();
        } else if (hasBadge()) {
            ObjectAnimator.ofFloat(this, BADGE_SCALE_PROPERTY, 0, 1).start();
        }
    }

    private boolean hasBadge() {
        return mBadgeInfo != null;
    }

    public void getIconBounds(Rect outBounds) {
        int top = getPaddingTop();
        int left = (getWidth() - mIconSize) / 2;
        int right = left + mIconSize;
        int bottom = top + mIconSize;
        outBounds.set(left, top, right, bottom);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mIcon instanceof PreloadIconDrawable) {
            ((PreloadIconDrawable) mIcon).applyPreloaderTheme(getPreloaderTheme());
        }
    }

    public void setTextSizePx(float size) {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }
    public boolean getTextVisibility(){
        return mTextVisibility;
    }
    public void setTextVisibility(boolean visible) {

        mTextVisibility = visible;

        Resources res = getResources();

        if (visible) {
            super.setTextColor(mTextColor);
        } else {

            super.setTextColor(res.getColor(android.R.color.transparent));
        }
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        mLongPressHelper.cancelLongPress();
    }

    public void applyState(boolean promiseStateChanged) {
        if (getTag() instanceof ShortcutInfo) {
            ShortcutInfo info = (ShortcutInfo) getTag();

            if (info.getTargetComponent() != null){
                this.mComponentName = info.getTargetComponent().flattenToString();
            }

            final boolean isPromise = info.isPromise();
            final int progressLevel = isPromise ?
                    ((info.hasStatusFlag(ShortcutInfo.FLAG_INSTALL_SESSION_ACTIVE) ?
                            info.getInstallProgress() : 0)) : 100;

            if (mIcon != null) {
                final PreloadIconDrawable preloadDrawable;
                if (mIcon instanceof PreloadIconDrawable) {
                    preloadDrawable = (PreloadIconDrawable) mIcon;
                } else {
                    preloadDrawable = new PreloadIconDrawable(mIcon, getPreloaderTheme());
                    setIcon(preloadDrawable, mIconSize);
                }

                preloadDrawable.setLevel(progressLevel);
                if (promiseStateChanged) {
                    preloadDrawable.maybePerformFinishedAnimation();
                }
            }
        }
    }

    public void applyBadgeState(ItemInfo itemInfo, boolean animate) {
        if (mIcon instanceof FastBitmapDrawable) {
            boolean wasBadged = mBadgeInfo != null;
            mBadgeInfo = mLauncher.getPopupDataProvider().getBadgeInfoForItem(itemInfo);
            boolean isBadged = mBadgeInfo != null;
            float newBadgeScale = isBadged ? 1f : 0;
            mBadgeRenderer = mLauncher.getDeviceProfile().mBadgeRenderer;
            if (wasBadged || isBadged) {
                mBadgePalette = IconPalette.getBadgePalette(getResources());
                if (mBadgePalette == null) {
                    mBadgePalette = ((FastBitmapDrawable) mIcon).getIconPalette();
                }
                // Animate when a badge is first added or when it is removed.
                if (animate && (wasBadged ^ isBadged) && isShown()) {
                    ObjectAnimator.ofFloat(this, BADGE_SCALE_PROPERTY, newBadgeScale).start();
                } else {
                    mBadgeScale = newBadgeScale;
                    invalidate();
                }
            }
        }
    }

    public IconPalette getBadgePalette() {
        return mBadgePalette;
    }

    private Theme getPreloaderTheme() {
        Object tag = getTag();
        int style = ((tag != null) && (tag instanceof ShortcutInfo) &&
                (((ShortcutInfo) tag).container >= 0)) ? R.style.PreloadIcon_Folder
                : R.style.PreloadIcon;
        Theme theme = sPreloaderThemes.get(style);
        if (theme == null) {
            theme = getResources().newTheme();
            theme.applyStyle(style, true);
            sPreloaderThemes.put(style, theme);
        }
        return theme;
    }

    /**
     * Sets the icon for this view based on the layout direction.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public Drawable setIcon(Drawable icon, int iconSize) {
        mIcon = icon;

        if (iconSize != -1) {
            mIcon.setBounds(0, 0, iconSize, iconSize);
        }
        if (mIsIconVisible) {
            applyCompoundDrawables(mIcon);
        }
        return icon;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void scaleIconSize(float scale) {
        if (mIcon != null) {
            mIconSize = (int) (scale * mLauncher.getDeviceProfile().originalIconSizePx);
            mIcon.setBounds(0, 0, mIconSize, mIconSize);
        }
        if (mIsIconVisible) {
            applyCompoundDrawables(mIcon);
        }
    }

    public void setIconVisible(boolean visible) {
        mIsIconVisible = visible;
        mDisableRelayout = true;
        Drawable icon = mIcon;
        if (!visible) {
            icon = new ColorDrawable(Color.TRANSPARENT);
            icon.setBounds(0, 0, 1, 1);
        }
        applyCompoundDrawables(icon);
        mDisableRelayout = false;
    }

    protected void applyCompoundDrawables(Drawable icon) {
        if (mLayoutHorizontal) {
            if (Utilities.ATLEAST_JB_MR1) {
                //mLabelTV.setCompoundDrawablesRelative(icon,null,null,null);
                setCompoundDrawablesRelative(icon, null, null, null);
            } else {
                setCompoundDrawables(icon,null,null,null);
                //setCompoundDrawables(icon, null, null, null);
            }
        } else {
            setCompoundDrawables(null, icon, null, null);
        }
    }

    @Override
    public void requestLayout() {
        if (!mDisableRelayout) {
            super.requestLayout();
        }
    }

    /**
     * Applies the item info if it is same as what the view is pointing to currently.
     */
    public void reapplyItemInfo(final ItemInfo info) {
        if (getTag() == info) {
            FastBitmapDrawable.State prevState = FastBitmapDrawable.State.NORMAL;
            if (mIcon instanceof FastBitmapDrawable) {
                prevState = ((FastBitmapDrawable) mIcon).getCurrentState();
            }
            mIconLoadRequest = null;
            mDisableRelayout = true;
            if (info instanceof AppInfo) {
                applyFromApplicationInfo((AppInfo) info);
            } else if (info instanceof ShortcutInfo) {
                applyFromShortcutInfo((ShortcutInfo) info,
                        LauncherAppState.getInstance().getIconCache());
                if ((info.rank < FolderIcon.NUM_ITEMS_IN_PREVIEW) && (info.container >= 0)) {
                    View folderIcon =
                            mLauncher.getWorkspace().getHomescreenIconByItemId(info.container);
                    if (folderIcon != null) {
                        folderIcon.invalidate();
                    }
                }
            } else if (info instanceof PackageItemInfo) {
                applyFromPackageItemInfo((PackageItemInfo) info);
            }

            // If we are reapplying over an old icon, then we should update the new icon to the same
            // state as the old icon
            if (mIcon instanceof FastBitmapDrawable) {
                ((FastBitmapDrawable) mIcon).setState(prevState);
            }

            mDisableRelayout = false;
        }
    }

    /**
     * Verifies that the current icon is high-res otherwise posts a request to load the icon.
     */
    public void verifyHighRes() {
        if (mIconLoadRequest != null) {
            mIconLoadRequest.cancel();
            mIconLoadRequest = null;
        }
        if (getTag() instanceof AppInfo) {
            AppInfo info = (AppInfo) getTag();
            if (info.usingLowResIcon) {
                mIconLoadRequest = LauncherAppState.getInstance().getIconCache()
                        .updateIconInBackground(BubbleTextView.this, info);
            }
        } else if (getTag() instanceof ShortcutInfo) {
            ShortcutInfo info = (ShortcutInfo) getTag();
            if (info.usingLowResIcon) {
                mIconLoadRequest = LauncherAppState.getInstance().getIconCache()
                        .updateIconInBackground(BubbleTextView.this, info);
            }
        } else if (getTag() instanceof PackageItemInfo) {
            PackageItemInfo info = (PackageItemInfo) getTag();
            if (info.usingLowResIcon) {
                mIconLoadRequest = LauncherAppState.getInstance().getIconCache()
                        .updateIconInBackground(BubbleTextView.this, info);
            }
        }
    }

    private void drawUnreadEvent(Canvas canvas) {
        UnreadLoaderCompact.drawUnreadEventIfNeed(mLauncher, canvas, this);
    }

    // --- IShakeInterface implementation --
    @Override
    public void beginOrAdjustHintAnimations() {
        if (this.mShakeAnimators == null) {
            this.mShakeAnimators = new ReorderHintAnimation(this);
            this.mShakeAnimators.animate();
        }
    }

    @Override
    public void beginOrAdjustHintAnimations(int i) {
        if (this.mShakeAnimators == null) {
            this.mShakeAnimators = new ReorderHintAnimation(this);
            this.mShakeAnimators.animate(i);
        }
    }

    @Override
    public void completeAndClearReorderHintAnimations() {
        if (this.mShakeAnimators != null) {
            this.mShakeAnimators.completeAnimationImmediately();
            this.mShakeAnimators = null;
        }
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

    /**
     * Interface to be implemented by the grand parent to allow click shadow effect.
     */
    public interface BubbleTextShadowHandler {
        void setPressedIcon(BubbleTextView icon, Bitmap background);
    }


    private boolean startCheckPermission() {
        // Log.d("startCheckPermission", "startCheckPermission");
        if (ContextCompat.checkSelfPermission(mLauncher, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    private int getMissCallCount() {

        int missCallCount = 0;
        Uri missingCallUri = Calls.CONTENT_URI;
        String where = Calls.TYPE + "='" + Calls.MISSED_TYPE + "'" + " AND new=1";
        Cursor cursorCall = null;
        try {
            cursorCall = mLauncher.getContentResolver().query(missingCallUri,
                    null, where, null, null);
        } catch (SQLiteException e) {
            return missCallCount;
        }

        if (cursorCall != null) {
            missCallCount = cursorCall.getCount();
            cursorCall.close();
        }

        return missCallCount;
    }

    private void initClock(ComponentName componentName) {
        if (componentName == null)
            return;
//        isClock = false;
//        AppTypeProvider appTypeProvider = AppTypeProvider.getAppTypeProvider();
//        ComponentName cn = appTypeProvider.getClockComp();
//        if (cn != null && cn.equals(componentName)) {
//            if (mClockDrawable == null) {
//                mClockDrawable = new ClockDrawable(getContext(), mIconSize, mIconSize, -1);
//            }
//            isClock = true;
//            postDraw();
//        }
    }

    private void postDraw() {
        if (isClock) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    postInvalidate();
                    postDraw();
                }
            }, 1000);
        }
    }

    private void drawClock(Canvas canvas) {
        if (mIsIconVisible && mClockDrawable != null && isClock) {
            mClockDrawable.draw(canvas);
        }
    }

    public static class ReorderHintAnimation {
        private static final int DURATION = 300;

        ObjectAnimator objectAnimator1;

        ObjectAnimator objectAnimator2;

        AnimatorSet animatorSet;

        View child;

        public ReorderHintAnimation(View view) {
            this.child = view;
        }

        private void cancel() {
            child.clearAnimation();
            if (objectAnimator1 != null) {
                objectAnimator1.setRepeatCount(1);
                if (objectAnimator1.isRunning()) {
                    objectAnimator1.cancel();
                }
                objectAnimator1 = null;
            }

            if (objectAnimator2 != null) {
                objectAnimator2.setRepeatCount(1);
                if (objectAnimator2.isRunning()) {
                    objectAnimator2.cancel();
                }
                objectAnimator2 = null;
            }

            if (animatorSet != null) {
                animatorSet.setStartDelay(0L);
                if (animatorSet.isRunning()) {
                    animatorSet.cancel();
                }
                animatorSet = null;
            }
        }

        public void completeAnimationImmediately() {
            this.child.setScaleX(1.0f);
            this.child.setScaleY(1.0f);
            this.child.setTranslationX(0.0f);
            this.child.setTranslationY(0.0f);
            this.child.setRotation(0);
            if (animatorSet != null)
                animatorSet.pause();
            cancel();
            if (this.animatorSet == null) {
                return;
            }

        }

        public void join(View view) {

        }

        public void animate() {
            animate(DURATION);
        }

        public void animate(int duration) {
            ObjectAnimator objectAnimator = ObjectAnimator.ofPropertyValuesHolder(child, PropertyValuesHolder.ofFloat(View.ROTATION, -1.0f, 1.0f));
            // : ObjectAnimator.ofPropertyValuesHolder(child, PropertyValuesHolder.ofFloat(View.ROTATION, -2.5f, 2.5f));;
            objectAnimator1 = objectAnimator;
            objectAnimator.setDuration(duration);
            objectAnimator.setRepeatCount(-1);
            objectAnimator.setRepeatMode(ValueAnimator.REVERSE);
            objectAnimator.setInterpolator(new LinearInterpolator());

            ObjectAnimator objectAnimator2 = ObjectAnimator.ofPropertyValuesHolder(child, PropertyValuesHolder.ofFloat(View.SCALE_X, 0.99f, 1.01f), PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.99f, 1.01f));;
            this.objectAnimator2 = objectAnimator2;
            objectAnimator2.setDuration(duration);
            objectAnimator2.setRepeatCount(-1);
            objectAnimator2.setRepeatMode(ValueAnimator.REVERSE);
            objectAnimator2.setInterpolator(new LinearInterpolator());

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(objectAnimator,objectAnimator2);
            animatorSet.setStartDelay(new Random().nextInt(300));
            animatorSet.start();
        }
    }

    public boolean uninstallEnable() {
        return this.mShortcutInfo != null &&
                (this.mShortcutInfo.uninstallable || this.mShortcutInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT);
    }

    /**
     * Có được VẼ dấu trừ ở edit mode hay không — TÁCH BIỆT với {@link #uninstallEnable()}
     * (= app có GỠ CÀI ĐẶT được hay không).
     *
     * LÝ DO TÁCH: trước đây dấu trừ bám theo uninstallEnable(), mà app hệ thống (Điện thoại, Danh bạ,
     * Tin nhắn, Camera — 4 app mặc định trong hotseat) bị LauncherModel gán uninstallable = false khi
     * flags == 0, nên hotseat KHÔNG hề hiện dấu trừ dù vẫn rung lắc. Theo hành vi iOS, MỌI app đều
     * hiện dấu trừ khi edit; app hệ thống chỉ khác ở chỗ dialog không có mục "Xóa ứng dụng" (xử lý ở
     * {@link Launcher#showRemoveAppDialog}), chứ không phải giấu luôn dấu trừ.
     */
    public boolean canShowDelIcon() {
        return this.mShortcutInfo != null;
    }

    /**
     * Lề trái của dấu trừ, tính theo bề rộng THẬT của chính view này.
     *
     * BUG FIX: trước đây dùng thẳng {@link #mDelIconLeftPadding} — hằng số tính 1 lần lúc dựng view
     * theo {@code Launcher.getCellWidth()} (bề rộng ô WORKSPACE). Ô hotseat rộng khác ô workspace
     * (đo trên Samsung A50: view hotseat 254px), nên ở hotseat dấu trừ bị lệch: vùng chạm rơi ra
     * ngoài ảnh dấu trừ -> bấm dấu trừ ở dock lại MỞ APP thay vì hiện dialog.
     * Tính lại theo getWidth() để 2 chỗ (vẽ + chạm) luôn khớp nhau ở mọi container.
     */
    private int getDelIconLeftPadding() {
        int width = getWidth();
        if (width <= 0) {
            // Chưa layout xong -> tạm dùng giá trị cũ để không vẽ lệch ra ngoài.
            return mDelIconLeftPadding;
        }
        return (width - mIconSize - mDelIconSize) / 2;
    }

    public void drawDelIcon(Canvas canvas) {
        int scrollX = getScrollX();
        int scrollY = getScrollY();
        float scale = !this.mNormalSize ? DelIconAnim.getScale() : 1.0f;
        boolean canUninstall = canShowDelIcon();
        boolean isRefresh = DelIconAnim.shouldRefresh();
        if (canUninstall && isRefresh) {
            canvas.save();
            canvas.translate((float) scrollX, (float) scrollY);

            Rect bound = new Rect();
            getIconBounds(bound);

            int left = (int)(mDelIconSize / 2 - mDelIconSize * scale * .5f) + getDelIconLeftPadding();
            int top = (int)(mDelIconSize / 2 - mDelIconSize * scale * .5f);

            Rect rect = new Rect(
                    left,
                    top,
                    (int) (((float) left) + (((float) mDelIconSize) * scale)),
                    (int) (((float) top) + (((float) mDelIconSize) * scale)));

            this.mLeftPaint.setAlpha((int) (scale * 255.0f));
            VectorDrawable drawable = (VectorDrawable)getContext().getResources().getDrawable(R.drawable.delete_button);
            drawable.setBounds(rect);
            drawable.draw(canvas);
            canvas.translate((float) (-scrollX), (float) (-scrollY));
            canvas.restore();
        }
    }

    public void drawRecommendIcon(Canvas canvas) {
        if (this.mShortcutInfo == null)
            return;

        if (this.mShortcutInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_VIRTUAL_APP && this.mShortcutInfo.url != null) {
            if (this.mRecommendRect == null) {
                int dimensionPixelOffset = getContext().getResources().getDimensionPixelOffset(R.dimen.recommand_icon_width);
                int dimensionPixelOffset2 = getContext().getResources().getDimensionPixelOffset(R.dimen.recommand_icon_height);
                int width = getWidth();
                int iconDimen = ((width - ((width - Utilities.getIconDimen(getContext())) / 2)) + (dimensionPixelOffset / 3)) - dimensionPixelOffset;
                int i = iconDimen + dimensionPixelOffset;
                int i2 = dimensionPixelOffset2 + 0;
                if (i >= width - 6) {
                    i = width - 6;
                    iconDimen = i - dimensionPixelOffset;
                }
                this.mRecommendRect = new Rect(iconDimen, 0, i, i2);
                this.mRecommendDrawable.setBounds(this.mRecommendRect);
            }
            canvas.save();
            canvas.translate((float) getScrollX(), (float) getScrollY());
            this.mRecommendDrawable.draw(canvas);
            canvas.translate((float) (-getScrollX()), (float) (-getScrollY()));
            canvas.restore();
        }
    }

    public boolean checkTouchArea(MotionEvent motionEvent) {
        /*
        if (getCompoundDrawables()[1] != null) {
            Rect rect = new Rect();
            int lineHeight = getLineHeight();
            int paddingTop = getPaddingTop();
            getGlobalVisibleRect(rect);
            int i = (rect.right - rect.left) / 2;
            int iconWidth = Utilities.getIconWidth() / 2;
            if (!new Rect(i - iconWidth, 0, i + iconWidth, (rect.bottom - rect.top) + paddingTop + (lineHeight * 2)).contains((int) motionEvent.getX(), (int) motionEvent.getY())) {
                return false;
            }
        }

         */
        return true;
    }

    private boolean checkUninstallPressed(int x, int y) {

        // Dùng canShowDelIcon() (KHÔNG phải uninstallEnable()) để vùng chạm khớp ĐÚNG với điều kiện
        // vẽ ở drawDelIcon() — nếu lệch nhau thì app hệ thống sẽ thấy dấu trừ mà bấm không ăn.
        if (!canShowDelIcon()) {
            return false;
        }

        Rect bound = new Rect();
        getIconBounds(bound);

        int scale = 1;
        // BUG FIX: thiếu lề trái nên vùng chạm LỆCH khỏi dấu trừ đang vẽ (drawDelIcon() có cộng lề).
        // Dùng CHUNG getDelIconLeftPadding() với lúc vẽ để 2 chỗ luôn khớp, kể cả trong hotseat
        // (ô hotseat rộng khác ô workspace).
        int left = (int)(mDelIconSize / 2 - mDelIconSize * scale * .5f) + getDelIconLeftPadding();
        int top = (int)(mDelIconSize / 2 - mDelIconSize * scale * .5f);

        // Nới vùng chạm ra mỗi phía 1/4 kích thước dấu trừ: dấu trừ khá nhỏ (45% icon), chạm sát mép
        // rất dễ trượt ra ngoài -> rơi xuống nhánh mở app. Nới nhẹ để bấm "ăn" như iOS.
        int touchPadding = mDelIconSize / 4;
        Rect rect = new Rect(
                left - touchPadding,
                top - touchPadding,
                (int) (((float) left) + (((float) mDelIconSize) * scale)) + touchPadding,
                (int) (((float) top) + (((float) mDelIconSize) * scale)) + touchPadding);

        return rect.contains(x,y);
    }

    public void click() {
        ShortcutInfo shortcutInfo = this.mShortcutInfo;
        if (shortcutInfo != null && shortcutInfo.itemType == ITEM_TYPE_VIRTUAL_APP) {
            int shortcutId = shortcutInfo.intent.getIntExtra(Shortcuts.SHORTCUT_ID, -1);
            switch (shortcutId) {
                case Shortcuts.SHORTCUT_T9_SEARCH:
                    this.mLauncher.closeFolder();
//                    this.mLauncher.showSearchView();
                    return;
                case Shortcuts.SHORTCUT_LOCKSCREEN:
//                    this.mLauncher.showLockScreen();
                    return;
                case Shortcuts.SHORTCUT_OS_SETTING:
//                    this.mLauncher.startActivity(new Intent(this.mLauncher, LauncherPreferenceActivity.class));
                    return;
                case Shortcuts.SHORTCUT_RESPONSE:
//                    this.mLauncher.startActivity(new Intent(this.mLauncher, FeedbackActivity.class));
                    return;
                case Shortcuts.SHORTCUT_CHANGE_THEME:
//                    Folder openFolder = this.mLauncher.getOpenFolder();
//                    if (openFolder != null) {
//                        openFolder.resetContentChanged();
//                    }
//                    this.mLauncher.setRandomTheme(0);
                    return;
                case Shortcuts.SHORTCUT_THEMECULB:
//                    this.mLauncher.startInnerActivity(new ComponentName(this.mLauncher, MainActivity.class));
                    return;
                case Shortcuts.SHORTCUT_SECURITY:
//                    this.mLauncher.startInnerActivity(new ComponentName(this.mLauncher, SC_NewLauncherActivity.class));
                    return;
                default:
                    this.mLauncher.onClick(this);
                    return;
            }
        } else {
            this.mLauncher.onClick(this);
        }
    }

    public boolean handEventWhenNotTidyUp(MotionEvent motionEvent) {
        boolean z = false;
        boolean onTouchEvent = super.onTouchEvent(motionEvent);
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                this.mLongPressHelper.postCheckForLongPress();
                break;
            case MotionEvent.ACTION_UP:
                boolean isLongPressed = mLongPressHelper.hasPerformedLongPress();
                this.mLongPressHelper.cancelLongPress();
                if (this.mLauncher != null && checkTouchArea(motionEvent)) {
                    Workspace workspace = this.mLauncher.getWorkspace();
                    if (!(getParent() instanceof Hotseat) || this.mLauncher.isInBatchMode()) {
                        if (workspace.isSpringLoaded()) {
                            if (!this.mLauncher.isInBatchMode()) {
                                break;
                            } else {
                                if (!this.mIsBatchSeleted) {
                                    z = true;
                                }
                                break;
                            }
                        } else {
                            if (!isLongPressed) {
                                playSoundEffect(SoundEffectConstants.CLICK);
                                click();
                            }
                            break;
                        }
                    } else {
                        if (!workspace.isSpringLoaded()) {
                            if (!isLongPressed) {
                                playSoundEffect(SoundEffectConstants.CLICK);
                                click();
                            }
                        }
                        return onTouchEvent;
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                this.mLongPressHelper.cancelLongPress();
                break;
        }
        return onTouchEvent;
    }

    public boolean handEventWhenTidyUp(MotionEvent motionEvent) {
        boolean onTouchEvent = super.onTouchEvent(motionEvent);
        int x = (int) motionEvent.getX();
        int y = (int) motionEvent.getY();
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                this.mTidyUpDownX = motionEvent.getX();
                this.mTidyUpDownY = motionEvent.getY();
                if (!checkUninstallPressed(x, y)) {
                    this.mLongPressHelper.postCheckForLongPress();
                    this.mPressedDelIcon = false;
                    return true;
                } else {
                    this.mPressedDelIcon = true;
                    cancelLongPress();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                // iOS-style: đang ở edit mode, chạm & kéo icon để bắt đầu di chuyển ngay
                if (!this.mPressedDelIcon
                        && this.mLauncher != null
                        && (getTag() instanceof ItemInfo)
                        && !this.mLauncher.getDragController().isDragging()) {
                    int touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
                    if (Math.abs(motionEvent.getX() - this.mTidyUpDownX) > touchSlop
                            || Math.abs(motionEvent.getY() - this.mTidyUpDownY) > touchSlop) {
                        this.mLongPressHelper.cancelLongPress();
                        // Đóng menu nổi nếu đang mở để không nuốt sự kiện kéo
                        this.mLauncher.closeFloatingMenu();
                        CellLayout.CellInfo cellInfo =
                                new CellLayout.CellInfo(this, (ItemInfo) getTag());
                        this.mLauncher.getWorkspace().startDrag(cellInfo, false);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (this.mPressedDelIcon) {
                    post(new Runnable() {
                        public void run() {
                            // Bấm dấu trừ KHÔNG gỡ cài đặt ngay nữa (trước đây gọi thẳng
                            // uninstallApplication() -> bắn Intent.ACTION_DELETE). Theo hành vi iOS:
                            // mở dialog cho người dùng chọn Gỡ khỏi màn hình / Xóa ứng dụng / Hủy.
                            BubbleTextView.this.mLauncher.showRemoveAppDialog(BubbleTextView.this.mShortcutInfo);
                        }
                    });
                }
                /*else {
                    playSoundEffect(SoundEffectConstants.CLICK);
                    click();
                }
                 */
                else {
//                    this.mLongPressHelper.cancelLongPress();
                    this.mPressedDelIcon = false;
                    return true;
                }
                this.mLongPressHelper.cancelLongPress();
                this.mPressedDelIcon = false;
                break;
            case MotionEvent.ACTION_CANCEL:
                this.mLongPressHelper.cancelLongPress();
                this.mPressedDelIcon = false;
                break;
        }
        return onTouchEvent;
    }

    public boolean isBatchSelected() {
        return this.mIsBatchSeleted;
    }

    /* Implementation Of PopUp Interface */

    @Override
    public void showLabel(boolean flag) {
        setTextVisibility(flag);
    }

    @Override
    public int getContentHeight() {
        return getIcon().getBounds().height();
    }

    @Override
    public int getContentWidth() {
       return getWidth() - getTotalPaddingLeft() - getTotalPaddingRight();
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

    boolean mNormalSize;
    protected ShortcutInfo mShortcutInfo;
    private int mDelIconLeftPadding;
    private int mDelIconTopPadding;
    private int mDelIconSize;

    private Drawable mNewInstallPrefix;
    private Paint mLeftPaint;
    private boolean mPressedDelIcon = false;
    protected Rect mRecommendRect;
    protected Drawable mRecommendDrawable;
    private boolean mIsBatchSeleted;
    private boolean mTextVisibility = true;

    // Toạ độ điểm chạm ban đầu để phát hiện thao tác kéo trong edit mode (tidy-up)
    private float mTidyUpDownX;
    private float mTidyUpDownY;
}
