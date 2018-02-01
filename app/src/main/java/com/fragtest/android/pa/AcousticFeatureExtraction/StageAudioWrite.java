package com.fragtest.android.pa.AcousticFeatureExtraction;

import android.util.Log;

import com.fragtest.android.pa.Core.AudioFileIO;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.Channels;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Write raw audio to disk
 */

public class StageAudioWrite extends Stage {

    final static String LOG = "StageConsumer";

    AudioFileIO io;
    DataOutputStream stream;

    StageAudioWrite(LinkedBlockingQueue queue, int id, int samplerate, String filename) {
        super(queue, 0, id );

        io = new AudioFileIO(filename);
        stream = io.openDataOutStream(
                samplerate,
                2,
                16,
                true);

    }

    @Override
    protected void process() {

        boolean abort = false;

        Log.d(LOG, "Start consuming");

        float[][] data;

        while (!Thread.currentThread().isInterrupted() && !abort) {

            data = receive();

            if (data != null) {

                byte[] buffer = new byte[data.length * data[0].length * 2];

                for (int i = 0; i < data[0].length; i++) {

                    short tmp = (short) data[0][i];
                    buffer[i * 4] = (byte) (tmp & 0xff);
                    buffer[i * 4 + 1] = (byte) ((tmp >> 8) & 0xff);

                    tmp = (short) data[1][i];
                    buffer[i * 4 + 2] = (byte) (tmp & 0xff);
                    buffer[i * 4 + 3] = (byte) ((tmp >> 8) & 0xff);

                }

                try {
                    stream.write(buffer);
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
