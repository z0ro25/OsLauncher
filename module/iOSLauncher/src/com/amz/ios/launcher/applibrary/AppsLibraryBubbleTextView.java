package com.amz.ios.launcher.applibrary;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import com.amz.ios.launcher.BubbleTextView;
import com.amz.ios.launcher.Launcher;

public class AppsLibraryBubbleTextView extends BubbleTextView {

    Launcher mLauncher;
    public OpenedLibraryView mOpenView;

    public AppsLibraryBubbleTextView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public AppsLibraryBubbleTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, 0);
        if (context instanceof Launcher){
            mLauncher = (Launcher) context;
        }
        mOpenView = new OpenedLibraryView(context);
    }

    public void clearBackground(){

    }

    @Override
    protected void applyCompoundDrawables(Drawable icon) {

        setCompoundDrawablesRelative(icon,null,null,null);
        setTextColor(Color.RED);
    }

}
