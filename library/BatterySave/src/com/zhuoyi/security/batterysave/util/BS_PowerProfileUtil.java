package com.zhuoyi.security.batterysave.util;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.Log;

import com.zhuoyi.security.batterysave.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.sax2.Driver;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by Administrator on 2016/10/9.
 */

public class BS_PowerProfileUtil {

    /**
     * Average power consumption when camera flashlight is on.
     */
    public static final String POWER_FLASHLIGHT = "camera.flashlight";

    /**
     * Average power consumption when the camera is on over all standard use cases.
     */
    public static final String POWER_CAMERA = "camera.avg";

    public static final String POWER_CPU_SPEEDS = "cpu.speeds";

    /**
     * Battery capacity in milliAmpHour (mAh).
     */
    public static final String POWER_BATTERY_CAPACITY = "battery.capacity";

    private static final String TAG_DEVICE = "device";
    private static final String TAG_ITEM = "item";
    private static final String TAG_ARRAY = "array";
    private static final String TAG_ARRAYITEM = "value";
    private static final String ATTR_NAME = "name";

    static final HashMap<String, Object> sPowerMap = new HashMap<>();

    public BS_PowerProfileUtil(Context context) {
        // Read the XML file for the given profile (normally only one per
        // device)
        if (sPowerMap.size() == 0) {
            readPowerValuesFromXml(context);
        }
    }

    private void readPowerValuesFromXml(Context context) {
        //int id = com.android.internal.R.xml.power_profile;
        Resources mResources = Resources.getSystem();  //getResources()测试也可以
        int id = mResources.getIdentifier("power_profile", "xml", "android");
        Log.e("zengrui","============"+id+"===");
        if (id <= 0) {
            id = R.xml.bs_power_profile;
        }

        //Resources.getSystem().getXml()
        final Resources resources = context.getResources();
        XmlResourceParser parser = resources.getXml(id);
        boolean parsingArray = false;
        ArrayList<Double> array = new ArrayList<Double>();
        String arrayName = null;

        try {
            BS_XmlUtils.beginDocument(parser, TAG_DEVICE);

            while (true) {
                BS_XmlUtils.nextElement(parser);

                String element = parser.getName();
                //Log.e("zengrui","============"+element+"==="+parsingArray);
                if (element == null) break;

                if (parsingArray && !element.equals(TAG_ARRAYITEM)) {
                    // Finish array
                   // Log.e("zengrui","============"+element+"=----"+arrayName);
                    sPowerMap.put(arrayName, array.toArray(new Double[array.size()]));
                    parsingArray = false;
                }
                if (element.equals(TAG_ARRAY)) {
                    parsingArray = true;
                    array.clear();
                    arrayName = parser.getAttributeValue(null, ATTR_NAME);
                } else if (element.equals(TAG_ITEM) || element.equals(TAG_ARRAYITEM)) {
                    String name = null;
                    if (!parsingArray) name = parser.getAttributeValue(null, ATTR_NAME);
                    if (parser.next() == XmlPullParser.TEXT) {
                        String power = parser.getText();
                        double value = 0;
                        try {
                            value = Double.valueOf(power);
                        } catch (NumberFormatException nfe) {
                        }
                        if (element.equals(TAG_ITEM)) {
                            sPowerMap.put(name, value);
                        } else if (parsingArray) {
                            array.add(value);
                        }
                    }
                }
            }
            if (parsingArray) {
                sPowerMap.put(arrayName, array.toArray(new Double[array.size()]));
            }
        } catch (XmlPullParserException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            parser.close();
        }


    }

    public void getsPowerMap() {
        for (Map.Entry<String, Object> entry : sPowerMap.entrySet()) {
            String value = "";
            Object v = entry.getValue();
           /* if ( v instanceof Double){
                DecimalFormat df = new DecimalFormat("0.00");
                value = df.format(v);
                //value = (int) v+"";
                //Log.e("zengrui", entry.getKey() + "====####" + value);
            } else {
                value = v+"";
            }*/
            Log.e("zengrui", entry.getKey() + "+++====" + entry.getValue());
        }
    }

    /**
     * Returns the battery capacity, if available, in milli Amp Hours. If not available,
     * it returns zero.
     *
     * @return the battery capacity in mAh
     */
    public double getBatteryCapacity() {
        return getAveragePower(POWER_BATTERY_CAPACITY);
    }

    /**
     * Returns the average current in mA consumed by the subsystem
     *
     * @param type the subsystem type
     * @return the average current in milliAmps.
     */
    public double getAveragePower(String type) {
        return getAveragePowerOrDefault(type, 0);
    }


    /**
     * Returns the average current in mA consumed by the subsystem, or the given
     * default value if the subsystem has no recorded value.
     *
     * @param type         the subsystem type
     * @param defaultValue the value to return if the subsystem has no recorded value.
     * @return the average current in milliAmps.
     */
    public double getAveragePowerOrDefault(String type, double defaultValue) {
        if (sPowerMap.containsKey(type)) {
            Object data = sPowerMap.get(type);
            if (data instanceof Double[]) {
                return ((Double[]) data)[0];
            } else {
                return (Double) sPowerMap.get(type);
            }
        } else {
            return defaultValue;
        }
    }

    /**
     * Returns the average current in mA consumed by the subsystem for the given level.
     *
     * @param type  the subsystem type
     * @param level the level of power at which the subsystem is running. For instance, the
     *              signal strength of the cell network between 0 and 4 (if there are 4 bars max.)
     *              If there is no data for multiple levels, the level is ignored.
     * @return the average current in milliAmps.
     */
    public double getAveragePower(String type, int level) {
        if (sPowerMap.containsKey(type)) {
            Object data = sPowerMap.get(type);
            if (data instanceof Double[]) {
                final Double[] values = (Double[]) data;
                if (values.length > level && level >= 0) {
                    return values[level];
                } else if (level < 0 || values.length == 0) {
                    return 0;
                } else {
                    return values[values.length - 1];
                }
            } else {
                return (Double) data;
            }
        } else {
            return 0;
        }
    }

    /**
     * Returns the number of speeds that the CPU can be run at.
     * @return
     */
    public int getNumSpeedSteps() {
        Object value = sPowerMap.get(POWER_CPU_SPEEDS);
        if (value != null && value instanceof Double[]) {
            return ((Double[])value).length;
        }
        return 1; // Only one speed
    }
}
