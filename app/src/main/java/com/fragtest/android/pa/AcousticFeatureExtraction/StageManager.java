package com.fragtest.android.pa.AcousticFeatureExtraction;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Sets up and starts stages, i.e. producers, conducers and consumers.
 */

public class StageManager {

    StageProducerAudioCapture producer;
    StageConsumerAudioWrite consumer;

    public StageManager() {

        LinkedBlockingQueue[] queue;

        producer = new StageProducerAudioCapture(1, 1);
        queue = producer.getQueues();
        consumer = new StageConsumerAudioWrite(queue[0], 2);

    }

    public void start() {

        producer.start();
        consumer.start();

    }

    public void stop() {

        producer.setStopRecording();
        consumer.stop();

    }


}
