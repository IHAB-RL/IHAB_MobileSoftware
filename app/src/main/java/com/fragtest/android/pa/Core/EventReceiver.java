package com.fragtest.android.pa.Core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Messenger;
import android.util.Log;

/**
 * Receiver events set in EventTimer
 */



public class EventReceiver extends BroadcastReceiver {

    private static final String LOG = "EventReceiver";
    private static Context mContext;


    @Override
    public void onReceive(Context context, Intent intent) {

        mContext = context.getApplicationContext();
        Log.d(LOG, "Context:" + mContext.toString());


        Messenger messenger = intent.getParcelableExtra("Messenger");

        Log.e(LOG,"messenger from pete: "+messenger.toString());

        sendMessageToService();

    }

    private static void sendMessageToService() {
        Intent intent = new Intent("AlarmReceived");
        mContext.sendBroadcast(intent);
    }
}