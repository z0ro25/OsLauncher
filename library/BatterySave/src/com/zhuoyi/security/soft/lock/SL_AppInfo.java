package com.zhuoyi.security.soft.lock;
import android.graphics.drawable.Drawable;

public class SL_AppInfo {

    private String mPackageName;
    private String appName;
    private String className;
    private int mPermissionSize;
    private Drawable appicon = null;


 
    public SL_AppInfo(String mPackageName, String appName, Drawable appicon,int PermissionSize) {
        super();
        this.mPackageName = mPackageName;
        this.appName = appName;
        this.appicon = appicon;
        this.mPermissionSize = PermissionSize;

    }
    public SL_AppInfo(String mPackageName, String appName,String classN, Drawable appicon,int PermissionSize) {
        super();
        this.mPackageName = mPackageName;
        this.appName = appName;
        this.appicon = appicon;
        this.mPermissionSize = PermissionSize;
        this.className = classN;
    }


    public String getClassName() {
        return className;
    }
    public void setClassName(String className) {
        this.className = className;
    }
    public String getmPackageName() {
        return mPackageName;
    }
    public void setmPackageName(String mPackageName) {
        this.mPackageName = mPackageName;
    }
    public Drawable getAppicon() {
        return appicon;
    }
    public void setAppicon(Drawable appicon) {
        this.appicon = appicon;
    }
    public String getAppName() {
        return appName;
    }
    public void setAppName(String appName) {
        this.appName = appName;
    }


    public int getPermissionSize() {
        return mPermissionSize;
    }
    public void setPermissonSize(int permissionSize) {
        this.mPermissionSize = permissionSize;
    }
}
