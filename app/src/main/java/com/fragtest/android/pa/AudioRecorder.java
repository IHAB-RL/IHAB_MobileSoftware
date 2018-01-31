package com.fragtest.android.pa;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.fragtest.android.pa.Core.AudioFileIO;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Queue;

/**
 * Record audio using Android's AudioRecorder
 */

public class AudioRecorder {

    private final static int CHANNELS = 2;
    private final static int BITS = 16;

    final static String LOG = "AudioRecorder";

    private AudioRecord audioRecord;
    private Thread recordingThread;
    private Queue queue;
    private boolean stopRecording = true;
    private int chunklengthInBytes, bufferSize, channels;
    private Messenger messenger;


    AudioRecorder(Messenger _messenger, Queue _queue, int _samplerate) {

        messenger = _messenger;
        queue = _queue;

        channels = 2;

        bufferSize = AudioRecord.getMinBufferSize(_samplerate,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.DEFAULT,
                _samplerate,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        );

        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                recordAudio();
            }
        }, "AudioRecorder Thread");
    }


    public void start() {

        stopRecording = false;
        recordingThread.start();
    }


    public void stop() {

        stopRecording = true;
    }


    public void close() {

        audioRecord.release();
    }


    private void recordAudio() {

        audioRecord.startRecording();

        short[] buffer = new short[bufferSize]; // effectively 2x buffersize?
        float samples = 0;

        // recording loop
        while (!stopRecording &&
                (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING)) {

            samples += audioRecord.read(buffer, 0, bufferSize);

            short[][] output = new short[channels][bufferSize / 2];

            for (int i = 0; i < bufferSize/2; i++) {
                output[0][i] = buffer[i*2];
                output[1][i] = buffer[i*2+1];
            }

            queue.add(output);
        }

        audioRecord.stop();

        // report back to service
        Message msg = Message.obtain(null, ControlService.MSG_RECORDING_STOPPED);
        Bundle data = new Bundle();
        data.putFloat("totalsamples", samples);
        msg.setData(data);
        try {
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}
