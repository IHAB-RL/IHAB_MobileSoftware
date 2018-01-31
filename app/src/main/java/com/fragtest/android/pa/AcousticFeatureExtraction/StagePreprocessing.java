package com.fragtest.android.pa.AcousticFeatureExtraction;

import android.util.Log;

import com.fragtest.android.pa.Processing.Preprocessing.FilterHP;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Audio preprocessing
 */

public class StagePreprocessing extends Stage {

    final static String LOG = "StagePreprocessing";

    StagePreprocessing(LinkedBlockingQueue queue, int nConsumer, int id) {
        super(queue, nConsumer, id);

    }

    @Override
    protected void process() {

        boolean abort = false;

        Log.d(LOG, "Start processing");

        FilterHP[] highpass = new FilterHP[2];
        for (int i = 0; i < 2; i++) {
            highpass[i] = new FilterHP(16000, 250);
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
