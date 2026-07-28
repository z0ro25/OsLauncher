package com.amz.ios.launcher.views;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import androidx.appcompat.widget.AppCompatTextView;
import android.util.AttributeSet;

/**
 * Created by 37 on 10/31/2019.
 *
 */

public class CustomTextView extends AppCompatTextView {
    public CustomTextView(Context context) {
        super(context);
        init();
    }
    public CustomTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        init();
    }

    public CustomTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }
    private void init() {
        updateFont();
    }

    public void updateFont() {
//        AssetManager assets = getContext().getAssets();
//        String str = "fonts/SFProTextLight.otf";
//        setTypeface(Typeface.createFromAsset(assets, str));
        setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
    }

    public void setSpecialFont(int font_type){
//        Themes.setSpacialTypeFace(getContext(), font_type, this);
    }

    public void getLocalIconCenter(int[] iArr) {
        Drawable[] compoundDrawables = getCompoundDrawables();
        if (compoundDrawables[1] != null) {
            new Rect();
            getLineHeight();
            int paddingTop = getPaddingTop();
            Rect bounds = compoundDrawables[1].getBounds();
            iArr[0] = getWidth() / 2;
            iArr[1] = (bounds.height() / 2) + paddingTop;
        }
    }
}
