package com.example.watchlist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class TimeCheckReceiver extends BroadcastReceiver {

    private static final String TAG = "TimeCheckReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "TimeCheckReceiver triggered");
        Intent serviceIntent = new Intent(context, MonitorService.class);
        context.startService(serviceIntent);
    }
}
