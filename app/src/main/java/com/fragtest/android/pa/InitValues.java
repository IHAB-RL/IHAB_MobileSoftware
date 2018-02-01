package com.fragtest.android.pa;

import android.os.Bundle;

/**
 * Created by ul1021 on 18.10.2017.
 */

public class InitValues {

    // preferences
    public static boolean isTimer = true,
            isWave = false,
            keepAudioCache = false,
            filterHp = false,
            downsample = true,
            showConfigButton = true,
            showRecordingButton = true;

    public static int samplerate = 16000,
            chunklengthInS = 60,
            filterHpFrequency = 100,
            finalCountDown = -255,
            timerInterval = -255;

    Bundle initBundle;

    // Load initial values
    public InitValues() {

        initBundle = new Bundle();
        initBundle.putBoolean("isTimer", isTimer);
        initBundle.putBoolean("isWave", isWave);
        initBundle.putBoolean("keepAudioCache", keepAudioCache);
        initBundle.putBoolean("filterHp", filterHp);
        initBundle.putBoolean("downsample", downsample);
        initBundle.putBoolean("showConfigButton", showConfigButton);
        initBundle.putBoolean("showRecordingButton", showRecordingButton);

        initBundle.putInt("samplerate", samplerate);
        initBundle.putInt("chunklengthInS", chunklengthInS);
        initBundle.putInt("filterHpFrequency", filterHpFrequency);
        initBundle.putInt("finalCountDown", finalCountDown);
        initBundle.putInt("timerInterval", timerInterval);

    }

    public Bundle getInitBundle() {
        return initBundle;
    }

    public int getInitInt(String string) {
        return initBundle.getInt(string);
    }

    public boolean getInitBoolean(String string) {
        return initBundle.getBoolean(string);
    }
}
