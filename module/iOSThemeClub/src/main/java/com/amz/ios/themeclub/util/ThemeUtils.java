package com.amz.ios.themeclub.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;

import com.amz.ios.themeclub.bean.ThemeInfo;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by lideqian on 16-11-23.
 */
public class ThemeUtils {

    public static String TAG = ThemeUtils.class.getSimpleName();
    public static final String APPLY_THEME = "com.ios.ioslite.action.THEME_CHANGED";
    public static final String BROADCAST_THEME_NEED_UPDATE = "broadcast.theme.need.update";
    public static final String PEFIX_OF_THEME_PACKAGE_NAME = "com.ios.theme.";
    private static final String THEME_PREVIEW_FOLDER_PREFIX = "preview";
    public static final String THEME_PREVIEW_SUFFIX = ".jpg";
    private static final String THEME_DESCRIPTION_PATH = "description.xml";
    private static final String DEFAULT_THEME_PATH = "/system/framework/framework-res.apk";
    public static String sThemePath = DEFAULT_THEME_PATH;
    public static final String BiglauncherPackageName = "com.ios.theme.biglauncher";
    private static  String dimens = "xxhdpi";

    public static String THEME_PREVIEW_THUMB = "/theme_preview_thumb.jpg";
    public static String THEME_PREVIEW_ONE = "theme_preview_op0";
    public static String THEME_PREVIEW_TWO = "theme_preview_op1";
    public static String THEME_PREVIEW_THREE = "theme_preview_op2";

    public static String THEME_PREVIEW_ICON = "theme_preview_icon"+THEME_PREVIEW_SUFFIX;
    public static String THEME_PREVIEW_LSUNCHER = "theme_preview_launcher"+THEME_PREVIEW_SUFFIX;
    public static String THEME_PREVIEW_LOCKSCREEN = "theme_preview_lockscreen"+THEME_PREVIEW_SUFFIX;

//    public static int THEME_THUMB_QUALITY = 30;
    public static int THEME_THUMB_QUALITY = 100;

    private static boolean inArray(String str, String [] arr){
        for (int i = 0; i < arr.length; i++) {
            if (str.equals(arr[i])) {
                return true;
            }
        }
        return false;
    }

    public static ThemeInfo getThemeInfo(Context context , String themePath , String packageName) {

        if(themePath == null || packageName == null){
            return null;
        }
        ThemeInfo themeInfo = new ThemeInfo();
        themeInfo.themePath = themePath;
        themeInfo.packageName = packageName;
        InputStream is = null;
        AssetManager am =null;
        try {
            Context mRemoteContext = context.createPackageContext(packageName,
                    Context.CONTEXT_IGNORE_SECURITY);
            am = mRemoteContext.getAssets();
//            if(am == null) {
//                return null;
//            }
            is = am.open(THEME_DESCRIPTION_PATH);
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setValidating(false);
            XmlPullParser myxml = factory.newPullParser();
            myxml.setInput(is, null);
            int eventType = myxml.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        String elementName = myxml.getName();
                        if ("title".equals(elementName)) {
                            themeInfo.title = myxml.nextText();
                        } else if ("author".equals(elementName)) {
                            themeInfo.author = myxml.nextText();
                        } else if ("description".equals(elementName)) {
                            themeInfo.description = myxml.nextText();
                        } else if ("version".equals(elementName)) {
                            themeInfo.version = myxml.nextText();
                        } else if ("type".equals(elementName)){
                            themeInfo.themeType=myxml.nextText();
                        } else if ("font".equals(elementName)){
                            themeInfo.font=myxml.nextText();
                        }

                        break;
                    case XmlPullParser.END_TAG:
                        break;
                    default:
                        break;
                }
                eventType = myxml.next();
            }

            //String str[] = am.list("preview-" + dimens);

            is.close();
            is = am.open("preview-" + dimens + "/theme_preview_thumb.jpg");
            themeInfo.thumb = BitmapFactory.decodeStream(is);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }finally {
            if (is != null) {
                try {
                    is.close();
                    is = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return themeInfo;
    }

    public static ArrayList<String> getPicList(Context context,String packageName) {
        ArrayList<String> picList = new ArrayList<>();
        if (packageName == null) {
            return null;
        }
        AssetManager am = null;
        try {
            Context mRemoteContext = context.createPackageContext(packageName,
                    Context.CONTEXT_IGNORE_SECURITY);
            am = mRemoteContext.getAssets();
            String str[] = am.list("preview-" + dimens);

            for (int i = 0; i < str.length; i++) {
                if (str[i].startsWith("theme_preview_op")) {
                    picList.add(str[i]);
                }
            }
            //old theme
            if ( picList.size() == 0 ) {
                if (inArray(THEME_PREVIEW_ICON, str)) {
                    picList.add(THEME_PREVIEW_ICON);
                }
                if (inArray(THEME_PREVIEW_LSUNCHER, str)) {
                    picList.add(THEME_PREVIEW_LSUNCHER);
                }
                if (inArray(THEME_PREVIEW_LOCKSCREEN, str)) {
                    picList.add(THEME_PREVIEW_LOCKSCREEN);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return picList;
    }

    public static BitmapDrawable getThemePreview(
            Context context,
            String packageName,
            String themePath,
            String previewType) {
        String fileName = previewType.indexOf(".") >= 0 ?
                previewType : previewType + THEME_PREVIEW_SUFFIX;
        BitmapDrawable dr = getThemeImage(
                context,
                packageName,
                themePath,
                fileName);
        return dr;
    }

    public static BitmapDrawable getThemeImage(
            Context context,
            String packageName,
            String themePath,
            String fileName) {
        BitmapDrawable dr = null;

        if (android.text.TextUtils.isEmpty(themePath))
            themePath = sThemePath;

        Context mRemoteContext;
        try {
            mRemoteContext = context.createPackageContext(packageName,
                    Context.CONTEXT_IGNORE_SECURITY);
            AssetManager am = mRemoteContext.getAssets();
            InputStream is = null;
            Bitmap bitmap = null;
            try {
                is = am.open(THEME_PREVIEW_FOLDER_PREFIX+"-"+dimens+"/" +fileName);
                bitmap = BitmapFactory.decodeStream(is);

            } catch (IOException e) {

                e.printStackTrace();
                try {
                    if(bitmap == null){
                        is = am.open(THEME_PREVIEW_FOLDER_PREFIX+dimens+"/" +fileName);
                        bitmap = BitmapFactory.decodeStream(is);
                    }

                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            }
            if(is != null){
                is.close();
            }
            if (bitmap != null)
                dr = new BitmapDrawable(Resources.getSystem(), bitmap);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return dr;
    }

    public BitmapDrawable getResPicture(
            Context context,
            String packageName,
            String themePath,
            String fileName) {
        BitmapDrawable dr = null;
        Bitmap bitmap = null;

        if (android.text.TextUtils.isEmpty(themePath))
            themePath = sThemePath;

        try {

            Context mRemoteContext = context.createPackageContext(packageName,
                    Context.CONTEXT_IGNORE_SECURITY);
            Resources res = mRemoteContext.getResources();
            int identifier = 0;
            identifier = res.getIdentifier(fileName, "drawable",packageName);

            if (identifier > 0) {
                bitmap = BitmapFactory.decodeResource(res, identifier);
            }
            if (bitmap != null)
                dr = new BitmapDrawable(Resources.getSystem(), bitmap);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return dr;
    }
}
