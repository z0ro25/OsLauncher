package com.amz.ios.ioslite.common.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import java.lang.ref.WeakReference;


public class LauncherRouter {
    // This action declared in Launcher;
    private static final String LAUNCHER_CUSTOM_SCREEN = "launch_custom_screen";
    private static final String LAUNCHER_APPLY_THEME = "launch_apply_theme";
    public static final String LAUNCHER_RESTART_REQUEST = "launcer_restart";
    public static final String LAUNCHER_RESET_REQUEST = "launcer_reset";


    private static WeakReference<LauncherDelegate> sLauncherDelegate;

    public static void launch(Context context) {
        Intent intent = createLauncherIntent(context);
        context.startActivity(intent);
    }

    public static void launchToNewTheme(Context context, String packageName) {
        Intent intent = createLauncherIntent(context);
        if (!TextUtils.isEmpty(packageName)) {
            intent.putExtra(LAUNCHER_APPLY_THEME, packageName);
        }
        context.startActivity(intent);
    }

    public static void launchToCustomContent(Context context) {
        Intent intent = createLauncherIntent(context);
        intent.putExtra(LAUNCHER_CUSTOM_SCREEN, true);
        context.startActivity(intent);
    }


    public static void restartLauncher(Context context) {
        Intent intent = createLauncherIntent(context);
        intent.putExtra(LAUNCHER_RESTART_REQUEST, true);
        context.startActivity(intent);
    }

    public static void resetLauncher(Context context) {
        Intent intent = createLauncherIntent(context);
        intent.putExtra(LAUNCHER_RESET_REQUEST, true);
        context.startActivity(intent);
    }

    private static Intent createLauncherIntent(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                | Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP
                | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
                | Intent.FLAG_ACTIVITY_FORWARD_RESULT
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setPackage(context.getPackageName());
        return intent;
    }


    public static void launchSettingActivity(Context context) {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setComponent(new ComponentName(context.getPackageName(), "com.amz.ios.launcher.config.LauncherSettingActivity"));

        context.startActivity(intent);
    }
    public static void launchLocalSettingActivity(Context context) {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setComponent(new ComponentName(context.getPackageName(), "com.ios.iossettings.SettingsActivity"));

        context.startActivity(intent);
    }


    public static void scrollLauncherToDefaultScreen(boolean animate) {
        if (getLauncherDelegate() != null) {
            getLauncherDelegate().scrollLauncherToDefaultScreen(animate);
        }
    }

    public static void startClockApp() {
        if (getLauncherDelegate() != null) {
            getLauncherDelegate().startClockApp();
        }
    }

    public static void startCalendarApp() {
        if (getLauncherDelegate() != null) {
            getLauncherDelegate().startCalendarApp();
        }
    }


    public static void setLauncherDelegate(LauncherDelegate delegate) {
        sLauncherDelegate = new WeakReference<LauncherDelegate>(delegate);
    }

    private static LauncherDelegate getLauncherDelegate() {
        return sLauncherDelegate.get();
    }


    public interface LauncherDelegate {
        // Launcher.Workspace move to default screen, called ba newspage;
        void scrollLauncherToDefaultScreen(boolean animate);

        /**
         * Start app of clock  on device;
         */
        void startClockApp();

        /**
         * Start app of calendar on device;
         */
        void startCalendarApp();
    }

}
