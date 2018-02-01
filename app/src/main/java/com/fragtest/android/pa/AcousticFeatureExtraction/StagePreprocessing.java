package com.fragtest.android.pa.AcousticFeatureExtraction;

import android.util.Log;

import com.fragtest.android.pa.Processing.Preprocessing.FilterHP;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Audio preprocessing
 */

public class StagePreprocessing extends Stage {

    final static String LOG = "StagePreprocessing";

    int samplerate;

    StagePreprocessing(LinkedBlockingQueue queue, int nConsumer, int id, int samplerate) {
        super(queue, nConsumer, id);

        this.samplerate = samplerate;
    }

    @Override
    protected void process() {

        boolean abort = false;

        Log.d(LOG, "Start processing");

        FilterHP[] highpass = new FilterHP[2];
        for (int i = 0; i < 2; i++) {
            highpass[i] = new FilterHP(samplerate, 250);
        }

        while (!Thread.currentThread().isInterrupted() && !abort) {

            float[][] buffer;

            buffer = receive();

            if (buffer != null) {

                for (int i = 0; i < 2; i++) {
                    highpass[i].filter(buffer[i]);
                }

                send(buffer);

            } else {
                abort = true;
            }

        }


        Log.d(LOG, "Stopped consuming");

    }

}
