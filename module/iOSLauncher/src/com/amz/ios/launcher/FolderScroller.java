package com.amz.ios.launcher;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import com.amz.ios.launcher.widget.IOSScrollView;

import java.util.ArrayList;

public class FolderScroller extends IOSScrollView {
    private static final String TAG = "FolderScroller";

    public static int DRAG_SCROLL_ANIMATION_DURATION = 740;
    public static final int STATE_SCROLL_CANCEL = 0;
    public static final int STATE_SCROLL_UP = 1;
    public static final int STATE_SCROLL_DOWN = 2;
    public static final int MAX_SHOW_LINE = 4;

    protected CellLayout mContent;
    private int mContentHeightGap;
    private int mContentWidthGap;
    ValueAnimator mDragScrollAnimation;
    ValueAnimator mScrollAnimation;
    protected ArrayList<View> mVisibleItems = new ArrayList<>();
    int yStart = 0;

    public FolderScroller(Context context) {
        super(context);
        initLayoutParam();
    }

    public FolderScroller(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initLayoutParam();
    }

    public FolderScroller(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        initLayoutParam();
    }

    private void getInvalidateRows(int[] rows) {
        if (rows.length < 2) {
            Log.e(TAG, "getInvalidateRows -- array length < 2!!!!");
            return;
        }
        int contentCellHeight = getContentCellHeight() + this.mContentHeightGap;
        if (contentCellHeight <= 0) {
            rows[0] = 0;
            rows[1] = 0;
            return;
        }
        rows[0] = getScrollY() / contentCellHeight;
        rows[1] = rows[0] + 4;
    }

    private void initLayoutParam() {
        this.mContentWidthGap = getResources().getDimensionPixelSize(R.dimen.folder_content_width_gap);
        this.mContentHeightGap = getResources().getDimensionPixelSize(R.dimen.folder_content_height_gap);
    }

    public void cancelDragScroll() {
        if (this.mDragScrollAnimation != null) {
            this.mDragScrollAnimation.cancel();
            this.mDragScrollAnimation = null;
        }
    }

    public void cancelSmoothScroll() {
        if (this.mScrollAnimation != null) {
            this.mScrollAnimation.cancel();
            this.mScrollAnimation = null;
        }
    }

    public void checkFolderScrollDirection(int direction) {
        switch (direction) {
            case STATE_SCROLL_CANCEL:
                cancelDragScroll();
                return;
            case STATE_SCROLL_UP:
                dragScroll(-getContentCellHeight(), DRAG_SCROLL_ANIMATION_DURATION);
                return;
            case STATE_SCROLL_DOWN:
                dragScroll(getContentCellHeight(), DRAG_SCROLL_ANIMATION_DURATION);
                return;
            default:
                return;
        }
    }

    public void dragScroll(int toScrollY, int duration) {
        if (this.mDragScrollAnimation == null || !this.mDragScrollAnimation.isStarted()) {
            this.yStart = getScrollY();
            ValueAnimator durationAnimator = ValueAnimator.ofFloat(0.0f, (float) toScrollY).setDuration((long) duration);
            durationAnimator.setInterpolator(new DecelerateInterpolator(1.0f));
            durationAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                /* class com.ios.home.FolderScroller.AnonymousClass3 */

                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    FolderScroller.this.scrollTo(0, ((Float) valueAnimator.getAnimatedValue()).intValue() + FolderScroller.this.yStart);
                }
            });
            durationAnimator.addListener(new Animator.AnimatorListener() {
                public void onAnimationCancel(Animator animator) {}
                public void onAnimationEnd(Animator animator) {}
                public void onAnimationRepeat(Animator animator) {}
                public void onAnimationStart(Animator animator) {}
            });
            this.mDragScrollAnimation = durationAnimator;
            this.mDragScrollAnimation.start();
        }
    }

    public ArrayList<View> getAndInvalidateVisibleItems() {
        this.mVisibleItems.clear();
        int[] rows = new int[2];
        getInvalidateRows(rows);
        int x = rows[0];
        int y = rows[1];
        int cellCountY = this.mContent.getCountY();
        if (x >= cellCountY) {
            x = cellCountY - 1;
        }
        if (y >= cellCountY) {
            y = cellCountY - 1;
        }
        for (int i = x; i <= y; i ++) {
            for (int j = 0; j < this.mContent.getCountX(); j ++) {
                View childAt = this.mContent.getChildAt(j, i);
                if (childAt != null) {
                    childAt.invalidate();
                    this.mVisibleItems.add(childAt);
                }
            }
        }
        return this.mVisibleItems;
    }

    public int getContentCellHeight() {
        return this.mContent.getCellHeight();
    }

    public int getDesiredHeight() {
        return this.mContent.getCountY() <= 4 ? this.mContent.getDesiredHeight() + getPaddingTop() + getPaddingBottom() : this.mContent.getDesiredHeight(4) + getPaddingTop() + getPaddingBottom();
    }

    public int getDesiredWidth() {
        return this.mContent.getDesiredWidth() + getPaddingLeft() + getPaddingRight();
    }

    public int getFirstChildPaddingLeft() {
        return getPaddingLeft() + this.mContent.getPaddingLeft();
    }

    public int getFirstChildPaddingTop() {
        return getPaddingTop() + this.mContent.getPaddingTop();
    }

    public CellLayout getFolderContent() {
        return this.mContent;
    }

    public void onFinishInflate() {
        super.onFinishInflate();
        this.mContent = (CellLayout) findViewById(R.id.folder_content);
        this.mContent.setGridSize(1, 1);
        this.mContent.setGap(this.mContentWidthGap, this.mContentHeightGap);
        this.mContent.setMotionEventSplittingEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.mContent.measure(
                View.MeasureSpec.makeMeasureSpec(this.mContent.getDesiredWidth(), MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(this.mContent.getDesiredHeight(), MeasureSpec.EXACTLY)
        );

        setMeasuredDimension(View.MeasureSpec.getSize(widthMeasureSpec), View.MeasureSpec.getSize(heightMeasureSpec));
    }

    public void setContentCellDimension() {
        getContext().getResources().getDimensionPixelSize(R.dimen.folder_bg_padding);
        this.mContent.setCellDimension(getResources().getDimensionPixelSize(R.dimen.folder_content_width), getResources().getDimensionPixelSize(R.dimen.folder_content_height));
    }

    public void smoothScroll(final int toScrollY, int duration) {
        this.mScrollAnimation = null;
        ValueAnimator animator = ValueAnimator.ofFloat(0.0f, (float) toScrollY);
        animator.setInterpolator(new DecelerateInterpolator(1.0f));
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            /* class com.ios.home.FolderScroller.AnonymousClass1 */

            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                FolderScroller.this.scrollTo(0, ((Float) valueAnimator.getAnimatedValue()).intValue());
            }
        });

        animator.addListener(new Animator.AnimatorListener() {
            public void onAnimationCancel(Animator animator) {
                FolderScroller.this.scrollTo(0, 0);
            }

            public void onAnimationEnd(Animator animator) {
                FolderScroller.this.scrollTo(0, toScrollY);
            }

            public void onAnimationRepeat(Animator animator) {
            }

            public void onAnimationStart(Animator animator) {
            }
        });
        animator.setDuration((long) duration);
        animator.start();
        this.mScrollAnimation = animator;
    }
}
