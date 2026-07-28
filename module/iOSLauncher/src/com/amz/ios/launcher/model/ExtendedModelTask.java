/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.amz.ios.launcher.model;

import android.os.UserHandle;

import com.amz.ios.launcher.LauncherModel.BaseModelUpdateTask;
import com.amz.ios.launcher.LauncherModel.CallbackTask;
import com.amz.ios.launcher.LauncherModel.Callbacks;
import com.amz.ios.launcher.ShortcutInfo;
import com.amz.ios.launcher.compat.UserHandleCompat;
import com.amz.ios.launcher.util.ComponentKey;
import com.amz.ios.launcher.util.MultiHashMap;

import java.util.ArrayList;

/**
 * Extension of {@link BaseModelUpdateTask} with some utility methods
 */
public abstract class ExtendedModelTask extends BaseModelUpdateTask {

    public void bindUpdatedShortcuts(
            ArrayList<ShortcutInfo> updatedShortcuts, UserHandleCompat user) {
        bindUpdatedShortcuts(updatedShortcuts, new ArrayList<ShortcutInfo>(), user);
    }

    public void bindUpdatedShortcuts(
            final ArrayList<ShortcutInfo> updatedShortcuts,
            final ArrayList<ShortcutInfo> removedShortcuts,
            final UserHandleCompat user) {
        if (!updatedShortcuts.isEmpty() || !removedShortcuts.isEmpty()) {
            scheduleCallbackTask(new CallbackTask() {
                @Override
                public void execute(Callbacks callbacks) {
                    callbacks.bindShortcutsChanged(updatedShortcuts, removedShortcuts, user);
                }
            });
        }
    }

    public void bindDeepShortcuts(BgDataModel dataModel) {
        final MultiHashMap<ComponentKey, String> shortcutMapCopy = dataModel.deepShortcutMap.clone();
        scheduleCallbackTask(new CallbackTask() {
            @Override
            public void execute(Callbacks callbacks) {
                callbacks.bindDeepShortcutMap(shortcutMapCopy);
            }
        });
    }
}
