package com.amz.ios.launcher.leftpage.custom;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatButton;

import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.LauncherAnimUtils;

public class CustomZoomButton extends AppCompatButton {

    /**
     * Animator ẩn/hiện ĐANG chạy (null khi không có). Giữ tham chiếu để huỷ khi có yêu cầu đổi
     * visibility ngược lại giữa chừng — xem {@link #setVisibility(int)}.
     */
    private ObjectAnimator mVisibilityAnimator;

    /**
     * Visibility ĐÍCH của lần gọi {@link #setVisibility(int)} gần nhất — tức trạng thái mà view SẼ có
     * khi animation chạy xong. Khác với {@link #getVisibility()} là trạng thái THẬT tại thời điểm hiện
     * tại, vốn còn chưa kịp cập nhật khi animation ẩn đang chạy dở.
     */
    private int mTargetVisibility = View.GONE;

    public CustomZoomButton(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mTargetVisibility = super.getVisibility();
    }

    /**
     * [BUG FIX] "Đang ở edit mode nhưng bấm nút Chỉnh sửa không hiện menu; giữ app cũng không ra popup."
     *
     * NGUYÊN NHÂN (đo bằng dumpsys trên Samsung A50 Android 9, đúng lúc bug xảy ra):
     *   CustomZoomButton{... GFED..C.. 0,0-0,0 app:id/add_widgets}
     *   -> nút Ở TRẠNG THÁI GONE và kích thước 0, TRONG KHI màn hình vẫn ĐANG VẼ cả 2 nút Chỉnh
     *   sửa/Done. Cái người dùng nhìn thấy chỉ là phần hình còn sót lại, còn view thật đã GONE nên
     *   KHÔNG nhận touch -> bấm bao nhiêu lần cũng không ra menu.
     *
     *   Vì sao rơi vào trạng thái đó: đường ẩn nút chạy animation 300ms rồi mới gọi
     *   {@code super.setVisibility(GONE)} tại onAnimationEnd. Nếu trong 300ms ấy có yêu cầu hiện lại
     *   (vào edit lần nữa — rất dễ xảy ra khi thoát rồi vào edit liên tiếp), thì:
     *     - {@code getVisibility()} lúc đó VẪN là VISIBLE (chưa tới onAnimationEnd),
     *     - nên điều kiện {@code getVisibility() != visibility} SAI -> BỎ QUA hoàn toàn, không hiện lại,
     *     - animation ẩn vẫn chạy nốt và đặt GONE + scale 0 đè lên.
     *   Kết quả: logic tưởng nút đang hiện (isShaking() == true, 2 nút "đã VISIBLE"), thực tế nút GONE.
     *
     * FIX: so sánh với {@link #mTargetVisibility} (trạng thái ĐÍCH) thay vì {@code getVisibility()}
     * (trạng thái tức thời), và HUỶ animation đang chạy trước khi bắt đầu animation ngược lại. Nhờ vậy
     * yêu cầu hiện-lại giữa chừng luôn được thực thi, không bị animation cũ ghi đè.
     */
    @Override
    public void setVisibility(final int visibility) {
        if (mTargetVisibility == visibility) {
            return;
        }
        mTargetVisibility = visibility;

        // Huỷ lượt animation ngược đang chạy dở, nếu không listener của nó sẽ đặt visibility CŨ đè lên
        // trạng thái mới ngay sau đây.
        if (mVisibilityAnimator != null) {
            mVisibilityAnimator.cancel();
            mVisibilityAnimator = null;
        }

        if (visibility == View.VISIBLE) {
            setEnabled(true);
            // Đặt VISIBLE NGAY (không đợi animation) để view nhận được touch tức thì; animation chỉ lo
            // phần phóng to cho đẹp. Tránh khoảng "đã bật edit mà nút chưa bấm được".
            CustomZoomButton.super.setVisibility(View.VISIBLE);
            ObjectAnimator animator = LauncherAnimUtils.ofPropertyValuesHolder(this, PropertyValuesHolder.ofFloat("scaleX", 1.0f), PropertyValuesHolder.ofFloat("scaleY", 1.0f));
            animator.setDuration(300L);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mVisibilityAnimator = null;
                }
            });
            animator.setInterpolator(Launcher.initInterpolator(0.02f, 0.11f, 0.13f, 1.0f));
            mVisibilityAnimator = animator;
            animator.start();
        } else if (visibility == View.INVISIBLE || visibility == View.GONE) {
            ObjectAnimator animator = LauncherAnimUtils.ofPropertyValuesHolder(this, PropertyValuesHolder.ofFloat("scaleX", 0.0f), PropertyValuesHolder.ofFloat("scaleY", 0.0f));
            animator.setDuration(300L);
            animator.addListener(new AnimatorListenerAdapter() {
                private boolean mCancelled = false;

                @Override
                public void onAnimationCancel(Animator animation) {
                    super.onAnimationCancel(animation);
                    mCancelled = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mVisibilityAnimator = null;
                    // BỊ HUỶ giữa chừng = đã có yêu cầu hiện lại -> TUYỆT ĐỐI không đặt GONE đè lên,
                    // vì đó chính là cách nút rơi vào trạng thái "GONE mà trông như đang hiện".
                    if (mCancelled || mTargetVisibility == View.VISIBLE) {
                        return;
                    }
                    CustomZoomButton.super.setVisibility(visibility);
                }
            });
            animator.setInterpolator(Launcher.initInterpolator(0.02f, 0.11f, 0.13f, 1.0f));
            mVisibilityAnimator = animator;
            animator.start();
        }
    }
}
