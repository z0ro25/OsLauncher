package com.zhuoyi.security.soft.lock;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ios.sc.common.db.softlock.SL_LocksoftDatabaseHelp;
import com.ios.sc.common.logs.SL_Log;
import com.ios.sc.common.utils.C_C_Util;
import com.ios.sc.common.utils.C_SC_Service_Communication;
import com.zhuoyi.security.batterysave.R;
import com.zhuoyi.security.batterysave.util.SL_Util;
import com.zhuoyi.security.batterysave.views.SL_FloatView;

import java.util.ArrayList;
import java.util.List;

public class SL_LockSoftService extends Service implements View.OnClickListener {

    private Context mContext = null;

    /* softlock by txh start */
    private CheckSoftLockTast checkSoftLock = null;
    private String pkgActivityName = "";
    private ArrayList<SL_LockSoftInfo> list = new ArrayList<SL_LockSoftInfo>();
    SL_FloatView mFloatLayout;
    WindowManager.LayoutParams wmParams;
    LayoutInflater inflater = null;
    WindowManager mWindowManager;
    private ImageButton num0, num1, num2, num3, num4, num5, num6, num7, num8, num9;
    private ImageButton numReinput;
    private ImageButton numBack;
    private EditText etLockviewPwd;
    private TextView etLockview;
    private SharedPreferences settingsLock;
    SL_WatcherHomeKey mWatcherHomeKey = null;
    private boolean isFirst = true;
    private LinearLayout fingerLay;

    private final int MSG_ADD_FLOAT_WINDOW = 92002;
    private final int MSG_LOCK_SUCCESS = MSG_ADD_FLOAT_WINDOW + 1;
    private final int MSG_KEY_REMOVE = MSG_LOCK_SUCCESS + 1;
    private boolean isSystemPrivApp = false;

    /* softlock by txh end */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = this;
        isSystemPrivApp = (mContext.getPackageManager().checkPermission("android.permission.INSTALL_PACKAGES", getPackageName()) == PackageManager.PERMISSION_GRANTED);

        toRegisterCommonReceiver();

        mWatcherHomeKey = new SL_WatcherHomeKey(mContext.getApplicationContext());
        mWatcherHomeKey.setOnHomePressedListener(myOnHomePressedListener);
        list = SL_Util.getLockList(mContext);
        stateLockSetting = getLockState(mContext);
        if (stateLockSetting && (list.size() > 0)) {
            checkSoftLock = new CheckSoftLockTast();
            checkSoftLock.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Log.d("SL_Log", "onStartCommand() intent = " + intent.getAction());
        }
        init(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    synchronized void init(Intent intent) {

        if (null != intent) {
            int operation_id = intent.getIntExtra(C_SC_Service_Communication.KEY_OPERATION, 0);
            switch (operation_id) {
                case C_SC_Service_Communication.SOFT_LOCK_LIST_UPDATE_DATA:/*soft lock start*/
                    list.clear();
                    list = SL_Util.getLockList(mContext);
                    setSoftLockAsyncTaskState(1);
                    break;
                case C_SC_Service_Communication.SOFT_LOCK_STATA_UPDATE:/*soft lock end*/
                    String pkgName = intent.getStringExtra("slPkgName");
                    if (!TextUtils.isEmpty(pkgName)) {
                        for (SL_LockSoftInfo ls : list) {
                            if (pkgName.equals(ls.getPackageName())) {
                                ls.setNeedLock("false");
                                break;
                            }
                        }
                    }
                    break;
                case C_SC_Service_Communication.SOFT_LOCK_SWITCH:
                    setSoftLockAsyncTaskState(1);
                    break;
            }
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ADD_FLOAT_WINDOW:
                    if (isFirst) {
                        createFloatView();
                    } else {
                        SL_Util.reInputEditText(etLockviewPwd);
                    }
                    etLockview.setText(R.string.sl_enter_psw_16);
                    fingerLay.setVisibility(View.GONE);
                    mWatcherHomeKey.startWatch();
                    isAddView(true);
                    isFirst = false;
                    break;
                case MSG_LOCK_SUCCESS:
                    Intent myIntent = C_SC_Service_Communication.getServiceIntent(C_SC_Service_Communication.SOFT_LOCK_STATA_UPDATE);
                    myIntent.putExtra("slPkgName", pkgActivityName);
                    C_SC_Service_Communication.startServiceForIntent(mContext, myIntent);
                    isAddView(false);
                    mWatcherHomeKey.stopWatch();
                    SL_LocksoftDatabaseHelp.setLastPackName(mContext, pkgActivityName + ",true");
                    break;
                case MSG_KEY_REMOVE:
                    isAddView(false);
                    mWatcherHomeKey.stopWatch();
                    break;
            }
        }
    };

    /* softlock by txh start */
    private void setSoftLockAsyncTaskState(int state) {
        if (state == 0) {
            if (checkSoftLock != null && checkSoftLock.getStatus() != AsyncTask.Status.FINISHED) {
                checkSoftLock.cancel(true);
                checkSoftLock = null;
            }
        } else {
            stateLockSetting = getLockState(mContext);
            if (stateLockSetting && (list.size() > 0)) {
                if (checkSoftLock == null) {
                    checkSoftLock = new CheckSoftLockTast();
                    checkSoftLock.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            } else {
                if (checkSoftLock != null && checkSoftLock.getStatus() != AsyncTask.Status.FINISHED) {
                    checkSoftLock.cancel(true);
                    checkSoftLock = null;
                }
            }
        }
    }

    boolean stateLockSetting = false;

    class CheckSoftLockTast extends AsyncTask<Object, Object, String> {
        private String lastTopPkg = null;
        private String lastTopAct = null;

        protected String doInBackground(Object... arg0) {
            SL_LockSoftInfo ls = null;
            String pkg;
            String top_pkg = "";
            String top_activity = "";
            while (true) {

                if (isCancelled()) {
                    break;
                }

                if (isSystemPrivApp || !C_C_Util.isAndroidSdk_api_21_plus()) {
                    ComponentName info = getTopActivity();
                    top_pkg = info.getPackageName();
                    top_activity = info.getClassName();
                } else {
                    top_pkg = SL_Util.getTopPackageNameFor21(mContext);
                }
                Log.e("SL_Log", "top_pkg = " + top_pkg);
                lastTopAct = top_activity;
                String lastPkgName = SL_LocksoftDatabaseHelp.getLastPackName(mContext);
                String[] temp = lastPkgName.split(",");
                if (TextUtils.isEmpty(top_pkg) || (lastTopPkg != null && top_pkg.equals(lastTopPkg)) || ((temp.length >= 2) && ("true").equals(temp[1]) && (top_pkg.equals(temp[0])))) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        //e.printStackTrace();
                        SL_Log.logII("CheckSoftLockTast doInBackground() InterruptedException: " + e.getMessage());
                    }
                    continue;
                }

                lastTopPkg = top_pkg;
                SL_LocksoftDatabaseHelp.setLastPackName(mContext, lastTopPkg + ",false");
                if (!top_activity.equals("com.tencent.security.locksoft.UnLocksoftListActivity") && !top_activity.equals("com.tencent.security.locksoft.LockSettingActivity")) {
                    ls = isExsit(top_pkg);

                    if (null != ls && (!TextUtils.isEmpty(ls.getNeedLock()) && ls.getNeedLock().equals("true"))) {
                        pkg = ls.getPackageName();
                        // modify by swang . movieAct can't be locked .
                        // 2013-03-30
                        //Log.d("SL_Log","LockSoft intentTOLockScree");
                        if (!top_activity.equals("com.android.gallery3d.app.MovieActivity")) {
                            pkgActivityName = pkg;
                            intentTOLockScreen(pkg);
                        }
                    } else if (null == ls && SL_FloatView.addViewState) {
                        /*if (mFloatLayout != null) {
                            try {
                                mWindowManager.removeView(mFloatLayout);
                            } catch (Exception e) {
                                SL_Log.logD("WindowManager.removeView err=" + e.getMessage());
                            }
                            try {
                                if (mFingerprintScenarioCallback != null) {
                                    mFingerprintScenarioCallback.stopDetectFinger();
                                }
                            } catch (Exception e) {
                                SL_Log.logD("FingerprintScenarioCallback onPause" + e.getMessage());
                            }
                            SL_FloatView.addViewState = false;
                        }*/
                    } else if (null == ls && !top_activity.equals("com.ios.sc.soft.lock.SL_EnterPassWord")) {
                        setAllToFalse();
                    }
                }

                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    SL_Log.logE("CheckSoftLockTast doInBackground err:" + e.toString());
                }
            }
            return "";
        }
    }

    private synchronized SL_LockSoftInfo isExsit(String topActivty) {
        SL_LockSoftInfo lockSoftInfo = null;
        try {
            for (SL_LockSoftInfo ls : list) {
                if (topActivty.equals(ls.getPackageName())) {
                    lockSoftInfo = ls;
                    break;
                }
            }
        } catch (Exception e) {
            SL_Log.logE("SL_LockSoftInfo err:" + e.toString());
        }
        return lockSoftInfo;
    }

    private void setAllToFalse() {
        for (SL_LockSoftInfo ls : list) {
            ls.setNeedLock("true");
        }
    }

    private synchronized ComponentName getTopActivity() {
        ActivityManager mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> tasksInfo = mActivityManager.getRunningTasks(1);
        if (null != tasksInfo && tasksInfo.size() > 0) {
            return tasksInfo.get(0).topActivity;
        }
        return null;
    }

    private void intentTOLockScreen(String packageName) {
        if (stateLockSetting) {
//            Intent mIntent = new Intent("com.ios.sc.soft.lock.SL_EnterPassWord");
//            mIntent.putExtra("packageName", packageName);
//            mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            startActivity(mIntent);
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_ADD_FLOAT_WINDOW;
            msg.arg1 = 1;
            mHandler.sendMessage(msg);
        }
    }

    public boolean getLockState(Context mCon) {
        SharedPreferences sp = mCon.getSharedPreferences("LOCK_SOFT", Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
        return sp.getBoolean("LockSwitch_button", true);
    }


    SL_WatcherHomeKey.OnHomePressedListener myOnHomePressedListener = new SL_WatcherHomeKey.OnHomePressedListener() {

        public void onHomePressed() {
            if (!C_C_Util.isFastMultipleClick() && null != mHandler) {
                mHandler.sendEmptyMessageDelayed(MSG_KEY_REMOVE, 500L);
            }
        }

        public void onHomeLongPressed() {
            //wm.removeView(view);
            //mWatcherHomeKey.stopWatch();
        }
    };

    private void createFloatView() {

        wmParams = new WindowManager.LayoutParams();
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        wmParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;

        if (C_C_Util.isAndroidSdk_api_23_plus()) {
            wmParams.type = WindowManager.LayoutParams.TYPE_TOAST;
        } else {
            wmParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        wmParams.format = PixelFormat.RGBA_8888;
        wmParams.x = 0;
        wmParams.y = 0;
        wmParams.screenOrientation = 1;
        inflater = LayoutInflater.from(mContext);
        mFloatLayout = (SL_FloatView) inflater.inflate(R.layout.sl_layout_privacy_digital_psw_enter, null);
        mFloatLayout.setPadding(0, 0, 0, 0);
        mFloatLayout.setWM(mHandler);
        wmParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        wmParams.height = WindowManager.LayoutParams.MATCH_PARENT;

        initView(mFloatLayout);
        setOnClick();

        initSharePreference();
        // back onclick
        mFloatLayout.setOnKeyListener(new OnKeyListener() {

            public boolean onKey(View v, int keyCode, KeyEvent event) {

                return false;
            }
        });
//        mWatcherHomeKey.startWatch();
//        isAddView(true);
    }

    private void initView(View view) {
        etLockviewPwd = (EditText) view.findViewById(R.id.sl_et_lockview_pwd);
        etLockview = (TextView) view.findViewById(R.id.sl_et_lockview);
        numReinput = (ImageButton) view.findViewById(R.id.sl_num_reinput);
        numBack = (ImageButton) view.findViewById(R.id.sl_num_back);
        num0 = (ImageButton) view.findViewById(R.id.sl_num0);
        num1 = (ImageButton) view.findViewById(R.id.sl_num1);
        num2 = (ImageButton) view.findViewById(R.id.sl_num2);
        num3 = (ImageButton) view.findViewById(R.id.sl_num3);
        num4 = (ImageButton) view.findViewById(R.id.sl_num4);
        num5 = (ImageButton) view.findViewById(R.id.sl_num5);
        num6 = (ImageButton) view.findViewById(R.id.sl_num6);
        num7 = (ImageButton) view.findViewById(R.id.sl_num7);
        num8 = (ImageButton) view.findViewById(R.id.sl_num8);
        num9 = (ImageButton) view.findViewById(R.id.sl_num9);
        fingerLay = (LinearLayout) view.findViewById(R.id.sl_finger_lay);
    }

    private void setOnClick() {
        etLockviewPwd.addTextChangedListener(watcher);
        num0.setOnClickListener(this);
        num1.setOnClickListener(this);
        num2.setOnClickListener(this);
        num3.setOnClickListener(this);
        num4.setOnClickListener(this);
        num5.setOnClickListener(this);
        num6.setOnClickListener(this);
        num7.setOnClickListener(this);
        num8.setOnClickListener(this);
        num9.setOnClickListener(this);
        numBack.setOnClickListener(this);
        numReinput.setOnClickListener(this);
    }

    private TextWatcher watcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            // TODO Auto-generated method stub
            int lenght = etLockviewPwd.length();
            if (lenght == 6) {
                buttonSure();
            }
        }
    };

    private void initSharePreference() {
        settingsLock = mContext.getSharedPreferences("LOCK_SOFT", 1);
    }

    @Override
    public void onClick(View v) {
        etLockview.setText(R.string.sl_enter_psw_16);
        int id = v.getId();
        if (id == R.id.sl_num0) {
            SL_Util.setEditText(etLockviewPwd, "0");
        } else if (id == R.id.sl_num1) {
            SL_Util.setEditText(etLockviewPwd, "1");
        } else if (id == R.id.sl_num2) {
            SL_Util.setEditText(etLockviewPwd, "2");
        } else if (id == R.id.sl_num3) {
            SL_Util.setEditText(etLockviewPwd, "3");
        } else if (id == R.id.sl_num4) {
            SL_Util.setEditText(etLockviewPwd, "4");
        } else if (id == R.id.sl_num5) {
            SL_Util.setEditText(etLockviewPwd, "5");
        } else if (id == R.id.sl_num6) {
            SL_Util.setEditText(etLockviewPwd, "6");
        } else if (id == R.id.sl_num7) {
            SL_Util.setEditText(etLockviewPwd, "7");
        } else if (id == R.id.sl_num8) {
            SL_Util.setEditText(etLockviewPwd, "8");
        } else if (id == R.id.sl_num9) {
            SL_Util.setEditText(etLockviewPwd, "9");
        } else if (id == R.id.sl_num_reinput) {
            SL_Util.reInputEditText(etLockviewPwd);
        } else if (id == R.id.sl_num_back) {
            SL_Util.backEditText(etLockviewPwd, etLockview);
        }
    }

    private void buttonSure() {
        String pwd = etLockviewPwd.getText().toString();
        String pwd_sure = settingsLock.getString("Pass_Word", "");
        if (!TextUtils.isEmpty(pwd)) {
            if (pwd_sure.equals(pwd)) {
                mHandler.sendEmptyMessageDelayed(MSG_LOCK_SUCCESS, 0L);
            } else {
                etLockviewPwd.setText("");
                etLockview.setText(R.string.sl_enter_psw_16_error);
            }
        } else {
            etLockview.setText(R.string.sl_enter_psw_16_empty);
        }
    }

    public void isAddView(boolean add) {
        if (add) {
            mHandler.removeMessages(MSG_KEY_REMOVE);
            if (!SL_FloatView.addViewState) {
                if (mFloatLayout != null) {
                    try {
                        mWindowManager.addView(mFloatLayout, wmParams);
                    } catch (Exception e) {
                        SL_Log.logD("mWindowManager.addView err=" + e.getMessage());
                    }
                    SL_FloatView.addViewState = true;
                }
            }
        } else {
            if (SL_FloatView.addViewState) {
                if (mFloatLayout != null) {
                    try {
                        mWindowManager.removeView(mFloatLayout);
                    } catch (Exception e) {
                        SL_Log.logD("WindowManager.removeView err=" + e.getMessage());
                    }
                    SL_FloatView.addViewState = false;
                }
            }
        }
    }
    /* softlock by txh end */

    private void toRegisterCommonReceiver() {
        IntentFilter screenOffFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(mScreenOffReceiver, screenOffFilter,RECEIVER_EXPORTED);
        }else   registerReceiver(mScreenOffReceiver, screenOffFilter);

        IntentFilter screenOnFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(mScreenOnReceiver, screenOnFilter,RECEIVER_EXPORTED);
        }else registerReceiver(mScreenOnReceiver, screenOnFilter);
    }

    private BroadcastReceiver mScreenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            /* softlock by txh start */
            setSoftLockAsyncTaskState(0);
            if (mFloatLayout != null) {
                try {
                    mWindowManager.removeView(mFloatLayout);
                } catch (Exception e) {
                    SL_Log.logD("WindowManager.removeView err=" + e.getMessage());
                }
                SL_FloatView.addViewState = false;
            }
            /* softlock by txh end */
        }
    };

    private BroadcastReceiver mScreenOnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            /* softlock by txh start */
            SL_LocksoftDatabaseHelp.setLastPackName(mContext, "");
            String topPkg = "";
            if (isSystemPrivApp || !C_C_Util.isAndroidSdk_api_21_plus()) {
                ComponentName info = getTopActivity();
                topPkg = info.getPackageName();
            } else {
                topPkg = SL_Util.getTopPackageNameFor21(mContext);
            }
            SL_LockSoftInfo ls = isExsit(topPkg);
            if (null != ls) {
                ls.setNeedLock("true");
            }
            setSoftLockAsyncTaskState(1);
            /* softlock by txh end */
        }
    };
}
