package com.amz.ios.launcher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.appwidget.AppWidgetHostView;
import android.view.View;
import android.view.ViewGroup;

import com.amz.ios.launcher.folder.Folder;

public class DelIconAnim {

    private static boolean sNeedRefersh = false;
    private static float sScale = 0.0f;
    private ValueAnimator mAnim;
    private Runnable mCallback = null;
    private Workspace mWorkspace;
    private final ZoomInInterpolator mZoomInInterpolator = new ZoomInInterpolator();
    private boolean mZoomOut = false;

    public DelIconAnim(Workspace workspace) {
        this.mWorkspace = workspace;
        this.mAnim = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.mAnim.setInterpolator(this.mZoomInInterpolator);
        this.mAnim.setDuration(300L);
    }

    public static float getScale() {
        return sScale;
    }

    private void invalidate(ViewGroup viewGroup) {
        if (viewGroup != null) {
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View childAt = viewGroup.getChildAt(i);
                if (childAt instanceof AppWidgetHostView) {
                    childAt.invalidate(childAt.getScrollX(), childAt.getScrollY(), childAt.getScrollX() + childAt.getWidth() + 2, childAt.getScrollY() + childAt.getHeight() + 2);
                } else {
                    childAt.invalidate();
                }
            }
        }
    }

    public static boolean shouldRefresh() {
        return sNeedRefersh;
    }

    public void doAnimParam() {
        this.mAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationCancel(Animator animator) {
                if (!DelIconAnim.this.mZoomOut) {
                    float unused = DelIconAnim.sScale = 0.0f;
                    boolean unused2 = DelIconAnim.sNeedRefersh = false;
                }
            }

            public void onAnimationEnd(Animator animator) {
                if (!DelIconAnim.this.mZoomOut) {
                    float unused = DelIconAnim.sScale = 0.0f;
                    boolean unused2 = DelIconAnim.sNeedRefersh = false;
                }
            }

            public void onAnimationStart(Animator animator) {
                boolean unused = DelIconAnim.sNeedRefersh = true;
            }
        });
        this.mAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            /* class com.ios.home.DelIconAnim.AnonymousClass2 */

            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (DelIconAnim.this.mZoomOut) {
                    float unused = DelIconAnim.sScale = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                } else {
                    float unused2 = DelIconAnim.sScale = 1.0f - ((Float) valueAnimator.getAnimatedValue()).floatValue();
                }
                DelIconAnim.this.updateView(((Float) valueAnimator.getAnimatedValue()).floatValue());
            }
        });
        this.mAnim.start();
    }

    public void initAnimParam(boolean isZoomout) {
        initAnimParam(isZoomout, null);
    }

    public void initAnimParam(boolean isZoomout, Runnable runnable) {
        this.mCallback = runnable;
        if (this.mZoomOut != isZoomout) {
            if (this.mAnim.isRunning()) {
                this.mAnim.cancel();
            }
            this.mZoomOut = isZoomout;
            doAnimParam();
        }
    }

    public boolean isZoomOut() {
        return this.mZoomOut;
    }

    public void updateView(float f) {
        Folder openFolder = this.mWorkspace.getOpenFolder();
        if (openFolder != null) {
            invalidate(openFolder.mFolderContent);
        } else if (f != 0.0f) {
            invalidate(this.mWorkspace.getCurrentDropLayout());
            invalidate(this.mWorkspace.getHotseatCellLayout());
        }
        if (f == 1.0f) {
            for (int i = 0; i < this.mWorkspace.getChildCount(); i++) {
                invalidate((ViewGroup) this.mWorkspace.getChildAt(i));
            }
            if (this.mCallback != null) {
                this.mCallback.run();
                this.mCallback = null;
            }
        }
    }
}
