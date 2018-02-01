package com.fragtest.android.pa.AcousticFeatureExtraction;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Sets up and starts stages, i.e. producers, conducers and consumers.
 *
 *
 */

public class StageManager {

    StageAudioCapture audioCapture;
    StagePreprocessing preprocFilter;
    StageAudioWrite audioWriteFilter, audioWriteRaw;

    public StageManager() {

        int samplerate = 44100;

        LinkedBlockingQueue[] queue1, queue2;

        audioCapture = new StageAudioCapture(2, 1, samplerate);
        queue1 = audioCapture.getQueues();

        preprocFilter = new StagePreprocessing(queue1[0], 1, 2, samplerate);
        queue2= preprocFilter.getQueues();

        audioWriteRaw = new StageAudioWrite(queue1[1], 3, samplerate, "raw");
        audioWriteFilter = new StageAudioWrite(queue2[0], 4, samplerate, "filter");

    }

    public void start() {

        audioCapture.start();
        preprocFilter.start();
        audioWriteRaw.start();
        audioWriteFilter.start();

    }

    public void stop() {

        audioCapture.setStopRecording();
        preprocFilter.stop();
        audioWriteRaw.stop();
        audioWriteFilter.stop();

    }


}
