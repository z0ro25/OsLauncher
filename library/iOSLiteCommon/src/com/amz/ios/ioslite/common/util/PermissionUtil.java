package com.amz.ios.ioslite.common.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;
import android.util.Log;
import android.util.SparseArray;

public final class PermissionUtil {
    public static final String TAG = "PermissionUtil";

    private static final SparseArray<PermissionsRequestCallback> sCallbacks = new SparseArray<PermissionsRequestCallback>();

    /**
     * Returns true if the Activity or Fragment has access to all given
     * permissions.
     */
    public static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (PermissionChecker.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    public static void requestPermissions(final @NonNull Activity activity,
                                          final @NonNull String[] permissions, final int requestCode) {

        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    public static void checkSelfPermissions(Activity activity, String... permissions) {
        if (!hasPermissions(activity, permissions)) {
            ActivityCompat.requestPermissions(activity, permissions, 0);
        }
    }

    public static void checkSelfPermissions(Activity activity, int requestCode, PermissionsRequestCallback callback,
                                            int groupId, String... permissions) {
        if (hasPermissions(activity, permissions)) {
            if (callback != null) {
                callback.onPermissionAllowed();
            }
        } else {
            if (callback != null) {
                sCallbacks.put(requestCode, callback);
            }
            ActivityCompat.requestPermissions(activity, permissions, requestCode);
        }
    }

    /**
     * Process results of request permissions from Activity
     */
    public static void onRequestPermissionsResult(Activity activity, int requestCode, int[] grantResults) {
        PermissionsRequestCallback callback = sCallbacks.get(requestCode);
        Log.i(TAG, "onRequestPermissionsResult callback = " + callback + ",requestCode = " + requestCode);

        if (callback == null) {
            Log.e(TAG, "onRequestPermissionsResult callback is null.");
            return;
        } else {
            sCallbacks.remove(requestCode);
        }

        if (BuildUtil.getTargetSdkVersion(activity) < 23
                && !PermissionUtil.hasPermissions(activity, callback.onGetPermissions())) {
            callback.onPermissionDenied();
            return;
        }

        if (PermissionUtil.verifyPermissions(grantResults)) {
            callback.onPermissionAllowed();
        } else {
            callback.onPermissionDenied();
        }
    }

    private static boolean verifyPermissions(int... grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    public static class PermissionsRequestCallBackAdapter implements PermissionsRequestCallback {

        @Override
        public void onPermissionAllowed() {
        }

        @Override
        public void onPermissionDenied() {
        }


        @Override
        public String[] onGetPermissions() {
            return new String[0];
        }
    }

    public interface PermissionsRequestCallback {
        public void onPermissionAllowed();

        public void onPermissionDenied();

        public String[] onGetPermissions();
    }


}
