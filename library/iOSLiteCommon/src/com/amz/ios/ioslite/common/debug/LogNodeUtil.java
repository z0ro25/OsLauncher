package com.amz.ios.ioslite.common.debug;

import android.util.Log;

/**
 * Helper class for a list (or tree) of LoggerNodes.
 * <p/>
 * <p>When this is set as the head of the list,
 * an instance of it can function as a drop-in replacement for {@link android.util.Log}.
 * Most of the methods in this class server only to map a method call in Log to its equivalent
 * in LogNode.</p>
 */
public class LogNodeUtil {
    // Grabbing the native values from Android's native logging facilities,
    // to make for easy migration and interop.
    public static final int NONE = -1;
    public static final int VERBOSE = android.util.Log.VERBOSE;
    public static final int DEBUG = android.util.Log.DEBUG;
    public static final int INFO = android.util.Log.INFO;
    public static final int WARN = android.util.Log.WARN;
    public static final int ERROR = android.util.Log.ERROR;

    // Stores the beginning of the LogNode topology.
    private static LogNode mLogNode;

    /**
     * Returns the next LogNode in the linked list.
     */
    public static LogNode getLogNode() {
        return mLogNode;
    }

    /**
     * Sets the LogNode data will be sent to.
     */
    public static void setLogNode(LogNode node) {
        mLogNode = node;
    }

    public static void println(int priority, String tag, String msg, Throwable tr) {
        // If this isn't the last node in the chain, move things along.
        if (mLogNode != null) {
            mLogNode.println(priority, tag, msg, tr);
        }

    }


    public static void println(int priority, String tag, String msg) {
        println(priority, tag, msg, null);
    }


    public static void v(String tag, String msg, Throwable tr) {
        println(VERBOSE, tag, msg, tr);
    }


    public static void v(String tag, String msg) {
        v(tag, msg, null);
    }


    public static void d(String tag, String msg, Throwable tr) {
        println(DEBUG, tag, msg, tr);
    }


    public static void d(String tag, String msg) {
        d(tag, msg, null);
    }


    public static void i(String tag, String msg, Throwable tr) {
        println(INFO, tag, msg, tr);
    }


    public static void i(String tag, String msg) {
        i(tag, msg, null);
    }


    public static void w(String tag, String msg, Throwable tr) {
        println(WARN, tag, msg, tr);
    }


    public static void w(String tag, String msg) {
        w(tag, msg, null);
    }


    public static void w(String tag, Throwable tr) {
        w(tag, null, tr);
    }


    public static void e(String tag, String msg, Throwable tr) {
        println(ERROR, tag, msg, tr);
    }


    public static void e(String tag, String msg) {
        e(tag, msg, null);
    }

}