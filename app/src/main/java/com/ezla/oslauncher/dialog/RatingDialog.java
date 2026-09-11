package com.ezla.oslauncher.dialog;


import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ezla.oslauncher.R;
import com.ezla.oslauncher.tool.languageTool.LanguageUtil;
import com.google.android.material.bottomsheet.BottomSheetDialog;

/**
 * Dialog "Rate App" — BOTTOM SHEET kiểu iOS (trượt từ đáy lên), hỗ trợ đủ light + dark.
 *
 * [ĐỔI THIẾT KẾ] Bản cũ là {@link android.app.Dialog} nổi giữa màn, có icon mặt cười đổi theo số
 * sao và hai nút "Rate us" / "Not now". Bản mới bám đáy màn hình, bo 2 góc trên, và bỏ nút
 * "Not now" — đóng bằng nút X ở header.
 *
 * GIỮ NGUYÊN giao kèo {@link OnPress} và luồng xử lý theo số sao (<=3 gửi mail góp ý, >3 mở Play
 * Store) để các màn đang gọi (BaseActivity.showRateDialog) không phải sửa theo.
 */
public class RatingDialog extends BottomSheetDialog {
    private OnPress onPress;
    private final Context context;
    private final TextView btnRate;
    private final ImageView btnClose;

    /** 5 ngôi sao theo thứ tự trái -> phải. */
    private final ImageView[] stars = new ImageView[5];
    /** Số sao đang chọn (1..5); mặc định 5 như thiết kế. */
    private int rating = 5;

    public RatingDialog(Context context2) {
        super(context2);
        this.context = context2;
        LanguageUtil.setLocale(context2);
        setContentView(R.layout.dialog_rating_app);

        // Nền sheet do layout tự vẽ (bo 2 góc trên, màu theo mode) -> xoá nền mặc định của
        // BottomSheetDialog, nếu không sẽ lộ một lớp trắng/xám vuông góc dưới lớp bo góc.
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        View sheet = findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) {
            sheet.setBackgroundColor(Color.TRANSPARENT);
        }

        btnRate = findViewById(R.id.btnRateUs);
        btnClose = findViewById(R.id.btnClose);

        stars[0] = findViewById(R.id.star1);
        stars[1] = findViewById(R.id.star2);
        stars[2] = findViewById(R.id.star3);
        stars[3] = findViewById(R.id.star4);
        stars[4] = findViewById(R.id.star5);

        bindAppName();
        setupStars();
        onclick();
    }

    /**
     * Gắn chạm cho 5 sao: bấm sao thứ i -> chọn i sao.
     * Thay cho RatingBar (xem lý do ở comment trong dialog_rating_app.xml).
     */
    private void setupStars() {
        for (int i = 0; i < stars.length; i++) {
            if (stars[i] == null) {
                continue;
            }
            final int value = i + 1;
            stars[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    rating = value;
                    applyStars();
                }
            });
        }
        applyStars();
    }

    /** Tô sao: từ 1 tới {@link #rating} là sao vàng, còn lại là sao xám. */
    private void applyStars() {
        for (int i = 0; i < stars.length; i++) {
            if (stars[i] == null) {
                continue;
            }
            stars[i].setImageResource(i < rating
                    ? R.drawable.ic_rate_star_filled
                    : R.drawable.ic_rate_star_empty);
        }
    }

    /** Điền tên app vào câu hỏi ("Do you like ...?") thay vì viết cứng trong string. */
    private void bindAppName() {
        TextView tvTitle = findViewById(R.id.tvTitle);
        if (tvTitle != null) {
            tvTitle.setText(context.getString(
                    R.string.rate_app_question, context.getString(R.string.app_name)));
        }
    }

    public interface OnPress {
        void send(float star);

        void rating(float star);

        void cancel();

        void later();
    }

    public void init(OnPress onPress) {
        this.onPress = onPress;
    }

    public String getRating() {
        return String.valueOf((float) rating);
    }

    public void onclick() {
        btnRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onPress == null) {
                    return;
                }
                if (rating == 0) {
                    Toast.makeText(context, context.getString(R.string.please_reate_use),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                // <=3 sao: đánh giá thấp -> gửi góp ý riêng cho đội phát triển.
                // >3 sao: mời đánh giá trên Play Store. Giữ đúng ngưỡng của bản cũ.
                if (rating <= 3) {
                    onPress.send(rating);
                } else {
                    onPress.rating(rating);
                }
            }
        });

        // Nút X thay cho nút "Not now" của bản cũ; vẫn báo later() để bên gọi xử lý như trước.
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onPress != null) {
                    onPress.later();
                }
                dismiss();
            }
        });
    }
}
