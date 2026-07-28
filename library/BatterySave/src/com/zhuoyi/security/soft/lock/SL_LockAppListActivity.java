package com.zhuoyi.security.soft.lock;

import java.util.ArrayList;
import java.util.Collections;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ios.sc.common.db.softlock.SL_LocksoftDatabaseHelp;
import com.ios.sc.common.logs.SL_Log;
import com.ios.sc.common.utils.C_C_Util;
import com.ios.sc.common.utils.C_GC_Util;
import com.ios.sc.common.utils.C_SC_Service_Communication;
import com.zhuoyi.security.batterysave.R;
import com.zhuoyi.security.batterysave.util.SL_Util.LockSoftInfoComparator;
import com.zhuoyi.security.batterysave.views.SL_GlobalActivity;
import com.zhuoyi.security.batterysave.views.SL_TitleBar;
import com.zhuoyi.security.batterysave.views.SL_ScaleView;

public class SL_LockAppListActivity extends SL_GlobalActivity implements OnClickListener, Callback, SL_TitleBar.CallBack{

    private ImageView okButton;
    private ListView lockList;
    private SL_LockAdapter adapter;
    public ArrayList<SL_LockSoftInfo> arrayList = new ArrayList<SL_LockSoftInfo>();
    private SL_LocksoftDatabaseHelp helper;
    private SQLiteDatabase database;
    private int count;
    private ProgressDialog dialog;
    public int UPDATE_LIST = 1;
    private RelativeLayout temp_lock;
    private SL_ScaleView add;
    private ImageView title_back_temp, title_setting_temp;
    private SL_TitleBar slLockTitleBar;
    private boolean slState = false;
    private Context mContext = null;
    private final int MSG_FINISH_ACTIVITY = 003;
    private final int MSG_REMOVE_FLOAT = 004;

    private WindowManager mWindowManager = null;
    private View myView = null;
    private RelativeLayout hintLay = null;
    private ImageView appIcon = null;
    private TextView appName = null;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case 1:
                // arrayList = getListFromDB();
                // adapter.notifyDataSetChanged();
                if (arrayList != null) {
                    count = arrayList.size();
                    if (count > 0) {
                        temp_lock.setVisibility(View.GONE);
                        okButton.setVisibility(View.VISIBLE);
                    } else {
                        temp_lock.setVisibility(View.VISIBLE);
                        okButton.setVisibility(View.GONE);
                    }
                }
                break;
            case 2:
                int p = msg.arg1;
                try {
                    if (arrayList != null) {
                        database.delete(SL_LocksoftDatabaseHelp.Locksoft_table, SL_LocksoftDatabaseHelp.PACKAGE_NAME + "=?", new String[] { arrayList.get(p).getPackageName() });
                        slState = true;
                        arrayList.remove(p);
                        adapter.notifyDataSetChanged();
                        mHandler.sendEmptyMessage(1);
                    }
                } catch (Exception e) {
                }
                break;
            case MSG_FINISH_ACTIVITY:
                if(C_C_Util.isFastMultipleClick()) {
                    return;
                }
                finish();
                break;
            case MSG_REMOVE_FLOAT:
                try {
                    if (myView != null) {
                        mWindowManager.removeView(myView);
                    }
                }catch (Exception e) {
                    SL_Log.logE("float remove err = " + e.getMessage());
                }
                break;
            }
            super.handleMessage(msg);
        }

    };


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sl_lock_app_list);
        mContext = SL_LockAppListActivity.this;
        helper = new SL_LocksoftDatabaseHelp(this);
        database = helper.getWritableDatabase();
        initView();
        dialog = getProgressDialog(getString(R.string.sl_un_locking));
        dialog.setCancelable(false);
    }

    @Override
    protected void onResume() {
        updataListView(SL_LockAppListActivity.this);
        super.onResume();
    }

    private void initView() {
        slLockTitleBar = (SL_TitleBar) findViewById(R.id.sl_lock_title);
        slLockTitleBar.setOnCallBack(SL_LockAppListActivity.this);
        add = (SL_ScaleView) findViewById(R.id.sl_add_app);
        add.setCallback(R.id.sl_add_app, SL_LockAppListActivity.this);
        temp_lock = (RelativeLayout) findViewById(R.id.sl_temp_lock);
        okButton = (ImageView) findViewById(R.id.sl_ok_button);
        okButton.setOnClickListener(this);
        lockList = (ListView) findViewById(R.id.sl_lock_listview);
        adapter = new SL_LockAdapter(this);
        lockList.setAdapter(adapter);
        mHandler.postDelayed(mRunnable, 1000);
    }

    class ListItemClick implements OnItemClickListener{

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {

            AnimationSet animationSet = new AnimationSet(true);
            TranslateAnimation translateAnimation = new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 0f,
                    Animation.RELATIVE_TO_SELF, 1.0f,
                    Animation.RELATIVE_TO_SELF, 0f,
                    Animation.RELATIVE_TO_SELF, 0f);
            translateAnimation.setFillAfter(true);
            translateAnimation.setDuration(300);
            animationSet.addAnimation(translateAnimation);
            view.startAnimation(animationSet);
            final int pos = position;
             Handler handler = new Handler();

                handler.postDelayed(new Runnable() {

                    public void run() {
                        if(mHandler != null){
                            Message m = mHandler.obtainMessage();
                            m.what=2;
                            m.arg1 = pos;
                            mHandler.sendMessage(m);
                        }
                    }

                }, 300);
        }

    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            lockList.setOnItemClickListener(new ListItemClick());
        }
    };

    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.sl_ok_button) {
            intentActivity();
        } /*else if (id == R.id.sl_hint_lay) {
            if (mHandler != null) {
                mHandler.sendEmptyMessageDelayed(MSG_REMOVE_FLOAT, 0);
            }
        }*/
    }

    private void intentTOAddActivity() {
        Intent i = new Intent();
        i.setClass(this, SL_UnLocksoftListActivity.class);
        startActivity(i);
    }

    private ArrayList<SL_LockSoftInfo> getListFromDB(Context context) {
        ArrayList<SL_LockSoftInfo> list = new ArrayList<SL_LockSoftInfo>();
        PackageManager packageManager = getPackageManager();
        ContextWrapper contextwrapper = (ContextWrapper) context;
        Cursor c = database.query(SL_LocksoftDatabaseHelp.Locksoft_table,
                SL_LocksoftDatabaseHelp.all, null, null, null, null, null);
        ApplicationInfo appInfo;
        try {
            // c.moveToFirst();
            c.getCount();
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                String packageName = c.getString(c
                        .getColumnIndex(SL_LocksoftDatabaseHelp.PACKAGE_NAME));
                appInfo = packageManager.getApplicationInfo(packageName, 0);
                String name = appInfo.loadLabel(packageManager).toString();
                if (packageName.equals("com.android.mms")) {
                    list.add(0, new SL_LockSoftInfo(packageName, "", name, SL_LocksoftDatabaseHelp.drawableToBitmap(appInfo.loadIcon(contextwrapper.getPackageManager()))));
                } else if (packageName.equals("com.android.contacts")) {
                    list.add(0, new SL_LockSoftInfo(packageName, "", name , SL_LocksoftDatabaseHelp.drawableToBitmap(appInfo.loadIcon(contextwrapper.getPackageManager()))));
                } else {
                    list.add(new SL_LockSoftInfo(packageName, "", name, SL_LocksoftDatabaseHelp.drawableToBitmap(appInfo.loadIcon(contextwrapper.getPackageManager()))));
                }

                // list.add(new LockSoftInfo(packageName, "", name,
                // LocksoftDatabaseHelp.getBitmap(c.getBlob(c.getColumnIndex(LocksoftDatabaseHelp.APP_ICON)))));
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (c != null && !c.isClosed()) {
                c.close();
            }
        }

        Collections.sort(list, new LockSoftInfoComparator());
        return list;

    }

    private void updataListView(Context context) {

        arrayList = getListFromDB(context);
        if (adapter != null) {
            adapter.setList(arrayList);
        }
        if (arrayList != null) {
            count = arrayList.size();
        }

        if (count > 0) {
            temp_lock.setVisibility(View.GONE);
            okButton.setVisibility(View.VISIBLE);
        } else {
            temp_lock.setVisibility(View.VISIBLE);
            okButton.setVisibility(View.GONE);
        }

    }

    public ProgressDialog getProgressDialog(String msg) {

        ProgressDialog progressDialog = new ProgressDialog(this);

        progressDialog.setIndeterminate(true);

        progressDialog.setMessage(msg);

        // progressDialog.setCancelable(true);

        return progressDialog;

    }

    @Override
    protected void onDestroy() {

        if (database != null && database.isOpen()) {
            database.close();
        }

        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }

        releaseImageView();
        super.onDestroy();
    }

    private void releaseImageView() {
        C_GC_Util.releaseImageViewMemory(title_back_temp);
        C_GC_Util.releaseImageViewMemory(title_setting_temp);
        C_GC_Util.releaseImageViewMemory(add, true);
    }

    @Override
    protected void onPause() {
        if (dialog != null && dialog.isShowing())
            dialog.cancel();

        if (arrayList != null) {
            arrayList.clear();
            arrayList = null;
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        if (slState) {
            Intent myIntent = C_SC_Service_Communication.getServiceIntent(C_SC_Service_Communication.SOFT_LOCK_LIST_UPDATE_DATA);
            C_SC_Service_Communication.startServiceForIntent(mContext, myIntent);
        }
        super.onPause();
    }

    @Override
    public boolean handleMessage(Message msg) {
        // TODO Auto-generated method stub
        if (msg.what == 999) {
            if(msg.arg1 == R.id.sl_add_app){
                intentActivity();
            }
        }
        return false;
    }

    public void viewClick(View v){
        int id = v.getId();
        if(id == R.id.sl_add_app){

        }
    }

    @SuppressLint("NewApi")
    private void intentActivity(){
        if(C_C_Util.isAndroidSdk_api_21_plus()){
            android.app.AppOpsManager aom = (android.app.AppOpsManager)mContext.getSystemService(Context.APP_OPS_SERVICE);
            try {
                int mode = aom.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(),mContext.getPackageName());

                SL_Log.logD("checkCleanTaskServiceVersion() mode = " + mode );

                if(android.app.AppOpsManager.MODE_ALLOWED != mode){
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                       builder.setMessage(getString(R.string.sl_dialog_text));
                       builder.setPositiveButton(getString(R.string.sl_ok), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                createFloatView();
                                Intent intent = new Intent(/*Settings.ACTION_USAGE_ACCESS_SETTINGS*/"android.settings.USAGE_ACCESS_SETTINGS");
                                startActivity(intent);
                                if (mHandler != null) {
                                    mHandler.removeMessages(MSG_REMOVE_FLOAT);
                                    mHandler.sendEmptyMessageDelayed(MSG_REMOVE_FLOAT, 2000L);
                                }
                            } catch (Exception e) {
                                SL_Log.logE("onClick start USAGE_ACCESS UI");
                            }
                        }
                    });
                       builder.show();
                }else{
                    intentTOAddActivity();
                }
            } catch (Exception e) {
                SL_Log.logE("checkCleanTaskServiceVersion() error: "+ e.toString());
            }
        }else{
            intentTOAddActivity();
        }
    }

    private void createFloatView() {
        WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        wmParams.flags = WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
        if (C_C_Util.isAndroidSdk_api_23_plus()) {
            wmParams.type = WindowManager.LayoutParams.TYPE_TOAST;
        } else {
            wmParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        wmParams.format = PixelFormat.TRANSLUCENT;
        wmParams.x = 0;
        wmParams.y = 0;
        wmParams.screenOrientation = 1;
        LayoutInflater inflater = LayoutInflater.from(mContext);
        myView = inflater.inflate(R.layout.sl_hint_view, null);
        wmParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        wmParams.height = WindowManager.LayoutParams.MATCH_PARENT;

        initFloat(myView);
        mWindowManager.addView(myView, wmParams);
    }

    private void initFloat(View view) {
        hintLay = (RelativeLayout) view.findViewById(R.id.sl_hint_lay);
        //hintLay.setOnClickListener(SL_LockAppListActivity.this);
        appIcon = (ImageView) view.findViewById(R.id.sl_appicon);
        appName = (TextView) view.findViewById(R.id.sl_appname);

        PackageManager packageManager = null;
        ApplicationInfo applicationInfo = null;
        try {
            packageManager = mContext.getPackageManager();
            applicationInfo = packageManager.getApplicationInfo(mContext.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException var4) {
            applicationInfo = null;
        }

        String applicationName = (String)packageManager.getApplicationLabel(applicationInfo);
        Drawable d=packageManager.getApplicationIcon(applicationInfo);
        BitmapDrawable bd = (BitmapDrawable) d;
        Bitmap bm = bd.getBitmap();
        appIcon.setImageBitmap(bm);
        appName.setText(applicationName);
    }

    @Override
    public void onLeftClick() {
        // TODO Auto-generated method stub
        Message m = mHandler.obtainMessage();
        m.what = MSG_FINISH_ACTIVITY;
        mHandler.sendMessage(m);
    }

    @Override
    public void onCenterClick() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onRightClick() {
        // TODO Auto-generated method stub
        startActivity(new Intent().setClass(this, SL_LockSettingActivity.class));
    }
}
