package com.truongnt.ios.launcher.shortcut;

import android.app.Activity;

// Marker icon "Control Center" trên desktop: chỉ để resolveActivity() ra label/icon; click bị
// Launcher.handlerIOSShortcutClick chặn và mở :module:OSControlCenter.
public class ControlCenter extends Activity {
}
