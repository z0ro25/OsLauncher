package com.amz.ios.launcher.compat;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.N)
public class UserManagerCompatVN extends UserManagerCompatVM {

    UserManagerCompatVN(Context context) {
        super(context);
    }

    @Override
    public boolean isQuietModeEnabled(UserHandleCompat user) {
        if (user != null) {
            try {
                return mUserManager.isQuietModeEnabled(user.getUser());
            } catch (IllegalArgumentException e) {
                // TODO remove this when API is fixed to not throw this
                // when called on user that isn't a managed profile.
            }
        }
        return false;
    }

    @Override
    public boolean isUserUnlocked(UserHandleCompat user) {
        return mUserManager.isUserUnlocked(user.getUser());
    }
}

