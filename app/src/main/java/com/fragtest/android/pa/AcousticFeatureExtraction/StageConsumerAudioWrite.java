package com.fragtest.android.pa.AcousticFeatureExtraction;

import android.util.Log;

import com.fragtest.android.pa.Core.AudioFileIO;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Write raw audio to disk
 */

public class StageConsumerAudioWrite extends Stage {

    final static String LOG = "StageConsumer";

    AudioFileIO io;
    DataOutputStream stream;

    StageConsumerAudioWrite(LinkedBlockingQueue queue, int id) {
        super(queue, -1, id );

        io = new AudioFileIO();
        stream = io.openDataOutStream(16000, 2, 16, true);

    }

    @Override
    protected void process() {

        boolean abort = false;

        Log.d(LOG, "Start consuming");

        while (!Thread.currentThread().isInterrupted() && !abort) {

            float[] buffer;

            buffer = receive();

            if (buffer != null) {

                byte[] data = new byte[buffer.length * 2];

                for (int i = 0; i < buffer.length; i++) {
                    short tmp = (short) buffer[i];
                    data[i * 2] = (byte) (tmp & 0xff);
                    data[i * 2 + 1] = (byte) ((tmp >> 8) & 0xff);
                }

                try {
                    stream.write(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {
                abort = true;
            }

        }


        Log.d(LOG, "Stopped consuming");

        io.closeDataOutStream();

    }



}
