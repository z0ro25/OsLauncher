package com.amz.ios.ioslite.common.launcher;

/**
 * LauncherStateCallback;
 * <p/>
 * Implemented by launcher; these methods will be called when launcher state changed;
 */
public interface LauncherStateCallback {

    /**
     * Called when user scroll workspaces pages;
     */
    void onPageSwitch();

    /**
     * Called when custom left page show;
     */
    void onLeftCustomContentShow();


    /**
     * Called when custom left page hide;
     */
    void onLeftCustomContentHide();
}
