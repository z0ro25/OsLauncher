package com.truongnt.ios.launcher.widget.view;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import com.truongnt.ios.launcher.R;

/**
 * Kính mờ compositor bằng API public {@link Window#setBackgroundBlurRadius} (API 31+): cùng cơ chế
 * blur-trong-khung như createBackgroundBlurDrawable cũ nhưng không reflection. Bọc {@link Dialog} để
 * có Window; vẫn là sub-window MEDIA (dưới window launcher, trên wallpaper), không focus/chạm.
 */
@TargetApi(Build.VERSION_CODES.S)
public final class CompositorBlurWindow {

    private static final String TAG = "CompositorBlurWin";

    private final Context mContext;
    private final int mBlurRadius;
    private final float mCornerRadius;
    private final int mTintColor;
    private final int mStrokeWidth;
    private final int mStrokeColor;

    private Dialog mDialog;

    public CompositorBlurWindow(Context context, int blurRadiusPx, float cornerRadiusPx,
                                int tintColor, float strokeWidthPx, int strokeColor) {
        mContext = context;
        mBlurRadius = blurRadiusPx;
        mCornerRadius = cornerRadiusPx;
        mTintColor = tintColor;
        mStrokeWidth = strokeWidthPx > 0f ? Math.max(1, Math.round(strokeWidthPx)) : 0;
        mStrokeColor = strokeColor;
    }

    public boolean isShowing() {
        return mDialog != null;
    }

    /** Hiện window tại [rect] (toạ độ màn hình), bám token của window launcher. */
    public boolean show(IBinder token, Rect rect) {
        dismiss();
        if (token == null) return false;
        // Mỗi lần hiện tạo Dialog MỚI: DecorView detach là bỏ blur drawable, re-attach không đăng ký
        // lại listener cập nhật bo góc -> dùng lại Dialog cũ thì vùng blur mất bo góc.
        Dialog dialog = new Dialog(mContext, R.style.CompositorBlurWindow);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        Window w = dialog.getWindow();
        if (w == null) return false;
        // Dựng decor TRƯỚC: generateLayout của window floating ghi đè size/flags khi decor được tạo.
        w.getDecorView();
        w.setBackgroundDrawable(buildBackground());
        w.setDecorFitsSystemWindows(false);

        // Params mới tinh y hệt overlay View cũ (setAttributes = copyFrom ghi đè cả flags/privateFlags
        // theme đã thêm) -> vị trí/xếp lớp giống hệt trước. Ép HW: vùng blur gửi từ RenderThread.
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_MEDIA;
        lp.token = token;
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.format = PixelFormat.TRANSLUCENT;
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        lp.windowAnimations = 0;
        applyRect(lp, rect);
        w.setAttributes(lp);
        w.setBackgroundBlurRadius(mBlurRadius);

        try {
            dialog.show();
        } catch (Throwable t) {
            android.util.Log.e(TAG, "show fail", t);
            return false;
        }
        mDialog = dialog;
        return true;
    }

    /** Dời/đổi kích thước window đang hiện. */
    public void update(Rect rect) {
        if (mDialog == null) return;
        Window w = mDialog.getWindow();
        if (w == null) return;
        WindowManager.LayoutParams lp = w.getAttributes();
        applyRect(lp, rect);
        try {
            w.setAttributes(lp);
        } catch (Throwable t) {
            android.util.Log.e(TAG, "update fail", t);
        }
    }

    public void dismiss() {
        if (mDialog == null) return;
        try {
            mDialog.dismiss();
        } catch (Throwable ignore) {
        }
        mDialog = null;
    }

    private static void applyRect(WindowManager.LayoutParams lp, Rect rect) {
        lp.x = rect.left;
        lp.y = rect.top;
        lp.width = rect.width();
        lp.height = rect.height();
    }

    // Hệ thống lấy bo góc vùng blur từ outline của nền này và vẽ nó (tint + viền) đè lên vùng blur.
    private GradientDrawable buildBackground() {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(mCornerRadius);
        bg.setColor(mTintColor);
        if (mStrokeWidth > 0) bg.setStroke(mStrokeWidth, mStrokeColor);
        return bg;
    }
}
