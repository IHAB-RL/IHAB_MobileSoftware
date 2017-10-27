
package com.fragtest.android.pa.Processing;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.fragtest.android.pa.ControlService;
import com.fragtest.android.pa.Processing.Preprocessing.CResampling;
import com.fragtest.android.pa.Processing.Preprocessing.FilterHP;

import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;
import java.util.Set;

/**
 * BasicProcessingThread.java
 *
 * Loads audio data, applies preprocessing and calls features using process.
 */

public class BasicProcessingThread extends Thread {

	protected static final String LOG = "HALLO:Processing";
	protected static final int DONE = 1;
	
	private Messenger serviceMessenger = null;	// instance of messenger to communicate with service
	protected final Messenger processMessenger = new Messenger(new ProcessHandler());
	private String filename			= null;	// name and full path of the file to process
    public static String timestamp      = null; // string with information when the recoding started
    protected byte[] buffer 			= null;	// byte buffer
	protected float[][] audioData 		= null;	// buffer
	private boolean filterHp;
    private int filterHpFrequency;
    static int samplerate;
    static int channels;
	int chunklengthInS;					// in ms
    private boolean downsample = false;

    private Set<String> activeFeatures;
    private int processedFeatures = 0;
	private ArrayList<String> featureFiles = new ArrayList<String>();

	Queue recordingQueue;

    CResampling[] resampler;
    FilterHP[] highpass;
	
	// constructor
	public BasicProcessingThread(Messenger messenger, Queue queue, Bundle settings){

        serviceMessenger = messenger;
		recordingQueue = queue;

        samplerate = settings.getInt("samplerate");
        activeFeatures = (Set) settings.getSerializable("activeFeatures");
        filterHp = settings.getBoolean("filterHp");
        filterHpFrequency = settings.getInt("filterHpFrequency");
        filename = settings.getString("filename");
        downsample = settings.getBoolean("downsample", false);
        channels = 2;

        // extract timestamp from filename
//        timestamp = filename.substring(filename.lastIndexOf("/")+1);
//        timestamp = timestamp.substring(0, timestamp.lastIndexOf("."));

        // set up preprocessing (downsampling & filtering)
        if (downsample) {
            CResampling[] resampler = new CResampling[channels];
            for (int i = 0; i < channels; i++) {
                resampler[i] = new CResampling();
            }
        }

        if (filterHp) {
            FilterHP[] highpass = new FilterHP[channels];
            for (int i = 0; i < channels; i++) {
                highpass[i] = new FilterHP(samplerate, filterHpFrequency);
            }
        }

	}


	@Override
	public void run(){
		audioData = preprocess();
		process();
	}


    // overload in MainProcessingThread
    public void process() {}


	private float[][] preprocess() {

        int frames = buffer.length / 2; // 2 channels

		float[][] audioData = new float[2][frames];

		// split channels
		for ( int kk = 0; kk < frames; kk++ ) {
			audioData[0][kk] = buffer[kk*2];
			audioData[1][kk] = buffer[kk*2 + 1];
		}

        // downsample audio data and resize array
		if (downsample) {

            samplerate /= 2;

            for (int i = 0; i< channels; i++) {
                resampler[i].Downsample2f(audioData[i], frames / 2);
                audioData[i] = Arrays.copyOf(audioData[i], frames / 2);
            }
		}

        // high-pass filter
        if (filterHp) {

            for (int i = 0; i < channels; i++) {
                highpass[i].filter(audioData[i]);
            }
        }

		return audioData;

	}


	// check if all active features have been processed
	private void isFinished() {

        processedFeatures++;

        if (activeFeatures.size() == processedFeatures) {

    		Message msg = Message.obtain(null, ControlService.MSG_CHUNK_PROCESSED);
    		
    		// attach filenames to message so we can notify MediaScanner. 
    		Bundle b = new Bundle();
    		b.putStringArrayList("featureFiles", featureFiles);
    		msg.setData(b);
    		
    		try {
    			serviceMessenger.send(msg);								// and tell service
    		} catch (RemoteException e) {
    			e.printStackTrace();
    		}
		}
	}


	// check if feature is active, called by MainProcessingThread to
    // determine whether or not to process a given feature
	protected boolean isActiveFeature(String s){
		boolean result = false;
		for (String feature : activeFeatures) {
			if (s.equalsIgnoreCase(feature)) {
				result = true;
                break;
			}
		}
		return result;
	}


    // Handler of incoming messages from clients.
	private class ProcessHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case DONE:
            	Bundle b = msg.getData();
				String featureFile = b.getString("featureFile");
            	featureFiles.add(featureFile);
				if (featureFile != null) {
					Logger.info("New feature:\t{}", featureFile);
				}
            	isFinished();
            	break;
            default:
            	super.handleMessage(msg);
            }
        }
	}
}
