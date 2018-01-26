package com.fragtest.android.pa.AcousticFeatureExtraction;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;


/**
 * Capture audio using Android's AudioRecorder
 */

public class StageProducerAudioCapture extends Stage {

    final static String LOG = "StageProducer";

    private AudioRecord audioRecord;
    private int samplerate, bufferSize;
    private boolean stopRecording = false;

    StageProducerAudioCapture(int nConsumer, int id) {
        super(null, nConsumer, id);

        Log.d(LOG, "Setting up producer");

        samplerate = 16000;

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

            Log.d(LOG, "Flag: " + stopRecording );

            float[] data = new float[bufferSize];

            samples += audioRecord.read(buffer, 0, bufferSize);

            // cast audio buffer to float
            for (int i = 0; i < buffer.length; i++) {
                data[i] = (float) buffer[i];
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
