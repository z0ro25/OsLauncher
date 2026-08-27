package com.amz.ios.launcher.leftpage.widgets;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Widget trang trái luôn VUÔNG: chiều cao bằng đúng chiều rộng thật sau khi đo.
 *
 * LÝ DO TỒN TẠI: các widget 2x2 ở trang trái khai chiều cao cố định
 * {@code @dimen/left_page_widget_2x2_height} (160dp) trong khi chiều rộng là {@code match_parent}
 * của một cột trong lưới 2 cột — tức phụ thuộc bề ngang màn hình. Hai con số đó không bằng nhau,
 * nên thẻ bị chữ nhật và tỉ lệ đổi theo từng máy; hai widget cạnh nhau cũng không cùng cỡ nếu
 * layout của chúng khai chiều cao khác nhau.
 *
 * Lớp này bỏ qua chiều cao khai báo và ép hình vuông, nên hai thẻ đầu trang trái luôn bằng nhau và
 * vuông trên mọi máy.
 *
 * KHÔNG sửa {@code left_page_widget_2x2_height} vì dimen đó dùng chung cho 5 layout widget 2x2
 * (clock, photo, calendar, calendar_month, battery) — đổi giá trị sẽ tác động cả những widget
 * không nằm trong yêu cầu này. Widget nào cần vuông thì đổi lớp cha sang lớp này.
 */
public class SquareBlurWidget extends BlurConstraintLayoutWidget {

    public SquareBlurWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareBlurWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Đo bình thường một lượt để lấy chiều rộng thật mà cha cấp cho, rồi đo lại với chiều cao
        // đúng bằng chiều rộng đó. Đo hai lượt là cần: ConstraintLayout bên trong phải bố trí lại
        // theo kích thước cuối cùng, nếu chỉ setMeasuredDimension thì nội dung vẫn theo cỡ cũ.
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int size = getMeasuredWidth();
        if (size <= 0) {
            return;
        }
        int exactly = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
        super.onMeasure(exactly, exactly);
    }
}
