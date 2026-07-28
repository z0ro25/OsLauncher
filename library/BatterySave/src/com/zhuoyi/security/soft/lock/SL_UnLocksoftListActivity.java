package com.zhuoyi.security.soft.lock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ios.sc.common.db.softlock.SL_LocksoftDatabaseHelp;
import com.ios.sc.common.logs.SL_Log;
import com.ios.sc.common.utils.C_C_Util;
import com.ios.sc.common.utils.C_SC_Service_Communication;
import com.zhuoyi.security.batterysave.R;
import com.zhuoyi.security.batterysave.util.SL_Util;
import com.zhuoyi.security.batterysave.util.SL_Util.PackageInfoComparator;
import com.zhuoyi.security.batterysave.views.SL_GlobalActivity;
import com.zhuoyi.security.batterysave.views.SL_LoadingDialog;
import com.zhuoyi.security.batterysave.views.SL_TitleBar;

public class SL_UnLocksoftListActivity extends SL_GlobalActivity implements SL_TitleBar.CallBack{

    private SL_TitleBar slUnlockTitle;
    private ListView unlockList;
    private TextView showText;
    private SL_UnlockAdapter adapter;
    public static ArrayList<SL_LockSoftInfo> arrayList= new ArrayList<SL_LockSoftInfo>();
    private Map<Integer,SL_LockSoftInfo> ids;
    private SL_LocksoftDatabaseHelp helper ;
    private SQLiteDatabase database ;
    private int count = 0;

    public Context mContext;
    private String systemApp ;
    private ProgressBar progress_bar;
    final int SHOW_PROCESS_DIALOG = 2;
    final int DIS_PROCESS_DIALOG = 3;
    private boolean slState = false;
    private Set<String> LAUNCHER_APPS = new HashSet<String>();
    private String systemAppMTK  ="com.android.mms,com.android.email,com.android.calendar,com.android.contacts,com.android.gallery3d,com.mediatek.camera,com.android.camera,"+
            "com.mediatek.filemanager,com.tyd.android.app.videogallery,com.tencent.mobileqq,com.tencent.qq,com.lenovo.ideafriend,com.lenovo.scgmtk,com.lenovo.email,com.lenovo.calendar,com.lenovo.FileBrowser,com.android.dialer,net.micode.fileexplorer,"+
            "com.htc.contacts,com.yulong.android.contacts.dial,com.sonyericsson.android.socialphonebook,com.htc.sense.mms,com.sonyericsson.conversations,com.huawei.message,com.htc.htcdialer,com.htc.android.mail,com.htc.album,com.htc.calendar,";

    private String []systemAppQcom  ={"com.android.mms","com.android.email","com.android.calendar",
            "com.android.contacts",//"com.android.gallery3d",
            //"com.mediatek.camera","com.android.camera",//"com.android.settings",
            "com.mediatek.filemanager","com.tyd.android.app.videogallery",
//            "com.mediatek.bluetooth","com.mediatek.FMRadio","com.android.soundrecorder","com.android.browser","com.android.music",
//            "com.baidu.BaiduMap","com.baidu.searchbox","com.tencent.mtt"
            "com.tencent.mobileqq","com.tencent.qq" }; //qq 2011

    private Handler mHandler = new Handler(){
        
        public void handleMessage(Message msg) {
            switch(msg.what){
            case 1:
                if (arrayList != null) {
                    count = arrayList.size();
                }
                if (count > 0) {
                    showText.setVisibility(View.GONE);
                } else {
                    showText.setVisibility(View.VISIBLE);
                }
                break;
                
            case  SHOW_PROCESS_DIALOG:
//                progress_bar.setVisibility(View.VISIBLE);
                if (showText.getVisibility() == View.VISIBLE) {
                    showText.setVisibility(View.GONE);
                }
                showMyDialog();
                break;
            case  DIS_PROCESS_DIALOG:
                if (arrayList != null) {
                    count = arrayList.size();
                }
                if (count > 0) {
                    showText.setVisibility(View.GONE);
                } else {
                    showText.setVisibility(View.VISIBLE);
                }
//                progress_bar.setVisibility(View.GONE);
                hideMyDialog();
                break;
            case 4:
                int p = msg.arg1;
                try{
                    if (arrayList != null) {
                        database.insert(SL_LocksoftDatabaseHelp.Locksoft_table, null, SL_LocksoftDatabaseHelp.lockSoftValues(arrayList.get(p).getPackageName(), arrayList.get(p).getName(), null));            
                        mHandler.sendEmptyMessage(1);
                        slState = true;
                        arrayList.remove(p);
                        adapter.notifyDataSetChanged();
                    }
                }catch(Exception e){
                    SL_Log.logI("exceptin" + "out");
                }
            
                break;
            }
            
            super.handleMessage(msg);
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sl_app_list_unlock_activity);
        slUnlockTitle = (SL_TitleBar) findViewById(R.id.sl_unlock_title);
        slUnlockTitle.setOnCallBack(SL_UnLocksoftListActivity.this);
        mContext = this;
        helper = new SL_LocksoftDatabaseHelp(this);
        database = helper.getWritableDatabase();
        // modify by swang , remove com.android.gallery3d
    /*    if (Utils.judge_qcom("ro.hardware", "0").contains("qcom")) {

            systemApp = systemAppQcom;


        }else*/
        {

            systemApp = systemAppMTK;
        }


        ids = new HashMap<Integer,SL_LockSoftInfo>();
        initView();
        initList();
    }

    private void initView(){
        progress_bar = (ProgressBar)findViewById(R.id.sl_progress_bar);
        unlockList = (ListView)findViewById(R.id.sl_unlock_list);
        showText = (TextView)findViewById(R.id.sl_unlock_activity_show_text);
    }

    @Override
    protected void onPause() {
        if (slState) {
            Intent myIntent = C_SC_Service_Communication.getServiceIntent(C_SC_Service_Communication.SOFT_LOCK_LIST_UPDATE_DATA);
            C_SC_Service_Communication.startServiceForIntent(mContext, myIntent);
        }
        if (SL_Util.getFloatState(mContext)) {
            Toast.makeText(this, getResources().getText(R.string.sl_float_hint_tv), Toast.LENGTH_LONG).show();
            SL_Util.setFloatState(mContext, false);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if(database!=null&&database.isOpen()){
            database.close();
        }

        if(arrayList != null){
            arrayList.clear();
            arrayList = null;
        }

        if(adapter != null){
            adapter = null;
        }

        if(mHandler != null){
            mHandler = null;
        }

        super.onDestroy();
    }

     ArrayList<SL_AppInfo> appList = new  ArrayList<SL_AppInfo> ();
    private void initList(){
        adapter = new SL_UnlockAdapter(this);
        unlockList.setAdapter(adapter);
        unlockList.setOnItemClickListener(new ListItemClick());
    }

    public  ArrayList<SL_LockSoftInfo> getAppInfoList(Context context)
    {
        String pkgName = mContext.getPackageName();
        List<PackageInfo> packages;
        ContextWrapper contextwrapper = (ContextWrapper) context;
        ArrayList<SL_LockSoftInfo> appList = new ArrayList<SL_LockSoftInfo>();
        packages = contextwrapper.getPackageManager().getInstalledPackages(0);
        Collections.sort(packages,new PackageInfoComparator(contextwrapper));
        Cursor c = database.query(SL_LocksoftDatabaseHelp.Locksoft_table, SL_LocksoftDatabaseHelp.all, null,null, null, null, null);
        try{
            String cursorList = "";
            // get cursor list
            for(c.moveToFirst();!c.isAfterLast();c.moveToNext()){
                String packageNameDB = c.getString(c.getColumnIndex(SL_LocksoftDatabaseHelp.PACKAGE_NAME));
                cursorList+=packageNameDB+",";
            }
            for (int i = 0; i < packages.size(); i++)
            {
                PackageInfo packageInfo = packages.get(i);
                if(!LAUNCHER_APPS.contains(packageInfo.packageName)){
                    continue;
                }
                ApplicationInfo applicationInfo = packageInfo.applicationInfo;
                if (((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0)&&!packageInfo.packageName.contains(pkgName)&&
                        systemApp.contains(packageInfo.packageName+",")&&!cursorList.contains(packageInfo.packageName+",")){
                    if(packageInfo.packageName.equals("com.android.mms")){
                        appList.add(0,new SL_LockSoftInfo(packageInfo.packageName,"",packageInfo.applicationInfo.loadLabel(contextwrapper.getPackageManager()).toString(), SL_LocksoftDatabaseHelp.drawableToBitmap(packageInfo.applicationInfo.loadIcon(contextwrapper.getPackageManager()))));
                    }
                    else if(packageInfo.packageName.equals("com.android.contacts")){
                        appList.add(0,new SL_LockSoftInfo(packageInfo.packageName,"",packageInfo.applicationInfo.loadLabel(contextwrapper.getPackageManager()).toString(), SL_LocksoftDatabaseHelp.drawableToBitmap(packageInfo.applicationInfo.loadIcon(contextwrapper.getPackageManager()))));
                    }
                    else{
                        appList.add(new SL_LockSoftInfo(packageInfo.packageName,"",packageInfo.applicationInfo.loadLabel(contextwrapper.getPackageManager()).toString(), SL_LocksoftDatabaseHelp.drawableToBitmap(packageInfo.applicationInfo.loadIcon(contextwrapper.getPackageManager()))));
                    }

                }else if(((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) &&applicationInfo.flags!=ApplicationInfo.FLAG_UPDATED_SYSTEM_APP&&!packageInfo.packageName.contains(pkgName)&&
                        !cursorList.contains(packageInfo.packageName+",")
                        &&!packageInfo.packageName.toLowerCase().contains("launcher")
                        &&getPackageManager().getLaunchIntentForPackage(packageInfo.packageName)!=null){
                    appList.add(new SL_LockSoftInfo(packageInfo.packageName,"",packageInfo.applicationInfo.loadLabel(contextwrapper.getPackageManager()).toString(), SL_LocksoftDatabaseHelp.drawableToBitmap(packageInfo.applicationInfo.loadIcon(contextwrapper.getPackageManager()))));

                }
            }
        }catch(Exception e){

        }finally{
            if(c!=null&&!c.isClosed()){
                c.close();
            }
        }
        return appList;
    }

      class GetListTask extends AsyncTask<Object, Object, String>{
            @Override
            protected void onPreExecute() {
                // TODO Auto-generated method stub
                mHandler.sendEmptyMessage(SHOW_PROCESS_DIALOG);
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(Object... params) {
                // TODO Auto-generated method stub
                loadAllLauncherApp();
                arrayList = getAppInfoList(mContext);
//                arrayList = getList(appList);
                return null;
            }
            @Override
            protected void onPostExecute(String result) {
                // TODO Auto-generated method stub
                super.onPostExecute(result);
                if (adapter != null) {
                    adapter.setList(arrayList);
                }
                if(mHandler!=null){
                    mHandler.sendEmptyMessage(DIS_PROCESS_DIALOG);
                }

            }

        }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        new GetListTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private SL_LoadingDialog mAlertDialog =null;
    private void showMyDialog(){
        if(null == mAlertDialog){
            mAlertDialog = new SL_LoadingDialog(SL_UnLocksoftListActivity.this);
        }
         //
        mAlertDialog.setShowLoadingText();//
        //mAlertDialog.setLoadingText("HAHAHHAHAHHA");  //
        mAlertDialog.show();
    }

    private void hideMyDialog(){
        if(null != mAlertDialog){
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
    }

    @Override
    public void onLeftClick() {
        // TODO Auto-generated method stub
        if(C_C_Util.isFastMultipleClick()) {
            return;
        }
        finish();
    }

    @Override
    public void onCenterClick() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onRightClick() {
        // TODO Auto-generated method stub

    }

    /**
     * Query launcher icon app.
     * @param context
     */
    private void loadAllLauncherApp() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = mContext.getPackageManager().queryIntentActivities(intent,0/*PackageManager.MATCH_DEFAULT_ONLY*/);
        if(resolveInfos != null && resolveInfos.size() >0 ){
            LAUNCHER_APPS.clear();
            for(ResolveInfo ri : resolveInfos){
                LAUNCHER_APPS.add(ri.activityInfo.packageName);
            }
        }
    }
    
        class ListItemClick implements OnItemClickListener{

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
            
            AnimationSet animationSet = new AnimationSet(true);
            TranslateAnimation translateAnimation = new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 0f,
                    Animation.RELATIVE_TO_SELF,-1f,
                    Animation.RELATIVE_TO_SELF, 0f,
                    Animation.RELATIVE_TO_SELF, 0f);
            translateAnimation.setFillAfter(true);
            translateAnimation.setDuration(200);
            animationSet.addAnimation(translateAnimation);
            view.startAnimation(animationSet);
            final int pos = position;
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {

                public void run() {
                    if (mHandler != null) {
                        Message m = mHandler.obtainMessage();
                        m.arg1 = pos;
                        m.what = 4;
                        mHandler.sendMessage(m);
                    }
                }

            }, 200);
        }
        
    }
}
