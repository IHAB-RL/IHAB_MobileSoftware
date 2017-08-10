package com.fragtest.android.pa.Processing.Features;

import android.os.Messenger;
import android.util.Log;

import com.fragtest.android.pa.Processing.BasicProcessRunnable;

public class ZCR extends BasicProcessRunnable
{
	public ZCR(float[][] data, int processBlock, int nHop, int nProcOutSamples, int nFeatures, Messenger messenger) {
		super(data, processBlock, nHop, nProcOutSamples, nFeatures, messenger);
		setFeature( "ZCR" );
	}

	@Override
	public void process(float[][] data, int iFrame) 
	{
		super.process(data, iFrame);
		
		float[] result = new float[nFeatures];

		for ( int iChannel = 0; iChannel < data.length; iChannel++ ) {
			result[iChannel] = zcr( data[iChannel] );
			result[iChannel + 2] = zcr( diff( data[iChannel] ) );
		}
		
		appendFeature(result);
	}
	
	private static int zcr(float in[]) {
		int count = 0;
		float data_sign[] = new float[in.length];

		data_sign[0] = Math.signum(in[0]);
		
		for ( int kk = 1; kk < in.length; kk++ ) {
			data_sign[kk] = Math.signum(in[kk]);
			
			if ( data_sign[kk]-data_sign[kk-1] != 0 )
				count++;
		}

		return count;
	}
	
	private float[] diff(float[] data) {
		float[] delta = new float [data.length-1];
		
		for (int kk = 0; kk < data.length-1; kk++)
			delta[kk] = data[kk+1] - data[kk];
		
		return delta;
	}
	
}
