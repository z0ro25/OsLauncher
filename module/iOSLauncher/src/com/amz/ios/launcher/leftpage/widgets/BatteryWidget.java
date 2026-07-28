package com.amz.ios.launcher.leftpage.widgets;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amz.ios.ioslite.common.ContextHelper;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.leftpage.model.Battery;
import com.amz.ios.launcher.leftpage.adapter.DeviceListAdapter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

public class BatteryWidget extends BlurConstraintLayoutWidget {

    Launcher mLauncher;
    LayoutInflater mLayoutInflater;
    int mMargin;
    Handler mHandler;
    Runnable mBatteryReloadRunnable;
    RecyclerView mDeviceListRV;
    DeviceListAdapter mDeviceListAdapter;
    List<Battery> mBatteries;

    public BatteryWidget(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public BatteryWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = (Launcher) context;
        mMargin = mLauncher.getDeviceProfile().edgeMarginPx;
        mLayoutInflater = LayoutInflater.from(context);
        mHandler = new Handler();
        mBatteryReloadRunnable = new BatteryReloadRunnable();
        mLayoutInflater.inflate(R.layout.battery_widget,this,true);
    }

    void setUpView(){
        mDeviceListRV = findViewById(R.id.list_devices);
    }

    void setAdapter(){
        mDeviceListRV.setLayoutManager(
                new LinearLayoutManager(mLauncher)
        );
        mDeviceListAdapter = new DeviceListAdapter(mLauncher);
        mDeviceListRV.setAdapter(mDeviceListAdapter);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setUpView();
        setAdapter();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        LinearLayoutCompat linearLayoutCompat = (LinearLayoutCompat) findViewById(R.id.battery_widget_content);
//        ((ConstraintLayout.LayoutParams) linearLayoutCompat.getLayoutParams()).setMargins(mMargin, mMargin, mMargin, mMargin);
        setTextAndBackgroundColor(linearLayoutCompat);
        mHandler.removeCallbacks(null);
        mHandler.postDelayed(mBatteryReloadRunnable,2000L);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public List<Battery> getBatteries(){
        if (mBatteries == null)
            mBatteries = new ArrayList<>();
        mBatteries.clear();
        Battery battery = getCurrentItem();
        if (battery != null)
            mBatteries.add(battery);
        int sdkInt = Build.VERSION.SDK_INT;
        if (sdkInt >= 18){
            Set<BluetoothDevice> bondedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
            bondedDevices.size();
            for (BluetoothDevice bluetoothDevice : bondedDevices) {
                if (bluetoothDevice != null && bluetoothDevice.getName() != null) {
                    bluetoothDevice.getName();
                    Battery h6Var = new Battery();
                    h6Var.a = ContextCompat.getDrawable(mLauncher, R.drawable.ic_bluetooth);
                    h6Var.b = bluetoothDevice.getName();
                    int bluetoothBattery = getBluetoothBattery(bluetoothDevice);
                    if (bluetoothBattery >= 0) {
                        h6Var.c = String.format("%s%%", String.valueOf(bluetoothBattery));
                        h6Var.d = getDrawable(false, bluetoothBattery);
                        this.mBatteries.add(h6Var);
                    }
                }
            }
        }
        return mBatteries;
    }

    @Override
    public void setAppSuggestionViewMargin() {
        super.setAppSuggestionViewMargin();
    }

    @Override
    public void r() {
        super.r();
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(this.mBatteryReloadRunnable, 2000L);
    }

    public Battery getCurrentItem(){
        try {
            Battery battery = new Battery();
            battery.a = ContextCompat.getDrawable(mLauncher,R.drawable.ic_phone);
            battery.b = "iPhone";
            Intent registerReceiver = ContextHelper.registerReceiver(mLauncher,null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
            int status = registerReceiver != null ? registerReceiver.getIntExtra("status", -1) : -1;
            int level = (int) (((registerReceiver == null ? registerReceiver.getIntExtra("level", -1) : -1) / (registerReceiver != null ? registerReceiver.getIntExtra("scale", -1) : -1)) * 100.0f);
            battery.c = String.format("%s%%", level);
            if (status != 2 && status != 5) {
                battery.d = getDrawable(false, level);
            }
            else {
                battery.d = getDrawable(true, level);
            }
            return battery;
        } catch (Throwable th) {
            th.getMessage();
            return null;
        }
    }

    public final Drawable getDrawable(boolean flag, int level) {
        if (flag) {
            if (level <= 5) {
                return ContextCompat.getDrawable(mLauncher, R.drawable.ic_battery_charging_0);
            }
            if (level <= 10) {
                return ContextCompat.getDrawable(mLauncher, R.drawable.ic_battery_charging_10);
            }
            if (level <= 20) {
                return ContextCompat.getDrawable(mLauncher, R.drawable.ic_battery_charging_20);
            }
            if (level <= 30) {
                return ContextCompat.getDrawable(mLauncher, R.drawable.ic_battery_charging_30);
            }
            if (level <= 40) {
                return ContextCompat.getDrawable(mLauncher, R.drawable.ic_battery_charging_40);
            }
            if (level <= 50) {
                return ContextCompat.getDrawable(mLauncher, R.drawable.ic_battery_charging_50);
            }
            if (level <= 60) {
                return ContextCompat.getDrawable(mLauncher, R.drawable.ic_battery_charging_60);
            }
            if (level <= 70) {
                return ContextCompat.getDrawable(mLauncher, R.drawable.ic_battery_charging_70);
            }
            if (level <= 80) {
                return ContextCompat.getDrawable(mLauncher, R.drawable.ic_battery_charging_80);
            }
            if (level <= 90) {
                return ContextCompat.getDrawable(mLauncher, R.drawable.ic_battery_charging_90);
            }
            if (level <= 100) {
                return ContextCompat.getDrawable(mLauncher, R.drawable.ic_battery_charging_100);
            }
        } else if (level <= 5) {
            return ContextCompat.getDrawable(mLauncher, R.drawable.ic_battery_0);
        } else {
            if (level <= 10) {
                return ContextCompat.getDrawable(mLauncher, R.drawable.ic_battery_10);
            }
            if (level <= 20) {
                return ContextCompat.getDrawable(mLauncher, R.drawable.ic_battery_20);
            }
            if (level <= 30) {
                return ContextCompat.getDrawable(mLauncher, R.drawable.ic_battery_30);
            }
            if (level <= 40) {
                return ContextCompat.getDrawable(mLauncher, R.drawable.ic_battery_40);
            }
            if (level <= 50) {
                return ContextCompat.getDrawable(mLauncher, R.drawable.ic_battery_50);
            }
            if (level <= 60) {
                return ContextCompat.getDrawable(mLauncher, R.drawable.ic_battery_60);
            }
            if (level <= 70) {
                return ContextCompat.getDrawable(mLauncher, R.drawable.ic_battery_70);
            }
            if (level <= 80) {
                return ContextCompat.getDrawable(mLauncher, R.drawable.ic_battery_80);
            }
            if (level <= 90) {
                return ContextCompat.getDrawable(mLauncher, R.drawable.ic_battery_90);
            }
            if (level <= 100) {
                return ContextCompat.getDrawable(mLauncher, R.drawable.ic_battery_100);
            }
        }
        return ContextCompat.getDrawable(mLauncher, R.drawable.ic_battery_100);
    }

    public final int getBluetoothBattery(BluetoothDevice bluetoothDevice) {
        try {
            Method method = bluetoothDevice.getClass().getMethod("getBatteryLevel", new Class[0]);
            method.setAccessible(true);
            return (Integer) method.invoke(bluetoothDevice, new Object[0]);
        } catch (Throwable unused) {
            return 100;
        }
    }

    public class BatteryReloadRunnable implements Runnable {
        @Override
        public void run() {
            try {
                List<Battery> batteries = new BatteryLoadCallable().call();
                mDeviceListAdapter.setDevices(batteries);
            }
            catch (Throwable th){

            }
        }
    }

    public class BatteryLoadCallable implements Callable<List<Battery>>{
        @Override
        public List<Battery> call() throws Exception {
            return getBatteries();
        }
    }

}
