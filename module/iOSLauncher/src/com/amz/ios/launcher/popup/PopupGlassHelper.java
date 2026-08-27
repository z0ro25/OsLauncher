package com.amz.ios.launcher.popup;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewTreeObserver;

import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.util.BlurBuilder;
import com.amz.ios.launcher.widget.view.GlassBlurDrawable;

/**
 * Nền KÍNH MỜ cho 3 popup: giữ app, giữ widget, và menu nút Edit.
 *
 * CÁCH LÀM: chụp ĐÚNG VÙNG nằm sau popup (icon app, hotseat, wallpaper — mọi thứ đang hiển thị ở
 * chỗ đó), blur ảnh chụp, rồi lấy làm background của popup. Nội dung sau popup nhoè đúng như ảnh
 * mẫu iOS, và CHỈ trong phạm vi popup — phần còn lại của màn hình giữ nguyên độ nét.
 *
 * Chụp MỘT LẦN lúc popup mở là đủ: nền phía sau đứng yên trong suốt thời gian popup hiển thị, nên
 * không cần blur realtime (vừa tốn CPU vừa không cần thiết).
 *
 * CÁC CÁCH ĐÃ THỬ VÀ LOẠI — ghi lại để khỏi quay lại:
 *   1. RealtimeBlurView (RenderScript): RenderScript bị gỡ từ Android 12, getBlurImpl() rơi vào
 *      BLUR_IMPL = -1 tức fallback RỖNG — không blur gì, chỉ còn tint phẳng.
 *   2. GlassBlurWindowController (compositor blur, cơ chế hotseat): overlay là window
 *      TYPE_APPLICATION_MEDIA xếp DƯỚI window chính nên nền kính chui xuống dưới icon app, nhìn
 *      xuyên qua vẫn đọc rõ nội dung. Đúng cho dock (nằm dưới icon) nhưng sai cho popup.
 *   3. RenderEffect trên workspace_root_view: blur được icon nhưng làm nhoè TOÀN MÀN HÌNH, không
 *      chỉ vùng sau popup — không phải thứ cần.
 */
public final class PopupGlassHelper {

    /** Bán kính blur ảnh nền (px), tính trên ảnh ĐÃ thu nhỏ nên số nhỏ vẫn rất nhoè. */
    private static final int BLUR_RADIUS = 8;
    /**
     * Hệ số thu nhỏ ảnh trước khi blur — vừa nhanh hơn, vừa cho độ nhoè mượt hơn.
     * Để 3 chứ không lớn hơn: thu quá mạnh thì ảnh phóng lại bị VỠ HẠT thành từng ô vuông.
     */
    private static final int DOWNSCALE = 3;

    private PopupGlassHelper() {}

    /**
     * Gắn nền kính cho popup: chụp vùng sau popup, blur, đặt làm background.
     *
     * Gọi ngay sau khi inflate. Việc chụp phải chờ tới lúc popup đã có kích thước và vị trí thật,
     * nên hàm này hoãn tới frame đầu tiên sau khi popup được đo xong.
     *
     * @param popup    view gốc của popup.
     * @param cornerPx bán kính bo góc của popup.
     */
    public static void bind(final View popup, final float cornerPx) {
        if (popup == null) return;

        // Nền tạm dùng ngay: chưa chụp được thì popup vẫn phải có mặt kính, không để trong suốt.
        popup.setBackground(fallbackGlass(popup, cornerPx));

        popup.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        if (popup.getWidth() <= 0 || popup.getHeight() <= 0) {
                            return true;   // chưa đo xong, chờ frame sau
                        }
                        popup.getViewTreeObserver().removeOnPreDrawListener(this);
                        final Drawable glass = captureGlass(popup, cornerPx);
                        if (glass != null) {
                            // Đặt background NGOÀI lượt vẽ hiện tại: setBackground() gọi
                            // requestLayout/invalidate, làm ngay trong onPreDraw sẽ kích hoạt thêm
                            // một lượt vẽ nữa ngay lập tức.
                            popup.post(new Runnable() {
                                @Override
                                public void run() {
                                    popup.setBackground(glass);
                                }
                            });
                        }
                        return true;
                    }
                });
    }

    /**
     * Chụp vùng nằm sau popup rồi blur.
     *
     * Vẽ workspace vào một bitmap cỡ popup, dịch canvas sao cho đúng phần bị popup che. KHÔNG vẽ cả
     * DragLayer vì popup nằm trong đó — sẽ tự chụp chính mình.
     *
     * @return drawable nền kính, hoặc null nếu không chụp được (để giữ nền tạm).
     */
    private static Drawable captureGlass(View popup, float cornerPx) {
        Launcher launcher = Launcher.getLauncher(popup.getContext());
        if (launcher == null) return null;
        View source = launcher.findViewById(R.id.workspace_root_view);
        if (source == null || source.getWidth() <= 0) return null;

        int w = Math.max(1, popup.getWidth() / DOWNSCALE);
        int h = Math.max(1, popup.getHeight() / DOWNSCALE);
        try {
            // Toạ độ popup so với view nguồn, để biết phải dịch canvas bao nhiêu.
            int[] popupLoc = new int[2];
            int[] srcLoc = new int[2];
            popup.getLocationInWindow(popupLoc);
            source.getLocationInWindow(srcLoc);
            int dx = popupLoc[0] - srcLoc[0];
            int dy = popupLoc[1] - srcLoc[1];

            // [FIX VIỀN ĐEN] Bitmap ĐỤC (RGB_565, không có kênh alpha) chứ không phải ARGB_8888.
            //   BlurBuilder.fastBlur giữ nguyên alpha nhưng vẫn trộn R/G/B của các pixel lân cận
            //   (BlurBuilder:251). Workspace trong suốt ở mọi chỗ không có icon, mà pixel trong suốt
            //   có RGB = 0 tức MÀU ĐEN — blur kéo màu đen đó lan ra, tạo quầng đen xì quanh mỗi logo.
            //   Bitmap không alpha thì mọi pixel đều đục, nền là màu ta lấp sẵn bên dưới, nên không
            //   còn màu đen nào để lan. RGB_565 cũng nhẹ hơn một nửa — ảnh đã blur nên mất màu
            //   không đáng kể.
            Bitmap shot = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(shot);
            // Lấp nền xám trung tính: chỗ nào wallpaper/workspace không vẽ tới thì lấy màu này thay
            // vì đen. Workspace vẽ đè lên ngay sau đó.
            canvas.drawColor(0xFFBDBDBD);
            canvas.scale(1f / DOWNSCALE, 1f / DOWNSCALE);
            canvas.translate(-dx, -dy);
            source.draw(canvas);

            Bitmap blurred = BlurBuilder.fastBlur(shot, BLUR_RADIUS);
            if (blurred == null) return null;

            return new PopupGlassDrawable(blurred, cornerPx,
                    popup.getResources().getColor(R.color.popup_glass_surface),
                    popup.getResources().getColor(R.color.dock_glass_stroke_color),
                    popup.getResources().getDisplayMetrics().density);
        } catch (Throwable th) {
            return null;   // OOM hoặc view chưa vẽ được -> dùng nền tạm
        }
    }

    /**
     * Nền dùng khi chưa chụp được (frame đầu) hoặc chụp thất bại.
     * GlassBlurDrawable là lớp sẵn có của dự án: blur wallpaper + viền kính.
     */
    private static Drawable fallbackGlass(View popup, float cornerPx) {
        float stroke = popup.getResources().getDisplayMetrics().density; // 1dp
        return new GlassBlurDrawable(popup, cornerPx, stroke,
                popup.getResources().getColor(R.color.dock_glass_stroke_color),
                popup.getResources().getColor(R.color.popup_glass_surface), null);
    }

    /** Bo góc chuẩn của popup giữ-app (khớp PopupItemView.getBackgroundRadius). */
    public static float appPopupCorner(View v) {
        return v.getResources().getDimensionPixelSize(R.dimen.bg_round_rect_radius);
    }

    /** Bo góc chuẩn của popup widget và popup menu Edit. */
    public static float widgetPopupCorner(View v) {
        return v.getResources().getDimension(R.dimen.widget_round_corner);
    }
}
