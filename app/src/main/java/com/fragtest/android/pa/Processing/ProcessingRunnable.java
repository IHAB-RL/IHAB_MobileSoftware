
package com.fragtest.android.pa.Processing;

import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.fragtest.android.pa.Core.AudioFileIO;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * ProcessingRunnable.java
 *
 * Passes cached audio data to the respective processing algorithms and writes the feature-data
 * to storage.
 */

public class ProcessingRunnable implements Runnable {

	protected static final String LOG = "Processing";
	private static final String EXTENSION = ".feat";
	private static String feature = "basicProcess";// name of the output file (will be overwritten by children)

	private float[][] audioData;
	private float[][] blockData;
	protected Messenger messenger = null;           // instance of processHandler

	private AudioFileIO ioClass = null;
	private RandomAccessFile featureRAF = null;
    private File featureFile = null;
	private String timestamp;
	private int procBlockSize;
	private int procHopSize;
	private int nProcBlocks;
	protected int samplingrate;
	protected int nFeatures;
	private float procHopDuration;
	private float[] procTimestamp;
	private int procBlockCount;
	private long posBlocks;

	// added for when procBlockSize != procOutBlockSize,
	// i.e. when one feature output represents a smaller time interval then one input block
	private int procOutBlockSize; // one feature output represents this many input samples
	private float procOutDuration;

	public ProcessingRunnable(float[][] audioData, int procBlockSize, int procHopSize,
                              int procOutBlockSize, int nFeatures, Messenger messenger) {

		// TODO how to handle this parameter-mess properly?

		this.audioData = audioData;
		this.procBlockSize = procBlockSize;
		this.procHopSize = procHopSize;
		this.nFeatures = nFeatures;
		this.messenger = messenger;

        // TODO: while at it, check this, too!
        samplingrate = BasicProcessingThread.samplerate;
        timestamp = BasicProcessingThread.timestamp;

		ioClass = new AudioFileIO();

		nProcBlocks = (int) Math.floor((audioData[0].length - procBlockSize) / procHopSize) + 1;
		blockData = new float[2][procBlockSize];                          // processing block

		procTimestamp = new float[2];

		// some features return smaller blocks then the input block size (and therefore more than 1 per input block; e.g. CPSD)
		this.procOutBlockSize = procOutBlockSize;
		this.procOutDuration = procOutBlockSize * samplingrate;
	}

	protected void setFeature(String s) {
		feature = s;
	}

	@Override
	public void run() {

        Message msg = Message.obtain(null, BasicProcessingThread.DONE);
        Bundle b = new Bundle();

		if (audioData[0].length >= procBlockSize) {

			// we need a global flag to indicate a new recording session started.. new: why?

			openFeatureFile();

			// calling processing runnables for each block..

			for (int iBlock = 1; iBlock <= nProcBlocks; iBlock++) {

				for (int kk = 0; kk < procBlockSize; kk++) {
					for (int ii = 0; ii < 2; ii++) {
						blockData[ii][kk] = audioData[ii][kk + (iBlock - 1) * procHopSize];
					}
				}

				process(blockData);
			}

			closeFeatureFile();

			b.putString("featureFile", featureFile.getAbsolutePath());
			msg.setData(b);
		} else {
            b.putString("featureFile", null);
        }

        // tell processThread we're finished
        msg.obj = feature;
        try {
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
	}


	// has to be implemented in each child process
	public void process(float[][] blockData) {
	}


	// feature file handling
	// TODO - separate class, overload appendFeature() 
	private void openFeatureFile() {

        File directory = Environment.getExternalStoragePublicDirectory(AudioFileIO.FEATURE_FOLDER);
        if( !directory.exists() ){
            directory.mkdir();
        }

		try {

            featureFile = new File(directory +
                    "/" + feature + "_" + timestamp + EXTENSION);
			featureRAF = new RandomAccessFile(featureFile,
                    "rw");

			// TODO: consider writing the header only once (minus timestamp)  

			featureRAF.writeInt(0);                        // block count will be written on close
			featureRAF.writeInt(nFeatures + 2);            // feature dimension count + timestamps (relative)
			if (procBlockSize == procOutBlockSize) {
				featureRAF.writeInt(procBlockSize);        // [samples]
				featureRAF.writeInt(procHopSize);          // [samples]
			}
			if (procBlockSize > procOutBlockSize) {
				featureRAF.writeInt(procOutBlockSize);     // [samples]
				featureRAF.writeInt(procOutBlockSize);     // [samples]
			}
			featureRAF.writeInt(samplingrate);

			featureRAF.writeBytes(timestamp.substring(9));    // HHMMssSSS, 9 bytes (absolute timestamp)

			procBlockCount = 0;
			procTimestamp[0] = 0;

			if (procBlockSize == procOutBlockSize) {
				procHopDuration = (float) procHopSize / samplingrate;
				procTimestamp[1] = (float) procBlockSize / samplingrate;
			}
			if (procBlockSize > procOutBlockSize) {
				procHopDuration = (float) procOutBlockSize / samplingrate;
				procTimestamp[1] = (float) procOutBlockSize / samplingrate;
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void appendFeature(float[] data) {

        ByteBuffer bbuffer = ByteBuffer.allocate(4 * (data.length + 2));
        FloatBuffer fbuffer = bbuffer.asFloatBuffer();

        fbuffer.put(procTimestamp);
        fbuffer.put(data);

        procTimestamp[0] += procHopDuration;
        procTimestamp[1] += procHopDuration;
        procBlockCount += 1;

        try {
            featureRAF.getChannel().write(bbuffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// called by PSD as we calculate a complete chunk in one go
	protected void appendFeature(float[][] data) {

        procBlockCount = data.length;
        procOutDuration = (float) procOutBlockSize / samplingrate;

        ByteBuffer bbuffer = ByteBuffer.allocate(4 * (data[0].length + 2) * data.length);
        FloatBuffer fbuffer = bbuffer.asFloatBuffer();

        for (float[] aData : data) {
            fbuffer.put(procTimestamp);
            fbuffer.put(aData);

            procTimestamp[0] += procOutDuration;
            procTimestamp[1] += procOutDuration;
        }

        try {
            featureRAF.getChannel().write(bbuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


	private void closeFeatureFile() {
		try {

			featureRAF.seek(0);
			featureRAF.writeInt(procBlockCount);
			featureRAF.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
