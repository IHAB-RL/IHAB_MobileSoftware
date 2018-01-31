package com.fragtest.android.pa.AcousticFeatureExtraction;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;


/**
 * Capture audio using Android's AudioRecorder
 */

public class StageAudioCapture extends Stage {

    final static String LOG = "StageProducer";

    private AudioRecord audioRecord;
    private int samplerate, bufferSize, channels;
    private boolean stopRecording = false;

    StageAudioCapture(int nConsumer, int id) {
        super(null, nConsumer, id);

        Log.d(LOG, "Setting up audioCapture");

        samplerate = 16000;
        channels = 2;

        bufferSize = AudioRecord.getMinBufferSize(samplerate,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.DEFAULT,
                samplerate,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        );

    }

    @Override
    protected void process() {

        float samples = 0;
        short[] buffer = new short[bufferSize];

        audioRecord.startRecording();

        Log.d(LOG, "Started producing");

        while (!stopRecording & !Thread.currentThread().isInterrupted()) {

            float[][] data = new float[channels][bufferSize / channels];

            samples += audioRecord.read(buffer, 0, bufferSize);

            // split channels
            for (int i = 0; i < data[0].length; i++) {
                data[0][i] = buffer[i * 2];
                data[1][i] = buffer[i * 2 + 1];
            }

            send(data);

        }

        Log.d(LOG, "Stopped producing");
        audioRecord.stop();

    }


    public void setStopRecording() {

        stopRecording = true;

    }

}
