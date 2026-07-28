package com.amz.ios.launcher.awareness;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.AsyncTask;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Xml;

import com.amz.ios.ioslite.common.ContextHelper;
import com.amz.ios.ioslite.common.debug.DebugUtil;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.ota.IOSOtaHandler;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

class UnreadSupportShortcut {
    public UnreadSupportShortcut(String pkgName, String clsName, String keyString, int type) {
        mComponent = new ComponentName(pkgName, clsName);
        mKey = keyString;
        mShortcutType = type;
        mUnreadNum = 0;
    }

    ComponentName mComponent;
    String mKey;
    int mShortcutType;
    int mUnreadNum;

    @Override
    public String toString() {
        return "{UnreadSupportShortcut[" + mComponent + "], key = " + mKey + ",type = "
                + mShortcutType + ",unreadNum = " + mUnreadNum + "}";
    }
}

/**
 * M: This class is a util class, implemented to do the following two things,:
 * <p/>
 * 1.Read config xml to get the shortcuts which support displaying unread number,
 * then get the initial value of the unread number of each component and update
 * shortcuts and folders through callbacks implemented in Launcher.
 * <p/>
 * 2. Receive unread broadcast sent by application, update shortcuts and folders in
 * workspace, hot seat and update application icons in app customize paged view.
 */
public class MTKUnreadLoader extends UnreadLoaderCompact {
    private static final String TAG = "MTKUnreadLoader";
    private static final String TAG_UNREADSHORTCUTS = "unreadshortcuts";

    public static final String ACTION_UNREAD_CHANGED = "com.mediatek.action.UNREAD_CHANGED";
    public static final String EXTRA_UNREAD_COMPONENT = "com.mediatek.intent.extra.UNREAD_COMPONENT";
    public static final String EXTRA_UNREAD_NUMBER = "com.mediatek.intent.extra.UNREAD_NUMBER";

    private static final ArrayList<UnreadSupportShortcut> UNREAD_SUPPORT_SHORTCUTS =
            new ArrayList<UnreadSupportShortcut>();

    private static int sUnreadSupportShortcutsNum = 0;
    private static final Object LOG_LOCK = new Object();

    private Context mContext;
    private WeakReference<UnreadCallbacks> mCallbacks;
    private boolean isInited;

    public MTKUnreadLoader(Context context) {
        mContext = context;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ACTION_UNREAD_CHANGED.equals(action)) {
                ComponentName componentName = intent
                        .getParcelableExtra(EXTRA_UNREAD_COMPONENT);
                final int unreadNum = intent.getIntExtra(EXTRA_UNREAD_NUMBER, -1);
                DebugUtil.debugUnread(TAG, "Receive unread broadcast: componentName = " + componentName
                        + ", unreadNum = " + unreadNum + ", mCallbacks = " + mCallbacks
                        + getUnreadSupportShortcutInfo());

                if (mCallbacks != null && componentName != null && unreadNum != -1) {
                    final int index = supportUnreadFeature(componentName);
                    if (index >= 0) {
                        boolean ret = setUnreadNumberAt(index, unreadNum);
                        if (ret) {
                            final UnreadCallbacks callbacks = mCallbacks.get();
                            if (callbacks != null) {
                                if (IOSOtaHandler.ENABLE_OTA_2_FEATURE) {
                                    ComponentName replacedComp = IOSOtaHandler.getAppEntryRepalced(mContext, componentName.getPackageName());
                                    if (replacedComp != null) {
                                        componentName = replacedComp;
                                    }
                                }
                                callbacks.bindComponentUnreadChanged(componentName, unreadNum);
                            }
                        }
                    }
                }
            }
        }
    };


    public void initInitFlag() {
        isInited = false;
    }
    /**
     * Set this as the current Launcher activity object for the loader.
     */
    public void initialize(Launcher launcher, UnreadCallbacks callbacks) {
        if (!isInited) {
            // Register unread change broadcast.
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_UNREAD_CHANGED);
            ContextHelper.registerReceiver(launcher, mReceiver, filter);
            mCallbacks = new WeakReference<UnreadCallbacks>(callbacks);
            DebugUtil.debugUnread(TAG, "initialize: callbacks = " + callbacks
                    + ", mCallbacks = " + mCallbacks);
            isInited = true;
        }
    }

    /**
     * Load and initialize unread shortcuts.
     *
     * @param context
     */
    public void loadAndInitUnreadShortcuts() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... unused) {
                loadUnreadSupportShortcuts();
                initUnreadNumberFromSystem();
                return null;
            }

            @Override
            protected void onPostExecute(final Void result) {
                if (mCallbacks != null) {
                    UnreadCallbacks callbacks = mCallbacks.get();
                    if (callbacks != null) {
                        callbacks.bindUnreadInfoIfNeeded();
                    }
                }
            }
        }.execute();
    }

    @Override
    public void onCancel(Launcher launcher) {
        if (isInited) {
            mCallbacks = new WeakReference<UnreadCallbacks>(null);
            try {
                launcher.unregisterReceiver(mReceiver);
            }catch (Exception e) {
                e.printStackTrace();
            }
            isInited = false;
        }
    }

    /**
     * Get unread number for the given component.
     *
     * @param component
     * @return
     */
    int getUnreadNumberOfComp(ComponentName component) {
        final int index = supportUnreadFeature(component);
        return getUnreadNumberAt(index);
    }

    /**
     * Initialize unread number by querying system settings provider.
     *
     * @param context
     */
    private void initUnreadNumberFromSystem() {
        final ContentResolver cr = mContext.getContentResolver();
        final int shortcutsNum = sUnreadSupportShortcutsNum;
        UnreadSupportShortcut shortcut = null;
        for (int i = 0; i < shortcutsNum; i++) {
            shortcut = UNREAD_SUPPORT_SHORTCUTS.get(i);
            try {
                shortcut.mUnreadNum = Settings.System.getInt(cr, shortcut.mKey);
                DebugUtil.debugUnread(TAG, "initUnreadNumberFromSystem: key = " + shortcut.mKey
                        + ", unreadNum = " + shortcut.mUnreadNum);
            } catch (Settings.SettingNotFoundException e) {
            }
        }
        DebugUtil.debugUnread(TAG, "initUnreadNumberFromSystem end:" + getUnreadSupportShortcutInfo());
    }


    private void loadUnreadSupportShortcuts() {
        long start = System.currentTimeMillis();
        DebugUtil.debugUnread(TAG, "loadUnreadSupportShortcuts begin: start = " + start);

        // Clear all previous parsed unread shortcuts.
        UNREAD_SUPPORT_SHORTCUTS.clear();

        try {
            XmlResourceParser parser = mContext.getResources().getXml(
                    R.xml.mtk_unread_support_shortcuts);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            beginDocument(parser, TAG_UNREADSHORTCUTS);

            final int depth = parser.getDepth();

            int type = -1;
            while (((type = parser.next()) != XmlPullParser.END_TAG || parser.getDepth() > depth)
                    && type != XmlPullParser.END_DOCUMENT) {

                if (type != XmlPullParser.START_TAG) {
                    continue;
                }

                TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.UnreadShortcut);
                synchronized (LOG_LOCK) {
                    String pkgName = a.getString(R.styleable.UnreadShortcut_unreadPackageName);
                    String clsName = a.getString(R.styleable.UnreadShortcut_unreadClassName);
                    String keyString = a.getString(R.styleable.UnreadShortcut_unreadKey);
                    int shortcutType = a.getInt(R.styleable.UnreadShortcut_unreadType, 0);
                    if (pkgName != null && clsName != null) {
                        UNREAD_SUPPORT_SHORTCUTS.add(new UnreadSupportShortcut(pkgName, clsName, keyString, shortcutType));
                    }
                }
                a.recycle();

            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        sUnreadSupportShortcutsNum = UNREAD_SUPPORT_SHORTCUTS.size();
        DebugUtil.debugUnread(TAG, "loadUnreadSupportShortcuts end: time used = "
                + (System.currentTimeMillis() - start) + ",sUnreadSupportShortcutsNum = "
                + sUnreadSupportShortcutsNum + getUnreadSupportShortcutInfo());
    }


    private static final void beginDocument(XmlPullParser parser, String firstElementName)
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

    /**
     * Get unread support shortcut information, since the information are stored
     * in an array list, we may query it and modify it at the same time, a lock
     * is needed.
     *
     * @return
     */
    private static String getUnreadSupportShortcutInfo() {
        String info = " Unread support shortcuts are ";
        synchronized (LOG_LOCK) {
            info += UNREAD_SUPPORT_SHORTCUTS.toString();
        }
        return info;
    }

    /**
     * Whether the given component support unread feature.
     *
     * @param component
     * @return
     */
    private static int supportUnreadFeature(ComponentName component) {
        DebugUtil.debugUnread(TAG, "supportUnreadFeature: component = " + component);
        if (component == null) {
            return -1;
        }

        final int size = UNREAD_SUPPORT_SHORTCUTS.size();
        for (int i = 0, sz = size; i < sz; i++) {
            ComponentName cn = UNREAD_SUPPORT_SHORTCUTS.get(i).mComponent;
            String pkgName = IOSOtaHandler.getHideAppPackageName(LauncherAppState.getInstance().getContext(), component.getClassName());

            if (cn.equals(component) || (pkgName != null && pkgName.equals(cn.getPackageName()))) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Set the unread number of the item in the list with the given unread number.
     *
     * @param index
     * @param unreadNum
     * @return
     */
    private static synchronized boolean setUnreadNumberAt(int index, int unreadNum) {
        if (index >= 0 || index < sUnreadSupportShortcutsNum) {
            DebugUtil.debugUnread(TAG, "setUnreadNumberAt: index = " + index
                    + ",unreadNum = " + unreadNum + getUnreadSupportShortcutInfo());
            if (UNREAD_SUPPORT_SHORTCUTS.get(index).mUnreadNum != unreadNum) {
                UNREAD_SUPPORT_SHORTCUTS.get(index).mUnreadNum = unreadNum;
                return true;
            }
        }
        return false;
    }

    /**
     * Get unread number of application at the given position in the supported
     * shortcut list.
     *
     * @param index
     * @return
     */
    private static synchronized int getUnreadNumberAt(int index) {
        if (index < 0 || index >= sUnreadSupportShortcutsNum) {
            return 0;
        }
        DebugUtil.debugUnread(TAG, "getUnreadNumberAt: index = " + index
                + getUnreadSupportShortcutInfo());
        return UNREAD_SUPPORT_SHORTCUTS.get(index).mUnreadNum;
    }
}
