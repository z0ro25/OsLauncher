package com.amz.ios.themeclub.util;

import android.text.TextUtils;
import android.util.Log;

import com.amz.ios.themeclub.app.AppConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Created by ZhangMingZhe on 11/18/16.
 */

public class CacheUtil {
    //读写缓存类
    public static void saveCache(String data, String cacheName) {
        if (TextUtils.isEmpty(data))
            return;
        String myPath;
        String sdPath = AppUtils.getSDPath();
        File myFile;
        if (TextUtils.isEmpty(sdPath))
            return;

        File downloadFile = new File(sdPath + AppConfig.CACHE_PATH);
        if (!downloadFile.isDirectory()) {
            downloadFile.delete();
            downloadFile.mkdirs();
        }
        myPath = sdPath +  AppConfig.CACHE_PATH ;

        myFile = new File(myPath);
        if (!myFile.exists()) {
            myFile.mkdirs();
        }

        OutputStreamWriter osw = null;
        try {
            osw = new OutputStreamWriter(new FileOutputStream(myPath + cacheName));
            osw.write(data, 0, data.length());
            osw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (osw != null)
                    osw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getCache(String cacheName) {
        String result = "";
        String data = null;
        BufferedReader br = null;
        String myPath;
        String sdPath = AppUtils.getSDPath();
        File myFile;
        if (TextUtils.isEmpty(sdPath))
            return result;

        myPath = sdPath + AppConfig.CACHE_PATH;
        Log.e("path",myPath);

        myFile = new File(myPath);
        if (!myFile.exists()) {
            myFile.mkdirs();
        }
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(myPath + cacheName)));
            while ((data = br.readLine()) != null) {
                result = result + data;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
