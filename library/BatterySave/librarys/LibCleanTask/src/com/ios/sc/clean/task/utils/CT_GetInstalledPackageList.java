package com.ios.sc.clean.task.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.Manifest.permission;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.ConnectivityManager;
import android.text.TextUtils;

import com.ios.sc.common.logs.CT_Log;
public class CT_GetInstalledPackageList {
    private Context mContext;
    private CT_GetInstalledPackageList() {}

    private String defPackage = "com.android.gallery3d,com.ios.themeclub,com.android.mms,com.android.browser,com.ios.calculator,com.android.email,com.ios.notes,com.ios.healthcenter,"
            + "com.ios.locknow,com.iostech.hotknot,com.ios.videogallery,com.mediatek.filemanager,com.zhuoyi.cloud,com.android.settings,com.sankuai.meituan,"
            + "com.sogou.novel,com.achievo.vipshop,com.paipai.wxd,com.netease.cloudmusic,com.yjyc.zycp,com.qiyi.video,com.baidu.BaiduMap,com.baidu.searchbox,com.android.browser,"
            + "com.tencent.news";

    private static class CP_GetInstalledPackageListHolder {
        private static CT_GetInstalledPackageList instance = new CT_GetInstalledPackageList();
    }

    public synchronized static CT_GetInstalledPackageList getInstance(Context context) {
        if(null == CP_GetInstalledPackageListHolder.instance.mContext){
            CP_GetInstalledPackageListHolder.instance.mContext = context.getApplicationContext();
            CP_GetInstalledPackageListHolder.instance.loadLauncherAndHomeApps();
        }
        return CP_GetInstalledPackageListHolder.instance;
    }

    public List<PackageInfo> getInstalledPackages() {
        List<PackageInfo> ls = mContext.getPackageManager().getInstalledPackages(0);
        List<PackageInfo> fls = new ArrayList<PackageInfo>();
        CT_Log.logI("HS_FROMNETDATA =" + HS_FROMNETDATA + "\n  LAUNCHER_APPS =" + LAUNCHER_APPS);
        for (PackageInfo pi : ls) {
            if(null != HS_FROMNETDATA && HS_FROMNETDATA.contains(pi.packageName)){//from network to clean
                fls.add(pi);
            }else{
                if( ((pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)==0) /*&& (pi.applicationInfo.flags & pi.applicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0*/){//data app and not-system update app
                    if( !filterDataApp(pi.packageName) && (LAUNCHER_APPS.contains(pi.packageName)) && !isInputMethodApp(pi.packageName) && !(HOME_APPS.contains(pi.packageName)) ){
                        fls.add(pi);
                    }
                }
            }
        }
        //FIXME:To get rid of not cleaning applications
        removeNocleanApp();
        CT_Log.logI("fls=" + fls.size() + "\n  fls=" + fls.toString());
        return fls;
    }
    public void removeNocleanApp(){
        //To get rid of not cleaning applications
    }

    private HashSet<String> HS_FROMNETDATA = new HashSet<String>();

    private Set<String> HOME_APPS = new HashSet<String>();
    private Set<String> LAUNCHER_APPS = new HashSet<String>();

    public void loadLauncherAndHomeApps(){
        loadAllLauncherApp();
        loadAllHomeApp();
    }

    /**
     * Query launcher icon app.
     */
    private void loadAllLauncherApp() {
        LAUNCHER_APPS.clear();
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = mContext.getPackageManager().queryIntentActivities(intent,0/*PackageManager.MATCH_DEFAULT_ONLY*/);
        if(resolveInfos != null && resolveInfos.size() >0 ){
            LAUNCHER_APPS.clear();
            for(ResolveInfo ri : resolveInfos){
                LAUNCHER_APPS.add(ri.activityInfo.packageName);
            }
        }
    }

    /**
     * Query Home apps.
     * @return
     */
    private void loadAllHomeApp() {
        HOME_APPS.clear();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> resolveInfos = mContext.getPackageManager().queryIntentActivities(intent,0/*PackageManager.MATCH_DEFAULT_ONLY*/);
        if(resolveInfos != null && resolveInfos.size() >0 ){
            HOME_APPS.clear();
            for(ResolveInfo ri : resolveInfos){
                HOME_APPS.add(ri.activityInfo.packageName);
            }
        }
    }

    /**
     * Query input method app
     * @param packageName
     * @return
     */
    public boolean isInputMethodApp(String packageName) {
        PackageManager pm = mContext.getPackageManager();
        boolean isInputMethodApp = false;
        try {
            PackageInfo pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_SERVICES);
            ServiceInfo[] sInfo = pkgInfo.services;
            if (sInfo != null) {
                for(int i = 0; i < sInfo.length; i++) {
                    ServiceInfo serviceInfo = sInfo[i];
                    if (serviceInfo.permission != null && serviceInfo.permission.equals(permission.BIND_INPUT_METHOD)) {
                        isInputMethodApp = true;
                        break;
                    }
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return isInputMethodApp;
    }

    /*Filter data app packagename.start*/
    private final String SECURITY = "com.zhuoyi.security.lite";
    private final String OURSELF_PACKAGENAME = "com.ios.smart.permission";
    private final String MARKET = "com.zhuoyi.market";
    private final String IOS = "com.zhuoyou.ios";
    private final String TYD_THEME = "com.tydtech.theme.";
    private final String TYD_LOCKSCREEN_THEME = "com.tydtech.lockscreen.";
    private final String FREME_THEME = "com.ios.theme.";
    private final String FREME_LOCKSCREEN_THEME ="com.ios.lockscreen.";
    private final String SECURITY_SERVICE = "com.zhuoyi.security.service";
    private final String CLEANTASK = "com.sc.cleantask";
    private final String IOS_IOS = "com.ios.ios";
    /*Filter data app packagename.end*/
    public boolean filterDataApp(String pn){
        boolean result = false ;
        if( (OURSELF_PACKAGENAME.equals(pn)) || (MARKET.equals(pn)) || (pn.startsWith(TYD_THEME)) || (pn.equals(SECURITY)) || (pn.startsWith(CLEANTASK))
            || (pn.startsWith(TYD_LOCKSCREEN_THEME)) || (pn.startsWith(FREME_THEME)) || (pn.startsWith(FREME_LOCKSCREEN_THEME)) || pn.equals(SECURITY_SERVICE)
            || (pn.startsWith(IOS)) || (pn.startsWith(IOS_IOS)) ){
            result = true;
        }
        return result;
    }


    public final String updateTimeTabName = "Boot_update_time";
    public final String autorunOffList = "AUTORUN_OFF_LIST";
    public synchronized int getBootListNum(boolean isSupport, String filterPkg) {
        int allow_boot =0;
        HashSet<String>  set = getAPKPackagenN();
        String systemFilterList = getSystemAPPBootCompleted();
        PackageManager mPackageManager = mContext.getPackageManager();
        Intent intent  = new Intent();
        intent.setAction(Intent.ACTION_BOOT_COMPLETED );
        List<ResolveInfo> resolveInfoList = mPackageManager.queryBroadcastReceivers(intent, PackageManager.GET_DISABLED_COMPONENTS);
        Intent intentNET  = new Intent();
        intentNET.setAction(ConnectivityManager.CONNECTIVITY_ACTION);
        List<ResolveInfo> resolveInfoListNET = mPackageManager.queryBroadcastReceivers(intentNET, PackageManager.GET_DISABLED_COMPONENTS);
        resolveInfoListNET.addAll(resolveInfoList);
        HashMap<String,String> tempallow = new HashMap<String,String>();
        for(ResolveInfo list :resolveInfoListNET){
            String pkg = list.activityInfo.packageName;
            if(pkg.equals(mContext.getPackageName())){
                continue;
            }
            if((systemFilterList.contains(pkg) &&!pkg.equals("android"))|| set.contains(pkg)){
                if(isSupport){
                    if(!filterPkg.isEmpty() && filterPkg.contains(pkg)){
                        continue;
                    }else{
                        tempallow.put(pkg, pkg);
                    }
                }else{
                    if (getComponentState(pkg,list.activityInfo.name) != 2) {
                        tempallow.put(pkg, pkg);
                    }
                }
            }
        }
        allow_boot = tempallow.size();
        return allow_boot;
    }

    private HashSet<String> getAPKPackagenN(){
        ArrayList<ApplicationInfo> apkList = new ArrayList<ApplicationInfo>();
        HashSet<String> set = new HashSet<String>();
        apkList = getAppInfo();
        for(ApplicationInfo apkl : apkList){
            set.add(apkl.packageName);
        }
        return set;
    }

    private ArrayList<ApplicationInfo> getAppInfo(){
        List<PackageInfo> packages = mContext.getPackageManager().getInstalledPackages(0);
        ArrayList<ApplicationInfo> appList = new ArrayList<ApplicationInfo>();
        int length = packages.size();
        for (int i = 0; i < length ; i++) {
            PackageInfo packageInfo = packages.get(i);
            ApplicationInfo ai = packageInfo.applicationInfo;

            if (((ai.flags & ApplicationInfo.FLAG_SYSTEM) == 0) && ai.flags!=ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
                  && !packageInfo.packageName.contains("com.zhuoyi.security") && !packageInfo.packageName.contains("com.tydtech.theme")
                  && !packageInfo.packageName.contains("com.ios.theme")) {
                    appList.add(ai);
            }
        }
        return appList;
    }

    public String getSystemAPPBootCompleted(){
        String result = "";
        result = getFromPreferences(updateTimeTabName, autorunOffList);
        if(!TextUtils.isEmpty(result)){
            return result;
        }
        try {
            InputStream in = mContext.getResources().getAssets().open("system_app_boot_completed");
            int length = in.available();
            byte [] buffer = new byte[length];
            in.read(buffer);
            result = new String(buffer,"UTF-8");
            //result = EncodingUtils.getString(buffer, "UTF-8");
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public String getFromPreferences(String tabName,String name){
        String times = "";
        SharedPreferences perf = mContext.getSharedPreferences(tabName, 1);
        times = perf.getString(name, "");
        return times;
    }

    public int getComponentState(String packageName,String clsName){
        ComponentName mComponentName = new ComponentName(packageName,clsName);
        int a = mContext.getPackageManager().getComponentEnabledSetting(mComponentName);
        return a;
    }
}
