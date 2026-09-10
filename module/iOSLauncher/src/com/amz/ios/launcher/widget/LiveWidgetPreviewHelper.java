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
     * Bản dựng preview sống KHÔNG phụ thuộc {@code ViewTreeObserver}, dùng cho màn chọn size
     * (carousel level-2).
     *
     * LÝ DO TÁCH RIÊNG KHỎI {@link #build}: ở màn chọn size, cell được tạo và nạp preview TRƯỚC khi
     * sheet mở ra (xem SlidingUpWidgetsCellAppStyle.setData): lúc đó sheet còn GONE nên toàn bộ cây
     * con bên dưới chưa hề được đo — cell/host đều 0x0. Mọi mốc "gọi 1 lần vào lúc dựng" (kể cả
     * onSizeChanged khi attach lần đầu, hay ViewTreeObserver như {@link #build}) đều chạy quá sớm
     * và áp scale lên kích thước 0x0, rồi không có dịp chạy lại khi sheet hiện ra — ô preview
     * thành trống. Đây là lỗi thấy được trên Android 9.
     *
     * Ở đây host tự áp scale trong {@code onLayout}: callback của CHÍNH view, nổ ở MỌI vòng layout
     * của host — kể cả vòng đầu tiên có kích thước THẬT khi sheet hiện ra và cell được layout lại.
     * applyScale tự bỏ qua khi kích thước chưa hợp lệ và chỉ ghi LayoutParams khi giá trị đổi, nên
     * gọi mỗi vòng layout không gây đo/layout lặp vô hạn.
     *
     * KHÔNG sửa {@link #build} vì màn 1 (GalleryWidgetCell) đang chạy tốt với nó — thêm đường mới
     * cho luồng mới, giữ nguyên đường cũ.
     *
     * @return host view, hoặc null nếu không dựng được.
     */
    public static View buildForSizeSheet(Context context, final LauncherAppWidgetProviderInfo info,
                                         final DeviceProfile grid) {
        return buildForSizeSheet(context, info, grid, 0, 0);
    }

    /**
     * Như trên nhưng ÁP SCALE NGAY bằng kích thước khung ĐÃ BIẾT TRƯỚC ({@code boxW} x {@code boxH}),
     * không chờ vòng layout nào.
     *
     * Vì sao cần: chờ đo runtime khiến preview chỉ hiện sau khi người dùng vuốt qua lại (lần layout
     * đầu cell chưa có kích thước). Màn chọn size biết trước khung rộng/cao bao nhiêu nên truyền
     * thẳng vào đây -> preview hiện ngay từ lần mở đầu tiên và đúng cỡ.
     * Truyền 0 cho boxW/boxH nếu chưa biết (khi đó chỉ dựa vào onLayout như cũ).
     */
    public static View buildForSizeSheet(Context context, final LauncherAppWidgetProviderInfo info,
                                         final DeviceProfile grid, int boxW, int boxH) {
        try {
            final int spanX = Math.max(1, info.spanX);
            final int spanY = Math.max(1, info.spanY);

            // Host tự áp scale ở mỗi vòng layout (xem javadoc hàm về lý do không dùng mốc dựng/attach).
            final FrameLayout host = new FrameLayout(context) {
                /**
                 * Áp scale ở onLayout — vòng layout NÀO của host cũng chạy qua đây, kể cả vòng đầu
                 * tiên có kích thước thật khi sheet vừa hiện.
                 *
                 * Không dùng onSizeChanged/onAttachedToWindow làm mốc: ở màn chọn size, cell được
                 * dựng và nạp preview khi sheet còn GONE, nên mọi kích thước lúc đó đều là 0x0 và
                 * lần áp scale đầu tiên bị bỏ qua. Phải áp lại khi sheet hiện ra và cell mới thật
                 * sự có kích thước — onLayout là mốc chắc chắn nổ vào đúng lúc đó.
                 */
                @Override
                protected void onLayout(boolean changed, int l, int t, int r, int b) {
                    super.onLayout(changed, l, t, r, b);
                    scaleChild(this, grid, spanX, spanY);
                }
            };
            host.setClipChildren(true);

            final View widgetView = LayoutInflater.from(context).inflate(
                    info.initialLayout, host, false);

            // Preview chỉ để NHÌN — chặn touch để cú chạm truyền lên cell (giống build()).
            disableTouch(widgetView);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER;
            host.addView(widgetView, lp);

            bindSpecialProviders(context, info, widgetView);
            // Biết trước kích thước khung -> áp scale NGAY, không chờ layout. Nhờ vậy preview hiện
            // đúng cỡ ngay lần mở đầu tiên thay vì phải vuốt qua lại mới thấy.
            if (boxW > 0 && boxH > 0) {
                applyScaleNoUpscale(widgetView, grid, spanX, spanY, boxW, boxH);
            }
            return host;
        } catch (Exception e) {
            return null;
        }
    }

    /** Áp scale cho widget con đầu tiên của host theo kích thước hiện tại của host. */
    private static void scaleChild(ViewGroup host, DeviceProfile grid, int spanX, int spanY) {
        if (host.getChildCount() == 0) {
            return;
        }
        applyScaleNoUpscale(host.getChildAt(0), grid, spanX, spanY,
                host.getWidth(), host.getHeight());
    }

    /**
     * Như {@link #applyScale} nhưng KHÔNG BAO GIỜ PHÓNG TO quá cỡ thật trên lưới (scale ≤ 1).
     *
     * Dùng cho màn chọn size: khung ở đó dùng CHUNG cho mọi cỡ nên phải cao bằng cỡ lớn nhất
     * (vd 4x4). Nếu cứ "lấp đầy khung" như applyScale thì cỡ nhỏ (2x2, tự nhiên chỉ ~426px) bị
     * kéo giãn gấp gần 2 lần cho bằng khung ~784px -> preview tràn ra ngoài, chữ/nét vỡ.
     *
     * Chặn ở 1.0 cho ra đúng ý nghĩa preview: mỗi cỡ hiện ĐÚNG kích thước như khi đặt lên màn
     * hình, cỡ nhỏ trông nhỏ hơn cỡ lớn — giống cách iOS thể hiện. Chỉ thu nhỏ khi widget lớn
     * hơn khung. Tách hàm riêng để KHÔNG đổi hành vi {@link #applyScale} mà màn 1 đang dùng.
     */
    private static void applyScaleNoUpscale(View widgetView, DeviceProfile grid, int spanX,
                                            int spanY, int boxW, int boxH) {
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
        if (scale > 1f) {
            scale = 1f;      // không phóng to: giữ đúng cỡ thật trên lưới
        }
        if (scale <= 0f) {
            scale = 1f;
        }
        widgetView.setPivotX(natW / 2f);
        widgetView.setPivotY(natH / 2f);
        widgetView.setScaleX(scale);
        widgetView.setScaleY(scale);
    }

    /**
     * Bind dữ liệu cho các widget nội bộ KHÔNG đi qua {@code onUpdate} (Battery, Photos) để preview
     * hiện đúng như khi đặt màn. Tách ra để {@link #build} và {@link #buildForSizeSheet} dùng chung
     * MỘT nguồn, tránh hai màn lệch nhau khi sau này thêm widget mới.
     */
    private static void bindSpecialProviders(Context context, LauncherAppWidgetProviderInfo info,
                                             View widgetView) {
        if (info.provider == null) {
            return;
        }
        String cls = info.provider.getClassName();
        // Preview widget Battery: bind vòng pin + % (widget nội bộ không đi qua onUpdate).
        if (com.amz.ios.launcher.widget.widgetprovider.BatteryWidgetProvider.class
                .getName().equals(cls)) {
            com.amz.ios.launcher.widget.widgetprovider.BatteryWidgetProvider
                    .bindInflatedView(context, widgetView);
            return;
        }
        // Preview widget Photos (cả 3 size): bind ảnh gần đây + overlay. Chưa cấp quyền -> ảnh
        // default, ẩn overlay, giống hành vi thật.
        if (com.amz.ios.launcher.widget.widgetprovider.PictureAppWidgetProvider.class.getName().equals(cls)
                || com.amz.ios.launcher.widget.widgetprovider.PictureMediumWidgetProvider.class.getName().equals(cls)
                || com.amz.ios.launcher.widget.widgetprovider.PictureLargeWidgetProvider.class.getName().equals(cls)) {
            com.amz.ios.launcher.widget.widgetprovider.PictureAppWidgetProvider
                    .bindInflatedView(context, widgetView);
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
