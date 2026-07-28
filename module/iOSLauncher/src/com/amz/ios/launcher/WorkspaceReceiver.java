package com.amz.ios.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WorkspaceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        LauncherAppState.getInstance().getModel().dumpWorkspace();
    }
}
