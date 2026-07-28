package com.amz.ios.ioslite.common.update;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.amz.ios.ioslite.common.R;
import com.amz.ios.ioslite.common.util.BuildUtil;
import com.amz.ios.ioslite.common.util.IOSToastUtil;
import com.amz.ios.ioslite.common.util.NetworkStateUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;


public class VersionUpdateManager implements BaseUpdateClient.Callback {
    private static final String TAG = "VersionUpdateManager";

    private static Application sAppContext;
    private static VersionUpdateManager sInstance;
    private static BaseUpdateClient sIOSVersionUpdate;
    private static boolean mNewVersionChecked;
    private static boolean mNewVersionPrompted;
    private static boolean mSilentCheck;


    public static void initalize(Application application) {
        sAppContext = application;
        if (sInstance == null) {
            sInstance = new VersionUpdateManager();
            initalizeClient(application);
            sIOSVersionUpdate = getUpdateClient(application);
            sIOSVersionUpdate.registerCallback(sInstance);
        }
    }

    private VersionUpdateManager() {
    }


    public static void checkUpdate(boolean force, boolean silent) {
        if (sInstance == null) {
            return;
        }

        mSilentCheck = silent;
        if (!NetworkStateUtil.isNetworkConnected(sAppContext)) {
            if (!mSilentCheck)
                IOSToastUtil.showToast(sAppContext, sAppContext.getString(R.string.network_error_tip), 0, Toast.LENGTH_LONG);
            return;
        }
        if (!mSilentCheck) {
            IOSToastUtil.showToast(sAppContext, sAppContext.getString(R.string.update_start), R.drawable.ic_version_update, Toast.LENGTH_LONG);
        }
        sIOSVersionUpdate.checkUpdate(force);
    }

    public static void updateApp() {
        if (!NetworkStateUtil.isNetworkConnected(sAppContext)) {
            IOSToastUtil.showToast(sAppContext, sAppContext.getString(R.string.network_error_tip), 0, Toast.LENGTH_LONG);
            return;
        }
        sIOSVersionUpdate.updateApp();
    }

    public void onNewVersionChecked(boolean newChecked) {
        if (newChecked) {
            mNewVersionChecked = true;
            if (!mSilentCheck)
                IOSToastUtil.showToast(sAppContext, sAppContext.getString(R.string.update_to_latest_version), R.drawable.ic_version_update, Toast.LENGTH_LONG);
        } else {
            if (!mSilentCheck)
                IOSToastUtil.showToast(sAppContext, sAppContext.getString(R.string.update_check_no_update), R.drawable.ic_version_update, Toast.LENGTH_LONG);
        }
    }

    public static void registerCallback(BaseUpdateClient.Callback callback) {
        if (sIOSVersionUpdate != null) {
            sIOSVersionUpdate.registerCallback(callback);
        }
    }

    public static void unregisterCallback(BaseUpdateClient.Callback callback) {
        if (sIOSVersionUpdate != null) {
            sIOSVersionUpdate.unregisterCallback(callback);
        }
    }

    public static boolean hasNewVersionChecked() {
        return mNewVersionChecked;
    }

    public static void setNewVersionPromptedFlag() {
        mNewVersionPrompted = true;
    }

    public static boolean hasNewVersionPrompted() {
        return mNewVersionPrompted;
    }


    /**
     * Init update client with application;
     * Droid update sdk need this; so wrap it in base class;
     * <p/>
     * {@link BaseUpdateClient#init(Application)}
     */
    private static void initalizeClient(Application application) {
        Class clazz;
        Method method;
        try {
            if (BuildUtil.BUILD_MODE == BuildUtil.Mode.CUSTOMER_CN
                    || BuildUtil.BUILD_MODE == BuildUtil.Mode.CUSTOMER_HW) {
                clazz = Class.forName("com.ios.ioslite.update.DroiUpdateClient");
                method = clazz.getMethod("init", Application.class);
                method.invoke(null, application);
            }
        } catch (Exception e) {
            Log.e(TAG, "VersionUpdateManager init fail ", e);
        }

        getUpdateClient(application);
    }

    private static BaseUpdateClient getUpdateClient(Context context) {
        Class clazz;
        Constructor ctor;
        try {
            if (BuildUtil.BUILD_MODE == BuildUtil.Mode.PUBLIC_HW) {
                clazz = Class.forName("com.ios.ioslite.update.GooglePlayUpdate");
                ctor = clazz.getConstructor(Context.class);
                return (BaseUpdateClient) ctor.newInstance(context);
            } else if (BuildUtil.BUILD_MODE == BuildUtil.Mode.CUSTOMER_CN
                    || BuildUtil.BUILD_MODE == BuildUtil.Mode.CUSTOMER_HW) {
                clazz = Class.forName("com.ios.ioslite.update.DroiUpdateClient");
                ctor = clazz.getConstructor(Context.class);
                return (BaseUpdateClient) ctor.newInstance(context);
            }
        } catch (Exception e) {
            Log.e(TAG, "getUpdateClient fail ", e);
        }

        // Return default implemention if none match, to avoid crash exception;
        return new DefaultUpdateClient(context);
    }
}
