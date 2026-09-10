package com.truongnt.ios.launcher.leftpage.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.truongnt.ios.launcher.IShakeInterface;

public class WidgetBaseLayout extends ConstraintLayout implements IShakeInterface {

    public WidgetBaseLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        disableClipForDelBadge();
    }

    public WidgetBaseLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, 0);
        disableClipForDelBadge();
    }

    /**
     * Dấu trừ (edit) của widget trang trái canh giữa tại GÓC TRÊN-TRÁI -> nửa icon nhô ra ngoài bounds
     * widget. Tắt clip để phần nhô hiển thị (RecyclerView list cũng tắt clip ở CustomContentView).
     */
    private void disableClipForDelBadge() {
        setClipChildren(false);
        setClipToPadding(false);
    }

    public void r() {

    }

    public void setAppSuggestionViewMargin() {

    }

    @Override
    public void beginOrAdjustHintAnimations() {

    }

    @Override
    public void beginOrAdjustHintAnimations(int i) {

    }

    @Override
    public void completeAndClearReorderHintAnimations() {

    }

    @Override
    public void joinAnimations(View view) {

    }
}
