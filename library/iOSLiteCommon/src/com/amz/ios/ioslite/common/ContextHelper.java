package com.amz.ios.ioslite.common;

import static android.content.Context.RECEIVER_EXPORTED;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

public class ContextHelper {

    public static Intent registerReceiver(Context context,
                                          BroadcastReceiver receiver,
                                          IntentFilter filter) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context.registerReceiver(receiver, filter, RECEIVER_EXPORTED);
        }else {
            return context.registerReceiver(receiver, filter);
        }
    }

}
