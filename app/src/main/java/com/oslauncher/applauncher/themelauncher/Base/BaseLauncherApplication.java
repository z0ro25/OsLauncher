package com.oslauncher.applauncher.themelauncher.Base;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.amz.ios.ioslite.common.CommonSdk;
import com.amz.ios.ioslite.common.LiteAction;
import com.amz.ios.ioslite.common.analytics.AnalyticsDelegate;
import com.amz.ios.ioslite.common.debug.DebugUtil;
import com.amz.ios.ioslite.common.debug.ExceptionHandler;
import com.amz.ios.ioslite.common.util.ProcessUtil;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.themeclub.ThemeClubApplication;

public class BaseLauncherApplication extends Application {
    private static final String TAG = "BaseLauncherApplication";
    private static final String KEY_EX_CATCHER = "ios_ex_catcher";
    protected Application mApplication;
    //private WeatherApplication mWeatherApplication;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            // Launcher首次加载完成桌面后通知
            if (LiteAction.ACTION_LAUNCHER_LOAD_COMPLETE.equals(action)) {
                initalizeAfterLauncherLoadCompelte();
            }
        }
    };

    public void onCreate() {
        super.onCreate();

        // Common 模块全局Context注入
        CommonSdk.initalize(this);
        mApplication = this;

        DebugUtil.debugLaunch(TAG, "onCreate");
        String processName = ProcessUtil.getProcessNameFromId(this, android.os.Process.myPid());
        if (processName != null && processName.equals(getPackageName())) {
            DebugUtil.debugLaunch(TAG, "main process init start");
            // 应用异常捕获，日志记录； 默认只在正式版本中开启；
            if (enableExCatcher()) {
                ExceptionHandler.initalize(this);
            }

            //Droi Core 需要在使用模块之前（天气，负一屏，DroiAnalytics）初始化,且设置渠道号必须在initialize之前进行
//            Core.setChannelName(DeviceInfoUtil.getChannel(this));
//            Core.initialize(this);

            // 桌面初始化；
            LauncherAppState.initalize(this);

            // 天气初始化
//            mWeatherApplication = new WeatherApplication();
//            mWeatherApplication.initalize(this);

            // 美化中心初始化
            ThemeClubApplication.initalize(this);

            // 定位服务初始化
           // IOSLocationManager.initalize(this);

            //负一平初始化
//            ThirdPartAdManager.initNewsSDKAndAd(this);

            //IOSSwitchControl
            //NetworkManager.registerNetworkChangeReceiver(this);

            // 注册广播监听
//            registReveiver();

            // Khởi tạo môi trường quảng cáo (broadcast ACTION_LAUNCHER_LOAD_COMPLETE
            // đang tắt nên gọi thẳng ở đây thay vì chờ initalizeAfterLauncherLoadCompelte).
            setupAdEnvironment();

            DebugUtil.debugLaunch(TAG, "main process init complete");
        }
    }

    /**
     * 桌面首次加载完成后，模块初始化
     */
    private void initalizeAfterLauncherLoadCompelte() {
        // 版本检查更新初始化；
//        VersionUpdateManager.initalize(this);

        // 数据统计初始化
        AnalyticsDelegate.initalize(this);

        // 桌面加载完成后， 其他服务初始化；
        // 初始化广告相关
        setupAdEnvironment();
    }


    private void registReveiver() {
        IntentFilter filter = new IntentFilter();
        //  桌面加载完成通知
        filter.addAction(LiteAction.ACTION_LAUNCHER_LOAD_COMPLETE);
        registerReceiver(mReceiver, filter,RECEIVER_EXPORTED);
    }

    private void unRegistReveiver() {
        unregisterReceiver(mReceiver);
    }


    /**
     * 广告环境建立
     */
    private void setupAdEnvironment() {
        // Registry ad đã được khởi tạo ở CommonSdk.initalize() (cả 2 process).
        // Đây là điểm gắn SDK ad thật sau này — chỉ cần tiêm 1 lớp con IOSAdManager
        // (native + fullscreen) là mọi slot/điểm hiện có tự hoạt động:
        //
        //     IOSAdManager.setInstance(new AdMobAdManager(mApplication));
        //     IOSAdManager.getInstance(mApplication).init();
        //
        // Có thể set thêm policy hiển thị:
        //     IOSAdManager.getInstance(mApplication).setDiaplayHelper(adController);
        //
        // Hiện chưa có SDK -> giữ DefaultAdManager no-op, không hiển thị gì.
    }


    @Override
    public void onTerminate() {
        super.onTerminate();
        unRegistReveiver();
        // mWeatherApplication.onTerminate(this);
        //IOSLocationManager.onTerminate();
        ThemeClubApplication.release(this);
        //NetworkManager.unRegisterNetworkChangeReceiver(this);
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
//        MultiDex.install(this);
    }

    /**
     * 是否开启主线程异常捕获
     */
    private static boolean enableExCatcher() {
        return !DebugUtil.isPropertyEnabled(KEY_EX_CATCHER);
    }


}
