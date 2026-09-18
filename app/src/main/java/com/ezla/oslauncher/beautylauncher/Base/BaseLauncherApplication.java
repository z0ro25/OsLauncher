package com.ezla.oslauncher.beautylauncher.Base;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;

import com.ezla.oslauncher.beautylauncher.BuildConfig;
import com.ezla.oslauncher.beautylauncher.tool.languageTool.LanguageUtil;
import com.ezla.oslauncher.beautylauncher.utils.RemoteConfigUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.truongnt.ios.ioslite.common.CommonSdk;
import com.truongnt.ios.ioslite.common.LiteAction;
import com.truongnt.ios.ioslite.common.ads.Ads;
import com.truongnt.ios.ioslite.common.ads.AdsOpen;
import com.truongnt.ios.ioslite.common.ads.AdsSlot;
import com.truongnt.ios.ioslite.common.analytics.AnalyticsDelegate;
import com.truongnt.ios.ioslite.common.debug.DebugUtil;
import com.truongnt.ios.ioslite.common.debug.ExceptionHandler;
import com.truongnt.ios.ioslite.common.noti.DailyNotiScheduler;
import com.truongnt.ios.ioslite.common.util.ProcessUtil;
import com.truongnt.ios.launcher.LauncherAppState;
import com.truongnt.ios.themeclub.ThemeClubApplication;

import java.util.Locale;

public class BaseLauncherApplication extends Application {
    private static final String TAG = "BaseLauncherApplication";
    private static final String KEY_EX_CATCHER = "ios_ex_catcher";

    // Key Remote Config cho notification hằng ngày — khai báo giá trị mặc định ở
    // res/xml/remote_config_defaults.xml, đọc ở setupDailyNotification().
    private static final String KEY_DAILY_NOTI_ENABLED = "daily_noti_enabled";
    private static final String KEY_DAILY_NOTI_TIME = "daily_noti_time";
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

            // Notification hằng ngày — mốc giờ lấy từ Remote Config.
            setupDailyNotification();

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
        registerReceiver(mReceiver, filter, RECEIVER_EXPORTED);
    }

    private void unRegistReveiver() {
        unregisterReceiver(mReceiver);
    }


    /**
     * Khởi tạo hạ tầng quảng cáo.
     *
     * <p>Được gọi từ onCreate() của process chính, và cả từ initalizeAfterLauncherLoadCompelte().
     * Ads.init() tự chặn lần gọi thứ hai nên gọi bao nhiêu lần cũng an toàn.
     */
    private void setupAdEnvironment() {
        // Token Adjust — ĐIỀN VÀO ĐÂY khi có. Để rỗng thì Ads.init() bỏ qua luôn phần
        // Adjust (chỉ log cảnh báo); hạ tầng quảng cáo vẫn chạy bình thường, chỉ mất
        // phần revenue tracking qua Adjust.
        final String adjustToken = "seu0i6ptzxmo";

        // isSubs/tier1 hiện để mặc định false vì dự án CHƯA có code billing nào (đã kiểm:
        // không có BillingClient/queryPurchases ở đâu cả; billing:8.0.0 mới chỉ khai báo).
        // Khi có IAP thật thì gọi Ads.setPremium(isSubs, isTier1) ở chỗ biết trạng thái mua.
        Ads.init(this, adjustToken, BuildConfig.DEBUG, Ads.DEFAULT_INTER_DELAY_MS, new Runnable() {
            @Override
            public void run() {
                // App-open KHÔNG có hàm show(): FSDAds tự đăng ký ActivityLifecycleCallbacks
                // và tự hiện khi Activity được tạo. Xem AdsOpen.
//                AdsOpen.setup(mApplication, AdsSlot.APP_OPEN, null);
            }
        });
    }


    /**
     * Đẩy cấu hình notification hằng ngày từ Remote Config xuống iOSLiteCommon.
     *
     * <p>Vì sao tách ở đây: iOSLiteCommon cố ý KHÔNG phụ thuộc Firebase (chỉ :app có
     * firebase-config). Module common chỉ giữ phần hẹn giờ + bắn noti, còn đọc Remote Config
     * thì :app đọc rồi đẩy xuống qua DailyNotiScheduler.applyRemoteConfig(). Nhờ vậy đổi mốc
     * giờ trên console có hiệu lực ở lần mở app kế tiếp, không cần build lại.
     *
     * <p>Lưu ý: initRemoteConfig trước đây không được gọi ở đâu — Remote Config chưa từng được
     * fetch. Đây là chỗ gọi đầu tiên, nên nó cũng là điểm duy nhất kích hoạt fetch.
     */
    private void setupDailyNotification() {
        RemoteConfigUtils.INSTANCE.initRemoteConfig(new OnCompleteListener() {
            @Override
            public void onComplete(Task task) {
                boolean enabled = false;
                String time = null;
                try {
                    enabled = RemoteConfigUtils.INSTANCE.getRemoteConfigBoolean(mApplication,KEY_DAILY_NOTI_ENABLED);
                    time = RemoteConfigUtils.INSTANCE.getRemoteConfigString(mApplication,KEY_DAILY_NOTI_TIME);
                } catch (Exception e) {

                    DebugUtil.debugLaunch(TAG, "đọc Remote Config notification lỗi: " + e.getMessage());
                }
                DailyNotiScheduler.applyRemoteConfig(enabled, time);
            }
        });
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
        // Localize: áp ngôn ngữ đã chọn ở màn hình Language cho TOÀN tiến trình (kể cả desktop
        // iOSLauncher — trước đây chỉ có module app tự setLocale, desktop chạy theo locale hệ thống).
        // Đặt ở Application là sớm nhất trong vòng đời process nên mọi Activity/plugin (search,
        // settings launcher, theme club) đều thừa hưởng đúng configuration khi được tạo mới.
        // Nếu chưa từng chọn ngôn ngữ (KEY_LANGUAGE rỗng) thì giữ NGUYÊN hành vi cũ.
        String lang = LanguageUtil.getPreLanguage(base);
        if (lang != null && !lang.isEmpty()) {
            Locale locale = new Locale(lang);
            Locale.setDefault(locale);
            Configuration cfg = new Configuration(base.getResources().getConfiguration());
            cfg.setLocale(locale);
            base = base.createConfigurationContext(cfg);
        }
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
