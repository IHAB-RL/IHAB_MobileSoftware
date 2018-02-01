package com.fragtest.android.pa.Core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.fragtest.android.pa.MainActivity;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(mainIntent);
    }
}