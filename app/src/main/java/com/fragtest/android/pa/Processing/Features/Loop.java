package com.fragtest.android.pa.Processing.Features;

import android.os.Messenger;
import android.util.Log;

import com.fragtest.android.pa.Processing.ProcessingRunnable;
import com.fragtest.android.pa.Processing.Preprocessing.CResampling;

public class Loop extends ProcessingRunnable {
	protected static final String LOG = "Loop";
	
	public Loop(float[][] audioData, int procBlockSize, int nHop, int nOutBlockSize, int nFeatures, Messenger messenger) {
		super(audioData, procBlockSize, nHop, nOutBlockSize, nFeatures, messenger);
		setFeature("Loop");
		Log.d(LOG, "Loop object created");
	}

	@Override
	public void process(float[][] data) {
		super.process(data);
		
		float[] out = new float[data.length * data[0].length];
		
		// concatenate data
		System.arraycopy(data[0], 0, out, 0, data[0].length);
		System.arraycopy(data[1], 0, out, data[0].length, data[1].length);
		
		appendFeature( data );
		
	}

	// simple 3:1 downsampling w/o LP
	private float[] ds_one(float[][] data) {

		// interleave
		float[] out = new float[data.length * data[0].length];

		for (  int kk = 0; kk < data[0].length; kk++ ) {
				out[kk * 2] 	= data[0][kk];
				out[kk * 2 + 1] = data[1][kk];
		}
		
		float[] result = new float[out.length / 3]; 

		// downsample from 48k to 16k -> get 4 bytes and discard the next 8
		for ( int kk = 0; kk < out.length/12; kk++ ) {
			for ( int ii = 0; ii < 4; ii++ ) {
				result[kk*4 + ii] = out[kk*12 + ii];
			}
		}
		
		return result;
		
	}

	// simple 2:1 downsampling with LP
//	private float[] ds_two(float[][] data) {
//
//		float[][] tmp = new float[data.length][data[0].length / 2];
//		CResampling cr = new CResampling();
//
//		tmp[0] = cr.Downsample2fnoLP(data[0], tmp[0].length);
//		cr.reset();
//		tmp[1] = cr.Downsample2fnoLP(data[1], tmp[1].length);
//
//		// interleave
//		float[] result = new float[tmp.length * tmp[0].length];
//
//		for (  int kk = 0; kk < tmp[0].length; kk++ ) {
//				result[kk * 2] 	= tmp[0][kk];
//				result[kk * 2 + 1] = tmp[1][kk];
//		}
//
//		return result;
//
//	}
	
	
}
