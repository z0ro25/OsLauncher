package com.amz.ios.launcher.expdev;

import android.content.Context;
import androidx.appcompat.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class TextScrollView extends AppCompatTextView {
    public TextScrollView(Context context) {
        super(context);
    }

    public TextScrollView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public TextScrollView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!isFocused()) {
            requestFocus();
        }
        return super.onTouchEvent(motionEvent);
    }

    public void setEnabled(boolean z) {
        if (!z) {
            requestFocus();
        }
        super.setEnabled(z);
    }
}
