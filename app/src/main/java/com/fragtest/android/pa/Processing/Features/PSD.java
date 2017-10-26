package com.fragtest.android.pa.Processing.Features;

import android.os.Messenger;
import android.util.Log;

import com.fragtest.android.pa.Processing.ProcessingRunnable;

import org.jtransforms.fft.FloatFFT_1D;

import java.util.Arrays;

public class PSD extends ProcessingRunnable {
	
	public PSD(float[][] audioData, int procBlockSize, int nHop, int nOutBlockSize, int nFeatures, Messenger messenger) {
		super(audioData, procBlockSize, nHop, nOutBlockSize, nFeatures, messenger);
		setFeature("PSD");
	}

	@Override
	public void process(float[][] data) {
		super.process(data);
		
		double blockDuration = 0.025;
		int blockSize = (int) (blockDuration * samplingrate);
				
		appendFeature(cpsd(data[0], data[1], blockSize, samplingrate));
	}
	
	public float[][] cpsd(float[] x, float[] y, int blockSize, int fs){
		/* One-sided cross power spectral density with hann windowing and 50% overlap. Inputs must be even.
		 * Result is the same as with Matlab's cpsd() */

		// implement some sort of arg checking here (x.length == y.length, x.length%2==0 etc.)

		/* compute hann window */
		float[] hannIndex 	= new float[blockSize];
		float[] hannWin 	= new float[blockSize];
		
		for (int i = 0; i < hannIndex.length; i++) {
			hannIndex[i] = (float) i / (blockSize-1);
		}
		
		for (int i = 0; i < hannWin.length; i++) {
			hannWin[i] = (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * hannIndex[i]));
		}
		
		/* compute cross power spectrum, starting with block indices */
		int NFFT 		= 1 << (32 - Integer.numberOfLeadingZeros(blockSize - 1));			// next power of 2 of blocksize
		Log.d(LOG, "FFT-Length: " + NFFT);
		int L 			= x.length;															// length of complete signal(s)
		int overlap 	= blockSize / 2;													// overlap of 50% in samples
		int nBlocks		= (int) Math.floor(((float) L - overlap) / (blockSize - overlap));	// number of overlapping blocks
		int nPxyBlocks	= nBlocks / 10 + 1;													// number of 5 non-overlapping block segments (for later averaging)
		float[][] mLastSxy = new float[3][2*NFFT];											// last power spectra, first row holds last cross power spectrum of x and y, 2nd and 3rd rows hold last auto power spectra of x/x and y/y
		float[][][] mGlidingSxy = new float[nPxyBlocks][3][2*NFFT];							// temp matrix that will hold recursive power spectra in 5 (non-overlapping) block increments. second dimension is for cross and auto power spectras of the two channels

		float alpha = (float) Math.exp(-overlap/(fs*0.125));
		
		int bsMinusOverlap 	= blockSize - overlap;
		int[] blockStartIdx = new int[nBlocks];
		
		for (int block = 0; block < nBlocks; block++) {
			blockStartIdx[block] = block * bsMinusOverlap;
		}
		
		float winNorm = 0;									// Window normalization constant. 1/N omitted because it cancels below
		for (int i = 0; i < blockSize; i++) {
			winNorm += hannWin[i] * hannWin[i];
		}
		winNorm *= (float) blockSize / NFFT;				// when NFFT > blocksize, the energy from blocksize samples is spread over (NFFT/blocksize) * blocksize spectral coefficients,
															// so the normalization constant has to be scaled down by the inverse or too much energy is taken out of the spectrum.
															// check with someone smart if this is correct! :-)
		
		float[][] Sxy = new float[3][2*NFFT];				// temp matrix, first row holds cross power spectrum of x and y, 2nd and 3rd rows hold auto power spectra of x/x and y/y
				
		int iAvgCount = 0;

		FloatFFT_1D fft = new FloatFFT_1D(NFFT);

		for (int block = 0; block < nBlocks; block++) {
			
			/* get current block and window the data */
			float[] xBlock = new float[blockSize];
			float[] yBlock = new float[blockSize];
		
			for (int i = 0; i < blockSize; i++) {
				xBlock[i] = x[i + blockStartIdx[block]] * hannWin[i];
				yBlock[i] = y[i + blockStartIdx[block]] * hannWin[i];
			}
			
			float[] Xx = Arrays.copyOf(xBlock, 2*NFFT);
			float[] Yy = Arrays.copyOf(yBlock, 2*NFFT);

			/* compute raw STFT of both channels */

			fft.realForwardFull(Xx);
			fft.realForwardFull(Yy);
			
			float[][] mFFT = new float[2][2*NFFT];
			for (int i = 0; i < 2*NFFT; i++) {
				mFFT[0][i] = Xx[i];
				mFFT[1][i] = Yy[i];
			}
			
			int[][] mIdx = {			// channel index table for cross and auto power spectral densities
					{0, 1},
					{0, 0},
					{1, 1}};
			
			for (int psd = 0; psd < mIdx.length; psd++) {
								
				/* compute cross power spectrum, start by complex conjugating the STFT of the 2nd channel */
				
				// TODO: complex conjugate only once..
				float[] vConjSecChan = new float[2*NFFT];
				for (int i = 0; i < vConjSecChan.length-1; i += 2) {
					vConjSecChan[i] = mFFT[mIdx[psd][1]][i];
				}
				for (int i = 1; i < vConjSecChan.length; i += 2) {
					vConjSecChan[i] = -mFFT[mIdx[psd][1]][i];
				}
				
				float[] P = new float[Xx.length];
				
				for (int i = 0; i < Xx.length-1; i += 2) {
					/* multiply spectra of both channels to get cross power spectrum */
					P[i] 	= mFFT[mIdx[psd][0]][i] * vConjSecChan[i] - mFFT[mIdx[psd][0]][i+1] * vConjSecChan[i+1];
					P[i+1]	= mFFT[mIdx[psd][0]][i] * vConjSecChan[i+1] + mFFT[mIdx[psd][0]][i+1] * vConjSecChan[i];
					
					/* account for window power */
					P[i] 	/= winNorm;					// 1/N omitted because it cancels with above  
					P[i+1] 	/= winNorm;
				}
				
				/* recursive averaging */
				for (int i = 0; i < P.length; i++) {
//					Sxy[psd][i] = (float) block / (block+1) * mLastSxy[psd][i] + (float) 1 / (block+1) * P[i];
					Sxy[psd][i] = alpha * mLastSxy[psd][i] + (1-alpha) * P[i];
				}
				
				for (int i = 0; i < 2*NFFT; i++) {
					mLastSxy[psd][i] = Sxy[psd][i];
				}
				
				/* fill matrix with every nth average */
				if ((block+1)%10 == 0){						// 10 overlapping blocks == 5 non-overlapping blocks == 125 ms 
					for (int i = 0; i < 2*NFFT; i++) {
						mGlidingSxy[iAvgCount][psd][i] = Sxy[psd][i];
					}
				}	
			}
			if ((block+1)%10 == 0){				
				iAvgCount++;
			}
		}
		
		/* compute cross power spectral  density */
		float[][] mPxyOnesided = new float[nPxyBlocks][2 * (NFFT + 2)];	// create one-sided power spectra. one row contains 1 complex CPS and two real APS
																		// one complex spectrum is 2*NFFT long (real and imaginary parts), so one-sided is NFFT+2
																		// the two APS's are half of that (no imaginary parts), so complete length is 2(NFFT+2) 
		/* copy CPS  */
		for (int i = 0; i < nPxyBlocks; i++) {
			for (int j = 0; j < NFFT+2; j++) {
				mPxyOnesided[i][j] = mGlidingSxy[i][0][j];
			}
		}
		
		/* copy the two APS of 1st and 2nd channel, ditch all-zero imaginary parts */
		for (int i = 0; i < nPxyBlocks; i++) {
			for (int j = 0; j < NFFT/2+1; j++) {
				mPxyOnesided[i][j+NFFT+2]			= mGlidingSxy[i][1][2*j];
				mPxyOnesided[i][j+NFFT+2+NFFT/2+1] 	= mGlidingSxy[i][2][2*j];
			}
		}
		
		/* scale accordingly, excluding DC offset and nyquist point */
		for (int i = 0; i < nPxyBlocks; i++) {
			for (int j = 2; j < NFFT+2 - 2; j++) {
				mPxyOnesided[i][j] = 2*mPxyOnesided[i][j];
			}			
		}
		
		for (int i = 0; i < nPxyBlocks; i++) {
			for (int j = 1; j < NFFT/2+1 - 1; j++) {
				mPxyOnesided[i][j+NFFT+2] 			= 2*mPxyOnesided[i][j+NFFT+2]; 
				mPxyOnesided[i][j+NFFT+2+NFFT/2+1] 	= 2*mPxyOnesided[i][j+NFFT+2+NFFT/2+1];
			}
		}
		
				
		for (int i = 0; i < nPxyBlocks; i++) {
			for (int j = 0; j < mPxyOnesided[0].length; j++) {			// divide by the sampling frequency to get the cross power spectral density
				mPxyOnesided[i][j] = mPxyOnesided[i][j] / fs;
			}			
		}
				
		return mPxyOnesided;
	}	
}
