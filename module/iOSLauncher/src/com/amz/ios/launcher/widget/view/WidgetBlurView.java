package com.amz.ios.launcher.widget.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;

import com.amz.ios.launcher.DockBlurView;

/**
 * Biến thể {@link DockBlurView} dành cho WIDGET đặt trên workspace (các trang cuộn ngang).
 *
 * <p>{@link DockBlurView} gốc thiết kế cho DOCK NEO CỐ ĐỊNH: nó chỉ đồng bộ overlay-window blur khi
 * view re-layout hoặc đổi hiển thị. Với widget, khi người dùng CUỘN trang, vị trí thật của widget
 * trên màn đổi liên tục nhưng KHÔNG có layout-pass -> overlay blur đứng im tại chỗ cũ, thấy "dính"
 * lại trên các trang sau.
 *
 * <p>View này chỉ THÊM listener cuộn: mỗi khi cây view cuộn (workspace đổi trang) thì gọi
 * {@link DockBlurView#refreshBlur()} để overlay bám lại đúng vị trí widget. Nhờ vậy lớp blur TRÔI
 * theo widget và tự trượt ra khỏi màn khi widget cuộn sang trang khác. KHÔNG sửa {@link DockBlurView}
 * (dùng chung với dock) — dock giữ nguyên hành vi cũ.
 */
public class WidgetBlurView extends DockBlurView {

    private final ViewTreeObserver.OnScrollChangedListener mScrollListener = this::refreshBlur;

    public WidgetBlurView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        try {
            getViewTreeObserver().addOnScrollChangedListener(mScrollListener);
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        try {
            getViewTreeObserver().removeOnScrollChangedListener(mScrollListener);
        } catch (Throwable ignored) {
        }
        super.onDetachedFromWindow();
    }
}
