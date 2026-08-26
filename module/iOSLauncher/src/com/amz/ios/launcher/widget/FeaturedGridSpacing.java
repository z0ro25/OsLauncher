package com.amz.ios.launcher.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Khoảng cách của lưới widget nổi bật ở đầu khay Add Widget.
 *
 * LÝ DO TỒN TẠI: trước đây lưới nằm trong {@code widgets_full_row_view.xml} — một LinearLayout bọc
 * ngoài có {@code layout_marginStart/End = 15dp} tạo LỀ NGOÀI, còn từng thẻ tự mang margin nhỏ tạo
 * KHE GIỮA các thẻ. Khi tách mỗi widget thành một item RecyclerView riêng, container đó không còn,
 * nên lề ngoài 15dp mất theo.
 *
 * Không đặt lại khoảng cách bằng margin trong LayoutParams của holder vì
 * {@link GridLayoutManager#checkLayoutParams} chỉ chấp nhận {@link GridLayoutManager.LayoutParams};
 * LayoutParams sai kiểu sẽ bị RecyclerView chuyển đổi lại mỗi lần view được gắn lại sau khi tái
 * dùng. ItemDecoration là chỗ đúng: RecyclerView hỏi lại khoảng cách ở MỖI lần layout nên giá trị
 * không thể "rơi mất" khi cuộn đi cuộn lại.
 *
 * Chỉ áp cho các item widget nổi bật; hàng danh sách app phía dưới giữ nguyên như cũ (outRect = 0).
 */
public class FeaturedGridSpacing extends RecyclerView.ItemDecoration {

    private final WidgetAppListAdapter mAdapter;
    private final int mSpanCount;
    /** Lề ngoài hai bên lưới — thay cho marginStart/End 15dp của container cũ. */
    private final int mSideMargin;
    /** Nửa khe giữa hai thẻ; mỗi thẻ góp một nửa nên khe tổng = 2 * mGap, đúng như bản cũ. */
    private final int mGap;
    /** Khoảng hở dưới thẻ cuối, thay cho marginBottom 20dp của container cũ. */
    private final int mBottomMargin;

    public FeaturedGridSpacing(Context context, WidgetAppListAdapter adapter, int spanCount,
                               int gapPx) {
        mAdapter = adapter;
        mSpanCount = spanCount;
        mGap = gapPx;
        mSideMargin = dp(context, 15);
        mBottomMargin = dp(context, 20);
    }

    private static int dp(Context context, int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics());
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        if (position == RecyclerView.NO_POSITION) return;

        int featuredCount = mAdapter.getFeaturedItemCount();
        if (position >= featuredCount) {
            // Hàng danh sách app: giữ nguyên hành vi cũ, không chèn khoảng cách.
            outRect.setEmpty();
            return;
        }

        ViewGroup.LayoutParams raw = view.getLayoutParams();
        int spanIndex = 0;
        int spanSize = 1;
        if (raw instanceof GridLayoutManager.LayoutParams) {
            GridLayoutManager.LayoutParams lp = (GridLayoutManager.LayoutParams) raw;
            spanIndex = lp.getSpanIndex();
            spanSize = lp.getSpanSize();
        }

        if (spanSize >= mSpanCount) {
            // Thẻ rộng chiếm trọn hàng: hai mép thẳng với mép ngoài của lưới 2 cột phía trên.
            outRect.left = mSideMargin;
            outRect.right = mSideMargin;
        } else if (spanIndex == 0) {
            outRect.left = mSideMargin;   // cột trái: ngoài là lề lưới
            outRect.right = mGap;         // trong là nửa khe
        } else {
            outRect.left = mGap;
            outRect.right = mSideMargin;  // cột phải
        }

        outRect.top = mGap;
        outRect.bottom = (position == featuredCount - 1) ? mBottomMargin : mGap;
    }
}
