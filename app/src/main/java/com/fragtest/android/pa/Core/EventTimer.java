package com.fragtest.android.pa.Core;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Messenger;
import android.util.Log;

import com.fragtest.android.pa.BuildConfig;

/**
 * Timer to trigger questionnaire and display remaining time.
 */

public class EventTimer {

    private static final String LOG = "EventTimer";
    private Context context;
    private Messenger messenger;
    private AlarmManager mAlarmManager;
    private PendingIntent mAlarmIntent;
    private int mFinalCountDown;
    private boolean isSet = false;

    public EventTimer(Context ctx, Messenger msg) {

        context = ctx;
        messenger = msg;
        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (BuildConfig.DEBUG) {
            Log.d(LOG,"EventTimer created.");
        }
    }

    public void setTimer(int interval) {

        // Alarm will call EventReceiver class by sending broadcast along with messenger context
        Intent intent = new Intent(context, EventReceiver.class);
        intent.putExtra("Messenger", messenger);

        mAlarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        // Schedules the initialisation of new questionnaire
        mAlarmManager.setExact(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + interval*1000, mAlarmIntent);

        // Final alarm time for visual count down;
        mFinalCountDown = (int) (System.currentTimeMillis()/1000) + interval;

        isSet = true;

        if (BuildConfig.DEBUG){
            Log.d(LOG,"New timer interval set to "+interval+"s");
        }
    }

    public void stopTimer() {

        if (mAlarmManager == null) {
            Log.e(LOG, "AlarmManager is null.");
        } else {
            Log.i(LOG, "AlarmManager: "+mAlarmManager);
        }

        if (isSet) {
            mAlarmManager.cancel(mAlarmIntent);
        }

        isSet = false;

        if (BuildConfig.DEBUG) {
            Log.i(LOG,"Timer cancelled.");
        }
    }

    public int getFinalCountDown() {
        return mFinalCountDown;
    }
}
