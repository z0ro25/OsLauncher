package com.amz.ios.launcher.widget;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.amz.ios.launcher.DeviceProfile;
import com.amz.ios.launcher.LauncherAppWidgetProviderInfo;

/**
 * Dựng preview SỐNG (live) cho widget iOS: inflate {@code initialLayout} thật rồi thu nhỏ vừa
 * khung preview, giữ nguyên tỉ lệ spanX:spanY. Nhờ vậy preview hiển thị ĐÚNG widget và tự chạy
 * (đồng hồ chạy kim, kính mờ...) thay vì ảnh tĩnh {@code previewImage}.
 *
 * CHỈ áp dụng cho widget iOS (custom view). Widget hệ thống (RemoteViews) vẫn đi đường bitmap
 * tĩnh cũ — helper này KHÔNG đụng tới để tránh ảnh hưởng chức năng khác.
 */
public final class LiveWidgetPreviewHelper {

    private LiveWidgetPreviewHelper() {}

    /** true nếu parcelable là widget iOS có initialLayout hợp lệ để dựng preview sống. */
    public static boolean isLivePreviewSupported(Object parcelable) {
        if (!(parcelable instanceof LauncherAppWidgetProviderInfo)) {
            return false;
        }
        LauncherAppWidgetProviderInfo info = (LauncherAppWidgetProviderInfo) parcelable;
        return info.isIOSWidget && info.initialLayout != 0 && info.initialLayout != -1;
    }

    /**
     * Dựng host chứa widget đã inflate. Host đặt vào khung preview với match_parent; việc scale
     * widget cho vừa khung (letterbox theo tỉ lệ span) được tính lại mỗi lần host đổi kích thước
     * — nên không cần biết kích thước khung tại thời điểm gọi.
     *
     * @return host view, hoặc null nếu không dựng được.
     */
    public static View build(Context context, final LauncherAppWidgetProviderInfo info,
                             final DeviceProfile grid) {
        try {
            final FrameLayout host = new FrameLayout(context);
            host.setClipChildren(true);

            final View widgetView = LayoutInflater.from(context).inflate(
                    info.initialLayout, host, false);

            // Preview chỉ để NHÌN. Một số layout (vd Photos) đặt clickable/focusable ở root để
            // widget ĐÃ ĐẶT mở configure khi chạm — nhưng trong khay, view đó sẽ "nuốt" cú chạm
            // khiến ô thẻ (cell) không nhận được click mở carousel. Vô hiệu hoá touch toàn cây
            // preview để cú chạm truyền lên cell. Không ảnh hưởng widget đặt màn (đi qua createView).
            disableTouch(widgetView);

            final int spanX = Math.max(1, info.spanX);
            final int spanY = Math.max(1, info.spanY);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER;
            host.addView(widgetView, lp);

            // Preview widget Battery: bind vòng pin + % (widget nội bộ không đi qua onUpdate)
            // để preview sống hiện đúng như khi đặt màn.
            if (info.provider != null
                    && com.amz.ios.launcher.widget.widgetprovider.BatteryWidgetProvider.class
                    .getName().equals(info.provider.getClassName())) {
                com.amz.ios.launcher.widget.widgetprovider.BatteryWidgetProvider
                        .bindInflatedView(context, widgetView);
            }

            // Preview widget Photos (cả 3 size): bind ảnh gần đây + overlay để preview khớp khi đặt màn
            // (nếu chưa cấp quyền -> ảnh default, ẩn overlay, giống hành vi thật).
            if (info.provider != null) {
                String cls = info.provider.getClassName();
                if (com.amz.ios.launcher.widget.widgetprovider.PictureAppWidgetProvider.class.getName().equals(cls)
                        || com.amz.ios.launcher.widget.widgetprovider.PictureMediumWidgetProvider.class.getName().equals(cls)
                        || com.amz.ios.launcher.widget.widgetprovider.PictureLargeWidgetProvider.class.getName().equals(cls)) {
                    com.amz.ios.launcher.widget.widgetprovider.PictureAppWidgetProvider
                            .bindInflatedView(context, widgetView);
                }
            }

            host.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int l, int t, int r, int b,
                                           int ol, int ot, int or, int ob) {
                    applyScale(widgetView, grid, spanX, spanY, r - l, b - t);
                }
            });
            // Host có thể được thêm vào một khung ĐÃ layout sẵn đúng kích thước (thẻ được tái dùng
            // khi cuộn khay). Khi đó kích thước host không đổi nên onLayoutChange KHÔNG nổ, widget
            // giữ scale mặc định và ô trông như trống. Áp scale ngay ở lần layout đầu tiên để không
            // phụ thuộc vào việc listener có nổ hay không.
            host.getViewTreeObserver().addOnPreDrawListener(
                    new android.view.ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            int boxW = host.getWidth();
                            int boxH = host.getHeight();
                            if (boxW <= 0 || boxH <= 0) {
                                return true;   // chưa có kích thước, chờ lần vẽ sau
                            }
                            host.getViewTreeObserver().removeOnPreDrawListener(this);
                            applyScale(widgetView, grid, spanX, spanY, boxW, boxH);
                            return true;
                        }
                    });
            return host;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Cho widget đo/vẽ ở kích thước TỰ NHIÊN (theo tỉ lệ span) rồi scale đồng đều cho vừa khung.
     * Tách riêng để dùng chung cho cả onLayoutChange lẫn lần áp scale đầu tiên.
     */
    private static void applyScale(View widgetView, DeviceProfile grid, int spanX, int spanY,
                                   int boxW, int boxH) {
        if (boxW <= 0 || boxH <= 0) {
            return;
        }
        int natW = spanX * grid.cellWidthPx;
        int natH = spanY * grid.cellHeightPx;
        if (natW <= 0 || natH <= 0) {
            return;
        }
        ViewGroup.LayoutParams wlp = widgetView.getLayoutParams();
        if (wlp.width != natW || wlp.height != natH) {
            wlp.width = natW;
            wlp.height = natH;
            widgetView.setLayoutParams(wlp);
        }
        float scale = Math.min(boxW / (float) natW, boxH / (float) natH);
        if (scale <= 0f) {
            scale = 1f;
        }
        widgetView.setPivotX(natW / 2f);
        widgetView.setPivotY(natH / 2f);
        widgetView.setScaleX(scale);
        widgetView.setScaleY(scale);
    }

    /** Vô hiệu hoá clickable/focusable trên toàn bộ cây view (dùng cho preview không tương tác). */
    private static void disableTouch(View v) {
        if (v == null) return;
        v.setClickable(false);
        v.setLongClickable(false);
        v.setFocusable(false);
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                disableTouch(vg.getChildAt(i));
            }
        }
    }
}
