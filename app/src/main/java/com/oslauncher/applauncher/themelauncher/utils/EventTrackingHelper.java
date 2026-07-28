package com.oslauncher.applauncher.themelauncher.utils;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.HashMap;

public class EventTrackingHelper {
    public static void logEvent(Context context, String eventName) {
        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        Bundle bundle = new Bundle();
        firebaseAnalytics.logEvent(eventName, bundle);
    }

    public static void logEventWithParam(Context context, String eventName, String param, String value) {
        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        Bundle bundle = new Bundle();
        bundle.putString(param, value);
        firebaseAnalytics.logEvent(eventName, bundle);
    }

    public static void logEventWithMultipleParam(Context context, String eventName, HashMap<String, String> params) {
        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        Bundle bundle = new Bundle();
        for (String key : params.keySet()) {
            bundle.putString(key, params.get(key));
        }
        firebaseAnalytics.logEvent(eventName, bundle);
    }
}
