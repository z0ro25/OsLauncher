package com.ezt.ios.serverswitchcontrol;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by Administrator on 2016/12/2.
 */
public class CommonDeviceInfo {

    public static String getProperties(String str) {
        Object result = null;
        try {
            Class<?> classType = Class.forName("android.os.SystemProperties");
            Object invokeOperation = classType.newInstance();
            Method getMethod = classType.getMethod("get", new Class[]{String.class});
            result = getMethod.invoke(invokeOperation, new Object[]{new String(str)});
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result != null ? result.toString() : "";
    }

    public static JSONObject getVersion(String v) {
        JSONObject json = new JSONObject();
        int[] version = new int[]{0, 0, 0, 0};
        String[] vs = v.split("\\.");
        for (int i = 0; i < vs.length && i < version.length; i++) {
            try {
                version[i] = Integer.parseInt(vs[i]);
            } catch (Exception e) {
            }
        }
        try {
            json.put("major", version[0]);
            json.put("minor", version[1]);
            json.put("micro", version[2]);
            json.put("build", version[3]);
        } catch (Exception e) {
        }
        return json;
    }

    public static String getIPAddress(Context context) {
        NetworkInfo info = ((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            if (info.getType() == ConnectivityManager.TYPE_MOBILE) {//当前使用2G/3G/4G网络
                try {
                    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                        NetworkInterface intf = en.nextElement();
                        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                            InetAddress inetAddress = enumIpAddr.nextElement();
                            if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                                return inetAddress.getHostAddress();
                            }
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                }

            } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {//当前使用无线网络
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String ipAddress = intIP2StringIP(wifiInfo.getIpAddress());//得到IPV4地址
                return ipAddress;
            }
        } else {
            //当前无网络连接,请在设置中打开网络
        }
        return null;
    }

    /**
     * 将得到的int类型的IP转换为String类型
     *
     * @param ip
     * @return
     */
    public static String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }
}
