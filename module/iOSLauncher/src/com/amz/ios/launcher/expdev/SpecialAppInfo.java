package com.amz.ios.launcher.expdev;

public class SpecialAppInfo {
    private String mComponent = null;
    private int mIconResId = -1;

    public SpecialAppInfo(int i, String str) {
        setIconResId(i);
        setComponent(str);
    }

    public String getComponent() {
        return this.mComponent;
    }

    public int getIconResId() {
        return this.mIconResId;
    }

    public void setComponent(String str) {
        this.mComponent = str;
    }

    public void setIconResId(int i) {
        this.mIconResId = i;
    }
}
