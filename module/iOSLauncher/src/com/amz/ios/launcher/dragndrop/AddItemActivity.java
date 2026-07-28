/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amz.ios.launcher.dragndrop;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.LauncherApps.PinItemRequest;
import android.graphics.PointF;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;

import com.amz.ios.launcher.InstallShortcutReceiver;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.compat.LauncherAppsCompatVO;
import com.amz.ios.launcher.model.WidgetItem;
import com.amz.ios.launcher.shortcuts.ShortcutInfoCompat;

@TargetApi(Build.VERSION_CODES.O)
public class AddItemActivity extends Activity implements OnLongClickListener, OnTouchListener {

    private static final int SHADOW_SIZE = 10;

    private final PointF mLastTouchPos = new PointF();

    private PinItemRequest mRequest;
    private LauncherAppState mApp;

    private LivePreviewWidgetCell mWidgetCell;

    private boolean mFinishOnPause = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRequest = LauncherAppsCompatVO.getPinItemRequest(getIntent());
        if (mRequest == null) {
            finish();
            return;
        }

        mApp = LauncherAppState.getInstance(this);

        setContentView(R.layout.add_item_confirmation_activity);
        mWidgetCell = findViewById(R.id.widget_cell);

        if (mRequest.getRequestType() == PinItemRequest.REQUEST_TYPE_SHORTCUT) {
            setupShortcut();
        } else {
            finish();
        }

        mWidgetCell.setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        mLastTouchPos.set(motionEvent.getX(), motionEvent.getY());
        return false;
    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mFinishOnPause) {
            finish();
        }
    }

    private void setupShortcut() {
        PinShortcutRequestActivityInfo shortcutInfo =
                new PinShortcutRequestActivityInfo(mRequest, this);
        WidgetItem item = new WidgetItem(shortcutInfo);
//        mWidgetCell.getWidgetView().setTag(new PendingAddShortcutInfo(shortcutInfo));
        mWidgetCell.applyFromCellItem(item, mApp.getWidgetCache());
        mWidgetCell.ensurePreview();
    }

    /**
     * Called when the cancel button is clicked.
     */
    public void onCancelClick(View v) {
        finish();
    }

    /**
     * Called when place-automatically button is clicked.
     */
    public void onPlaceAutomaticallyClick(View v) {
        if (mRequest.getRequestType() == PinItemRequest.REQUEST_TYPE_SHORTCUT) {
            InstallShortcutReceiver.queueShortcut(
                    new ShortcutInfoCompat(mRequest.getShortcutInfo()), this);
            mRequest.accept();
            finish();
            return;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }
}
