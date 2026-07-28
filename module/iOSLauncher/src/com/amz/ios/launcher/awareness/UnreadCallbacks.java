package com.amz.ios.launcher.awareness;

import android.content.ComponentName;

public interface UnreadCallbacks {
    /**
     * Bind shortcuts and application icons with the given component, and
     * update folders unread which contains the given component.
     *
     * @param component
     * @param unreadNum
     */
    void bindComponentUnreadChanged(ComponentName component, int unreadNum);

    /**
     * Bind unread shortcut information if needed, this call back is used to
     * update shortcuts and folders when launcher first created.
     */
    void bindUnreadInfoIfNeeded();
}