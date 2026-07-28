package com.amz.ios.serverswitchcontrol;

import android.content.Context;
import android.os.IBinder;
import android.text.TextUtils;

import com.amz.ios.ioslite.common.util.PreferencesUtil;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonUUID {

    public static String getDeviceUUID(Context context) {
        final String KEY_DEVICE_UUID = "key_deviceUUID";
        String uuid = PreferencesUtil.getString(context, KEY_DEVICE_UUID, "");
        if (TextUtils.isEmpty(uuid)) {
            uuid = readUUID();
            if (TextUtils.isEmpty(uuid)) {
                uuid = CommonDeviceInfo.getChipId();
            }
            if (TextUtils.isEmpty(uuid)) {
                UUID uuidtmp = UUID.randomUUID();
                uuid = uuidtmp + "";
            }
            PreferencesUtil.putString(context, KEY_DEVICE_UUID, uuid);
        }
        return uuid;
    }

    private static boolean isUUid(String str) {
        Pattern pattern = Pattern.compile("[\\d|[a-f]|\\-]*");
        Matcher isUUid = pattern.matcher(str);
        return isUUid.matches();
    }

    private static String readUUID() {
        Object result, result1, result2 = null;
        try {
            Class<?> classType = Class.forName("android.os.ServiceManager");
            Object invokeOperation = classType.newInstance();
            Method getMethod = classType.getMethod("getService",
                    new Class[]{String.class});
            result = getMethod.invoke(invokeOperation,
                    new Object[]{new String("TydNativeMisc")});
            Class<?> classType1 = Class
                    .forName("com.ios.internal.server.INativeMiscService$Stub");
            Method getMethod1 = classType1.getMethod("asInterface",
                    new Class[]{IBinder.class});
            result1 = getMethod1.invoke(classType1, new Object[]{result});
            Class<?> classType2 = result1.getClass();
            Method getMethod2 = classType2.getMethod("getDeviceUUID",
                    new Class[]{});
            result2 = getMethod2.invoke(result1, new Object[]{});
        } catch (Exception e) {
        }
        return (result2 != null) ? result2.toString() : "";
    }
}
