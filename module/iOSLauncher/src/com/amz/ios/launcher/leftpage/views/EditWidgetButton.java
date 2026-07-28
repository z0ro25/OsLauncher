package com.amz.ios.launcher.leftpage.views;


import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.appcompat.widget.AppCompatTextView;

import com.amz.ios.launcher.DragLayer;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;

public class EditWidgetButton extends AppCompatTextView {
    public EditWidgetButton(Context context, AttributeSet attributeSet) {

        super(context, attributeSet);
        setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/SFProTextLight.otf"));
        setBackgroundResource(R.drawable.circle_bg_light);
        int dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.edit_text_padding);
        setPadding(dimensionPixelSize, 0, dimensionPixelSize, 0);
        setGravity(17);
        setText(R.string.edit);
        setTextColor(Color.BLACK);
        setTextSize(2, 12.0f);
    }
}
