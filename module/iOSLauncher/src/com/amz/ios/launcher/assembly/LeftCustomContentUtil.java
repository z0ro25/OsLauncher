package com.amz.ios.launcher.assembly;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.XmlResourceParser;

import com.amz.ios.ioslite.common.LiteAction;
import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.util.Utilities;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.config.Settings;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.util.List;

public class LeftCustomContentUtil {
    private static int mCustomLeftContentLayout = -1;
    private static String mCustomContentPkg;

    public static final String NEWS_PAGE_PACKAGE_NAME = "com.ios.widget.newspage";
    public static final String NEWS_PAGE_APK_NAME = "IOSPrivateNewsPage.apk";
    public static boolean isNewsPageInstall;
    public static boolean isNewsPageExists;

    public static String getCustomPkg() {
        return mCustomContentPkg;
    }

    public static int getCustomLayout() {
        return mCustomLeftContentLayout;
    }

    public static boolean shouldShow(Context context) {
        return Settings.isLeftPageEnabled(context) && hasCustomContentToLeft(context)
                || (isNewsPageExists = Utilities.isAssetsAppsExists(context, NEWS_PAGE_APK_NAME));
    }

    public static boolean hasCustomContentToLeft(Context context) {
        return false;//hasCustomContentToLeft(context, false);
    }

    public static boolean hasCustomContentToLeft(Context context, boolean force) {
        if (!Partner.getBoolean(context, Partner.FEATURE_LEFT_NEWS_PAGE_ENABLE,true)) {
            return false;
        }


        if (mCustomLeftContentLayout > 0 && !force) {
            return true;
        }

        List<ResolveInfo> queryInfos = context.getPackageManager().queryBroadcastReceivers(new Intent(
                LiteAction.ACTION_LAUNCHER_LEFT_CUSTOM_CONTENT), PackageManager.GET_META_DATA);
        ActivityInfo activityInfo;
        if (queryInfos.isEmpty()) {
            mCustomLeftContentLayout = -1;
        } else {
            activityInfo = queryInfos.get(0).activityInfo;
            mCustomLeftContentLayout = getCutomLyoutFromActInfo(context, activityInfo);
            mCustomContentPkg = activityInfo.packageName;
        }
        return mCustomLeftContentLayout != -1;
    }


    private static int getCutomLyoutFromActInfo(Context context, ActivityInfo info) {
        int customContentLayout = -1;
        try {
            XmlResourceParser parser;

            parser = info.loadXmlMetaData(context.getPackageManager(), "android.appwidget.provider");
            if (parser == null) {
                throw new XmlPullParserException("null");
            } else {
                int type;
                while ((type = parser.next()) != XmlPullParser.END_DOCUMENT && type != XmlPullParser.START_TAG) {
                }

                String namespace = parser.getAttributeNamespace(0);
                customContentLayout = parser.getAttributeResourceValue(namespace, "initialLayout", -1);
            }
        } catch (Exception e) {
            return -1;
        }
        return customContentLayout;
    }
}
