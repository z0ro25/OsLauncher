package com.zhuoyi.security.soft.lock.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import com.ios.sc.common.db.softlock.SL_LocksoftDatabaseHelp;
import com.ios.sc.common.utils.C_SC_Service_Communication;
 
public class SL_ApkRemoveReceiver extends BroadcastReceiver {
    private SQLiteDatabase database;
    private SL_LocksoftDatabaseHelp helper;

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals("android.intent.action.PACKAGE_REMOVED")) {
            helper = new SL_LocksoftDatabaseHelp(context);
            database = helper.getWritableDatabase();
            String packageName = intent.getDataString().substring(8);
            try{
                database.delete(SL_LocksoftDatabaseHelp.Locksoft_table,SL_LocksoftDatabaseHelp.PACKAGE_NAME + "=?",
                        new String[] {packageName});

                Intent myIntent = C_SC_Service_Communication.getServiceIntent(C_SC_Service_Communication.SOFT_LOCK_LIST_UPDATE_DATA);
                C_SC_Service_Communication.startServiceForIntent(context, myIntent);
            }catch(Exception e){

            }finally{
                if(database!=null&&database.isOpen()){
                    database.close();
                }

            }
        }

    }

}
