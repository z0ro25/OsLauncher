package com.amz.ios.launcher.editpage;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.GridLayoutManager;

import com.amz.ios.launcher.CellLayout;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.Workspace;
import com.amz.ios.launcher.bounce.BouncyRecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Overlay toàn màn hình cho màn "Edit Pages" (kiểu iOS).
 *
 * Hiển thị tất cả page dạng thumbnail render thật, xếp thành 2 hàng (grid ngang, cuộn ngang
 * nếu tràn). Cho phép kéo-thả đổi thứ tự và bấm dấu tích để ẩn/hiện từng page.
 * Bấm Done -> áp thứ tự + trạng thái ẩn về Workspace rồi gỡ overlay.
 */
public class EditPagesOverlay extends FrameLayout {

    private final Launcher mLauncher;
    private final EditPagesAdapter mAdapter;

    public EditPagesOverlay(Launcher launcher) {
        super(launcher);
        this.mLauncher = launcher;

        LayoutInflater.from(launcher).inflate(R.layout.overlay_edit_pages, this, true);

        Workspace workspace = launcher.getWorkspace();
        Set<Long> hidden = HiddenPagesPrefs.getHidden(launcher);

        int[] thumbSize = computeThumbSize(launcher, workspace);
        mAdapter = new EditPagesAdapter(workspace, hidden, thumbSize[0], thumbSize[1]);

        BouncyRecyclerView rcv = findViewById(R.id.rcv_pages);
        // 2 hàng, cuộn ngang.
        GridLayoutManager lm = new GridLayoutManager(launcher, 2, GridLayoutManager.HORIZONTAL, false);
        rcv.setLayoutManager(lm);
        rcv.setAdapter(mAdapter);
        rcv.setLongPressDragEnabled(true);

        findViewById(R.id.btn_done).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                close();
            }
        });

        // Chặn chạm xuyên xuống workspace phía sau.
        setClickable(true);

        // Đẩy nút Done + title xuống dưới status bar/notch để không bị đè.
        applyTopInset(launcher);
    }

    /**
     * Cộng thêm chiều cao status bar vào margin trên của nút Done và title để chúng nằm
     * dưới thanh trạng thái/notch thay vì bị đè.
     */
    private void applyTopInset(Context ctx) {
        int topInset = getStatusBarHeight(ctx);
        if (topInset <= 0) {
            return;
        }
        addTopMargin(findViewById(R.id.btn_done), topInset);
        addTopMargin(findViewById(R.id.tv_edit_pages_title), topInset);
    }

    private void addTopMargin(View view, int extra) {
        if (view == null) {
            return;
        }
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
            mlp.topMargin += extra;
            view.setLayoutParams(mlp);
        }
    }

    private int getStatusBarHeight(Context ctx) {
        int id = ctx.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (id > 0) {
            return ctx.getResources().getDimensionPixelSize(id);
        }
        // Fallback ~24dp.
        return (int) (ctx.getResources().getDisplayMetrics().density * 24);
    }

    /**
     * Tính kích thước thumbnail sao cho TẤT CẢ page hiển thị đầy đủ trong màn (2 hàng),
     * KHÔNG phải cuộn ngang. Với N page và 2 hàng -> số cột = ceil(N/2); thumbnail được
     * co lại vừa cả ràng buộc chiều ngang (các cột lấp vừa bề rộng) lẫn chiều dọc (2 hàng
     * lấp vừa chiều cao), lấy giá trị nhỏ hơn để không méo và không tràn.
     */
    private int[] computeThumbSize(Context ctx, Workspace workspace) {
        DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        float density = dm.density;

        int pageCount = Math.max(1, countRealPages(workspace));
        int columns = (int) Math.ceil(pageCount / 2.0);

        // Padding khớp layout: recyclerview paddingStart/End = 16dp; mỗi item paddingStart/End
        // = 8dp (tổng 16dp/ item ngang) và paddingTop/Bottom = 8dp + tích 26dp + marginTop 10dp.
        int rcvHPadding = (int) (density * 16) * 2;
        int itemHPadding = (int) (density * 16);
        int itemVPadding = (int) (density * 16);
        int checkArea = (int) (density * 46); // dấu tích 26dp + margin 10dp + đệm

        // --- Ràng buộc chiều ngang: mọi cột vừa bề rộng ---
        int availableWidth = dm.widthPixels - rcvHPadding;
        int maxThumbWidth = availableWidth / columns - itemHPadding;

        // --- Ràng buộc chiều dọc: 2 hàng vừa chiều cao ---
        int reservedVertical = (int) (density * 120); // title trên + đệm
        int availableHeight = dm.heightPixels - reservedVertical;
        int maxThumbHeight = availableHeight / 2 - itemVPadding - checkArea;

        // Tỉ lệ page thật (rộng/cao).
        float pageAspect = 0.5f;
        CellLayout sample = firstRealPage(workspace);
        if (sample != null && sample.getWidth() > 0 && sample.getHeight() > 0) {
            pageAspect = (float) sample.getWidth() / sample.getHeight();
        } else if (dm.heightPixels > 0) {
            pageAspect = (float) dm.widthPixels / dm.heightPixels;
        }

        // Chọn chiều cao lớn nhất thỏa CẢ hai ràng buộc, giữ đúng tỉ lệ.
        int thumbHeightByWidth = (int) (maxThumbWidth / pageAspect);
        int thumbHeight = Math.min(maxThumbHeight, thumbHeightByWidth);

        int minThumbHeight = (int) (density * 70);
        if (thumbHeight < minThumbHeight) {
            thumbHeight = minThumbHeight;
        }

        int thumbWidth = (int) (thumbHeight * pageAspect);
        return new int[]{thumbWidth, thumbHeight};
    }

    private int countRealPages(Workspace workspace) {
        int n = 0;
        ArrayList<Long> order = workspace.getScreenOrder();
        for (int i = 0; i < order.size(); i++) {
            if (order.get(i) >= 0) {
                n++;
            }
        }
        return n;
    }

    private CellLayout firstRealPage(Workspace workspace) {
        ArrayList<Long> order = workspace.getScreenOrder();
        for (int i = 0; i < order.size(); i++) {
            long id = order.get(i);
            if (id >= 0) {
                CellLayout cl = workspace.getScreenWithId(id);
                if (cl != null) {
                    return cl;
                }
            }
        }
        return null;
    }

    /** Áp thay đổi về Workspace và gỡ overlay. */
    public void close() {
        ArrayList<Long> order = mAdapter.getOrder();
        HashSet<Long> hidden = mAdapter.getHiddenSet();
        mLauncher.applyPageChanges(order, hidden);
        mLauncher.getDragLayer().removeView(this);
    }
}
