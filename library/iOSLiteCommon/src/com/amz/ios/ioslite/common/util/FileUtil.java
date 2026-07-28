package com.amz.ios.ioslite.common.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

/**
 * Common access package files;
 */
public class FileUtil {
    private static final String TAG = "FileUtil";

    private static final String EXTERNAL_ROOT_FOLDER_DIR = "IOSLite";
    private static final String LOG_FILE_DIR_NAME = "Logs";
    private static final String TRACE_FILE_DIR_NAME = "Traces";

    private static File rootFilesDir;
    private static File logFilesDir;
    private static File traceFilesDir;

    /**
     * 获取外部存储缓存路径
     */
    public static File getDiskCacheDir(Context context, String fileName) {
        String cachePath;
        try {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                    || !Environment.isExternalStorageRemovable()) {
                cachePath = context.getExternalCacheDir().getPath();
            } else {
                cachePath = context.getCacheDir().getPath();
            }
        } catch (Exception e) {
            cachePath = context.getFilesDir().getPath();
        }
        return new File(cachePath + File.separator + fileName);
    }

    /**
     * IOS外部存储文件夹根目录
     */
    public static File getRootFilesDir() {
        if (rootFilesDir == null) {
            StringBuffer path = new StringBuffer();
            path.append(Environment.getExternalStorageDirectory().getAbsolutePath()).append(File.separator).append(EXTERNAL_ROOT_FOLDER_DIR);
            rootFilesDir = new File(path.toString());
        }

        if (!rootFilesDir.exists()) {
            rootFilesDir.mkdirs();
        }
        return rootFilesDir;
    }

    /**
     * 异常捕获路径
     */
    public static File getLogFilesDir() {
        if (logFilesDir == null) {
            logFilesDir = new File(getRootFilesDir(), LOG_FILE_DIR_NAME);
        }

        if (!logFilesDir.exists()) {
            logFilesDir.mkdirs();
        }
        return logFilesDir;
    }

    /**
     * Trace 日志
     */
    public static File getTraceFilesDir() {
        if (traceFilesDir == null) {
            traceFilesDir = new File(getRootFilesDir(), TRACE_FILE_DIR_NAME);
        }

        if (!traceFilesDir.exists()) {
            traceFilesDir.mkdirs();
        }
        return traceFilesDir;
    }

    /**
     * 文件复制
     */
    public static boolean copyFile(File inFile, File outFile) {
        if (outFile == null || inFile == null) {
            Log.w(TAG, "copy file fail , outFile or inFile is null");
            return false;
        }

        boolean isOk = true;
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            if (outFile.exists()) {
                outFile.delete();
            }
            outFile.createNewFile();

            inChannel = new FileInputStream(inFile).getChannel();// 只读
            outChannel = new FileOutputStream(outFile).getChannel();// 只写
            long size = inChannel.transferTo(0, inChannel.size(), outChannel);
            if (size <= 0) {
                isOk = false;
            }
        } catch (Exception e) {
            isOk = false;
            Log.e(TAG, "copy file fail : " + outFile.getName(), e);
        } finally {
            CommonUtilities.close(inChannel);
            CommonUtilities.close(outChannel);
        }
        return isOk;
    }


    /**
     * 删除文件夹
     */
    public static void deleteDirectory(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            for (File item : directory.listFiles()) {
                item.delete();
            }
        }
    }
}
