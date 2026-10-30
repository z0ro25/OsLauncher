package com.truongnt.ios.launcher.util;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.truongnt.ios.ioslite.common.analytics.AnalyticsDelegate;

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

    // Lưới an toàn cho AIOOBE ở getSpaceForSpanRange (span lệch vì dữ liệu đổi giữa lượt layout):
    // bỏ lượt hỏng, xoá cache span rồi layout lại frame sau với dữ liệu đã ổn định; báo Crashlytics.
    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        try {
            super.onLayoutChildren(recycler, state);
        } catch (IndexOutOfBoundsException e) {
            java.util.Map<String, String> keys = new java.util.HashMap<>();
            keys.put("grid_item_count", String.valueOf(state.getItemCount()));
            keys.put("grid_span_count", String.valueOf(getSpanCount()));
            AnalyticsDelegate.logNonFatal(e, keys);
            getSpanSizeLookup().invalidateSpanIndexCache();
            getSpanSizeLookup().invalidateSpanGroupIndexCache();
            removeAndRecycleAllViews(recycler);
            RecyclerView rv = mRecyclerView;
            if (rv != null) rv.post(rv::requestLayout);
        }
    }

    private RecyclerView mRecyclerView;

    @Override
    public void onAttachedToWindow(RecyclerView view) {
        super.onAttachedToWindow(view);
        mRecyclerView = view;
    }

    @Override
    public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
        super.onDetachedFromWindow(view, recycler);
        mRecyclerView = null;
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state,
                                       int position) {
        LinearSmoothScroller smoothScroller = new LinearSmoothScroller(recyclerView.getContext());
        smoothScroller.setTargetPosition(position);
        startSmoothScroll(smoothScroller);
    }
}
