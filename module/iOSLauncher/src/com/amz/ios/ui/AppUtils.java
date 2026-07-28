package com.amz.ios.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AppUtils {

    private SharedPreferences mSavedData;
    private static AppUtils instance;

    static final String PAGE_TRANSFORMER = "PAGE_TRANSFORMER";
    static final String SIZE_ICON = "SIZE_ICON";
    static final String SIZE_TEXT = "SIZE_TEXT";
    static final String COLOR_TEXT = "COLOR_TEXT";
    static final String IS_SHOW_NAVIGATION = "IS_SHOW_NAVIGATION";
    static final String IS_SHOW_NOTIFICATION = "IS_SHOW_NOTIFICATION";
    static final String ANIM_APP = "ANIM_APP";
    static final String STYLE_PHONE_8 = "STYLE_PHONE_8";
    static final String TIME_CHANGE_PHOTO = "TIME_CHANGE_PHOTO";
    static final String TIME_24 = "TIME_24";
    static final String ENA_BADGES = "ENA_BADGES";
    static final String SETTING_SAVE_PREF = "SETTING_SAVE_PREF";
    public static final String GRID_SIZE = "use_grid_size";
    Context mContext;

    private AppUtils(Context context){

        mContext = context;

        if (mContext == null)
            throw new IllegalArgumentException("Context is null");

        mSavedData = PreferenceManager.getDefaultSharedPreferences(
            mContext
        );
    }

    public static AppUtils getInstance(Context context){
        if (instance == null){
            synchronized (AppUtils.class){
                instance = new AppUtils(context);
            }
        }
        return instance;
    }

    public int getGridSize(){
        return getInt(GRID_SIZE);
    }

    public void setGridSize(int size){
        if (size != 5 && size != 6){
            return;
        }
        setInt(GRID_SIZE,size);
    }

    public int getPageTransformer(){
        return getInt(PAGE_TRANSFORMER);
    }

    public void setPageTransformer(int pageTransformer){
        setInt(PAGE_TRANSFORMER,pageTransformer);
    }

    public int getSizeText(){
        return getInt(SIZE_TEXT);
    }

    public void setSizeText(int size){
        setInt(SIZE_TEXT, size);
    }

    public int getSizeIcon(){
        return getInt(SIZE_ICON);
    }

    public void setSizeIcon(int size){
        setInt(SIZE_ICON, size);
    }

    public int getColorText(){
        return getInt(COLOR_TEXT);
    }

    public void setColorText(int color){
        setInt(COLOR_TEXT, color);
    }

    public boolean isShowNavigation(){
        return getBoolean(IS_SHOW_NAVIGATION);
    }

    public void setIsShowNavigation(boolean flag){
        setBoolean(IS_SHOW_NAVIGATION, flag);
    }

    public boolean getIsShowNotification(){
        return getBoolean(IS_SHOW_NOTIFICATION);
    }

    public void setIsShowNotification(boolean flag){
        setBoolean(IS_SHOW_NOTIFICATION, flag);
    }

    public boolean getAnimApp(){
        return getBoolean(ANIM_APP);
    }

    public void setAnimApp(boolean flag){
        setBoolean(ANIM_APP,flag);
    }

    public boolean getStylePhone8(){
        return getBoolean(STYLE_PHONE_8);
    }

    public void setStylePhone8(boolean flag){
        setBoolean(STYLE_PHONE_8, flag);
    }

    public int getTimeChangePhoto(){
        return getInt(TIME_CHANGE_PHOTO);
    }

    public void setTimeChangePhoto(int i){
        setInt(TIME_CHANGE_PHOTO , i);
    }

    public boolean getTime24(){
        return getBoolean(TIME_24);
    }

    public void setTime24(boolean flag){
        setBoolean(TIME_24, flag);
    }

    public boolean getEnaBadges(){
        return getBoolean(ENA_BADGES);
    }

    public void setEnaBadges(boolean flag){
        setBoolean(ENA_BADGES, flag);
    }

    public int getInt(String key){
        return mSavedData.getInt(key, 0);
    }

    public void setInt(String key, int value){
        SharedPreferences.Editor editor = mSavedData.edit();
        editor.putInt(key, value);
        if (!editor.commit()) editor.apply();
    }

    public String getString(String key){
        return mSavedData.getString(key,"");
    }

    public void setString(String key, String value){
        SharedPreferences.Editor editor = mSavedData.edit();
        editor.putString(key, value);
        if (!editor.commit()) editor.apply();
    }

    public boolean getBoolean(String key){
        return mSavedData.getBoolean(key,false);
    }

    public void setBoolean(String key, boolean value){
        SharedPreferences.Editor editor = mSavedData.edit();
        editor.putBoolean(key, value);
        if (!editor.commit()) editor.apply();
    }

}
