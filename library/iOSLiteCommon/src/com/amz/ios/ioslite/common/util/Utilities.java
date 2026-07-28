package com.amz.ios.ioslite.common.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.amz.ios.ioslite.common.util.BuildUtil.ATLEAST_JB_MR1;

/**
 * Created by YiYang on 16-10-18.
 */

public class Utilities {

    public static final boolean ATLEAST_LOLLIPOP =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    public static final boolean ATLEAST_KITKAT =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

    public static final boolean ATLEAST_JB_MR1 =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;

    public static final boolean ATLEAST_JB_MR2 =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;

    private static String sCommonFolderName = null;
    private static final Pattern sTrimPattern =
            Pattern.compile("^[\\s|\\p{javaSpaceChar}]*(.*)[\\s|\\p{javaSpaceChar}]*$");
    public static boolean copyApkFromAssets(Context context, String packageName, String asserPath) {
        boolean copyIsFinish = false;
        File dir = new File(Environment.getExternalStorageDirectory() + "/" + getCommonFolderName(context));
        File file = new File(dir, packageName + ".apk");
        InputStream is = null;
        FileOutputStream os = null;
        if (!file.exists()) {
            try {
                is = context.getAssets().open(asserPath);
                if (!dir.exists()) {
                    dir.mkdir();
                }
                if (file.createNewFile()) {
                    byte[] buffer = new byte[1024];
                    os = new FileOutputStream(file);
                    int count = -1;
                    while ((count = is.read(buffer)) != -1) {
                        os.write(buffer, 0, count);
                    }
                    copyIsFinish = true;
                }
            } catch (IOException e1) {
            } finally {
                try {
                    if (os != null) {
                        os.close();
                        os = null;
                    }
                    if (is != null) {
                        is.close();
                        is = null;
                    }
                } catch (IOException e1) {
                }
            }
        } else {
            copyIsFinish = true;
        }
        return copyIsFinish;
    }

    public static void installNewApp(Context context, String packageName) {
        String apkFilePath = new StringBuilder(Environment
                .getExternalStorageDirectory().getAbsolutePath())
                .append(File.separator).append(getCommonFolderName(context))
                .append(File.separator).append(packageName).append(".apk").toString();
        File file = new File(apkFilePath);
        if (file != null && file.length() > 0 && file.exists() && file.isFile()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse("file://" + apkFilePath), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    public static boolean isAssetsAppsExists(Context context, String packageName) {
        try {
            String[] names = context.getAssets().list("apps");
            for (int i = 0; i < names.length; i++) {
                if (names[i].equals(packageName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            //
        }
        return false;
    }

    public static String getCommonFolderName(Context context) {
        if (sCommonFolderName == null) {
            sCommonFolderName = context.getPackageName();
        }
        return sCommonFolderName;
    }

    public static int isContain(int[] ints, int i) {
        for (int i1 = 0; i1 < ints.length; i1++) {
            if (ints[i1] == i) {
                return i1;
            }
        }
        return -1;
    }

    public static Integer getSystemPropertiesInt(Context context, String key, int def) {
        Integer ret = def;
        try {
            ClassLoader cl = context.getClassLoader();
            Class SystemProperties = cl.loadClass("android.os.SystemProperties");

            Class[] paramTypes = new Class[2];
            paramTypes[0] = String.class;
            paramTypes[1] = int.class;

            Method getInt = SystemProperties.getMethod("getInt", paramTypes);

            Object[] params = new Object[2];
            params[0] = new String(key);
            params[1] = new Integer(def);

            ret = (Integer) getInt.invoke(SystemProperties, params);
        } catch (Exception e) {
            ret = def;
        }
        return ret;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static boolean isRtl(Resources res) {
        return ATLEAST_JB_MR1 &&
                (res.getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);
    }

    /**
     * Trims the string, removing all whitespace at the beginning and end of the string.
     * Non-breaking whitespaces are also removed.
     */
    public static String trim(CharSequence s) {
        if (s == null) {
            return null;
        }

        // Just strip any sequence of whitespace or java space characters from the beginning and end
        Matcher m = sTrimPattern.matcher(s);
        return m.replaceAll("$1");
    }

    public static float getDescendantCoordRelativeToParent(View descendant, View root,
                                                           int[] coord, boolean includeRootScroll) {
        ArrayList<View> ancestorChain = new ArrayList<View>();

        float[] pt = {coord[0], coord[1]};

        View v = descendant;
        while(v != root && v != null) {
            ancestorChain.add(v);
            v = (View) v.getParent();
        }
        ancestorChain.add(root);

        float scale = 1.0f;
        int count = ancestorChain.size();
        for (int i = 0; i < count; i++) {
            View v0 = ancestorChain.get(i);
            // For TextViews, scroll has a meaning which relates to the text position
            // which is very strange... ignore the scroll.
            if (v0 != descendant || includeRootScroll) {
                pt[0] -= v0.getScrollX();
                pt[1] -= v0.getScrollY();
            }

            v0.getMatrix().mapPoints(pt);
            pt[0] += v0.getLeft();
            pt[1] += v0.getTop();
            scale *= v0.getScaleX();
        }

        coord[0] = Math.round(pt[0]);
        coord[1] = Math.round(pt[1]);
        return scale;
    }
}
