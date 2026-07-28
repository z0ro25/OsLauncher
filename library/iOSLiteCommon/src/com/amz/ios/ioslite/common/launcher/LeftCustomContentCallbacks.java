package com.amz.ios.ioslite.common.launcher;

/**
 * left custom view of launcher workspace implement this;
 */
public interface LeftCustomContentCallbacks {

    // Indicates whether the user is allowed to scroll away from the custom content.
    boolean isScrollingAllowed(float downX, float downY, float lastX, float lastY, float rawX, float rawY);

    // Custom content is completely shown. {@code fromResume} indicates whether this was caused
    // by a onResume or by scrolling otherwise.
    public void onShow(boolean fromResume);

    // Custom content is completely hidden
    public void onHide();

    // Custom content scroll progress changed. From 0 (not showing) to 1 (fully showing).
    public void onScrollProgressChanged(float progress);

    //if page need to block backPressed
    boolean onBackPressed();
}
