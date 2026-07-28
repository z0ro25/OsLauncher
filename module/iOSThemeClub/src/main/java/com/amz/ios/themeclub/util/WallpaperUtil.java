package com.amz.ios.themeclub.util;


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by lideqian on 16-11-17.
 */
public class WallpaperUtil {

    public static final String TAG = "WallpaperUtil";
    public static final String THEME_DESCRIPTION_PATH = "description.xml";
    public static final String WALLPAPER_NEED_UPDATE = "com.amz.ios.themeclub.wallpaprneedupdate";
    public static final String FOLDERPATH = "/themes/";
    public static final String ONLINEWALLPAPERBEAN = "wallpaperbean";
    public static final String ONLINEWALLPAPERLIST = "wallpaperbeans";
    public static final String ONLINEWALLPAPER_POSITION = "wallpaperposition";
    public static final String THEMECLUB_TYPE = "themeclubtype";
    public static final String LOCALWALLPAPER_POSITION = "localwallpaperposion";
    public static final String LOCALWALLPAPER_PATHS = "localwallpaperpaths";
    public static String DOWNLOAD_WALLPAPER_FOLDER = AppUtils.getSDPath() + "/themes/defaultwallpapers/";
    public static final boolean ATLEAST_N = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;

    public static boolean saveBitmap(Bitmap data, String fileName) {
        File saveFile = null;
        String sdPath = AppUtils.getSDPath();
        if (TextUtils.isEmpty(sdPath) || data == null)
            return false;
        String savePath = sdPath + FOLDERPATH + "download/";
        saveFile = new File(savePath);
        if (saveFile != null && !saveFile.exists()) {
            saveFile.mkdirs();
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(savePath + fileName);
            data.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean saveImageToGallery(Context context, Bitmap bmp, String fileName) {

        File saveFile = null;
        String sdPath = AppUtils.getSDPath();
        if (TextUtils.isEmpty(sdPath) || bmp == null)
            return false;
        String savePath = sdPath + FOLDERPATH + "download/";
        String path = savePath + fileName;
        saveFile = new File(savePath);
        if (!saveFile.exists()) {
            if (saveFile.mkdirs()) {
                Log.e(TAG, "mkdirs" + "====download + " + savePath);
            } else {
                Log.e(TAG, "mkdirs  fail "  + "====download + " + savePath);
            }

        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(savePath + fileName);
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        try {
            File file = new File(savePath,fileName);
            if(!file.exists()) {
                return false;
            }
  //        saveToGalleryStore(context,bmp,savePath);
//          MediaStore.Images.Media.insertImage(context.getContentResolver(),
//                        file.getAbsolutePath(), fileName, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + path)));
        return true;
    }

    public static boolean saveToGalleryStore(Context context,Bitmap image,String path) {
        Resources r = context.getResources();

        try {
            // media provider uses seconds for DATE_MODIFIED and DATE_ADDED, but milliseconds
            // for DATE_TAKEN
            // Save the screenshot to the MediaStore
            ContentValues values = new ContentValues();
            ContentResolver resolver = context.getContentResolver();
            values.put(MediaStore.Images.ImageColumns.DATE_TAKEN,System.currentTimeMillis());
            values.put(MediaStore.Images.ImageColumns.DATE_ADDED, System.currentTimeMillis());
            values.put(MediaStore.Images.ImageColumns.DATE_MODIFIED,System.currentTimeMillis());
            values.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.ImageColumns.WIDTH,image.getWidth());
            values.put(MediaStore.Images.ImageColumns.HEIGHT,image.getHeight());
            Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            OutputStream out = resolver.openOutputStream(uri);
            image.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            // update file size in the database
            values.clear();
            values.put(MediaStore.Images.ImageColumns.SIZE, new File(path).length());
            resolver.update(uri, values, null, null);
            return true;
        } catch (Exception e) {

        }
        return false;
    }

    public static Pair<Integer, Integer> getWallpaperExpectedSize(Context context,Boolean isScrool) {
        DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
        int scrWidth = dm.widthPixels;
        int scrHeight = dm.heightPixels;
        if(isScrool) {
            scrWidth *=2;
        }
        return new Pair<Integer, Integer>(scrWidth, scrHeight);
    }


    public static void copyPicture(final String oldPath,final String newPath) {
        try {
            int byteRead;
            File oldFile = new File(oldPath);
            if (oldFile.exists()) {
                InputStream inStream = new FileInputStream(oldPath);
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1024];
                while ( (byteRead = inStream.read(buffer)) != -1) {
                    fs.write(buffer, 0, byteRead);
                }
                inStream.close();
                fs.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
