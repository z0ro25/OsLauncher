package com.amz.ios.launcher;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.ioslite.common.launcher.Insettable;
import com.amz.ios.launcher.config.Settings;

public class InsettableFrameLayout extends FrameLayout implements
        ViewGroup.OnHierarchyChangeListener, Insettable {
    private static final String TAG = "InsettableFrameLayout";

    protected Rect mInsets = new Rect();

    public Rect getInsets() {
        return mInsets;
    }

    public InsettableFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnHierarchyChangeListener(this);
    }

    public void setFrameLayoutChildInsets(View child, Rect newInsets, Rect oldInsets) {
        boolean hotseatLabelForcedShow = Partner.getBoolean(getContext(), Partner.DEF_HOTSEAT_LABEL_FORCED_SHOW);
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (child instanceof Insettable) {
            ((Insettable) child).setInsets(newInsets);
        } else if (!lp.ignoreInsets) {
            lp.topMargin += (newInsets.top - oldInsets.top);
            lp.leftMargin += (newInsets.left - oldInsets.left);
            lp.rightMargin += (newInsets.right - oldInsets.right);
            if (hotseatLabelForcedShow){
                lp.bottomMargin += (newInsets.bottom - oldInsets.bottom);
            }
            boolean defLabelShow = Partner.getBoolean(getContext(), Partner.DEF_HOTSEAT_LABEL_SHOW_ENABLE);
            if (child instanceof PageIndicator){
                DebugLog.w(TAG,"==========setFrameLayoutChildInsets&&!lp.ignoreInsets:"+newInsets.bottom+"/"+lp.bottomMargin);
            }
            if (child instanceof Hotseat) {
                if (hotseatLabelForcedShow){
                    Settings.setHotseatAppNameVisibility(getContext(), true);
                }else {
                    if (newInsets.bottom == 0) {
                        Settings.setHotseatAppNameVisibility(getContext(), defLabelShow);
                    } else {
                        Settings.setHotseatAppNameVisibility(getContext(), defLabelShow);
                    }
                }
            }
        }
        if (child instanceof Hotseat){
            ((Hotseat)child).getLayout().mShortcutsAndWidgets.requestLayout();
        }
        child.setLayoutParams(lp);
    }

    @Override
    public void setInsets(Rect insets) {
        // If the insets haven't changed, this is a no-op. Avoid unnecessary layout caused by
        // modifying child layout params.
        //if (insets.equals(mInsets)) return;

        final int n = getChildCount();
        for (int i = 0; i < n; i++) {
            final View child = getChildAt(i);
            setFrameLayoutChildInsets(child, insets, mInsets);
        }
        mInsets.set(insets);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new InsettableFrameLayout.LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    // Override to allow type-checking of LayoutParams.
    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof InsettableFrameLayout.LayoutParams;
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    public static class LayoutParams extends FrameLayout.LayoutParams {
        boolean ignoreInsets = false;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs,
                    R.styleable.InsettableFrameLayout_Layout);
            ignoreInsets = a.getBoolean(
                    R.styleable.InsettableFrameLayout_Layout_layout_ignoreInsets, false);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams lp) {
            super(lp);
        }
    }

    @Override
    public void onChildViewAdded(View parent, View child) {
        setFrameLayoutChildInsets(child, mInsets, new Rect());
    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
    }

}
