package com.truongnt.ios.launcher.applibrary;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.truongnt.ios.launcher.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Thanh chữ cái A-Z dọc mép phải danh sách app (kiểu iOS): chạm hoặc VUỐT dọc thanh để nhảy
 * thẳng tới nhóm chữ tương ứng trong list.
 *
 * Tự vẽ bằng Canvas (không phải N TextView) để nhẹ và luôn chia đều chiều cao cho các chữ dù
 * danh sách chữ dài ngắn khác nhau.
 *
 * Chỉ hiển thị những chữ CÓ THẬT trong list (xem {@link #setLetters}) — không cứng nhắc A..Z —
 * nên khi search lọc còn vài app thì thanh cũng rút gọn theo, không có chữ bấm vào chẳng đi đâu.
 */
public class AlphabetIndexBar extends View {

    /** Báo cho màn ngoài biết người dùng đang chọn chữ nào để cuộn list tới đó. */
    public interface OnLetterSelectedListener {
        void onLetterSelected(String letter);
    }

    /**
     * Chiều cao mỗi ô chữ = cỡ chữ * hệ số này. Càng nhỏ các chữ càng SÁT nhau.
     * 1.15 cho khoảng hở mảnh vừa đủ để không dính chữ, giống thanh index của iOS.
     */
    private static final float LETTER_SPACING_RATIO = 1.15f;

    private final List<String> mLetters = new ArrayList<>();
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private OnLetterSelectedListener mListener;
    /** Vị trí chữ đang chạm; -1 = không chạm. Dùng để tô sáng chữ đó. */
    private int mActiveIndex = -1;

    private int mTextColor = 0xB3FFFFFF;        // trắng mờ
    private int mActiveTextColor = Color.WHITE;  // chữ đang chạm: trắng rõ

    public AlphabetIndexBar(Context context) {
        this(context, null);
    }

    public AlphabetIndexBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AlphabetIndexBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setTextSize(getResources().getDimensionPixelSize(R.dimen.apps_library_index_bar_text));
        setClickable(true);
    }

    public void setOnLetterSelectedListener(OnLetterSelectedListener l) {
        mListener = l;
    }

    /**
     * Đặt danh sách chữ cái hiển thị. Truyền đúng những chữ có nhóm trong list hiện tại.
     *
     * CHỈ đổi dữ liệu, KHÔNG tự đổi visibility — việc ẩn/hiện thanh do màn ngoài quyết định theo
     * trạng thái search, nếu tự ý đổi ở đây sẽ đá nhau với luồng chuyển màn.
     * Danh sách rỗng thì onDraw không vẽ gì nên cũng không lộ thanh trống.
     */
    public void setLetters(List<String> letters) {
        mLetters.clear();
        if (letters != null) {
            mLetters.addAll(letters);
        }
        mActiveIndex = -1;
        // Số chữ đổi -> chiều cao mong muốn đổi theo (layout dùng wrap_content) nên phải ĐO LẠI,
        // chỉ invalidate() thì thanh giữ nguyên cỡ cũ.
        requestLayout();
        invalidate();
    }

    /** Có chữ nào để hiển thị không (màn ngoài dùng để quyết định ẩn/hiện thanh). */
    public boolean hasLetters() {
        return !mLetters.isEmpty();
    }

    /**
     * Tự đo chiều cao vừa đúng cụm chữ khi layout dùng wrap_content.
     *
     * Thiếu hàm này thì wrap_content cho ra chiều cao 0 -> thanh "biến mất". Bề rộng vẫn theo
     * layout (đã khai cố định), chỉ chiều cao là tự tính.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int count = mLetters.size();
        if (count == 0) {
            return;
        }
        int mode = MeasureSpec.getMode(heightMeasureSpec);
        if (mode == MeasureSpec.EXACTLY) {
            return;   // cha đã ấn định chiều cao -> giữ nguyên
        }
        int wanted = Math.round(mPaint.getTextSize() * LETTER_SPACING_RATIO * count)
                + getPaddingTop() + getPaddingBottom();
        int limit = MeasureSpec.getSize(heightMeasureSpec);
        if (mode == MeasureSpec.AT_MOST) {
            wanted = Math.min(wanted, limit);
        }
        setMeasuredDimension(getMeasuredWidth(), wanted);
    }

    /**
     * Chiều cao MỘT ô chữ. Ưu tiên cỡ gọn (theo cỡ chữ) để các chữ nằm SÁT nhau thành một cụm,
     * thay vì dàn đều hết chiều cao thanh (trông rời rạc). Chỉ khi danh sách quá dài, không đủ
     * chỗ, mới co lại cho vừa.
     */
    private float slotHeight(int count) {
        float usable = getHeight() - getPaddingTop() - getPaddingBottom();
        if (count <= 0 || usable <= 0) {
            return 0f;
        }
        float preferred = mPaint.getTextSize() * LETTER_SPACING_RATIO;
        float maxFit = usable / count;
        return Math.min(preferred, maxFit);
    }

    /** Toạ độ Y đỉnh của cụm chữ (căn giữa theo chiều dọc thanh). */
    private float blockTop(int count, float slot) {
        float usable = getHeight() - getPaddingTop() - getPaddingBottom();
        return getPaddingTop() + (usable - slot * count) / 2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int count = mLetters.size();
        if (count == 0) {
            return;
        }
        float slot = slotHeight(count);
        if (slot <= 0) {
            return;
        }
        float top = blockTop(count, slot);
        float cx = getWidth() / 2f;
        Paint.FontMetrics fm = mPaint.getFontMetrics();
        for (int i = 0; i < count; i++) {
            mPaint.setColor(i == mActiveIndex ? mActiveTextColor : mTextColor);
            float cy = top + slot * (i + 0.5f);
            // Canh giữa chữ theo trục dọc quanh tâm ô.
            float baseline = cy - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(mLetters.get(i), cx, baseline, mPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int count = mLetters.size();
        if (count == 0) {
            return super.onTouchEvent(event);
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE: {
                // Giành quyền xử lý: nếu để cha (RecyclerView/panel) intercept thì vuốt dọc thanh
                // sẽ biến thành cuộn list, không chọn được chữ.
                getParent().requestDisallowInterceptTouchEvent(true);
                int index = indexAt(event.getY(), count);
                if (index != mActiveIndex) {
                    mActiveIndex = index;
                    invalidate();
                    if (mListener != null) {
                        mListener.onLetterSelected(mLetters.get(index));
                    }
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                getParent().requestDisallowInterceptTouchEvent(false);
                mActiveIndex = -1;
                invalidate();
                return true;
            }
            default:
                return super.onTouchEvent(event);
        }
    }

    /**
     * Vị trí chữ tương ứng toạ độ Y đang chạm, đã kẹp trong [0, count-1].
     *
     * Phải tính theo ĐÚNG cách onDraw đặt chữ (cụm chữ căn giữa, mỗi ô cao slotHeight) — nếu dùng
     * công thức chia đều cả thanh như trước thì chạm một chỗ lại nhảy sang chữ khác.
     * Chạm phía trên/dưới cụm chữ được kẹp về chữ đầu/cuối để kéo quá đà vẫn mượt.
     */
    private int indexAt(float y, int count) {
        float slot = slotHeight(count);
        if (slot <= 0) {
            return 0;
        }
        int index = (int) ((y - blockTop(count, slot)) / slot);
        if (index < 0) return 0;
        if (index >= count) return count - 1;
        return index;
    }
}
