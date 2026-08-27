package com.amz.ios.launcher.util;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Bản GridLayoutManager giữ nguyên hành vi cuộn mượt của {@link SpeedLinearLayoutManager}.
 *
 * LÝ DO TỒN TẠI: khay Add Widget đổi từ LinearLayoutManager sang lưới 2 cột để MỖI widget nổi bật
 * là MỘT item riêng của RecyclerView (trước đây 5 widget bị nhồi chung vào một item, mỗi lần bind
 * phải removeAllViews + dựng lại toàn bộ -> Picture query MediaStore + decode ảnh trên main thread
 * nên ô bị trống một lúc). Đổi sang lưới thì mất hiệu ứng smooth-scroll của SpeedLinearLayoutManager,
 * nên tạo lớp này để giữ nguyên.
 *
 * KHÔNG sửa SpeedLinearLayoutManager (lớp dùng chung với màn khác) — tạo lớp mới song song.
 */
public class SpeedGridLayoutManager extends GridLayoutManager {

    public SpeedGridLayoutManager(Context context, int spanCount) {
        super(context, spanCount);
    }

    public SpeedGridLayoutManager(Context context, AttributeSet attrs, int defStyleAttr,
                                  int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state,
                                       int position) {
        LinearSmoothScroller smoothScroller = new LinearSmoothScroller(recyclerView.getContext());
        smoothScroller.setTargetPosition(position);
        startSmoothScroll(smoothScroller);
    }
}
