package com.amz.ios.launcher.leftpage.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import com.amz.ios.launcher.R;

/**
 * Widget Lịch LƯỚI THÁNG (đủ ngày) cho màn trang trái (Today), cỡ 2x2.
 * <p>
 * Chỉ là vỏ chứa {@link com.amz.ios.launcher.widget.view.CalendarMiniMonthView} — view này tự vẽ
 * thẻ trắng + lưới ngày bằng Canvas, tự làm mới mỗi 60s, KHÔNG cần quyền/dữ liệu lịch. Nhờ vậy
 * lớp này không cần logic gì thêm ngoài việc inflate layout (khác với {@link CalendarWidget_2x2}
 * vốn phải xử lý xin quyền + list sự kiện). Dùng đúng widget "đủ ngày" giống thẻ nổi bật ở khay.
 */
public class CalendarMonthWidget_2x2 extends SquareBlurWidget {

    public CalendarMonthWidget_2x2(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CalendarMonthWidget_2x2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.calendar_month_widget_2x2, this, true);
    }
}
