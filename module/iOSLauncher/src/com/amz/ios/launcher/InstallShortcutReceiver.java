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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Process;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.launcher.compat.LauncherActivityInfoCompat;
import com.amz.ios.launcher.compat.LauncherAppsCompat;
import com.amz.ios.launcher.compat.UserHandleCompat;
import com.amz.ios.launcher.compat.UserManagerCompat;
import com.amz.ios.launcher.config.Settings;
import com.amz.ios.launcher.graphics.LauncherIcons;
import com.amz.ios.launcher.shortcuts.DeepShortcutManager;
import com.amz.ios.launcher.shortcuts.ShortcutInfoCompat;
import com.amz.ios.launcher.shortcuts.ShortcutKey;
import com.amz.ios.launcher.util.Thunk;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class InstallShortcutReceiver extends BroadcastReceiver {
    private static final String TAG = "InstallShortcutReceiver";
    private static final boolean DBG = false;

    private static final String ACTION_INSTALL_SHORTCUT =
            "com.android.launcher.action.INSTALL_SHORTCUT";

    private static final String LAUNCH_INTENT_KEY = "intent.launch";
    private static final String NAME_KEY = "name";
    private static final String ICON_KEY = "icon";
    private static final String ICON_RESOURCE_NAME_KEY = "iconResource";
    private static final String ICON_RESOURCE_PACKAGE_NAME_KEY = "iconResourcePackage";

    private static final String APP_SHORTCUT_TYPE_KEY = "isAppShortcut";
    private static final String DEEPSHORTCUT_TYPE_KEY = "isDeepShortcut";
    private static final String USER_HANDLE_KEY = "userHandle";

    // The set of shortcuts that are pending install
    private static final String APPS_PENDING_INSTALL = "apps_to_install";

    public static final int NEW_SHORTCUT_BOUNCE_DURATION = 450;
    public static final int NEW_SHORTCUT_STAGGER_DELAY = 85;

    private static final Object sLock = new Object();

    private static void addToInstallQueue(
            SharedPreferences sharedPrefs, PendingInstallShortcutInfo info) {
        synchronized (sLock) {
            String encoded = info.encodeToString();
            if (encoded != null) {
                Set<String> strings = sharedPrefs.getStringSet(APPS_PENDING_INSTALL, null);
                if (strings == null) {
                    strings = new HashSet<String>(1);
                } else {
                    strings = new HashSet<String>(strings);
                }
                strings.add(encoded);
                sharedPrefs.edit().putStringSet(APPS_PENDING_INSTALL, strings).commit();
            }
        }
    }

    public static void removeFromInstallQueue(Context context, ArrayList<String> packageNames,
                                              UserHandleCompat user) {
        if (packageNames.isEmpty()) {
            return;
        }
        String spKey = LauncherAppState.getSharedPreferencesKey();
        SharedPreferences sp = context.getSharedPreferences(spKey, Context.MODE_PRIVATE);
        synchronized (sLock) {
            Set<String> strings = sp.getStringSet(APPS_PENDING_INSTALL, null);
            if (DBG) {
                Log.d(TAG, "APPS_PENDING_INSTALL: " + strings
                        + ", removing packages: " + packageNames);
            }
            if (strings != null) {
                Set<String> newStrings = new HashSet<String>(strings);
                Iterator<String> newStringsIter = newStrings.iterator();
                while (newStringsIter.hasNext()) {
                    String encoded = newStringsIter.next();
                    PendingInstallShortcutInfo info = decode(encoded, context);
                    if (info == null || (packageNames.contains(info.getTargetPackage())
                            && user.equals(info.user))) {
                        newStringsIter.remove();
                    }
                }
                sp.edit().putStringSet(APPS_PENDING_INSTALL, newStrings).commit();
            }
        }
    }

    private static ArrayList<PendingInstallShortcutInfo> getAndClearInstallQueue(
            SharedPreferences sharedPrefs, Context context) {
        synchronized (sLock) {
            Set<String> strings = sharedPrefs.getStringSet(APPS_PENDING_INSTALL, null);
            if (DBG) Log.d(TAG, "Getting and clearing APPS_PENDING_INSTALL: " + strings);
            if (strings == null) {
                return new ArrayList<PendingInstallShortcutInfo>();
            }
            ArrayList<PendingInstallShortcutInfo> infos =
                    new ArrayList<PendingInstallShortcutInfo>();
            for (String encoded : strings) {
                PendingInstallShortcutInfo info = decode(encoded, context);
                if (info != null) {
                    infos.add(info);
                }
            }
            sharedPrefs.edit().putStringSet(APPS_PENDING_INSTALL, new HashSet<String>()).commit();
            return infos;
        }
    }

    // Determines whether to defer installing shortcuts immediately until
    // processAllPendingInstalls() is called.
    private static boolean mUseInstallQueue = false;

    public void onReceive(Context context, Intent data) {
        Log.d(TAG, "onReceive: " + data);
        if (!Partner.getBoolean(context, Partner.FEATURE_INSTALL_SHORTCUT_ENABLE)) {
            Log.d(TAG, "not enable install shortcut, return");
            return;
        }

        if (!ACTION_INSTALL_SHORTCUT.equals(data.getAction())) {
            return;
        }

        PendingInstallShortcutInfo info = new PendingInstallShortcutInfo(data, context);
        if (info.launchIntent == null || info.label == null) {
            if (DBG) Log.e(TAG, "Invalid install shortcut intent");
            return;
        }

        if (isInBlackList(context, info.getTargetPackage())) {
            return;
        }


        if (hasSameLabelWithLauncherActivity(info)) {
            return;
        }

        queuePendingShortcutInfo(info, context);
    }

    private static String[] mBlackNames;

    private boolean isInBlackList(Context context, String packageName) {
        if (!TextUtils.isEmpty(packageName)) {
            if (mBlackNames == null) {
                mBlackNames = Partner.getStringArray(context, Partner.DEF_SHORTCUT_BLACK_LIST);
            }

            if(mBlackNames!=null){
                for (String mBlackName : mBlackNames) {
                    if (packageName.equals(mBlackName)) {
                        return true;
                    }
                }
            }

            if(mNetworkBlackNames!=null){
                for (String mNetworkBlackName : mNetworkBlackNames) {
                    if (packageName.equals(mNetworkBlackName)) {
                        return true;
                    }
                }
            }

        }
        return true;
    }

    private static String[] mNetworkBlackNames;

    public static void updateNetworkShortcutBlacklist(Context context){
        if(Settings.isNetworkShortcutBlacklistEnabled(context)){
            mNetworkBlackNames = Settings.getNetworkShortcutBlacklist(context);
        }
    }

    public static ShortcutInfo fromShortcutIntent(Context context, Intent data) {
        PendingInstallShortcutInfo info = new PendingInstallShortcutInfo(data, context);
        if (info.launchIntent == null || info.label == null) {
            if (DBG) Log.e(TAG, "Invalid install shortcut intent");
            return null;
        }
        info = convertToLauncherActivityIfPossible(info);
        return info.getShortcutInfo();
    }

    static void queueInstallShortcut(LauncherActivityInfoCompat info, Context context) {
        queuePendingShortcutInfo(new PendingInstallShortcutInfo(info, context), context);
    }

    public static void queueShortcut(ShortcutInfoCompat info, Context context) {
        queuePendingShortcutInfo(new PendingInstallShortcutInfo(info, context), context);
    }

    public static HashSet<ShortcutKey> getPendingShortcuts(Context context) {
        HashSet<ShortcutKey> result = new HashSet<>();

        Set<String> strings = Utilities.getPrefs(context).getStringSet(APPS_PENDING_INSTALL, null);
        if (Utilities.isEmpty(strings)) {
            return result;
        }

        for (String encoded : strings) {
            try {
                Decoder decoder = new Decoder(encoded, context);
                if (decoder.optBoolean(DEEPSHORTCUT_TYPE_KEY)) {
                    result.add(ShortcutKey.fromIntent(decoder.launcherIntent, decoder.user));
                }
            } catch (JSONException | URISyntaxException e) {
                Log.d(TAG, "Exception reading shortcut to add: " + e);
            }
        }
        return result;
    }

    private static void queuePendingShortcutInfo(PendingInstallShortcutInfo info, Context context) {
        // Queue the item up for adding if launcher has not loaded properly yet
        LauncherAppState.setApplicationContext(context.getApplicationContext());
        LauncherAppState app = LauncherAppState.getInstance();
        boolean launcherNotLoaded = app.getModel().getCallback() == null;

        String spKey = LauncherAppState.getSharedPreferencesKey();
        SharedPreferences sp = context.getSharedPreferences(spKey, Context.MODE_PRIVATE);
        addToInstallQueue(sp, info);
        if (!mUseInstallQueue && !launcherNotLoaded) {
            flushInstallQueue(context);
        }
    }

    static void enableInstallQueue() {
        mUseInstallQueue = true;
    }

    static void disableAndFlushInstallQueue(Context context) {
        mUseInstallQueue = false;
        flushInstallQueue(context);
    }

    static void flushInstallQueue(Context context) {
        String spKey = LauncherAppState.getSharedPreferencesKey();
        SharedPreferences sp = context.getSharedPreferences(spKey, Context.MODE_PRIVATE);
        ArrayList<PendingInstallShortcutInfo> installQueue = getAndClearInstallQueue(sp, context);
        if (!installQueue.isEmpty()) {
            Iterator<PendingInstallShortcutInfo> iter = installQueue.iterator();
            ArrayList<ItemInfo> addShortcuts = new ArrayList<ItemInfo>();
            while (iter.hasNext()) {
                final PendingInstallShortcutInfo pendingInfo = iter.next();
                final Intent intent = pendingInfo.launchIntent;

                // If the intent specifies a package, make sure the package exists
                String packageName = pendingInfo.getTargetPackage();
                if (!TextUtils.isEmpty(packageName)) {
                    UserHandleCompat myUserHandle = UserHandleCompat.myUserHandle();
                    if (!LauncherModel.isValidPackage(context, packageName, myUserHandle)) {
                        if (DBG) Log.d(TAG, "Ignoring shortcut for absent package:" + intent);
                        continue;
                    }
                }

                // Generate a shortcut info to add into the model
                addShortcuts.add(pendingInfo.getShortcutInfo());
            }

            // Add the new apps to the model and bind them
            if (!addShortcuts.isEmpty()) {
                LauncherAppState app = LauncherAppState.getInstance();
                app.getModel().addAndBindAddedWorkspaceItems(context, addShortcuts);
            }
        }
    }

    /**
     * Ensures that we have a valid, non-null name.  If the provided name is null, we will return
     * the application name instead.
     */
    @Thunk
    static CharSequence ensureValidName(Context context, Intent intent, CharSequence name) {
        if (name == null) {
            try {
                PackageManager pm = context.getPackageManager();
                ActivityInfo info = pm.getActivityInfo(intent.getComponent(), 0);
                name = info.loadLabel(pm);
            } catch (PackageManager.NameNotFoundException nnfe) {
                return "";
            }
        }
        return name;
    }

    private static class PendingInstallShortcutInfo {

        final LauncherActivityInfoCompat activityInfo;
        final ShortcutInfoCompat shortcutInfo;

        final Intent data;
        final Context mContext;
        final Intent launchIntent;
        final String label;
        final UserHandleCompat user;


        /**
         * Initializes a PendingInstallShortcutInfo received from a different app.
         */
        public PendingInstallShortcutInfo(Intent data, Context context) {
            this.data = data;
            mContext = context;

            launchIntent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
            label = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
            user = UserHandleCompat.myUserHandle();
            activityInfo = null;
            shortcutInfo = null;
        }

        /**
         * Initializes a PendingInstallShortcutInfo to represent a launcher target.
         */
        public PendingInstallShortcutInfo(LauncherActivityInfoCompat info, Context context) {
            this.data = null;
            mContext = context;
            activityInfo = info;
            user = info.getUser();

            launchIntent = AppInfo.makeLaunchIntent(context, info, user);
            label = info.getLabel().toString();
            shortcutInfo = null;
        }

        /**
         * Initializes a PendingInstallShortcutInfo to represent a launcher target.
         */
        public PendingInstallShortcutInfo(ShortcutInfoCompat info, Context context) {
            activityInfo = null;
            shortcutInfo = info;

            data = null;
            mContext = context;
            user = UserHandleCompat.fromUser(info.getUserHandle());

            launchIntent = info.makeIntent();
            label = info.getShortLabel().toString();
        }

        public String encodeToString() {
            try {
                if (activityInfo != null) {
                    // If it a launcher target, we only need component name, and user to
                    // recreate this.
                    return new JSONStringer()
                        .object()
                        .key(LAUNCH_INTENT_KEY).value(launchIntent.toUri(0))
                        .key(APP_SHORTCUT_TYPE_KEY).value(true)
                        .key(USER_HANDLE_KEY).value(UserManagerCompat.getInstance(mContext)
                                .getSerialNumberForUser(user))
                        .endObject().toString();
                } else if (shortcutInfo != null) {
                    // If it a launcher target, we only need component name, and user to
                    // recreate this.
                    return new JSONStringer()
                            .object()
                            .key(LAUNCH_INTENT_KEY).value(launchIntent.toUri(0))
                            .key(DEEPSHORTCUT_TYPE_KEY).value(true)
                            .key(USER_HANDLE_KEY).value(UserManagerCompat.getInstance(mContext)
                                    .getSerialNumberForUser(user))
                            .endObject().toString();
                }

                if (launchIntent.getAction() == null) {
                    launchIntent.setAction(Intent.ACTION_VIEW);
                } else if (launchIntent.getAction().equals(Intent.ACTION_MAIN) &&
                        launchIntent.getCategories() != null &&
                        launchIntent.getCategories().contains(Intent.CATEGORY_LAUNCHER)) {
                    launchIntent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                }

                // This name is only used for comparisons and notifications, so fall back to activity
                // name if not supplied
                String name = ensureValidName(mContext, launchIntent, label).toString();
                Bitmap icon = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
                Intent.ShortcutIconResource iconResource =
                    data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);

                // Only encode the parameters which are supported by the API.
                JSONStringer json = new JSONStringer()
                    .object()
                    .key(LAUNCH_INTENT_KEY).value(launchIntent.toUri(0))
                    .key(NAME_KEY).value(name);
                if (icon != null) {
                    byte[] iconByteArray = Utilities.flattenBitmap(icon);
                    json = json.key(ICON_KEY).value(
                            Base64.encodeToString(
                                    iconByteArray, 0, iconByteArray.length, Base64.DEFAULT));
                }
                if (iconResource != null) {
                    json = json.key(ICON_RESOURCE_NAME_KEY).value(iconResource.resourceName);
                    json = json.key(ICON_RESOURCE_PACKAGE_NAME_KEY)
                            .value(iconResource.packageName);
                }
                return json.endObject().toString();
            } catch (JSONException e) {
                Log.d(TAG, "Exception when adding shortcut: " + e);
                return null;
            }
        }

        public ShortcutInfo getShortcutInfo() {
            ShortcutInfo info;
            if (activityInfo != null) {
                info = ShortcutInfo.fromActivityInfo(activityInfo, mContext);
                info.itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
            } else if (shortcutInfo != null) {
                info = new ShortcutInfo(shortcutInfo, mContext);
                info.setIcon(LauncherIcons.createShortcutIcon(shortcutInfo, mContext));
            } else {
                info = LauncherAppState.getInstance().getModel().infoFromShortcutIntent(mContext, data);
                info.itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
            }
            return info;
        }

        public String getTargetPackage() {
            String packageName = launchIntent.getPackage();
            if (packageName == null) {
                packageName = launchIntent.getComponent() == null ? null :
                        launchIntent.getComponent().getPackageName();
            }
            return packageName;
        }

        public boolean isLauncherActivity() {
            return activityInfo != null;
        }
    }

    private static PendingInstallShortcutInfo decode(String encoded, Context context) {
        try {
            JSONObject object = (JSONObject) new JSONTokener(encoded).nextValue();
            Intent launcherIntent = Intent.parseUri(object.getString(LAUNCH_INTENT_KEY), 0);

            if (object.optBoolean(APP_SHORTCUT_TYPE_KEY)) {
                // The is an internal launcher target shortcut.
                UserHandleCompat user = UserManagerCompat.getInstance(context)
                        .getUserForSerialNumber(object.getLong(USER_HANDLE_KEY));
                if (user == null) {
                    return null;
                }

                LauncherActivityInfoCompat info = LauncherAppsCompat.getInstance(context)
                        .resolveActivity(launcherIntent, user);
                return info == null ? null : new PendingInstallShortcutInfo(info, context);
            } else if (object.optBoolean(DEEPSHORTCUT_TYPE_KEY)) {
                UserHandleCompat user = UserManagerCompat.getInstance(context)
                        .getUserForSerialNumber(object.getLong(USER_HANDLE_KEY));
                if (user == null) {
                    return null;
                }

                DeepShortcutManager sm = DeepShortcutManager.getInstance(context);
                List<ShortcutInfoCompat> si = sm.queryForFullDetails(
                        launcherIntent.getPackage(),
                        Arrays.asList(launcherIntent.getStringExtra(
                                ShortcutInfoCompat.EXTRA_SHORTCUT_ID)),
                                user.getUser());
                if (si.isEmpty()) {
                    return null;
                } else {
                    return new PendingInstallShortcutInfo(si.get(0), context);
                }
            }

            Intent data = new Intent();
            data.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launcherIntent);
            data.putExtra(Intent.EXTRA_SHORTCUT_NAME, object.getString(NAME_KEY));

            String iconBase64 = object.optString(ICON_KEY);
            String iconResourceName = object.optString(ICON_RESOURCE_NAME_KEY);
            String iconResourcePackageName = object.optString(ICON_RESOURCE_PACKAGE_NAME_KEY);
            if (iconBase64 != null && !iconBase64.isEmpty()) {
                byte[] iconArray = Base64.decode(iconBase64, Base64.DEFAULT);
                Bitmap b = BitmapFactory.decodeByteArray(iconArray, 0, iconArray.length);
                data.putExtra(Intent.EXTRA_SHORTCUT_ICON, b);
            } else if (iconResourceName != null && !iconResourceName.isEmpty()) {
                Intent.ShortcutIconResource iconResource =
                        new Intent.ShortcutIconResource();
                iconResource.resourceName = iconResourceName;
                iconResource.packageName = iconResourcePackageName;
                data.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
            }

            return new PendingInstallShortcutInfo(data, context);
        } catch (JSONException e) {
            Log.d(TAG, "Exception reading shortcut to add: " + e);
        } catch (URISyntaxException e) {
            Log.d(TAG, "Exception reading shortcut to add: " + e);
        }
        return null;
    }

    private static class Decoder extends JSONObject {
        public final Intent launcherIntent;
        public final UserHandle user;

        private Decoder(String encoded, Context context) throws JSONException, URISyntaxException {
            super(encoded);
            launcherIntent = Intent.parseUri(getString(LAUNCH_INTENT_KEY), 0);
            user = has(USER_HANDLE_KEY) ? UserManagerCompat.getInstance(context)
                    .getUserForSerialNumber(getLong(USER_HANDLE_KEY)).getUser()
                    : Process.myUserHandle();
            if (user == null) {
                throw new JSONException("Invalid user");
            }
        }
    }

    /**
     * Tries to create a new PendingInstallShortcutInfo which represents the same target,
     * but is an app target and not a shortcut.
     *
     * @return the newly created info or the original one.
     */
    private static PendingInstallShortcutInfo convertToLauncherActivityIfPossible(
            PendingInstallShortcutInfo original) {
        if (original.isLauncherActivity()) {
            // Already an activity target
            return original;
        }

        if (!Utilities.isLauncherAppTarget(original.launchIntent)
                || !original.user.equals(UserHandleCompat.myUserHandle())) {
            // We can only convert shortcuts which point to a main activity in the current user.
            return original;
        }

        PackageManager pm = original.mContext.getPackageManager();
        ResolveInfo info = pm.resolveActivity(original.launchIntent, 0);

        if (info == null) {
            return original;
        }

        // Ignore any conflicts in the label name, as that can change based on locale.
        LauncherActivityInfoCompat launcherInfo = LauncherActivityInfoCompat
                .fromResolveInfo(info, original.mContext);
        return new PendingInstallShortcutInfo(launcherInfo, original.mContext);
    }


    private static boolean hasSameLabelWithLauncherActivity(PendingInstallShortcutInfo original) {
        if (original.launchIntent.getComponent() != null) {
            Context context = original.mContext;
            ComponentName componentName = original.launchIntent.getComponent();
            UserHandleCompat user = UserHandleCompat.myUserHandle();
            final List<LauncherActivityInfoCompat> apps = LauncherAppsCompat.getInstance(context).getActivityList(componentName.getPackageName(), user);
            for (LauncherActivityInfoCompat info : apps) {
                if (info.getComponentName().equals(componentName) && info.getLabel().equals(original.label)) {
                    return true;
                }
            }
        }

        return false;
    }
}
