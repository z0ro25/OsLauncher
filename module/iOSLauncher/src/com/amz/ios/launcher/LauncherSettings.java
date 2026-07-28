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

package com.amz.ios.launcher;

import android.net.Uri;
import android.provider.BaseColumns;

import com.amz.ios.launcher.config.ProviderConfig;

/**
 * Settings related utilities.
 */
public class LauncherSettings {
    /**
     * Columns required on table staht will be subject to backup and restore.
     */
    public interface ChangeLogColumns extends BaseColumns {
        /**
         * The time of the last update to this row.
         * <P>Type: INTEGER</P>
         */
        String MODIFIED = "modified";
    }

    public interface BaseLauncherColumns extends ChangeLogColumns {
        /**
         * Descriptive name of the gesture that can be displayed to the user.
         * <P>Type: TEXT</P>
         */
        String TITLE = "title";

        /**
         * The Intent URL of the gesture, describing what it points to. This
         * value is given to {@link android.content.Intent#parseUri(String, int)} to create
         * an Intent that can be launched.
         * <P>Type: TEXT</P>
         */
        String INTENT = "intent";

        /**
         * The type of the gesture
         * <p/>
         * <P>Type: INTEGER</P>
         */
        String ITEM_TYPE = "itemType";

        /**
         * The gesture is an application
         */
        int ITEM_TYPE_APPLICATION = 0;

        /**
         * The gesture is an application created shortcut
         */
        int ITEM_TYPE_SHORTCUT = 1;


        /**
         * Hotseat screen id
         */
        public static final int HOTSEAT_SCREEN_ID = 999;


        /**
         * The type of the gesture
         * <p/>
         * <P>Type: INTEGER</P>
         */
        String FOLDER_CATEGORY_TYPE = "folderCategoryType";

        /**
         * The icon type.
         * <P>Type: INTEGER</P>
         */
        String ICON_TYPE = "iconType";

        /**
         * The icon is a resource identified by a package name and an integer id.
         */
        int ICON_TYPE_RESOURCE = 0;

        /**
         * The icon is a bitmap.
         */
        int ICON_TYPE_BITMAP = 1;

        /**
         * The icon package name, if icon type is ICON_TYPE_RESOURCE.
         * <P>Type: TEXT</P>
         */
        String ICON_PACKAGE = "iconPackage";

        /**
         * The icon resource id, if icon type is ICON_TYPE_RESOURCE.
         * <P>Type: TEXT</P>
         */
        String ICON_RESOURCE = "iconResource";

        /**
         * The custom icon bitmap, if icon type is ICON_TYPE_BITMAP.
         * <P>Type: BLOB</P>
         */
        String ICON = "icon";
    }

    /**
     * Workspace Screens.
     * <p/>
     * Tracks the order of workspace screens.
     */
    public static final class WorkspaceScreens implements ChangeLogColumns {

        public static final String TABLE_NAME = "workspaceScreens";

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + ProviderConfig.AUTHORITY + "/" + TABLE_NAME);

        /**
         * The rank of this screen -- ie. how it is ordered relative to the other screens.
         * <P>Type: INTEGER</P>
         */
        public static final String SCREEN_RANK = "screenRank";
    }

    /**
     * Empty Workspace Screens.
     * <p/>
     * Tracks the order of empty workspace screens.
     */
    public static final class EmptyWorkspaceScreens implements ChangeLogColumns {

        public static final String TABLE_NAME = "emptyWorkspaceScreens";

        /**
         * The content:// style URL for this table
         */
        static final Uri CONTENT_URI = Uri.parse("content://" + ProviderConfig.AUTHORITY + "/" + TABLE_NAME);

        /**
         * The rank of this screen -- ie. how it is ordered relative to the other screens.
         * <P>Type: INTEGER</P>
         */
        public static final String SCREEN_RANK = "screenRank";
    }

    /**
     * Favorites.
     */
    public static final class Favorites implements BaseLauncherColumns {

        public static final String TABLE_NAME = "favorites";

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + ProviderConfig.AUTHORITY + "/" + TABLE_NAME);

        /**
         * The content:// style URL for a given row, identified by its id.
         *
         * @param id The row id.
         * @return The unique content URL for the specified row.
         */
        public static Uri getContentUri(long id) {
            return Uri.parse("content://" + ProviderConfig.AUTHORITY + "/" + TABLE_NAME + "/" + id);
        }

        /**
         * The container holding the favorite
         * <P>Type: INTEGER</P>
         */
        public static final String CONTAINER = "container";

        /**
         * The icon is a resource identified by a package name and an integer id.
         */
        public static final int CONTAINER_DESKTOP = -100;
        public static final int CONTAINER_HOTSEAT = -101;

        public static final String containerToString(int container) {
            switch (container) {
                case CONTAINER_DESKTOP:
                    return "desktop";
                case CONTAINER_HOTSEAT:
                    return "hotseat";
                default:
                    return String.valueOf(container);
            }
        }

        /**
         * The screen holding the favorite (if container is CONTAINER_DESKTOP)
         * <P>Type: INTEGER</P>
         */
        public static final String SCREEN = "screen";

        /**
         * The X coordinate of the cell holding the favorite
         * (if container is CONTAINER_HOTSEAT or CONTAINER_HOTSEAT)
         * <P>Type: INTEGER</P>
         */
        public static final String CELLX = "cellX";

        /**
         * The Y coordinate of the cell holding the favorite
         * (if container is CONTAINER_DESKTOP)
         * <P>Type: INTEGER</P>
         */
        public static final String CELLY = "cellY";

        /**
         * The X span of the cell holding the favorite
         * <P>Type: INTEGER</P>
         */
        public static final String SPANX = "spanX";

         // The Y span of the cell holding the favorite
        public static final String SPANY = "spanY";

        // is hidden app?
        public static final String IS_HIDDEN = "hidden";

        /**
         * The profile id of the item in the cell.
         * <p/>
         * Type: INTEGER
         * </P>
         */
        public static final String PROFILE_ID = "profileId";

        /**
         * The favorite is a user created folder
         */
        public static final int ITEM_TYPE_FOLDER = 2;

        /**
         * The favorite is a widget
         */
        public static final int ITEM_TYPE_APPWIDGET = 4;

        /**
         * The favorite is a ios widget provided by the launcher
         */
        public static final int ITEM_TYPE_IOS_APPWIDGET = 5;

        /**
         * The gesture is an application created deep shortcut
         */
        public static final int ITEM_TYPE_DEEP_SHORTCUT = 6;

        /**
         * 
         */
        public static final int ITEM_TYPE_VIRTUAL_APP = 2001;

        /**
         * The appWidgetId of the widget
         * <p/>
         * <P>Type: INTEGER</P>
         */
        public static final String APPWIDGET_ID = "appWidgetId";

        /**
         * The ComponentName of the widget provider
         * <p/>
         * <P>Type: STRING</P>
         */
        public static final String APPWIDGET_PROVIDER = "appWidgetProvider";

        /**
         * Boolean indicating that his item was restored and not yet successfully bound.
         * <P>Type: INTEGER</P>
         */
        public static final String RESTORED = "restored";

        /**
         * Indicates the position of the item inside an auto-arranged view like folder or hotseat.
         * <p>Type: INTEGER</p>
         */
        public static final String RANK = "rank";

        /**
         * Stores general flag based options for {@link ItemInfo}s.
         * <p>Type: INTEGER</p>
         */
        public static final String OPTIONS = "options";
    }

    /**
     * Workspace Screens.
     * <p/>
     * Tracks the order of workspace screens.
     */
    public static final class NewInstall implements ChangeLogColumns {

        public static final String TABLE_NAME = "newinstall";

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + ProviderConfig.AUTHORITY + "/" + TABLE_NAME);

        public static final String PACKAGENAME = "packagename";

        public static final String NEW_INSTALL_FLAG = "newinstallflag";
    }

    /**
     * Launcher settings
     */
    public static final class Settings {

        public static final Uri CONTENT_URI = Uri.parse("content://" + ProviderConfig.AUTHORITY + "/settings");

        public static final String METHOD_GET_BOOLEAN = "get_boolean_setting";
        public static final String METHOD_SET_BOOLEAN = "set_boolean_setting";

        public static final String EXTRA_VALUE = "value";
        public static final String EXTRA_DEFAULT_VALUE = "default_value";
    }

    public static final class BackupSettings implements BaseColumns {

        public static final String TABLE_NAME = "backupsettings";

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + ProviderConfig.AUTHORITY + "/" + TABLE_NAME);


        /**
         * The content:// style URL for a given row, identified by its id.
         *
         * @param id The row id.
         * @return The unique content URL for the specified row.
         */
        public static Uri getContentUri(long id) {
            return Uri.parse(CONTENT_URI + "/" + id);
        }

        public static final String SETTING_NAME = "name";

        public static final String SETTING_VALUE = "value";

        public static final String MODIFIED = "modified";
    }
}
