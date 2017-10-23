package com.fragtest.android.pa.Processing;

import android.os.Bundle;
import android.os.Messenger;
import android.util.Log;

import com.fragtest.android.pa.Processing.Features.Loop;
import com.fragtest.android.pa.Processing.Features.PSD;
import com.fragtest.android.pa.Processing.Features.RMS;
import com.fragtest.android.pa.Processing.Features.ZCR;

import java.util.Queue;

/**
 * MainProcessingThread overloads mainRoutine() in BasicProcessingThread to start runnables
 * corresponding to selected features.
 *
 * Implement new features:
 * 
 * 1) create a new class by extending BasicProcessRunnable (use e.g. RMS.java as template)
 * 2) configure, instantiate and start the runnable class here in MainProcessingThread.java (see below)
 * 3) add the feature to res/values/features.xml
 * 
 * Note: 	The names of the class file (w/o java suffix), the string in checkForProcess() 
 * 			and the name in features.xml must match.    
 * 
 * */

public class MainProcessingThread extends BasicProcessingThread{

	public MainProcessingThread(Messenger messenger, Queue queue, Bundle b) {
		super(messenger, queue, b);
	}

	@Override
	public void mainRoutine() {
		super.mainRoutine();

		int nProcSamples, nHop, nFeatures, nProcOutSamples;
		double nProcSeconds, nOutSeconds;

		if (isActiveFeature("PSD")) {

			long start = System.currentTimeMillis();

			// CPSD takes nProcSeconds of audio data, recursively computes and averages
			// the cross and auto power spectral densities every 25 ms and returns a
			// matrix where every row is one average after an nOutSeconds increment.
			// one row contains the cross power spectral density (complex-valued)
			// followed by the auto power spectral densities (real-valued)
			nProcSeconds = chunklengthInS;
			nOutSeconds = 0.125; // must be a multiple of 0.025 s

			int nBlockSize = (int) (0.025 * samplerate);
			
			nProcSamples = (int) (nProcSeconds * samplerate);
			nProcOutSamples = (int) (nOutSeconds * samplerate);
			
			nHop = nProcSamples;
            // next power of two;
			int NFFT = 1 << (32 - Integer.numberOfLeadingZeros(nBlockSize - 1));
			nFeatures = 2 * (NFFT + 2); 			
			PSD cpsdRunnable = new PSD(audioData, nProcSamples, nHop, nProcOutSamples,
                    nFeatures, processMessenger);

			cpsdRunnable.run();

			Log.d(LOG, "PSD took " + (System.currentTimeMillis() - start) + " ms");
		}
		
		if (isActiveFeature("RMS")) {

			long start = System.currentTimeMillis();


			nProcSamples = (int) (0.025f * samplerate); // 25ms
			nHop = nProcSamples / 2;
			nProcOutSamples = nProcSamples;		
			nFeatures = 2; // [left right]
			RMS rmsRunnable = new RMS(audioData, nProcSamples, nHop, nProcOutSamples,
                    nFeatures, processMessenger);

			rmsRunnable.run();

			Log.d(LOG, "RMS took " + (System.currentTimeMillis() - start) + " ms");
		}

		if (isActiveFeature("ZCR")) {

			long start = System.currentTimeMillis();


			nProcSamples = (int) (0.025f * samplerate); // 25ms
			nHop = nProcSamples / 2;
			nProcOutSamples = nProcSamples;		
			nFeatures = 4; // [left right delta_left delta_right] 
			ZCR zcrRunnable = new ZCR(audioData, nProcSamples, nHop, nProcOutSamples,
                    nFeatures, processMessenger);

			zcrRunnable.run();

			Log.d(LOG, "ZCR took " + (System.currentTimeMillis() - start) + " ms");
		}				
	
		if (isActiveFeature("Loop")) {

			long start = System.currentTimeMillis();

			nProcSeconds = 5;
			nProcSamples = (int) (nProcSeconds * samplerate);
			nHop = nProcSamples;
			nProcOutSamples = nProcSamples;
			nFeatures = nProcSamples * 2;
			Loop loopRunnable = new Loop(audioData, nProcSamples, nHop, nProcOutSamples,
                    nFeatures, processMessenger);

			loopRunnable.run();

			Log.d(LOG, "Looping took " + (System.currentTimeMillis() - start) + " ms");
		}
/*
		if (isActiveFeature("SRMR")) {

			long start = System.currentTimeMillis();


			nProcSamples = (int) (0.025f * samplerate); // 25ms
			nHop = nProcSamples / 2;
			nProcOutSamples = 1;
			nFeatures = 2; // [left right]
			SRMR rmsRunnable = new SRMR(audioData, nProcSamples, nHop, nProcOutSamples,
					nFeatures, processMessenger, samplerate);

			rmsRunnable.run();

			Log.e(LOG, "SRMR took " + (System.currentTimeMillis() - start) + " ms");
		}
*/
	}
}
