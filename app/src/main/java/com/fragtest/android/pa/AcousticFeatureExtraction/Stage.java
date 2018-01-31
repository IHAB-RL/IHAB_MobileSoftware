package com.fragtest.android.pa.AcousticFeatureExtraction;

import android.util.Log;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Abstract class to implement producers (output), consumers (input) and conducers (in- and output).
 * Data is transferred using queues.
 */

abstract class Stage {

    private Thread thread;
    private LinkedBlockingQueue<float[][]> inQueue;
    private LinkedBlockingQueue[] outQueue;
    int nConsumer, id;


    Stage(LinkedBlockingQueue inQueue, int nConsumer, int id) {

        this.inQueue = inQueue;
        this.nConsumer = nConsumer;
        this.id = id;

        if (nConsumer > 0) {

            outQueue = new LinkedBlockingQueue[nConsumer];

            for (int i = 0; i < nConsumer; i++ ) {

                outQueue[i] = new LinkedBlockingQueue();

            }

        }

        System.out.println("Init Conducer " + id  + " with " + nConsumer + " consumers");
    }


    LinkedBlockingQueue[] getQueues() {

        return outQueue;
    }


    public void start() {

        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                process();
            }
        };

        thread = new Thread(runnable);
        thread.start();
    }


    public void stop() {
        thread.interrupt();
    }


    public float[][] receive() {

        try {
            Log.d("Stage", "Elements in inQueue " + id + ": " + inQueue.size());
            return inQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.d("Stage", "Cannot take element from queue. Empty?");
            return null;
        }
    }


    public void send(float[][] data) {

        try {
            for (int i = 0; i < outQueue.length; i++) {
                Log.d("Stage", "Elements in outQueue " + id + ":" + i + ": " + outQueue[i].size());
                outQueue[i].put(data);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    protected abstract void process();

}
