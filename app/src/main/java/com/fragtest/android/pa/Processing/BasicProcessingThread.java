
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
 * Loads audio data, applies preprocessing and calls features using mainRoutine.
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
	int chunklengthInS;					// in ms
    private boolean downsample = false;

    private Set<String> activeFeatures;
    private int processedFeatures = 0;
	private ArrayList<String> featureFiles = new ArrayList<String>();

	Queue recordingQueue;
	
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

        // extract timestamp from filename
//        timestamp = filename.substring(filename.lastIndexOf("/")+1);
//        timestamp = timestamp.substring(0, timestamp.lastIndexOf("."));
	}


	@Override
	public void run(){
		audioData = preprocess();	// read audio data from temp or wav file
		mainRoutine();				// do processing
	}


    // overload in MainProcessingThread
    public void mainRoutine() {}


	private float[][] preprocess() {

        int frames = buffer.length / 4; // 2 channels, 2 bytes per sample

		float[][] audioData = new float[2][frames];

		float invMaxShort = 1.0f / Short.MAX_VALUE;

		// convert bytes to short and split channels
		for ( int kk = 0; kk < frames; kk++ ) {
			audioData[0][kk] = ( (short) ((buffer[kk*4] & 0xFF) | (buffer[kk*4 + 1] << 8)) ) * invMaxShort;
			audioData[1][kk] = ( (short) ((buffer[kk*4 + 2] & 0xFF) | (buffer[kk*4 + 3] << 8)) ) * invMaxShort;
		}


        // downsample audio data
		if (downsample) {

			long start = System.currentTimeMillis();

            samplerate /= 2;

			CResampling cr = new CResampling();

			for (int kk = 0; kk < 2; kk++) {
				cr.Downsample2f(audioData[kk], audioData[kk].length/2);
				cr.reset();
                // resize
                audioData[kk] = Arrays.copyOf(audioData[kk], frames/2);
            }

			Log.d(LOG, "Resampling took " + (System.currentTimeMillis() - start) + " ms");

		}

		if (filterHp) {
			// high-pass filter
			FilterHP hp = new FilterHP(samplerate, filterHpFrequency);

			for (int kk = 0; kk < 2; kk++) {
				hp.filter(audioData[kk]);
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
