package com.amz.ios.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;

/**
 * Created by JMC
 *
 */
public class AppTypeParser {


    private static final String TAG = "AppTypeParser";
    private static final String FILE_PREFIX = "app_target";

    // system app type
    public static final String APP_TYPE_PHONE = "app_phone";
    public static final String APP_TYPE_CONTACTS = "app_contacts";
    public static final String APP_TYPE_MESSAGE = "app_mms";
    public static final String APP_TYPE_BROWSER = "app_browser";
    public static final String APP_TYPE_CAMERA = "app_camera";
    public static final String APP_TYPE_CLOCK = "app_clock";
    public static final String APP_TYPE_CALENDAR = "app_calendar";
    public static final String APP_TYPE_GALLERY = "app_gallery";
    public static final String APP_TYPE_MIRROR = "app_mirror";
    public static final String APP_TYPE_MUSIC = "app_music";
    public static final String APP_TYPE_VIDEO = "app_video";
    public static final String APP_TYPE_RECORDER = "app_soundrecorder";
    public static final String APP_TYPE_SETTINGS = "app_settings";
    public static final String APP_TYPE_SYSTEM_MANAGER = "app_systemmanager";
    public static final String APP_TYPE_FLASH_LIGHT = "app_flashlight";
    public static final String APP_TYPE_OFFICE = "app_office";
    public static final String APP_TYPE_FILE_MANAGER = "app_filemanager";
    public static final String APP_TYPE_CALCULATOR = "app_calculator";
    public static final String APP_TYPE_STEP_RECORDER = "app_steprecorder";
    public static final String APP_TYPE_QR_CODE = "app_qrcode";
    public static final String APP_TYPE_NOTE = "app_note";
    public static final String APP_TYPE_USER_BOOK = "app_userbook";
    public static final String APP_TYPE_THEME = "app_theme";
    public static final String APP_TYPE_TOOL_BAG = "app_toolbag";
    public static final String APP_TYPE_HISTORY = "app_history";
//    public static final String APP_TYPE_HIDDENFOLDER = "app_hiddenfolder";

//    private static final String APP_TYPE_EMAIL = "app_email";
//    private static final String APP_TYPE_SECURITY = "app_security";
//    private static final String APP_TYPE_BOOK = "app_book";
    // iOS override: map the app store (Google Play etc.) to the iOS App Store icon (ic_app_market).
    public static final String APP_TYPE_MARKET = "app_market";
//    private static final String APP_TYPE_DOWNLOAD = "app_download";
    //    public static final String APP_TYPE_TORCH = "app_torch";

//    private static final String APP_TYPE_FM_RADIO = "app_fmradio";


    // third app type
    public static final String APP_TYPE_CHAT = "app_chat";
    public static final String APP_TYPE_SOCIAL = "app_social";

    public static final String APP_TYPE_WEATHER = "app_weather";

    private static final HashSet<String> PRE_SYSTEM_APP_TYPES;
    private static final HashSet<String> PRE_THIRD_APP_TYPES;

    static {
        // system app types
        PRE_SYSTEM_APP_TYPES = new HashSet<>();
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_PHONE);
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_MESSAGE);
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_CONTACTS);
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_BROWSER);
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_CAMERA);
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_GALLERY);
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_MUSIC);
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_FILE_MANAGER);
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_NOTE);
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_CALENDAR);
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_CALCULATOR);
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_CLOCK);
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_SETTINGS);
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_RECORDER);
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_USER_BOOK);
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_VIDEO);
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_WEATHER);
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_THEME);
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_MIRROR);
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_SYSTEM_MANAGER);
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_FLASH_LIGHT);
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_OFFICE);
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_STEP_RECORDER);
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_QR_CODE);
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_TOOL_BAG);
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_HISTORY);
//        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_HIDDENFOLDER);

        // PRE_SYSTEM_APP_TYPES.add(APP_TYPE_TORCH);
//        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_FM_RADIO);
//        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_EMAIL);
//        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_SECURITY);
//        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_BOOK);
        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_MARKET);
//        PRE_SYSTEM_APP_TYPES.add(APP_TYPE_DOWNLOAD);

        // third app types
        PRE_THIRD_APP_TYPES = new HashSet<>();
        PRE_THIRD_APP_TYPES.add(APP_TYPE_CHAT);
        PRE_THIRD_APP_TYPES.add(APP_TYPE_SOCIAL);
    }

    private static final String TAG_ROOT = "items";
    private static final String TAG_FAVORITE = "favorite";
    private static final String ATTR_PACKAGE_NAME = "packageName";
    private static final String ATTR_CLASS_NAME = "className";
    private static final String ATTR_URI = "uri";


    private Context mContext;
    private List<ComponentName> mAllLauncherCompList;
    private AppTypeParserCallback mCallback;

    private PackageManager mPackgeManager;
    private Resources mRes;


    public AppTypeParser(Context context, AppTypeParserCallback callback, List<ComponentName> allLauncherComps) {
        mContext = context;
        mAllLauncherCompList = allLauncherComps;
        mCallback = callback;

        mPackgeManager = context.getPackageManager();
        mRes = context.getResources();
    }


    public void initAllAppType() {
        ComponentName cn;
        for (String type : PRE_SYSTEM_APP_TYPES) {
            cn = parseComponentForAppType(type);
            if (cn != null) {
                mCallback.insertAndCheck(type, cn);
            }
        }

        for (String type : PRE_THIRD_APP_TYPES) {
            cn = parseComponentForAppType(type);
            if (cn != null) {
                mCallback.insertAndCheck(type, cn);
            }
        }
    }

    private ComponentName parseComponentForAppType(String type) {
        int layoutId;
        if ((layoutId = getResourceForTYpe(type)) == 0) {
            Log.e(TAG, "not found  xml file for app type " + type);
            return null;
        }

        try {
            return parseXml(layoutId);
        } catch (Exception e) {
            Log.e(TAG, "Got exception parsing layout.", e);
            return null;
        }
    }

    private ComponentName parseXml(int layoutId)
            throws XmlPullParserException, IOException {
        XmlResourceParser parser = mRes.getXml(layoutId);
        beginDocument(parser, TAG_ROOT);
        final int depth = parser.getDepth();
        int type;
        ComponentName cp = null;

        while (((type = parser.next()) != XmlPullParser.END_TAG ||
                parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
            if (type != XmlPullParser.START_TAG || cp != null) {
                continue;
            }

            String item_name = parser.getName();
            if (TAG_FAVORITE.equals(item_name)) {
                cp = parseNode(parser);
            } else {
                Log.e(TAG, "app target xml can only contain favorites, but found " + item_name);
            }
        }

        return cp;
    }


    private ComponentName parseNode(XmlResourceParser parser) {
        final String packageName = getAttributeValue(parser, ATTR_PACKAGE_NAME);
        final String className = getAttributeValue(parser, ATTR_CLASS_NAME);
        final String uri = getAttributeValue(parser, ATTR_URI);

        ComponentName cn = null;
        if (!TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(className)) {
            try {

                try {
                    cn = new ComponentName(packageName, className);
                    mPackgeManager.getActivityInfo(cn, 0);
                } catch (PackageManager.NameNotFoundException nnfe) {
                    String[] packages = mPackgeManager.currentToCanonicalPackageNames(
                            new String[]{packageName});
                    cn = new ComponentName(packages[0], className);
                    mPackgeManager.getActivityInfo(cn, 0);
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "not found app : " + packageName + "/" + className);
                cn = null;
            }
        } else if (!TextUtils.isEmpty(uri)) {
            cn = invalidPackageOrClass(uri, mPackgeManager);
        }

        if (!isLauncherComp(cn)) {
            cn = null;
        }
        return cn;
    }


    private static ComponentName invalidPackageOrClass(String uri, PackageManager packageManager) {
        final Intent metaIntent;
        try {
            metaIntent = Intent.parseUri(uri, 0);
        } catch (URISyntaxException e) {
            Log.e(TAG, "Unable to add meta-favorite: " + uri, e);
            return null;
        }

        ResolveInfo resolved = packageManager.resolveActivity(metaIntent,
                PackageManager.MATCH_DEFAULT_ONLY);
        final List<ResolveInfo> appList = packageManager.queryIntentActivities(
                metaIntent, PackageManager.MATCH_DEFAULT_ONLY);

        // Verify that the result is an app and not just the resolver dialog asking which
        // app to use.
        if (wouldLaunchResolverActivity(resolved, appList)) {
            // If only one of the results is a system app then choose that as the default.
            final ResolveInfo systemApp = getSingleSystemActivity(appList, packageManager);
            if (systemApp == null) {
                // There is no logical choice for this meta-favorite, so rather than making
                // a bad choice just add nothing.
                Log.w(TAG, "No preference or single system activity found for "
                        + metaIntent.toString());
                return null;
            }
            resolved = systemApp;
        }
        final ActivityInfo info = resolved.activityInfo;
        return new ComponentName(info.packageName, info.name);
    }

    private static boolean wouldLaunchResolverActivity(ResolveInfo resolved,
                                                       List<ResolveInfo> appList) {
        // If the list contains the above resolved activity, then it can't be
        // ResolverActivity itself.
        for (int i = 0; i < appList.size(); ++i) {
            ResolveInfo tmp = appList.get(i);
            if (tmp.activityInfo.name.equals(resolved.activityInfo.name)
                    && tmp.activityInfo.packageName.equals(resolved.activityInfo.packageName)) {
                return false;
            }
        }
        return true;
    }


    private static ResolveInfo getSingleSystemActivity(List<ResolveInfo> appList, PackageManager packageManager) {
        ResolveInfo systemResolve = null;
        final int N = appList.size();
        for (int i = 0; i < N; ++i) {
            try {
                ApplicationInfo info = packageManager.getApplicationInfo(
                        appList.get(i).activityInfo.packageName, 0);
                if ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    if (systemResolve != null) {
                        return null;
                    } else {
                        systemResolve = appList.get(i);
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Unable to get info about resolve results");
                return null;
            }
        }
        return systemResolve;
    }


    protected static final void beginDocument(XmlPullParser parser, String firstElementName)
            throws XmlPullParserException, IOException {
        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG
                && type != XmlPullParser.END_DOCUMENT) ;

        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }

        if (!parser.getName().equals(firstElementName)) {
            throw new XmlPullParserException("Unexpected start tag: found " + parser.getName() +
                    ", expected " + firstElementName);
        }
    }


    protected static String getAttributeValue(XmlResourceParser parser, String attribute) {
        String value = parser.getAttributeValue(
                "http://schemas.android.com/apk/res-auto/com.ios.launcher", attribute);
        if (value == null) {
            value = parser.getAttributeValue(null, attribute);
        }
        return value;
    }


    private int getResourceForTYpe(String type) {
        String xmlName = FILE_PREFIX + "_" + type;
        return mRes.getIdentifier(xmlName, "xml", mContext.getPackageName());
    }

    // Check if this is a launcher component;
    private boolean isLauncherComp(ComponentName cn) {
        return cn != null && mAllLauncherCompList.contains(cn);
    }


    public interface AppTypeParserCallback {

        void insertAndCheck(String type, ComponentName componentName);
    }
}
