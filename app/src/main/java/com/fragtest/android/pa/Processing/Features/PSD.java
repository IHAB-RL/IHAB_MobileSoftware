package com.fragtest.android.pa.Processing.Features;

import android.os.Messenger;
import android.util.Log;

import com.fragtest.android.pa.Processing.BasicProcessRunnable;

import org.jtransforms.fft.FloatFFT_1D;

import java.util.Arrays;

public class PSD extends BasicProcessRunnable {
	
	public PSD(float[][] audioData, int procFrameSize, int nHop, int nOutFrameSize, int nFeatures, Messenger messenger) {
		super(audioData, procFrameSize, nHop, nOutFrameSize, nFeatures, messenger);
		setFeature("PSD");
	}

	@Override
	public void process(float[][] data, int iFrame) {
		super.process(data, iFrame);
		
		double frameDuration = 0.025;
		int frameSize = (int) (frameDuration * samplingrate);

		appendFeature(cpsd(data[0], data[1], frameSize, samplingrate));
	}
	
	public float[][] cpsd(float[] x, float[] y, int frameSize, int fs){
		/* One-sided cross power spectral density with hann windowing and 50% overlap. Inputs must be even.
		 * Result is the same as with Matlab's cpsd() */

		// implement some sort of arg checking here (x.length == y.length, x.length%2==0 etc.)


		Log.d(LOG, "framesize:" + frameSize);

		/* compute hann window */
		float[] hannIndex 	= new float[frameSize];
		float[] hannWin 	= new float[frameSize];
		
		for (int i = 0; i < hannIndex.length; i++) {
			hannIndex[i] = (float) i / (frameSize-1);
		}
		
		for (int i = 0; i < hannWin.length; i++) {
			hannWin[i] = (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * hannIndex[i]));
		}
		
		/* compute cross power spectrum, starting with block indices */
		int NFFT 		= 1 << (32 - Integer.numberOfLeadingZeros(frameSize - 1));			// next power of 2 of framesize
		Log.d(LOG, "FFT-Length: " + NFFT);
		int L 			= x.length;															// length of complete signal(s)
		int overlap 	= frameSize / 2;													// overlap of 50% in samples
		int nBlocks		= (int) Math.floor(((float) L - overlap) / (frameSize - overlap));	// number of overlapping blocks
		int nPxyBlocks	= nBlocks / 10 + 1;													// number of 5 non-overlapping block segments (for later averaging)
		float[][] mLastSxy = new float[3][2*NFFT];											// last power spectra, first row holds last cross power spectrum of x and y, 2nd and 3rd rows hold last auto power spectra of x/x and y/y

		float alpha = (float) Math.exp(-overlap/(fs*0.125));
		
		int bsMinusOverlap 	= frameSize - overlap;
		int[] blockStartIdx = new int[nBlocks];
		
		for (int block = 0; block < nBlocks; block++) {
			blockStartIdx[block] = block * bsMinusOverlap;
		}
		
		float winNorm = 0;									// Window normalization constant. 1/N omitted because it cancels below
		for (int i = 0; i < frameSize; i++) {
			winNorm += hannWin[i] * hannWin[i];
		}
		winNorm *= (float) frameSize / NFFT;				// when NFFT > blocksize, the energy from blocksize samples is spread over (NFFT/blocksize) * blocksize spectral coefficients,
															// so the normalization constant has to be scaled down by the inverse or too much energy is taken out of the spectrum.
															// check with someone smart if this is correct! :-)
		int iAvgCount = 0;

		FloatFFT_1D fft = new FloatFFT_1D(NFFT);

        float[] Xx = new float[2*NFFT];
        float[] Yy = new float[2*NFFT];

        float[] Pxx = new float[2*NFFT];
        float[] Pyy = new float[2*NFFT];
        float[] Pxy = new float[2*NFFT];

        float[] conjXx = new float[2*NFFT];
        float[] conjYy = new float[2*NFFT];

        float[][] glidingPxy = new float[nPxyBlocks][2*NFFT];
        float[][] glidingPxx = new float[nPxyBlocks][2*NFFT];
        float[][] glidingPyy = new float[nPxyBlocks][2*NFFT];

        for (int block = 0; block < nBlocks; block++) {

            /* get current block and window the data */
            for (int i = 0; i < frameSize; i++) {
                Xx[i] = x[i + blockStartIdx[block]] * hannWin[i];
                Yy[i] = y[i + blockStartIdx[block]] * hannWin[i];
            }

            // FFT
            fft.realForwardFull(Xx); // < 2ms
            fft.realForwardFull(Yy);

            // complex conjugate
            for (int i = 3; i < Xx.length; i+=2) {
                conjXx[i] = -Xx[i];
                conjYy[i] = -Yy[i];
            }

            // correlation
            for (int i = 0; i < Xx.length-1; i+=2) {
                Pxy[i]   = (Xx[i] * conjYy[i]   - Xx[i+1] * conjYy[i+1]) / winNorm;
                Pxy[i+1] = (Xx[i] * conjYy[i+1] - Xx[i+1] * conjYy[i]) / winNorm;

                Pxx[i]   = (Xx[i] * conjXx[i]   - Xx[i+1] * conjXx[i+1]) / winNorm;
                Pxx[i+1] = (Xx[i] * conjXx[i+1] - Xx[i+1] * conjXx[i]) / winNorm;

                Pyy[i]   = (Yy[i] * conjYy[i]   - Yy[i+1] * conjYy[i+1]) / winNorm;
                Pyy[i+1] = (Yy[i] * conjYy[i+1] - Yy[i+1] * conjYy[i]) / winNorm;
            }

            /* recursive averaging */
            for (int i = 0; i < Pxx.length; i++) {
                Pxy[i] = alpha * mLastSxy[0][i] + (1-alpha) * Pxy[i];
                Pxx[i] = alpha * mLastSxy[2][i] + (1-alpha) * Pyy[i];
                Pyy[i] = alpha * mLastSxy[1][i] + (1-alpha) * Pxx[i];
            }

            for (int i = 0; i < 2*NFFT; i++) {
                mLastSxy[0][i] = Pxy[i];
                mLastSxy[1][i] = Pxx[i];
                mLastSxy[2][i] = Pyy[i];
            }

            /* fill matrix with every nth average */
            if ((block+1)%10 == 0) {						// 10 overlapping blocks == 5 non-overlapping blocks == 125 ms
                glidingPxy[iAvgCount] = Pxy;
                glidingPxx[iAvgCount] = Pxx;
                glidingPyy[iAvgCount] = Pxy;
                iAvgCount++;
            }
		}

		/* compute cross power spectral  density */
		float[][] mPSD = new float[nPxyBlocks][2 * (NFFT + 2)];	// create one-sided power spectra. one row contains 1 complex CPS and two real APS
																		// one complex spectrum is 2*NFFT long (real and imaginary parts), so one-sided is NFFT+2
																		// the two APS's are half of that (no imaginary parts), so complete length is 2(NFFT+2)
		/* copy data to output array  */
		for (int i = 0; i < nPxyBlocks; i++) {
            /* Pxy */
            System.arraycopy(glidingPxy[i], 0, mPSD[i], 0, 2*NFFT);
            /* Pxx & Pyy */
            for (int j = 0; j < NFFT/2+1; j++) {
                mPSD[i][j+NFFT+2]			= glidingPxx[i][2*j];
                mPSD[i][j+NFFT+2+NFFT/2+1] 	= glidingPxy[i][2*j];
            }
		}

		/* scale accordingly, excluding DC offset and nyquist point */
		for (int i = 0; i < nPxyBlocks; i++) {
			for (int j = 2; j < NFFT+2 - 2; j++) {
				mPSD[i][j] = 2*mPSD[i][j];
			}
		}

		for (int i = 0; i < nPxyBlocks; i++) {
			for (int j = 1; j < NFFT/2+1 - 1; j++) {
				mPSD[i][j+NFFT+2] 			= 2*mPSD[i][j+NFFT+2];
				mPSD[i][j+NFFT+2+NFFT/2+1] 	= 2*mPSD[i][j+NFFT+2+NFFT/2+1];
			}
		}

		for (int i = 0; i < nPxyBlocks; i++) {
			for (int j = 0; j < mPSD[0].length; j++) {			// divide by the sampling frequency to get the cross power spectral density
				mPSD[i][j] = mPSD[i][j] / fs;
			}
		}

        return mPSD;
	}
}
