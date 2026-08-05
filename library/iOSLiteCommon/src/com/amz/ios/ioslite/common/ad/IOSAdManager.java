package com.amz.ios.ioslite.common.ad;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.Nullable;
import android.util.Log;

import com.amz.ios.ioslite.common.util.BuildUtil;

import java.lang.reflect.Constructor;

public abstract class IOSAdManager {
    protected static final boolean DEBUG = true;
    private static final String TAG = "AdManager";

    private static final String CLASS_DROID_AD_MANAGE = "com.ios.ioslite.ad.droi.DroiAdManager";
    private static final String CLASS_DU_AD_MANAGE = "com.ios.ioslite.ad.du.DuAdManager";
    private static final String CLASS_TW_AD_MANAGE = "com.ios.ioslite.ad.tw.TwAdManager";

    private static IOSAdManager sInstance;
    private static AdDisplayHelper sDisplayHelper;
    private static Object sInstanceLock = new Object();

    public static IOSAdManager getInstance(Context context) {
        synchronized (sInstanceLock) {
            if (sInstance == null) {
                Context appContext = context.getApplicationContext();
                Class clazz;
                Constructor ctor;
                try {
                    if (BuildUtil.isCNBuild()) {
                        clazz = Class.forName(CLASS_DROID_AD_MANAGE);
                    } else {
                        clazz = Class.forName(CLASS_TW_AD_MANAGE);
                    }
                    ctor = clazz.getConstructor(Context.class);
                    sInstance = (IOSAdManager) ctor.newInstance(appContext);
                } catch (Exception e) {
                    Log.e(TAG, "getUpdateClient fail ", e);
                }

                if (sInstance == null) {
                    if (DEBUG) Log.d(TAG, "getInstance : return default ad manager");
                    sInstance = new DefaultAdManager(appContext);
                }
            }
            return sInstance;
        }
    }

    /**
     * Tiêm implementation trực tiếp từ {@code :app} (đường gắn SDK thật, đơn giản
     * hơn cơ chế reflection ở {@link #getInstance(Context)}). Gọi sớm trong
     * Application.onCreate() trước mọi lần dùng ad.
     */
    public static void setInstance(IOSAdManager impl) {
        synchronized (sInstanceLock) {
            sInstance = impl;
        }
    }

    public static void initalize(Context context) {
        getInstance(context).init();
    }


    public abstract void init();

    /**
     * 原生广告
     */
    @Nullable
    public abstract IOSNativeAd getNativeAd(int id);

    /**
     * 应用推荐类广告;
     *
     * @return 返回可为空
     */
    @Nullable
    public IOSAppAd getAppAd(int id) {
        return null;
    }

    /**
     * 广告列表
     */
    @Nullable
    public abstract IOSListAd getListAd(int id);

    /**
     * Quảng cáo toàn màn (interstitial / app-open). Default trả null (no-op) —
     * lớp con ở {@code :app} override để cấp ad thật.
     */
    @Nullable
    public IOSFullScreenAd getFullScreenAd(int id) {
        return null;
    }

    /**
     * Hiển thị interstitial cho ad-id đã cho nếu sẵn sàng và policy cho phép.
     * An toàn khi chưa có SDK (getFullScreenAd trả null -> bỏ qua).
     */
    public static void showInterstitial(Activity activity, int id) {
        showFullScreen(activity, id);
    }

    /**
     * Hiển thị app-open ad cho ad-id đã cho nếu sẵn sàng và policy cho phép.
     */
    public static void showAppOpen(Activity activity, int id) {
        showFullScreen(activity, id);
    }

    private static void showFullScreen(Activity activity, int id) {
        if (activity == null) {
            return;
        }
        if (!shouldShowAd(id)) {
            return;
        }
        IOSFullScreenAd ad = getInstance(activity).getFullScreenAd(id);
        if (ad != null && ad.isReady()) {
            ad.show(activity);
            afterShowAd(id);
        }
    }

    public static void setDiaplayHelper(AdDisplayHelper helper) {
        sDisplayHelper = helper;
    }

    public static boolean shouldShowAd(int position) {
        if (sDisplayHelper != null) {
            return sDisplayHelper.shouldShowAd(position);
        }
        return true;
    }


    public static void afterShowAd(int position) {
        if (sDisplayHelper != null) {
            sDisplayHelper.afterShowAd(position);
        }
    }

}
