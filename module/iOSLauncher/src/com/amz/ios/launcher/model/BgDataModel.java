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
import android.util.MutableInt;

import com.amz.ios.launcher.compat.UserHandleCompat;
import com.amz.ios.launcher.shortcuts.ShortcutInfoCompat;
import com.amz.ios.launcher.shortcuts.ShortcutKey;
import com.amz.ios.launcher.util.ComponentKey;
import com.amz.ios.launcher.util.MultiHashMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * All the data stored in-memory and managed by the LauncherModel
 */
public class BgDataModel {

    private static final String TAG = "BgDataModel";

    /**
     * Maps all launcher activities to the id's of their shortcuts (if they have any).
     */
    public final MultiHashMap<ComponentKey, String> deepShortcutMap = new MultiHashMap<>();

    /**
     * Clears all the data
     */
    public synchronized void clear() {
        deepShortcutMap.clear();
    }

    /**
     * Clear all the deep shortcuts for the given package, and re-add the new shortcuts.
     */
    public synchronized void updateDeepShortcutMap(
            String packageName, UserHandle user, List<ShortcutInfoCompat> shortcuts) {
        if (packageName != null) {
            Iterator<ComponentKey> keysIter = deepShortcutMap.keySet().iterator();
            while (keysIter.hasNext()) {
                ComponentKey next = keysIter.next();
                if (next.componentName.getPackageName().equals(packageName)
                        && next.user.getUser().equals(user)) {
                    keysIter.remove();
                }
            }
        }

        // Now add the new shortcuts to the map.
        for (ShortcutInfoCompat shortcut : shortcuts) {
            boolean shouldShowInContainer = shortcut.isEnabled()
                    && (shortcut.isDeclaredInManifest() || shortcut.isDynamic());
            if (shouldShowInContainer) {
                ComponentKey targetComponent
                        = new ComponentKey(shortcut.getActivity(), UserHandleCompat.fromUser(shortcut.getUserHandle()));
                deepShortcutMap.addToList(targetComponent, shortcut.getId());
            }
        }
    }
}
