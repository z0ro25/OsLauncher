//package com.ios.home.expdev;
//
//import android.content.ComponentName;
//import android.content.Context;
//import android.content.pm.ActivityInfo;
//import android.content.pm.ResolveInfo;
//import android.graphics.drawable.Drawable;
//import android.os.Bundle;
//import android.provider.Settings;
//import android.util.Log;
//import com.ios.ioslite.cn.R;
//import com.ios.home.theme.ThemeUtil;
//import java.util.ArrayList;
//
//public class AppIconSpecialHandler {
//    public static final String[] BEAUTIFY_GOOGLE_ICON_PACKAGES = {"com.google.android.keep", "com.google.android.apps.docs", "com.google.android.apps.translate", "com.android.vending", "com.google.android.apps.maps", "com.google.android.apps.plus", "com.google.android.gm", "com.android.chrome", "com.google.android.talk", "com.google.android.apps.genie.geniewidget", "com.google.android.googlequicksearchbox", "com.google.android.music", "com.google.android.apps.books", "com.google.android.apps.magazines", "com.google.android.videos"};
//    private static final boolean DEBUG = false;
//    public static final String GOOGLEICONS_BEAUTIFY_TOGGLE_ACTION = "GOOGLEICONS_BEAUTIFY_TOGGLE_ACTION";
//    public static final String TAG = "AppIconSpecialHandle";
//    public static final String[] sNeedAddBackgroundAppsClassName = {"com.android.providers.downloads.ui.DownloadList", "com.facebook.katana.LoginActivity", "com.ios.facebook.BindFacebookAccountActivity", "com.qo.android.quickoffice.QuickofficeDispatcher", "com.nearme.sync.activities.LoginScreenActivity", "com.ios.wallpaperAPK.activity.Folder", "com.gameloft.android.Widget.FVGL.Top_games.EN_TH.Start", "com.adobe.flashplayer.SettingsManager", "ru.yandex.searchplugin.MainActivity", "ru.yandex.yandexmaps.MapActivity", "ru.yandex.yandexnavi.core.NavigatorActivity", "ru.yandex.market.ui.MainActivity", "ru.yandex.disk.MainActivity", "ru.yandex.rasp.AMainActivity", "ru.ivi.client.ui.SplashActivity", "ru.mail.instantmessanger.activities.contactlist.ContactListActivity", "ru.ok.android.ui.OdnoklassnikiActivity", "com.vkontakte.android.MainActivity", "com.ios.operationManual.Folder", "com.ios.themestore.activity.ThemeStoreActivityGroup", "com.ios.market.activity.MainActivity", "com.google.android.keep.activities.BrowseActivity", "com.google.android.apps.docs.app.NewMainProxyActivity", "com.google.android.apps.translate.HomeActivity", "com.google.android.talk.SigningInActivity"};
//    private Context mContext = null;
//    private ArrayList<Bundle> mSpecialIcon = null;
//
//    public AppIconSpecialHandler(Context context) {
//        this.mContext = context;
//    }
//
//    private String getSpecialBackgroundName(String str, boolean z) {
//        if (str == null) {
//            return null;
//        }
//        for (int i = 0; i < this.mSpecialIcon.size(); i++) {
//            if (str.equals(this.mSpecialIcon.get(i).getString(SpecialIconXmlHandler.TAG_Package))) {
//                if (ThemeUtil.isDefaultTheme()) {
//                    return (!z || !isBeautifyGoogleAppIconEnabled(this.mContext)) ? this.mSpecialIcon.get(i).getString(SpecialIconXmlHandler.TAG_BackName) : this.mSpecialIcon.get(i).getString(SpecialIconXmlHandler.TAG_Beautify_IconName);
//                } else {
//                    if (!z) {
//                        return null;
//                    }
//                    return this.mSpecialIcon.get(i).getString(SpecialIconXmlHandler.TAG_BackName);
//                }
//            }
//        }
//        return null;
//    }
//
//    public static boolean isAppHide(Context context, ComponentName componentName) {
//        String[] stringArray;
//        if (componentName == null) {
//            return false;
//        }
//        for (String str : context.getResources().getStringArray(R.array.hide_app_component)) {
//            if (componentName.flattenToShortString().equals(str)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    public static boolean isGoogleAppPkg(String str) {
//        boolean z = false;
//        String[] strArr = BEAUTIFY_GOOGLE_ICON_PACKAGES;
//        for (String str2 : strArr) {
//            if (str2.startsWith(str) || str2.equals(str)) {
//                z = true;
//            }
//        }
//        if (str.contains("com.google.android")) {
//            return true;
//        }
//        return z;
//    }
//
//    public Drawable getSpeciaAppIcon(ResolveInfo resolveInfo) {
//        boolean isGoogleAppPkg = isGoogleAppPkg(resolveInfo.activityInfo.packageName);
//        if (ThemeUtil.isDefaultTheme() || isGoogleAppPkg) {
//            return getSpecialDrawable(resolveInfo.activityInfo.name, isGoogleAppPkg);
//        }
//        return null;
//    }
//
//    /* access modifiers changed from: protected */
//    public Drawable getSpecialDrawable(String str, boolean z) {
//        try {
//            String specialBackgroundName = getSpecialBackgroundName(str, z);
//            if (specialBackgroundName != null) {
//                return this.mContext.getResources().getDrawable(this.mContext.getResources().getIdentifier(specialBackgroundName, "drawable", "com.ios.home"));
//            }
//            return null;
//        } catch (Exception e) {
//            Log.e(TAG, "error in load special app back --> className = " + str + " --> isGoogleApp = " + z);
//            return null;
//        }
//    }
//
//    public ArrayList<Bundle> getSpecialIcon() {
//        return this.mSpecialIcon;
//    }
//
//    public boolean isBeautifyGoogleAppIconEnabled(Context context) {
//        return Settings.System.getInt(context.getContentResolver(), "beautify_google_icons", 0) == 1;
//    }
//
//    public boolean isGoogleCurrents(String str, String str2) {
//        return "com.google.android.apps.currents".equals(str) && "com.google.apps.dots.android.app.activity.CurrentsStartActivity".equals(str2);
//    }
//
//    public boolean needAddIconBackground(ActivityInfo activityInfo) {
//        if (ThemeUtil.isDefaultTheme()) {
//            return false;
//        }
//        for (int length = sNeedAddBackgroundAppsClassName.length - 1; length >= 0; length--) {
//            if (sNeedAddBackgroundAppsClassName[length].equals(activityInfo.name)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    public void setSpecialIcon(ArrayList<Bundle> arrayList) {
//        this.mSpecialIcon = arrayList;
//    }
//}
