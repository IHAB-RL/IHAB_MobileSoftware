/* 2x up- and downsampling
 * 
 * Based on the corresponding C++ implementation by Marco Ruhland, 2009
 * 
 */

package com.fragtest.android.pa.Processing.Preprocessing;

import android.os.SystemClock;
import android.util.Log;

public class CResampling {

	private int m_xpos1_ds;
	private int m_xpos2_ds;
	private int m_xpos_us;
	
	private float[] m_bcoeff_ds 	= new float[32];
	private float[] m_xvec1_ds		= new float[64];
	private float[] m_xvec2_ds		= new float[33];
	private float[] m_bcoeff_us		= new float[32];
	private float[] m_xvec_us		= new float[64];
	
	public CResampling() {
		
		m_bcoeff_us[0]	=  -0.000007589279555f; // coefficients from ParksMcLellan-Design for anti-aliasing-filter
		m_bcoeff_us[1]	=   0.000014322198629f; // MATLAB: h=firpm(128,[0 0.45 0.55 1],[1 1 0 0]);
		m_bcoeff_us[2]	=  -0.000027380287300f; // only one half of the filter coeffs, and all the zeroes left away
		m_bcoeff_us[3]	=   0.000047790630650f; // --> therefore only 32 coeffs stored here instead of 128
		m_bcoeff_us[4]	=  -0.000078268005383f; // the 0th coefficient is 0.5f and is hard-coded in
		m_bcoeff_us[5]	=   0.000122159126424f; // the function Upsample2f.
		m_bcoeff_us[6]	=  -0.000183504414029f;
		m_bcoeff_us[7]	=   0.000267102568994f;
		m_bcoeff_us[8]	=  -0.000378580777739f;
		m_bcoeff_us[9]	=   0.000524455894125f;
		m_bcoeff_us[10]	=  -0.000712188588467f;
		m_bcoeff_us[11]	=   0.000950263467159f;
		m_bcoeff_us[12]	=  -0.001248287205107f;
		m_bcoeff_us[13]	=   0.001617101218669f;
		m_bcoeff_us[14]	=  -0.002068984059695f;
		m_bcoeff_us[15]	=   0.002617944057400f;
		m_bcoeff_us[16]	=  -0.003280117638575f;
		m_bcoeff_us[17]	=   0.004074486286745f;
		m_bcoeff_us[18]	=  -0.005023854567343f;
		m_bcoeff_us[19]	=   0.006156485422566f;
		m_bcoeff_us[20]	=  -0.007508592696345f;
		m_bcoeff_us[21]	=   0.009128402960935f;
		m_bcoeff_us[22]	=  -0.011082852122455f;
		m_bcoeff_us[23]	=   0.013469121084517f;
		m_bcoeff_us[24]	=  -0.016435487666445f;
		m_bcoeff_us[25]	=   0.020221490523925f;
		m_bcoeff_us[26]	=  -0.025241966087592f;
		m_bcoeff_us[27]	=   0.032283134342770f;
		m_bcoeff_us[28]	=  -0.043034753000682f;
		m_bcoeff_us[29]	=   0.061899269714142f;
		m_bcoeff_us[30]	=  -0.105037081083480f;
		m_bcoeff_us[31]	=   0.317953038191487f;
		
		for ( int kk = 0; kk < 32; kk++ )
			m_bcoeff_ds[kk]=m_bcoeff_us[kk]*2.f; 	// pre-multiply coeffs for downsampling-filter by 2,
													// since multiplying by 2 is needed in 2x-downsampling to maintain
													// the signal magnitude.
		reset();
		
	}

	public void reset() {
		
		for ( int kk = 0; kk < 64; kk++ )
			m_xvec1_ds[kk]=0.f;
		
		for ( int kk = 0; kk < 33; kk++ )
			m_xvec2_ds[kk]=0.f;
		
		m_xpos1_ds=0;
		m_xpos2_ds=0;
		
		for ( int kk=0; kk < 64; kk++ )
			m_xvec_us[kk]=0.f;
		
		m_xpos_us=0;
		
	}

	public float[] Downsample2f(float[] inbuf, int numoutsamples) {


//        int mm = 0;
		float outval;
		float[] outbuf = new float[numoutsamples];

//        long start, stop;
//        start = SystemClock.elapsedRealtimeNanos();

        for ( int kk = 0; kk < numoutsamples; kk++ ) {

			m_xvec1_ds[kk % 63]=inbuf[2*kk]; // downsampling
            m_xvec2_ds[kk % 33]=inbuf[2*kk+1];
//            m_xpos1_ds&=63;
//            m_xpos2_ds%=33;
            outval=m_xvec2_ds[kk % 33];

            for ( int jj = 0; jj < 32; jj++ ) { // anti-alias-filtering
                outval += (m_xvec1_ds[(kk + jj) % 63] + m_xvec1_ds[(63 + kk - jj) % 63]) * m_bcoeff_ds[jj];
            }
            outbuf[kk] = outval * 0.5f;

		}

//        stop = SystemClock.elapsedRealtimeNanos();
//        Log.d("CResampling", "Downsampling: " + (stop - start));

        return outbuf;
		
	}

	public float[] Downsample2fnoLP(float[] inbuf, int numoutsamples) {
		
		float[] outbuf = new float[numoutsamples];

		for ( int kk=0; kk < numoutsamples; kk++ ) // simple downsampling by factor 2 without anti-alias filtering
			outbuf[kk]=inbuf[kk*2]*2.f; // scaling by factor 2 is necessary, to maintain the signal magnitude
		
		return outbuf;
		
	}

	public float[] Upsample2f(float[] inbuf, int numinsamples)	{
		
		int mm = 0; 
		float outval1,outval2;
		float[] outbuf = new float[numinsamples*2];
		
		for ( int kk=0; kk < numinsamples; kk++ ) {
			
			m_xvec_us[m_xpos_us++]=inbuf[kk];
			m_xpos_us&=63;
			outval1=m_xvec_us[(31+m_xpos_us)&63]*0.5f; // middle coefficient of anti-alias filter (0.5f)
			outval2=0.f;
			
			for ( int jj = 0; jj < 32; jj++ ) { // rest of anti-alias filtering
                outval2 += (m_xvec_us[(m_xpos_us + jj) & 63] + m_xvec_us[(63 + m_xpos_us - jj) & 63]) * m_bcoeff_us[jj];
            }

			outbuf[mm++]=outval1; // upsampling
			outbuf[mm++]=outval2;
			
		}
		
		return outbuf;
		
	}
	
}
