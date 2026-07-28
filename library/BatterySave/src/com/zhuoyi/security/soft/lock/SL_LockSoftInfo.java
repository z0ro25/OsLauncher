package com.zhuoyi.security.soft.lock;

import android.graphics.Bitmap;

public class SL_LockSoftInfo {


    private String packageName;
    private String needLock;
    private String name ;
    private Bitmap icon;
    private String path;


    public SL_LockSoftInfo(String packageName, String needLock, String name,
            Bitmap icon) {
        super();
        this.packageName = packageName;
        this.needLock = needLock;
        this.name = name;
        this.icon = icon;
    }
    public String getPackageName() {
        return packageName;
    }
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    public String getNeedLock() {
        return needLock;
    }
    public void setNeedLock(String needLock) {
        this.needLock = needLock;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Bitmap getIcon() {
        return icon;
    }
    public void setIcon(Bitmap icon) {
        this.icon = icon;
    }
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }







}
