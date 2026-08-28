package com.amz.ios.launcher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.appwidget.AppWidgetHostView;
import android.view.View;
import android.view.ViewGroup;

import com.amz.ios.launcher.folder.Folder;

/**
 * Animation phóng to/thu nhỏ icon khi vào/ra EDIT MODE (icon rung + hiện dấu trừ).
 *
 * [BUG FIX] "Bật/tắt edit mode lần 2 trở đi không hiện gì, không kéo thả được" (Samsung A):
 *   Bản cũ gọi mAnim.addListener()/addUpdateListener() TRONG doAnimParam() — tức MỖI LẦN vào hoặc
 *   ra edit lại gắn thêm một bộ listener vào cùng một ValueAnimator dùng lại suốt vòng đời, và
 *   không bao giờ gỡ. Lần 1 vào: 1 listener; lần 1 ra: 2 listener cùng ghi sScale NGƯỢC CHIỀU nhau
 *   (nhánh if(mZoomOut) tự đọc trạng thái hiện tại); lần 2: 3 listener... Kết quả sScale bị ghi đè
 *   loạn -> BubbleTextView/FolderIcon đọc ra giá trị sai -> icon không phóng to, dấu trừ không
 *   hiện, callback không chạy -> không rung, không kéo thả được.
 *
 *   Nay listener gắn ĐÚNG MỘT LẦN trong constructor. Chúng vốn không phụ thuộc tham số của từng
 *   lượt nên không có lý do gắn lại.
 */
public class DelIconAnim {

    private static boolean sNeedRefersh = false;
    private static float sScale = 0.0f;
    private ValueAnimator mAnim;
    private Runnable mCallback = null;
    private Workspace mWorkspace;
    private final ZoomInInterpolator mZoomInInterpolator = new ZoomInInterpolator();
    private boolean mZoomOut = false;

    /**
     * Đang chủ động huỷ lượt cũ để bắt đầu lượt mới.
     *
     * cancel() làm ValueAnimator bắn onAnimationCancel RỒI onAnimationEnd. Không có cờ này thì lượt
     * huỷ sẽ chạy callback của lượt MỚI (mCallback đã bị thay trước khi cancel) và chốt sScale sai
     * ngay trước khi lượt mới bắt đầu.
     */
    private boolean mCancelingForRestart;

    public DelIconAnim(Workspace workspace) {
        this.mWorkspace = workspace;
        this.mAnim = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.mAnim.setInterpolator(this.mZoomInInterpolator);
        this.mAnim.setDuration(300L);
        attachListeners();
    }

    /** Gắn listener MỘT LẦN duy nhất cho animator dùng lại. Xem ghi chú lỗi ở đầu lớp. */
    private void attachListeners() {
        this.mAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                sNeedRefersh = true;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (mCancelingForRestart) return;   // lượt mới sắp chạy, để nó chốt
                finishCurrentRun();
            }
        });
        this.mAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float value = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                sScale = mZoomOut ? value : (1.0f - value);
                updateView(value);
            }
        });
    }

    /**
     * Chốt trạng thái cuối của một lượt: đặt sScale về đúng đích, vẽ lại toàn bộ trang, chạy callback.
     *
     * Làm ở onAnimationEnd chứ KHÔNG dựa vào giá trị update cuối (bản cũ so sánh {@code f == 1.0f}).
     * ValueAnimator không đảm bảo phát ra đúng 1.0f khi máy bỏ frame — logcat của máy Samsung A có
     * "Skipped 64 frames!" và "Davey! duration=1122ms", tức bỏ frame là chuyện đang xảy ra thật.
     * Mất frame cuối là mất luôn callback -> kẹt trạng thái edit.
     */
    private void finishCurrentRun() {
        // Vào edit (zoom out) -> icon ở cỡ lớn nhất (1.0); ra edit -> về cỡ thường (0.0).
        sScale = mZoomOut ? 1.0f : 0.0f;
        sNeedRefersh = mZoomOut;

        for (int i = 0; i < this.mWorkspace.getChildCount(); i++) {
            View child = this.mWorkspace.getChildAt(i);
            if (child instanceof ViewGroup) {
                invalidate((ViewGroup) child);
            }
        }
        invalidate(this.mWorkspace.getHotseatCellLayout());

        if (this.mCallback != null) {
            Runnable callback = this.mCallback;
            this.mCallback = null;   // xoá TRƯỚC khi chạy, phòng callback gọi lại vào đây
            callback.run();
        }
    }

    public static float getScale() {
        return sScale;
    }

    private void invalidate(ViewGroup viewGroup) {
        if (viewGroup != null) {
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View childAt = viewGroup.getChildAt(i);
                if (childAt instanceof AppWidgetHostView) {
                    childAt.invalidate(childAt.getScrollX(), childAt.getScrollY(), childAt.getScrollX() + childAt.getWidth() + 2, childAt.getScrollY() + childAt.getHeight() + 2);
                } else {
                    childAt.invalidate();
                }
            }
        }
    }

    public static boolean shouldRefresh() {
        return sNeedRefersh;
    }

    /** Chạy một lượt animation. Listener đã gắn sẵn ở constructor nên ở đây chỉ khởi động. */
    public void doAnimParam() {
        this.mAnim.start();
    }

    public void initAnimParam(boolean isZoomout) {
        initAnimParam(isZoomout, null);
    }

    /**
     * Đặt trạng thái đích (vào/ra edit) và chạy animation.
     *
     * [BUG FIX] Bản cũ bọc toàn bộ thân hàm trong {@code if (mZoomOut != isZoomout)}, tức khi trạng
     * thái đích TRÙNG với hiện tại thì bỏ qua HOÀN TOÀN — không chạy animation, và callback vừa
     * nhận vào cũng không bao giờ chạy. Chỉ cần một lượt bị huỷ giữa chừng khiến mZoomOut lệch với
     * thực tế là lần bấm sau bị nuốt, trong khi Workspace đã đặt sTidyUping = true. Kết quả: cờ nói
     * "đang edit" mà giao diện không hề vào edit.
     *
     * Nay luôn ĐẾN ĐÍCH, nhưng không chạy lại animation nếu đã đứng sẵn ở đích — xem dưới.
     */
    public void initAnimParam(boolean isZoomout, Runnable runnable) {
        this.mCallback = runnable;

        // [BUG FIX] "Giữ lâu ở desktop bị nháy."
        //   Long-press app khi ĐANG edit gọi startTidyUp() thêm một lần nữa (Launcher:4453). Nếu ở
        //   đây cứ vô điều kiện chạy lại animation thì icon thu về rồi phóng to lại từ đầu = nháy.
        //   Nên: đã đứng SẴN ở đích (cùng chiều VÀ animation đã dừng) thì chỉ chốt trạng thái + chạy
        //   callback, không animate lại.
        //   Khác với bản cũ ở chỗ bản cũ return luôn, KHÔNG chạy callback -> kẹt trạng thái.
        boolean alreadyAtTarget = (this.mZoomOut == isZoomout) && !this.mAnim.isRunning();
        this.mZoomOut = isZoomout;
        if (alreadyAtTarget) {
            finishCurrentRun();
            return;
        }

        if (this.mAnim.isRunning()) {
            // Chặn onAnimationEnd của lượt bị huỷ chốt nhầm trạng thái/callback của lượt mới.
            this.mCancelingForRestart = true;
            this.mAnim.cancel();
            this.mCancelingForRestart = false;
        }
        doAnimParam();
    }

    public boolean isZoomOut() {
        return this.mZoomOut;
    }

    /**
     * Vẽ lại phần đang animate ở mỗi frame.
     *
     * Việc chốt trạng thái cuối + chạy callback đã chuyển sang {@link #finishCurrentRun()} (gọi từ
     * onAnimationEnd). Trước đây làm ở đây với điều kiện {@code f == 1.0f} — so sánh float chính
     * xác trên giá trị animation, mất frame cuối là mất luôn callback.
     */
    public void updateView(float f) {
        Folder openFolder = this.mWorkspace.getOpenFolder();
        if (openFolder != null) {
            invalidate(openFolder.mFolderContent);
        } else if (f != 0.0f) {
            invalidate(this.mWorkspace.getCurrentDropLayout());
            invalidate(this.mWorkspace.getHotseatCellLayout());
        }
    }
}
