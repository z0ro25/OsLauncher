package com.amz.ios.launcher;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by 37 on 10/30/2019.
 *
 */

public class TypefaceUtil {
    public static void overridFont(Context context, String defaultFontNameToOverride, String customFontFileInAssets) {
        final Typeface customFontTypeface = Typeface.createFromAsset(context.getAssets(), customFontFileInAssets);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Map<String, Typeface> newMap = new HashMap<>();
            newMap.put(defaultFontNameToOverride, customFontTypeface);
            try {
                final Field staticField = Typeface.class.getDeclaredField("sSystemFontMap");
                staticField.setAccessible(true);
                staticField.set(null, newMap);
            }
            catch (NoSuchFieldException|IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                final Field defaultFontTypefaceField = Typeface.class.getDeclaredField(defaultFontNameToOverride);
                defaultFontTypefaceField.setAccessible(true);
                defaultFontTypefaceField.set(null, customFontTypeface);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
