package com.amz.ios.launcher;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import com.amz.ios.launcher.model.PackageItemInfo;

public class FullBubbleTextView extends BubbleTextView {

    public FullBubbleTextView(Context context) {
        super(context);
        setTextVisibility(false);
    }

    public FullBubbleTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setTextVisibility(false);
    }

    public FullBubbleTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setTextVisibility(false);
    }

    @Override
    public void reapplyItemInfo(ItemInfo info) {
        super.reapplyItemInfo(info);
        setText("");
    }

    @Override
    public void applyFromApplicationInfo(AppInfo info) {
        super.applyFromApplicationInfo(info);
        setText("");
    }

    @Override
    public void applyFromPackageItemInfo(PackageItemInfo info) {
        super.applyFromPackageItemInfo(info);
        setText("");
    }

    @Override
    public void applyFromShortcutInfo(ShortcutInfo shortcutInfo) {
        super.applyFromShortcutInfo(shortcutInfo);
        setText("");
    }

    @Override
    protected void applyCompoundDrawables(Drawable icon) {
        super.applyCompoundDrawables(icon);
        setCompoundDrawablesRelative(icon,null,null,null);
    }
}
