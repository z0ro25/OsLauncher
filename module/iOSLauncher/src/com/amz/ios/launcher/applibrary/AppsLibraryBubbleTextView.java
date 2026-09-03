package com.amz.ios.launcher.applibrary;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import com.amz.ios.launcher.BubbleTextView;
import com.amz.ios.launcher.Launcher;

/**
 * Icon preview trong khối App Library (1 trong 4 ô 2x2 của mỗi category).
 *
 * <p>Icon được đặt làm BACKGROUND của view thay vì compound-drawable của TextView:
 * <ul>
 *   <li>View vẽ background TRONG mọi draw pass (không phụ thuộc invalidate khi bind) → icon xuất
 *       hiện NGAY khi view được layout, không cần chờ RecyclerView recycle lại (fix lỗi "mở app
 *       library chỉ thấy khung trống, vuốt mới hiện").</li>
 *   <li>{@link #applyCompoundDrawables} được BubbleTextView gọi mỗi lần icon đổi → setBackground
 *       + setBounds theo kích thước view hiện tại → icon tự update + tự phủ đúng ô (fitXY, không
 *       bị crop như compound - TextView chỉ cấp chỗ theo dòng text rỗng).</li>
 *   <li>View là ô vuông (layout 0dp + margin trong apps_library_item_full.xml) nên icon bitmap
 *       vuông phủ khít, không méo.</li>
 * </ul>
 */
public class AppsLibraryBubbleTextView extends BubbleTextView {

    Launcher mLauncher;
    public OpenedLibraryView mOpenView;

    public AppsLibraryBubbleTextView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public AppsLibraryBubbleTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, 0);
        if (context instanceof Launcher){
            mLauncher = (Launcher) context;
        }
        mOpenView = new OpenedLibraryView(context);
    }

    public void clearBackground() {
        // Icon nằm ở background; "clear" = bỏ nền để không giữ icon cũ của lần bind trước.
        setBackground(null);
    }

    /**
     * Đặt icon làm background + ép bounds theo kích thước view (fit ô). KHÔNG dùng compound.
     * Mỗi lần icon đổi (reapplyItemInfo/verifyHighRes) BubbleTextView gọi hàm này -> tự cập nhật.
     */
    @Override
    protected void applyCompoundDrawables(Drawable icon) {
        if (icon == null) return;
        int w = getWidth();
        int h = getHeight();
        if (w > 0 && h > 0) {
            icon.setBounds(0, 0, w, h);
        }
        setBackground(icon);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // View vừa có kích thước thật (sau layout): ép bounds icon phủ đúng ô để không bị vẽ nhỏ/lệch.
        Drawable icon = getIcon();
        if (icon != null && w > 0 && h > 0) {
            icon.setBounds(0, 0, w, h);
        }
    }

}
