package com.amz.ios.launcher.awareness;


import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.NinePatchDrawable;
import android.view.View;

import com.amz.ios.ioslite.common.debug.DebugUtil;
import com.amz.ios.ioslite.common.util.DeviceInfoUtil;
import com.amz.ios.launcher.AppInfo;
import com.amz.ios.launcher.DeviceProfile;
import com.amz.ios.launcher.FolderInfo;
import com.amz.ios.launcher.ItemInfo;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.LauncherSettings;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.ShortcutInfo;

public abstract class UnreadLoaderCompact {
    private static final String TAG = "UnreadLoaderCompact";

    private static final int MAX_UNREAD_COUNT = 99;

    private static final Object sInstanceLock = new Object();
    private static UnreadLoaderCompact sInstance;

    public static UnreadLoaderCompact getInstance(Context context) {
        synchronized (sInstanceLock) {
            if (sInstance == null) {
                if (DeviceInfoUtil.isIOSOs() && DroiUnreadLoder.isAvaliable(context)) {
                    sInstance = new DroiUnreadLoder(context.getApplicationContext());
                } else {
                    if (DeviceInfoUtil.isMtkPlatform()) {
                        sInstance = new MTKUnreadLoader(context.getApplicationContext());
                    }
                }
            }
            return sInstance;
        }
    }

    public abstract void initInitFlag();
    public abstract void initialize(Launcher launcher, UnreadCallbacks callbacks);

    public abstract void loadAndInitUnreadShortcuts();

    public abstract void onCancel(Launcher launcher);

    abstract int getUnreadNumberOfComp(ComponentName component);


    /**
     * Get unread number for the given component.
     *
     * @param component
     * @return
     */
    public static int getUnreadNumberOfComponent(ComponentName component) {
        if (sInstance != null) {
            return sInstance.getUnreadNumberOfComp(component);
        }
        return 0;
    }


    /**
     * Draw unread number for the given icon.
     *
     * @param canvas
     * @param icon
     * @return
     */
    public static void drawUnreadEventIfNeed(Launcher launcher, Canvas canvas, View icon) {
        ItemInfo info = (ItemInfo) icon.getTag();
        if (info != null && info.unreadNum > 0) {
            Resources res = icon.getContext().getResources();

            Paint unreadTextNumberPaint = new Paint();
            unreadTextNumberPaint.setAntiAlias(true);
            unreadTextNumberPaint.setTypeface(Typeface.DEFAULT);
            unreadTextNumberPaint.setTextSize(res.getDimension(R.dimen.unread_text_number_size));
            unreadTextNumberPaint.setTextAlign(Paint.Align.CENTER);
            unreadTextNumberPaint.setColor(Color.WHITE);

            Paint unreadTextPlusPaint = new Paint(unreadTextNumberPaint);
            unreadTextPlusPaint.setTextSize(res.getDimension(R.dimen.unread_text_plus_size));

            String unreadTextNumber;
            String unreadTextPlus = "+";
            Rect unreadTextNumberBounds = new Rect(0, 0, 0, 0);
            Rect unreadTextPlusBounds = new Rect(0, 0, 0, 0);
            if (info.unreadNum > MAX_UNREAD_COUNT) {
                unreadTextNumber = String.valueOf(MAX_UNREAD_COUNT);
                unreadTextPlusPaint.getTextBounds(unreadTextPlus, 0,
                        unreadTextPlus.length(), unreadTextPlusBounds);
            } else {
                unreadTextNumber = String.valueOf(info.unreadNum);
            }
            unreadTextNumberPaint.getTextBounds(unreadTextNumber, 0,
                    unreadTextNumber.length(), unreadTextNumberBounds);
            int textHeight = unreadTextNumberBounds.height();
            int textWidth = unreadTextNumberBounds.width() + unreadTextPlusBounds.width();

            /// M: Draw unread background image.
            NinePatchDrawable unreadBgNinePatchDrawable =
                    (NinePatchDrawable) res.getDrawable(R.drawable.ic_newevents_numberindication);

            int unreadBgWidth = res.getDimensionPixelSize(R.dimen.unread_event_bg_width);
            int unreadBgHeight = res.getDimensionPixelSize(R.dimen.unread_event_bg_height);
            int unreadBgPadding = res.getDimensionPixelSize(R.dimen.unread_event_bg_padding);
            int unreadYOffset = res.getDimensionPixelSize(R.dimen.unread_event_y_offset);

            if (unreadBgWidth < textWidth + 2 * unreadBgPadding) {
                unreadBgWidth = textWidth + 2 * unreadBgPadding;
                unreadBgHeight = unreadBgWidth;
            }

            Rect unreadBgBounds = new Rect(0, 0, unreadBgWidth, unreadBgHeight);
            unreadBgNinePatchDrawable.setBounds(unreadBgBounds);

            int unreadMarginTop = 0;
            int unreadMarginRight = 0;
            int width = icon.getWidth();
            DeviceProfile grid = launcher.getDeviceProfile();
            if (info instanceof ShortcutInfo) {
                if (info.container == (long) LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                    unreadMarginTop = icon.getPaddingTop() - unreadBgWidth / 5;
                    unreadMarginRight = (width - grid.hotseatIconSizePx) / 2 + unreadBgWidth * 3 / 4;
                } else {
                    unreadMarginTop = icon.getPaddingTop() - unreadBgWidth / 5;
                    unreadMarginRight = (width - grid.iconSizePx) / 2 + unreadBgWidth * 3 / 4;
                }
            } else if (info instanceof FolderInfo) {
                unreadMarginTop = icon.getPaddingTop() - unreadBgWidth / 5;
                unreadMarginRight = (width - grid.folderIconSizePx) / 2 + unreadBgWidth * 3 / 4;
            } else if (info instanceof AppInfo) {
                // AllApps not show unread number, just return;
                return;
            }

            int unreadBgPosX = icon.getScrollX() + icon.getWidth() - unreadMarginRight;
            int unreadBgPosY = icon.getScrollY() + unreadMarginTop + unreadYOffset;
            if (unreadBgPosY < 0) {
                unreadBgPosY = 0;
            }

            canvas.save();
            canvas.translate(unreadBgPosX, unreadBgPosY);

            if (unreadBgNinePatchDrawable != null) {
                unreadBgNinePatchDrawable.draw(canvas);
            } else {
                DebugUtil.debugUnread(TAG, "drawUnreadEventIfNeed: "
                        + "unreadBgNinePatchDrawable is null pointer");
                return;
            }

            /// M: Draw unread text.
            Paint.FontMetrics fontMetrics = unreadTextNumberPaint.getFontMetrics();
            if (info.unreadNum > MAX_UNREAD_COUNT) {
                canvas.drawText(unreadTextNumber,
                        (unreadBgWidth - unreadTextPlusBounds.width()) / 2,
                        (unreadBgHeight + textHeight) / 2,
                        unreadTextNumberPaint);
                canvas.drawText(unreadTextPlus,
                        (unreadBgWidth + unreadTextNumberBounds.width()) / 2,
                        (unreadBgHeight + textHeight) / 2 + fontMetrics.ascent / 2,
                        unreadTextPlusPaint);
            } else {
                canvas.drawText(unreadTextNumber,
                        unreadBgWidth / 2,
                        (unreadBgHeight + textHeight) / 2,
                        unreadTextNumberPaint);
            }

            canvas.restore();
        }
    }
}
