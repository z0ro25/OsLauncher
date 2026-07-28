package com.amz.ios;

import android.app.Application;
import com.amz.ios.database.HiddenAppManager;
import org.litepal.LitePal;

public class IOSLauncher extends Application {

    static Application context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        LitePal.initialize(this);
        HiddenAppManager.INSTANCE.initDataBase(this);
    }

    public static Application getInstance(){
        return context;
    }
}
