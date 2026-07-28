/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.amz.ios.launcher.config;

import android.net.Uri;
import android.provider.BaseColumns;

import com.amz.ios.launcher.config.ProviderConfig;

public class GestureSettings {
    public static final class Gesture implements BaseColumns {

        public static final String TABLE_NAME = "gestures";

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + ProviderConfig.AUTHORITY_APPCONFIGURE + "/" + TABLE_NAME);

        /**
         * The content:// style URL for a given row, identified by its id.
         *
         * @param id The row id.
         * @return The unique content URL for the specified row.
         */
        public static Uri getContentUri(long id) {
            return Uri.parse(CONTENT_URI + "/" + id);
        }

        public static final String ACTION_DES = "actionDesc";

        public static final String ACTION_INTENT = "actionIntent";

        public static final String GESTURE_EVENT = "event";

        public static final String MODIFIED = "modified";
    }
}

