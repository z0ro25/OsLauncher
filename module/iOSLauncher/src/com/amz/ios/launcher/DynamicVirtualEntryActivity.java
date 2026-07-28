package com.amz.ios.launcher;

import android.os.Bundle;

import com.amz.ios.ioslite.common.CommonActivity;
import com.amz.ios.ioslite.common.launcher.LauncherRouter;

public class DynamicVirtualEntryActivity extends CommonActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        startLauncher();
        finish();
    }

    public void startLauncher() {
        LauncherRouter.launch(this);
    }
}
