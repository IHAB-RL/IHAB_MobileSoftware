
package com.fragtest.android.pa.Processing;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import com.fragtest.android.pa.ControlService;
import com.fragtest.android.pa.Core.AudioFileIO;
import com.fragtest.android.pa.Processing.Preprocessing.CResampling;
import com.fragtest.android.pa.Processing.Preprocessing.FilterHP;

import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Set;

/**
 * BasicProcessingThread.java
 *
 * Loads audio data, applies preprocessing and calls BasicProcessingRunnable.java
 */

public class BasicProcessingThread extends Thread {

	protected static final String LOG = "PROCESSING";
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
	int blocklengthInS;					// in ms
    private boolean downsample = false;

    private Set<String> activeFeatures;
    private int processedFeatures = 0;
	private ArrayList<String> featureFiles = new ArrayList<String>();

    static {
        System.loadLibrary("CResampling");
    }

	// constructor
	public BasicProcessingThread(Messenger messenger, Bundle settings){

        serviceMessenger = messenger;

        samplerate = settings.getInt("samplerate");
        blocklengthInS = settings.getInt("blocklengthInS");
        activeFeatures = (Set) settings.getSerializable("activeFeatures");
        filterHp = settings.getBoolean("filterHp");
        filterHpFrequency = settings.getInt("filterHpFrequency");
        filename = settings.getString("filename");
        downsample = settings.getBoolean("downsample", false);

        // extract timestamp from filename
        timestamp = filename.substring(filename.lastIndexOf("/")+1);
        timestamp = timestamp.substring(0, timestamp.lastIndexOf("."));
	}


	@Override
	public void run(){
		audioData = readAllData();	// read audio data from temp or wav file
		mainRoutine();				// do processing
		discardData();
	}


    // overload in MainProcessingThread
    public void mainRoutine() {}


	private void discardData() {
		buffer = null;
	}


	// read audio data from file
	private float[][] readAllData() {

        // skip wave header
        int skip = 0;
        if (filename.substring(filename.lastIndexOf(".") + 1, filename.length())
                .equals(AudioFileIO.CACHE_WAVE)) {
            skip = 44;
        }

        try {
            // load data from cache
            RandomAccessFile inputFile = new RandomAccessFile(filename, "r");
            buffer = new byte[(int) inputFile.length() - skip];
            inputFile.seek(skip);
            inputFile.readFully(buffer);
            inputFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        int frames = buffer.length / 4;

		float[][] audioData = new float[2][frames];

		float invMaxShort = 1.0f / Short.MAX_VALUE;

		// convert bytes to short and split channels
		for ( int kk = 0; kk < frames; kk++ ) {
			audioData[0][kk] = ( (short) ((buffer[kk*4] & 0xFF) | (buffer[kk*4 + 1] << 8)) ) * invMaxShort;
			audioData[1][kk] = ( (short) ((buffer[kk*4 + 2] & 0xFF) | (buffer[kk*4 + 3] << 8)) ) * invMaxShort;
		}

		if (filterHp) {
            long start = System.currentTimeMillis();

			// high-pass filter
			FilterHP hp = new FilterHP(samplerate, filterHpFrequency);

			for (int kk = 0; kk < 2; kk++) {
				audioData[kk] = hp.filter(audioData[kk]);
			}

            Log.d(LOG, "filtering: " + (System.currentTimeMillis()-start));
		}

        // downsample audio data
		if (downsample) {

            long start = System.currentTimeMillis();
            samplerate /= 2;

			float[][] audioData_ds = new float[2][frames/2];

			CResampling cr = new CResampling();

			for ( int kk = 0; kk < 2; kk++ ) {
				audioData_ds[kk] = cr.Downsample2f(audioData[kk], audioData_ds[kk].length);
                cr.reset();
            }

            Log.d(LOG, "downsampling: " + (System.currentTimeMillis()-start));
			return audioData_ds;
		} else {
			return audioData;
		}
	}


	// check if all active features have been processed
	private void isFinished() {

        processedFeatures++;

        if (activeFeatures.size() == processedFeatures) {

    		Message msg = Message.obtain(null, ControlService.MSG_BLOCK_PROCESSED);
    		
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
            	isFinished();
            	break;
            default:
            	super.handleMessage(msg);
            }
        }
	}

}


