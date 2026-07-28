package com.amz.ios.launcher;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

public class TextViewCustomFont extends AppCompatTextView {

    public static final int[] TextViewCustomFont = {R.attr.customFont};

    public TextViewCustomFont(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public TextViewCustomFont(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, 0);
        AssetManager assets;
        String str = "fonts/SFProTextMedium.otf";

        if (attributeSet == null) {
            setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/SFProTextMedium.otf"));
            return;
        }

        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, TextViewCustomFont);
        int customFont = obtainStyledAttributes.getInt(0, 0);
        assets = context.getAssets();

        if (customFont == 1) {
            str = "fonts/SFProTextLight.otf";
        } else if (customFont == 2) {
            str = "fonts/SFProTextUltralight.otf";
        }

        setTypeface(Typeface.createFromAsset(assets, str));
        obtainStyledAttributes.recycle();
    }
}

