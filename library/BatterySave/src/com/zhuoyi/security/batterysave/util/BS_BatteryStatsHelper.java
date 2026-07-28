package com.zhuoyi.security.batterysave.util;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Administrator on 2016/10/9.
 */

public class BS_BatteryStatsHelper {



    public static double cpuPowerCalculator(Context context) {
        BS_PowerProfileUtil profile = new BS_PowerProfileUtil(context);
        double mPowerCpuNormal = profile.getAveragePower("cpu.active", 0);
        if (mPowerCpuNormal < 1 ) {//default is 100mA
            mPowerCpuNormal = 100;
        }
        Log.e("zengrui","CpuRate==="+getProcessCpuRate(context));
        mPowerCpuNormal = mPowerCpuNormal *2/3 * getProcessCpuRate(context);
        Log.e("zengrui","mPowerCpuNormal==="+mPowerCpuNormal);
        return mPowerCpuNormal;
    }

    /** get CPU rate. top can't get the cpu rate. so this isn't the real
     * @return
     */
    private static double getProcessCpuRate(Context context) {

        //StringBuilder tv = new StringBuilder();
        double rate = 0;

        try {
           /* Process p = Runtime.getRuntime().exec("top -n 1");
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;
            while ((line = in.readLine()) != null) {
                if (line.trim().length() < 1) {
                    continue;
                } else {
                    String[] CPUusr = line.split("%");
                    String[] CPUusage = CPUusr[0].split("User");
                    String[] SYSusage = CPUusr[1].split("System");
                    rate = Integer.parseInt(CPUusage[1].trim()) + Integer.parseInt(SYSusage[1].trim());
                    break;
                }
            }*/
            String cmd = "ps";
            java.lang.Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;
            int num = 0;
            while ((line = in.readLine()) != null) {
                if (line.trim().startsWith("u0")){
                    Log.e("zengrui",num+"---"+line);
                    num ++;
                }else {
                    continue;
                }

            }
            num = num -1;

            int total = context.getPackageManager().getInstalledApplications(0).size();
            Log.e("zengrui","t---"+total);
            rate = num/(double)total;


        } catch (IOException e) {
            e.printStackTrace();
        }
        return rate;
    }

    public static double wifiPowerCalculator(Context context){//wifi.on 3mA
        BS_PowerProfileUtil profile = new BS_PowerProfileUtil(context);
        double wifiPowerOn = profile.getAveragePower("wifi.on", 0);
        if (wifiPowerOn < 1){
            wifiPowerOn = 3;
        }
        return wifiPowerOn;
    }

    public static double bluetoothPowerCalculator(Context context){//bluetooth.on 0.1mA
        BS_PowerProfileUtil profile = new BS_PowerProfileUtil(context);
        double bluetoothPowerOn = profile.getAveragePower("bluetooth.on", 0);
        if (bluetoothPowerOn <= 0){
            bluetoothPowerOn = 0.1;
        }
        return bluetoothPowerOn;
    }


    public static double cameraPowerCalculator(Context context){//camera.avg 450mA
        BS_PowerProfileUtil profile = new BS_PowerProfileUtil(context);
        double cameraPowerOn = profile.getAveragePower("camera.avg", 0);
        if (cameraPowerOn < 1){
            cameraPowerOn = 450;
        }
        return cameraPowerOn;
    }

    public static double flashlightPowerCalculator(Context context){//camera.flashlight 160mA
        BS_PowerProfileUtil profile = new BS_PowerProfileUtil(context);
        double flashlightPowerOn = profile.getAveragePower("camera.flashlight", 0);
        if (flashlightPowerOn < 1){
            flashlightPowerOn = 160;
        }
        return flashlightPowerOn;
    }

    public static double radioPowerCalculator(Context context){// radio.on 2mA
        BS_PowerProfileUtil profile = new BS_PowerProfileUtil(context);
        double radioPowerOn = profile.getAveragePower("radio.on", 0);
        if (radioPowerOn < 1){
            radioPowerOn = 2;
        }
        return radioPowerOn;
    }

    public static double sensorPowerCalculator(Context context){// gps.on 50mA
        BS_PowerProfileUtil profile = new BS_PowerProfileUtil(context);
        double sensorPowerOn = profile.getAveragePower("gps.on", 0);
        if (sensorPowerOn < 1){
            sensorPowerOn = 50;
        }
        return sensorPowerOn;
    }

    // Hardware
    private void processMiscUsage() {
        /*addUserUsage();
        addPhoneUsage();
        addScreenUsage();
        addWiFiUsage();
        addBluetoothUsage();
        addIdleUsage(); // Not including cellular idle power
        // Don't compute radio usage if it's a wifi-only device
        if (!mWifiOnly) {
            addRadioUsage();
        }*/
    }

   /* private void addUserUsage() {
        for (int i = 0; i < mUserSippers.size(); i++) {
            final int userId = mUserSippers.keyAt(i);
            BatterySipper bs = new BatterySipper(DrainType.USER, null, 0);
            bs.userId = userId;
            aggregateSippers(bs, mUserSippers.valueAt(i), "User");
            mUsageList.add(bs);
        }
    }*/
}
