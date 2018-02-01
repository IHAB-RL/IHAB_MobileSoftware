package com.fragtest.android.pa.AcousticFeatureExtraction;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.fragtest.android.pa.Core.AudioFileIO;

import java.io.DataOutputStream;
import java.io.IOException;


/**
 * Capture audio using Android's AudioRecorder
 */

public class StageAudioCapture extends Stage {

    final static String LOG = "StageProducer";

    private AudioFileIO io;
    private AudioRecord audioRecord;
    private DataOutputStream stream;
    private int bufferSize, channels;
    private boolean stopRecording = false;

    StageAudioCapture(int nConsumer, int id, int samplerate) {
        super(null, nConsumer, id);

        Log.d(LOG, "Setting up audioCapture");

        channels = 2;

        bufferSize = AudioRecord.getMinBufferSize(samplerate,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        Log.d(LOG, "Buffersize: " + bufferSize);

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.DEFAULT,
                samplerate,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        );

        io = new AudioFileIO("direct");
        stream = io.openDataOutStream(
                samplerate,
                2,
                16,
                true);

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

            // write to file
            byte[] outBuffer = new byte[data.length * data[0].length * 2];

            for (int i = 0; i < data[0].length; i++) {

                short tmp = (short) data[0][i];
                outBuffer[i * 4] = (byte) (tmp & 0xff);
                outBuffer[i * 4 + 1] = (byte) ((tmp >> 8) & 0xff);

                tmp = (short) data[1][i];
                outBuffer[i * 4 + 2] = (byte) (tmp & 0xff);
                outBuffer[i * 4 + 3] = (byte) ((tmp >> 8) & 0xff);

            }

            try {
                stream.write(outBuffer);
            } catch (IOException e) {
                e.printStackTrace();
            }


        }

        Log.d(LOG, "Stopped producing");
        audioRecord.stop();
        io.closeDataOutStream();

    }


    public void setStopRecording() {

        stopRecording = true;

    }

}
