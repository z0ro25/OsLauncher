//package com.amz.ios.launcher.expdev;
//
//import android.content.Context;
//import android.content.pm.ResolveInfo;
//import android.content.res.Resources;
//import android.os.Bundle;
//import com.ios.ioslite.cn.R;
//import java.util.ArrayList;
//import java.util.jar.Attributes;
//import org.xml.sax.helpers.DefaultHandler;
//
//public class SpecialIconXmlHandler extends DefaultHandler {
//    private static final String TAG = "SpecialIconXmlHandler";
//    public static final String TAG_BackName = "BackName";
//    public static final String TAG_Beautify_IconName = "BeautifyIconName";
//    public static final String TAG_Package = "Package";
//    public static final String TAG_SpecialBack = "SpecialBack";
//    private static ArrayList<Bundle> mSpecialIcon = new ArrayList<>();
//    private Bundle mBundle;
//    private Context mContext = null;
//    private String mCurrentTag = null;
//
//    @Override // org.xml.sax.helpers.DefaultHandler, org.xml.sax.ContentHandler
//    public void characters(char[] cArr, int i, int i2) {
//        String str = new String(cArr, i, i2);
//        if (str.length() != 1 || str.charAt(0) != '\n') {
//            if (TAG_Package.equals(this.mCurrentTag)) {
//                this.mBundle.putString(TAG_Package, str);
//            } else if (TAG_BackName.equals(this.mCurrentTag)) {
//                this.mBundle.putString(TAG_BackName, str);
//            } else if (TAG_Beautify_IconName.equals(this.mCurrentTag)) {
//                this.mBundle.putString(TAG_Beautify_IconName, str);
//            }
//        }
//    }
//
//    @Override // org.xml.sax.helpers.DefaultHandler, org.xml.sax.ContentHandler
//    public void endElement(String str, String str2, String str3) {
//        if (TAG_SpecialBack.equals(str2)) {
//            mSpecialIcon.add(this.mBundle);
//        }
//        this.mCurrentTag = null;
//    }
//
//    public Context getContext() {
//        return this.mContext;
//    }
//
//    public int isAppUseSpecialIcon(ResolveInfo resolveInfo) {
//        return isGoogleAppUseSpecialIcon(resolveInfo);
//    }
//
//    public boolean isGoogleApp(String str) {
//        String[] stringArray;
//        if (str.contains("com.google.android")) {
//            return true;
//        }
//        for (String str2 : this.mContext.getResources().getStringArray(R.array.google_pkgname)) {
//            if (str.equals(str2)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    public int isGoogleAppUseSpecialIcon(ResolveInfo resolveInfo) {
//        Resources resources = this.mContext.getResources();
//        String[] stringArray = resources.getStringArray(R.array.google_pkgname);
//        String[] stringArray2 = resources.getStringArray(R.array.google_clsname);
//        if (stringArray.length == stringArray2.length) {
//            for (int i = 0; i < stringArray.length; i++) {
//                if (stringArray[i].equals(resolveInfo.activityInfo.applicationInfo.packageName) && stringArray2[i].equals(resolveInfo.activityInfo.name)) {
//                    return i;
//                }
//            }
//        }
//        return -1;
//    }
//
//    public int isThirdAppUseSpecialIcon(ResolveInfo resolveInfo) {
//        Resources resources = this.mContext.getResources();
//        String[] stringArray = resources.getStringArray(R.array.thirdpart_pkgname);
//        String[] stringArray2 = resources.getStringArray(R.array.thirdpart_clsname);
//        if (stringArray.length == stringArray2.length) {
//            for (int i = 0; i < stringArray.length; i++) {
//                if (stringArray[i].equals(resolveInfo.activityInfo.applicationInfo.packageName) && stringArray2[i].equals(resolveInfo.activityInfo.name)) {
//                    return i;
//                }
//            }
//        }
//        return -1;
//    }
//
//    public void setContext(Context context) {
//        this.mContext = context;
//    }
//
//    public void startElement(String str, String str2, String str3, Attributes attributes) {
//        this.mCurrentTag = str2;
//        if (TAG_SpecialBack.equals(str2)) {
//            this.mBundle = new Bundle();
//        }
//    }
//}
