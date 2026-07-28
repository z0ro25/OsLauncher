package com.amz.ios.ioslite.common.debug;

import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.amz.ios.ioslite.common.util.BuildUtil;
import com.amz.ios.ioslite.common.util.FileUtil;
import com.amz.ios.ioslite.common.util.IOSToastUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Applicaton uncaughtExceptionHandler;
 * Dump crash ex to locat txt file  (@link FileUtil.getLogFilesDir() )
 */
public class ExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "ExceptionHandler";

    private static Context sContext;
    private static ExceptionHandler INSTANCE;
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    static Date sDateStamp = new Date();
    static DateFormat sDateFormat =
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    static long sRunStart = System.currentTimeMillis();

    static final ArrayList<String> sDumpLogs = new ArrayList<String>();


    private ExceptionHandler() {
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public static void initalize(Application context) {
        setApplicationContext(context);
        getInstance();
    }


    private static ExceptionHandler getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ExceptionHandler();
        }
        return INSTANCE;
    }


    private static void setApplicationContext(Context context) {
        if (sContext != null) {
            Log.w(TAG, "setApplicationContext called twice! old=" + sContext + " new=" + context);
        }
        sContext = context.getApplicationContext();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            // if not handle exception, exit program;
            // app will crash ,so wait a second to dump log;
            SystemClock.sleep(1000);
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }

        if (BuildUtil.DEBUG) {
            // Show exception in toast;
            new Thread() {
                @Override
                public void run() {
                    Looper.prepare();
                    IOSToastUtil.showToast(sContext, "Sorry, Fatal Exception Occur !", 0, Toast.LENGTH_LONG);
                    Looper.loop();
                }
            }.start();
        }

        addDumpLog(TAG, "handleException", ex);
        return true;
    }

    public static void addDumpLog(String tag, String log, Throwable e) {
        if (e != null) {
            Log.e(tag, log, e);
        } else {
            Log.e(tag, log);
        }

        sDateStamp.setTime(System.currentTimeMillis());
        synchronized (sDumpLogs) {
            sDumpLogs.add(sDateFormat.format(sDateStamp) + ": " + tag + ", " + log
                    + (e == null ? "" : (", Throwable: " + e + "\n" + getDetailInfo(e))));
        }

        dumpLogsToLocalData();
    }

    public static void addDumpLog(String tag, String log) {
        addDumpLog(tag, log, null);
    }

    static class ApplyDumpLogsToLocalDataTask extends AsyncTask<Void, Void, Void> {

        Context mContext;
        public ApplyDumpLogsToLocalDataTask(Context context) {
            mContext = context;
        }

        @Override
        public Void doInBackground(Void... args) {
            Log.w(TAG, "dumpLogsToLocalData start ");
            sDateStamp.setTime(sRunStart);
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
            String FILENAME = formatter.format(sDateStamp) + "_"
                    + BuildUtil.getIOSVersionCode(mContext)
                    + ".txt";

            FileOutputStream fos = null;
            File outFile = null;
            try {
                outFile = new File(FileUtil.getLogFilesDir(), FILENAME);
                outFile.createNewFile();
                fos = new FileOutputStream(outFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (fos != null) {
                PrintWriter writer = new PrintWriter(fos);

                writer.println(" ");
                writer.println("Debug logs: ");
                synchronized (sDumpLogs) {
                    for (int i = 0; i < sDumpLogs.size(); i++) {
                        writer.println("  " + sDumpLogs.get(i));
                    }
                }
                writer.close();
            }
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.w(TAG, "dumpLogsToLocalData complete ");
            return null;
        }

    }
    public static void dumpLogsToLocalData() {

        new ApplyDumpLogsToLocalDataTask(sContext).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
//        new AsyncTask<Void, Void, Void>() {
//            @Override
//            public Void doInBackground(Void... args) {
//                Log.w(TAG, "dumpLogsToLocalData start ");
//                sDateStamp.setTime(sRunStart);
//                SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
//                String FILENAME = formatter.format(sDateStamp) + "_"
//                        + BuildUtil.getIOSVersionCode(sContext)
//                        + ".txt";
//
//                FileOutputStream fos = null;
//                File outFile = null;
//                try {
//                    outFile = new File(FileUtil.getLogFilesDir(), FILENAME);
//                    outFile.createNewFile();
//                    fos = new FileOutputStream(outFile);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                if (fos != null) {
//                    PrintWriter writer = new PrintWriter(fos);
//
//                    writer.println(" ");
//                    writer.println("Debug logs: ");
//                    synchronized (sDumpLogs) {
//                        for (int i = 0; i < sDumpLogs.size(); i++) {
//                            writer.println("  " + sDumpLogs.get(i));
//                        }
//                    }
//                    writer.close();
//                }
//                try {
//                    if (fos != null) {
//                        fos.close();
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                Log.w(TAG, "dumpLogsToLocalData complete ");
//                return null;
//            }
//        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);

    }

    private static String getDetailInfo(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        builder.append("Cause : \n" + throwable.getCause() + "\n");
        builder.append("Message : \n" + throwable.getMessage() + "\n");
        builder.append("getLocalizedMessage : \n" + throwable.getLocalizedMessage() + "\n");
        StackTraceElement[] stackElements = throwable.getStackTrace();
        if (stackElements != null) {
            for (int i = 0; i < stackElements.length; i++) {
                builder.append(stackElements[i].getClassName() + "\t");
                builder.append(stackElements[i].getFileName() + "\t");
                builder.append(stackElements[i].getLineNumber() + "\t");
                builder.append(stackElements[i].getMethodName() + "\n");
            }
        }
        return builder.toString();
    }


}
