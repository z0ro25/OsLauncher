package com.amz.ios.serverswitchcontrol;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    public static String getImei(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String imei = tm.getDeviceId();
        if (TextUtils.isEmpty(imei)) {
            imei = "";
        }
        return imei;
    }

    public static String getImsi(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String imsi = tm.getSubscriberId();
        if (TextUtils.isEmpty(imsi)) {
            imsi = "";
        }
        return imsi;
    }

    public static String getMacAddress(Context context) {
        try {
            WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wifi.getConnectionInfo();
            return info.getMacAddress();
        } catch (Exception e) {
            return "";
        }
    }

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

    public static String getAndroidId(Context context) {
        String id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (id == null) {
            id = "";
        }
        return id;
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

    public static String getChipId() {
        String chipId = getMTKChipId();
        if(TextUtils.isEmpty(chipId)){
            chipId = getQcomChipId();
        }
        if(TextUtils.isEmpty(chipId)){
            chipId = getSparChipID();
        }
        return chipId;
    }

    private static String getMTKChipId() {
        String rid = "";
        InputStream is = null;
        byte[] b = null;
        try {
            Process p = Runtime.getRuntime().exec("cat proc/rid");
            int wait = p.waitFor();
            if (wait != 0)
                return "";
            is = p.getInputStream();
            b = new byte[is.available()];
            is.read(b);
            rid = byte2hex(b);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return rid;
    }

    private static String getQcomChipId() {
        String rid = "";
        InputStream is = null;
        byte[] b = null;
        try {
            Process p = Runtime.getRuntime().exec("cat /sys/devices/soc0/serial_number");
            int wait = p.waitFor();
            if (wait != 0)
                return "";
            is = p.getInputStream();
            b = new byte[is.available()];
            is.read(b);
            rid = (new String(b, "UTF-8")).replace("\n", "");
            rid = Long.toHexString(Long.parseLong(rid)); // to hex
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return rid;
    }

    private static String getSparChipID() {
        String str = "";
        try {
            str = execCommand("cat sys/class/misc/sprd_efuse_otp/dump");
            if (TextUtils.isEmpty(str))
                str = execCommand("cat sys/class/misc/sprd_otp_ap_efuse/dump");
            String strs[] = str.split("\n");
            String str0 = strs[0];
            String str1 = strs[1];
            String uidStr0 = str0.substring(5);
            String uidStr1 = str1.substring(5);
            long longStr0 = Long.parseLong(uidStr0, 16);
            long longStr1 = Long.parseLong(uidStr1, 16);
            long LOTID5 = ((longStr0 & 0x00fc0000) >> 18) + 48;
            long LOTID4 = ((longStr0 & 0x0003f000) >> 12) + 48;
            long LOTID3 = ((longStr0 & 0x00000fc0) >> 6) + 48;
            long LOTID2 = (longStr0 & 0x0000003f) + 48;
            long LOTID1 = ((longStr1 & 0x7e000000) >> 25) + 48;
            long LOTID0 = ((longStr1 & 0x01f80000) >> 19) + 48;
            long WaferID = (longStr1 & 0x0007c000) >> 14;
            long X = (longStr1 & 0x00003f89) >> 7;
            long Y = longStr1 & 0x0000007f;
            char szLOTID5 = (char) LOTID5;
            char szLOTID4 = (char) LOTID4;
            char szLOTID3 = (char) LOTID3;
            char szLOTID2 = (char) LOTID2;
            char szLOTID1 = (char) LOTID1;
            char szLOTID0 = (char) LOTID0;
            str = String.format("%s%s%s%s%s%s%02d%03d%03d", szLOTID5, szLOTID4, szLOTID3, szLOTID2, szLOTID1, szLOTID0, WaferID, X, Y);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return str;
    }


    private static String execCommand(String command) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        Process proc = runtime.exec(command);
        InputStream inputstream = proc.getInputStream();
        InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
        BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
        String line = "";
        StringBuilder sb = new StringBuilder(line);
        while ((line = bufferedreader.readLine()) != null) {
            //System.out.println(line);
            if (line.contains("efuse")) {
                continue;
            }
            sb.append(line);
            sb.append('\n');
        }
        try {
            bufferedreader.close();
        } catch (Exception e) {
            //
        }
        try {
            if (proc.waitFor() != 0) {
                System.err.println("exit value = " + proc.exitValue());
            }
        } catch (InterruptedException e) {
            System.err.println(e);
        }
        return sb.toString();
    }

    private static String byte2hex(byte[] b) {
        String hs = "";
        String stmp = "";
        for (int n = 0; n < b.length; n++) {
            stmp = (Integer.toHexString(b[n] & 0XFF));
            if (stmp.length() == 1) {
                hs = hs + "0" + stmp;
            } else {
                hs = hs + stmp;
            }
        }
        return hs.toUpperCase();
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
