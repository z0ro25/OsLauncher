package com.amz.ios.launcher.widget.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.amz.ios.launcher.R;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * TextView cho world clock: tự tính nội dung theo múi giờ cấu hình qua XML.
 *
 * - mode "day":  hiển thị Today / Tomorrow / Yesterday so với ngày ở múi giờ máy.
 * - mode "diff": hiển thị chênh lệch giờ so với múi giờ máy, dạng "+3HRS" / "-5HRS"
 *                / "0HRS".
 *
 * Tự cập nhật mỗi phút khi được gắn vào cửa sổ (widget nội bộ inflate thành View
 * thật nên post/removeCallbacks hoạt động bình thường).
 */
public class WorldClockTextView extends AppCompatTextView {

    private static final int MODE_DAY = 0;
    private static final int MODE_DIFF = 1;

    private TimeZone mTimeZone = TimeZone.getDefault();
    private int mMode = MODE_DAY;

    private final Runnable mTicker = new Runnable() {
        @Override
        public void run() {
            updateText();
            long now = System.currentTimeMillis();
            long delay = 60000L - (now % 60000L);
            postDelayed(this, delay);
        }
    };

    public WorldClockTextView(Context context) {
        this(context, null);
    }

    public WorldClockTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WorldClockTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WorldClockTextView);
            String tzId = a.getString(R.styleable.WorldClockTextView_wctvTimeZone);
            if (!TextUtils.isEmpty(tzId)) {
                mTimeZone = TimeZone.getTimeZone(tzId);
            }
            mMode = a.getInt(R.styleable.WorldClockTextView_wctvMode, MODE_DAY);
            a.recycle();
        }
    }

    private void updateText() {
        if (mMode == MODE_DIFF) {
            setText(formatDiff());
        } else {
            setText(formatDay());
        }
    }

    /** Chênh lệch giờ giữa múi giờ widget và múi giờ máy, tại thời điểm hiện tại. */
    private String formatDiff() {
        long now = System.currentTimeMillis();
        int here = TimeZone.getDefault().getOffset(now);
        int there = mTimeZone.getOffset(now);
        int diffMinutes = Math.round((there - here) / 60000f);
        int hours = diffMinutes / 60;
        int mins = Math.abs(diffMinutes % 60);
        String sign = diffMinutes > 0 ? "+" : (diffMinutes < 0 ? "-" : "");
        if (mins == 0) {
            return sign + Math.abs(hours) + "HRS";
        }
        return sign + Math.abs(hours) + ":" + String.format("%02d", mins) + "HRS";
    }

    /** Today / Tomorrow / Yesterday theo ngày ở múi giờ widget so với ngày máy. */
    private String formatDay() {
        Calendar here = Calendar.getInstance();
        Calendar there = Calendar.getInstance(mTimeZone);
        int dayHere = here.get(Calendar.DAY_OF_YEAR) + here.get(Calendar.YEAR) * 1000;
        int dayThere = there.get(Calendar.DAY_OF_YEAR) + there.get(Calendar.YEAR) * 1000;
        if (dayThere > dayHere) {
            return "Tomorrow";
        } else if (dayThere < dayHere) {
            return "Yesterday";
        }
        return "Today";
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        removeCallbacks(mTicker);
        post(mTicker);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(mTicker);
    }
}
