
/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.amz.ios.launcher.compat;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArrayMap;

import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.util.LongArrayMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserManagerCompatVL extends UserManagerCompat {
    private static final String USER_CREATION_TIME_KEY = "user_creation_time_";

    protected final UserManager mUserManager;
    private final PackageManager mPm;
    private final Context mContext;

    protected LongArrayMap<UserHandleCompat> mUsers;
    // Create a separate reverse map as LongArrayMap.indexOfValue checks if objects are same
    // and not {@link Object#equals}
    protected ArrayMap<UserHandleCompat, Long> mUserToSerialMap;

    UserManagerCompatVL(Context context) {
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mPm = context.getPackageManager();
        mContext = context;
    }

    @Override
    public long getSerialNumberForUser(UserHandleCompat user) {
        synchronized (this) {
            if (mUserToSerialMap != null) {
                Long serial = mUserToSerialMap.get(user);
                return serial == null ? 0 : serial;
            }
        }
        return mUserManager.getSerialNumberForUser(user.getUser());
    }

    @Override
    public UserHandleCompat getUserForSerialNumber(long serialNumber) {
        synchronized (this) {
            if (mUsers != null) {
                return mUsers.get(serialNumber);
            }
        }
        return UserHandleCompat.fromUser(mUserManager.getUserForSerialNumber(serialNumber));
    }

    @Override
    public boolean isQuietModeEnabled(UserHandleCompat user) {
        return false;
    }

    @Override
    public boolean isUserUnlocked(UserHandleCompat user) {
        return true;
    }

    @Override
    public boolean isDemoUser() {
        return false;
    }

    @Override
    public void enableAndResetCache() {
        synchronized (this) {
            mUsers = new LongArrayMap<>();
            mUserToSerialMap = new ArrayMap<>();
            List<UserHandle> users = mUserManager.getUserProfiles();
            if (users != null) {
                for (UserHandle user : users) {
                    long serial = mUserManager.getSerialNumberForUser(user);
                    mUsers.put(serial, UserHandleCompat.fromUser(user));
                    mUserToSerialMap.put(UserHandleCompat.fromUser(user), serial);
                }
            }
        }
    }

    @Override
    public List<UserHandleCompat> getUserProfiles() {
        synchronized (this) {
            if (mUsers != null) {
                List<UserHandleCompat> users = new ArrayList<>();
                users.addAll(mUserToSerialMap.keySet());
                return users;
            }
        }

        List<UserHandle> users = mUserManager.getUserProfiles();
        if (users == null) {
            return Collections.emptyList();
        }
        ArrayList<UserHandleCompat> compatUsers = new ArrayList<UserHandleCompat>(
                users.size());
        for (UserHandle user : users) {
            compatUsers.add(UserHandleCompat.fromUser(user));
        }
        return compatUsers;
    }

    @Override
    public CharSequence getBadgedLabelForUser(CharSequence label, UserHandleCompat user) {
        if (user == null) {
            return label;
        }
        return mPm.getUserBadgedLabel(label, user.getUser());
    }

    @Override
    public long getUserCreationTime(UserHandleCompat user) {
        SharedPreferences prefs = mContext.getSharedPreferences(
                LauncherAppState.getSharedPreferencesKey(), Context.MODE_PRIVATE);
        String key = USER_CREATION_TIME_KEY + getSerialNumberForUser(user);
        if (!prefs.contains(key)) {
            prefs.edit().putLong(key, System.currentTimeMillis()).apply();
        }
        return prefs.getLong(key, 0);
    }
}

