package com.amz.ios.launcher.awareness;

import android.content.ContentResolver;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.amz.ios.ioslite.common.util.DeviceInfoUtil;
import com.amz.ios.launcher.Launcher;

import java.lang.reflect.Method;

/**
 * IOS OS 悬浮手势操作；
 */
public class SensorGestureModel {
    private static final int TYPE_GESTURE = 46;
    private static final int IOS_GESTURE_LAUNCHER_SLIDE = 1 << 1;
    private boolean hasRegisterSensor = false;
    private GestureSensorListener mGestureSensorListener;
    private SensorManager mSensorManager;

    private Launcher mLauncher;

    public SensorGestureModel(Launcher launcher) {
        mLauncher = launcher;
        mGestureSensorListener = new GestureSensorListener();
        mSensorManager = (SensorManager) mLauncher.getSystemService(Context.SENSOR_SERVICE);
    }

    public boolean isWorkspaceNeedToScroll() {
        return !mLauncher.isWorkspaceLocked() && mLauncher.getWorkspace().isInNormalMode();
    }

    public boolean getEnableOfGestureSensor() {
        boolean result = false;
        try {
            if (DeviceInfoUtil.getSystemProperty("ro.ios.non_touch_operation", 0) == 0) {
                return false;
            }
            ClassLoader cl = mLauncher.getClassLoader();
            Class settingsClass = cl.loadClass("com.ios.provider.IOSSettings$System");
            Method method = settingsClass.getMethod("getBoolbit", ContentResolver.class, String.class, int.class, boolean.class);
            result = (Boolean) method.invoke(null, mLauncher.getContentResolver(), "ios_gesture_sets",
                    IOS_GESTURE_LAUNCHER_SLIDE, false);
        } catch (Exception e) {
            //
        }

        return result;
    }

    public void onResume() {
        if (getEnableOfGestureSensor()) {
            registerSensorListener();
        } else {
            unRegisterSensorListener();
        }
    }

    public void onPause() {
        unRegisterSensorListener();
    }

    public void registerSensorListener() {
        if (!hasRegisterSensor) {
            mSensorManager.registerListener(mGestureSensorListener, mSensorManager.getDefaultSensor(TYPE_GESTURE),
                    SensorManager.SENSOR_DELAY_FASTEST);
            hasRegisterSensor = true;
        }
    }

    public void unRegisterSensorListener() {
        if (hasRegisterSensor) {
            mSensorManager.unregisterListener(mGestureSensorListener);
            hasRegisterSensor = false;
        }
    }

    class GestureSensorListener implements SensorEventListener {
        private static final int GESTURE_LEFT = 1;

        private static final int GESTURE_RIGHT = 2;

        private static final int GESTURE_UP = 3;

        private static final int GESTURE_DOWN = 4;

        @Override
        public void onSensorChanged(SensorEvent event) {
            final float[] values = event.values;
            int action = -1;
            try {
                ClassLoader cl = mLauncher.getClassLoader();
                Class sensorClass = cl.loadClass("com.ios.hardware.IOSSensorManager");
                Method method = sensorClass.getMethod("mapGesSensorDataToWindow", int.class);
                action = (Integer) method.invoke(null, (int) values[0]);
            } catch (Exception e) {
            }

            switch (action) {
                case GESTURE_LEFT:
                case GESTURE_DOWN:
                    if (isWorkspaceNeedToScroll()) {
                        mLauncher.getWorkspace().exitWidgetResizeMode();
                        mLauncher.getWorkspace().scrollLeft();
                    }
                    break;
                case GESTURE_RIGHT:
                case GESTURE_UP:
                    if (isWorkspaceNeedToScroll()) {
                        mLauncher.getWorkspace().exitWidgetResizeMode();
                        mLauncher.getWorkspace().scrollRight();
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    }
}
