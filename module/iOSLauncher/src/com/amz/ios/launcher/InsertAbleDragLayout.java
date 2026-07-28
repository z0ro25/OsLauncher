package com.amz.ios.launcher;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amz.ios.ioslite.common.launcher.Insettable;

public class InsertAbleDragLayout extends LauncherDragLayer implements ViewGroup.OnHierarchyChangeListener, Insettable {

    public Rect mInsets;

    public static final int[] InsertAbleFrameLayout_Layout = {R.attr.layout_ignoreInsets};

    public class LayoutParams extends FrameLayout.LayoutParams {

        public boolean flag;

        public LayoutParams(){
            super(-2,-2);
            this.flag = false;
        }

        public LayoutParams(@NonNull Context c, @Nullable AttributeSet attrs) {
            super(c, attrs);
            TypedArray array = c.obtainStyledAttributes(attrs,InsertAbleFrameLayout_Layout);
            this.flag = array.getBoolean(0, false);
            array.recycle();
        }

        public LayoutParams(@NonNull ViewGroup.LayoutParams source) {
            super(source);
            this.flag = false;
        }
    }

    public InsertAbleDragLayout(@NonNull Context context) {
        this(context,null);
    }

    public InsertAbleDragLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public InsertAbleDragLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mInsets = new Rect();
        setOnHierarchyChangeListener(this);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected FrameLayout.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        return new LayoutParams(lp);
    }

    @Override
    public FrameLayout.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    public void onChildViewAdded(View parent, View child) {
        setFrameLayoutChildInsets(parent, this.mInsets, new Rect());
    }

    @Override
    public void onChildViewRemoved(View parent, View child) {

    }

    @Override
    public void setInsets(Rect rect) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            setFrameLayoutChildInsets(getChildAt(i), rect, this.mInsets);
        }
        this.mInsets.set(rect);
    }

    public void setFrameLayoutChildInsets(View view, Rect rect, Rect rect2) {
        LayoutParams params = (LayoutParams) view.getLayoutParams();
        if (view instanceof Insettable) {
            ((Insettable) view).setInsets(rect);
        } else if (!params.flag) {
            ((FrameLayout.LayoutParams) params).topMargin = (rect.top - rect2.top) + ((FrameLayout.LayoutParams) params).topMargin;
            ((FrameLayout.LayoutParams) params).leftMargin = (rect.left - rect2.left) + ((FrameLayout.LayoutParams) params).leftMargin;
            ((FrameLayout.LayoutParams) params).rightMargin = (rect.right - rect2.right) + ((FrameLayout.LayoutParams) params).rightMargin;
            ((FrameLayout.LayoutParams) params).bottomMargin = (rect.bottom - rect2.bottom) + ((FrameLayout.LayoutParams) params).bottomMargin;
        }
        if (params != null)
        view.setLayoutParams(params);
    }
}
