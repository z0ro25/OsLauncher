package com.ios.sc.common.db.softlock;

import java.io.ByteArrayOutputStream;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class SL_LocksoftDatabaseHelp extends SQLiteOpenHelper {

    public static String LocksoftDB="LocksoftDB";
    public static int Locksoft_VERSION=1;
    public static String Locksoft_table="Locksoft_table";
    public static String Sim_table="Sim_table";
    public static String Locksoft_ID="_id";
    public static String NAME = "name";
    public static String PACKAGE_NAME = "packageName";
    public static String APP_ICON = "app_icon";
    public static String SIM = "sim_phone";


    public static String all[] = {PACKAGE_NAME,NAME,APP_ICON};
    public static String all_sim[] = {SIM};

    public SL_LocksoftDatabaseHelp(Context context) {
        super(context, LocksoftDB, null, Locksoft_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(" CREATE TABLE IF NOT EXISTS " + Locksoft_table
                        + " ( " + Locksoft_ID + " integer primary key autoincrement , "
                        + PACKAGE_NAME + " text , "
                        + NAME + " text , "
                        + APP_ICON + " text );");

        db.execSQL(" CREATE TABLE IF NOT EXISTS " + Sim_table
                + " ( " + Locksoft_ID + " integer primary key autoincrement , "
                + SIM + " text );");

//        db.execSQL("insert into Locksoft_table(package_name,need_locked) values ('com.android.calendar','true') ");
//        db.execSQL("insert into Locksoft_table(package_name,need_locked) values ('com.android.mms','true') ");
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS"+Locksoft_table);
        db.execSQL("DROP TABLE IF EXISTS"+Sim_table);
        onCreate(db);
    }

    public static ContentValues lockSoftValues(String packageName,String name,byte[] app_icon){
        ContentValues mValues = new ContentValues();
        mValues.put(PACKAGE_NAME, packageName);
        mValues.put(NAME, name);
        mValues.put(APP_ICON, app_icon);
        return mValues;
    }

    public static ContentValues SimValues(String sim){
        ContentValues mValues = new ContentValues();
        mValues.put(SIM, sim);
        return mValues;
    }

     public static byte[] bitmapToBytes(Bitmap bitmap) {
            if (bitmap == null) {
                return null;
            }

            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            return os.toByteArray();
        }

     public static Bitmap getBitmap( byte[] data) {
//         mCursor.moveToPosition(position);
//         byte[] data = mCursor.getBlob(cursorIndex);
         if (data != null) {
             return Bitmap.createScaledBitmap(BitmapFactory.decodeByteArray(data,0,data.length), 60, 60, true);
         }else{

             return null;
         }
     }

     public static Bitmap drawableToBitmap(Drawable drawable){
        BitmapDrawable bd = (BitmapDrawable) drawable;
        Bitmap bm = bd.getBitmap();
        return bm;
     }

    public static String getLastPackName(Context context) {
        SharedPreferences sp = context.getSharedPreferences("LOCK_SOFT", Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
        return sp.getString("sl_last_packName", "");
    }

    public static void setLastPackName(Context context, String packName){
        SharedPreferences sp = context.getSharedPreferences("LOCK_SOFT", Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor et = sp.edit();
        et.putString("sl_last_packName", packName);
        et.commit();
    }
}
