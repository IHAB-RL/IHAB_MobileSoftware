package com.fragtest.android.pa.Core;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;

import static android.content.Context.VIBRATOR_SERVICE;

/**
 * Created by ul1021 on 13.07.2017.
 */

public class Vibration {

    private final static String LOG = "Vibration";
    private Context mContext;
    // Interval between bursts of vibration when reminder alarm is set off
    private final static int mVibrationInterval_ms = 1000;
    private final static int mVibrationDuration_ms = 200;
    private final static int mLengthWakeLock_ms = 2000;
    private static int mNumberOfBursts = 0;
    private final static int mMaxVibrationDuration_ms = 30000;
    private static int mMaxNumberOfBursts;
    private final Handler mTimerHandler = new Handler();
    private boolean isActive = false;
    private Vibrator mVibrator;

    private final Runnable loop = new Runnable() {
        @Override
        public void run() {
            if (isActive && (mNumberOfBursts < mMaxNumberOfBursts)) {
                mVibrator.vibrate(mVibrationDuration_ms);
                mNumberOfBursts++;
                Log.e(LOG, "Ring.");
                mTimerHandler.postDelayed(this, mVibrationInterval_ms);
            }
        }
    };

    public Vibration(Context context) {
        mContext = context;
        mMaxNumberOfBursts = mMaxVibrationDuration_ms / mVibrationInterval_ms;
        mVibrator = ((Vibrator) mContext.getSystemService(VIBRATOR_SERVICE));
    }

    public void singleBurst() {
        mVibrator.vibrate(mVibrationDuration_ms);
    }

    public void repeatingBurstOn() {
        if (!isActive) { // ensure that only one alarm is annoying us at any given time
            mTimerHandler.post(loop);
            PowerManager pm = (PowerManager) mContext.getSystemService(
                    Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                    PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
            wakeLock.acquire(mLengthWakeLock_ms);
        }
        mNumberOfBursts = 0;
        isActive = true;
    }

    public void repeatingBurstOff() {
        mTimerHandler.removeCallbacks(loop);
        KeyguardManager keyguardManager = (KeyguardManager) mContext.getSystemService(
                Context.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("TAG");
        keyguardLock.disableKeyguard();
        isActive = false;
    }
}
