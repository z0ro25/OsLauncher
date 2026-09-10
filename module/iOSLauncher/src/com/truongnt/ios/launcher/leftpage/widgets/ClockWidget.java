package com.truongnt.ios.launcher.leftpage.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.truongnt.ios.launcher.Launcher;
import com.truongnt.ios.launcher.R;
import com.truongnt.ios.launcher.leftpage.views.TimeView;

public class ClockWidget extends WidgetBaseLayout {

    TimeView mTimeView;

    public ClockWidget(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public ClockWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        int margin = ((Launcher) context).getDeviceProfile().edgeMarginPx;
        this.mTimeView = (TimeView) LayoutInflater.from(context).inflate(R.layout.clock_widget, (ViewGroup) this, true).findViewById(R.id.time_view);
        int dimensionPixelSize = context.getResources().getDimensionPixelSize(R.dimen.clock_padding);
        setPadding(dimensionPixelSize, dimensionPixelSize, dimensionPixelSize, dimensionPixelSize);
        ((ConstraintLayout.LayoutParams) this.mTimeView.getLayoutParams()).setMargins(margin, margin, margin, margin);
    }

    @Override
    public void r() {
        super.r();
    }

    @Override
    public void setAppSuggestionViewMargin() {
        super.setAppSuggestionViewMargin();
        invalidate();
    }
}
