package com.fragtest.android.pa.Processing.Features;

import android.os.Messenger;

import com.fragtest.android.pa.HelperClasses.ArrayHelper;
import com.fragtest.android.pa.HelperClasses.Find;
import com.fragtest.android.pa.Processing.ProcessingRunnable;
import com.fragtest.android.pa.Processing.Preprocessing.Buffer;
import com.fragtest.android.pa.Processing.Preprocessing.Envelope;
import com.fragtest.android.pa.Processing.Preprocessing.FilterBank;
import com.fragtest.android.pa.Processing.Preprocessing.SpeechProcessing;
import com.fragtest.android.pa.Processing.Preprocessing.Window;

import static com.fragtest.android.pa.Processing.Preprocessing.FilterBank.computeModulationsCFs;

/**
 * Created by ul1021 on 18.09.2017.
 */

public class SRMR extends ProcessingRunnable {

    private int fsamp, blocklen;

    public SRMR(float[][] audioData, int procBlockSize, int nHop, int procOutBlockSize, int nFeatures, Messenger messenger, int samplerate) {
        super(audioData, procBlockSize, nHop, procOutBlockSize, nFeatures, messenger);
        setFeature("SRMR");

        fsamp = samplerate;
        blocklen = procBlockSize;

    }

    @Override
    public void process(float[][] data) {
        super.process(data);


        // Feature here

/*
        if(fsamp != 8000 && fsamp != 16000){
            System.err.println("please use a samplingrate of 8 or 16 kHz!, " +
                    "default of 8 kHz is used");
            fsamp = 8000;
        }
*/
        //preprocessing of the input signal with VAD and ASL
        //double[] inSigNew = preprocessing(inSig,fsamp);

        //set fix parameters
        long lowFreq = 125;
        int nCochlearFilters = 23;
        int nModFilters = 8; //Modulation filterbank
        int minCf = 4;
        int maxCF = 30;

        double winLenSec = 0.256; // Window length in seconds.
        double winIncSec = 0.064; // Window increment in seconds;
        int winLen = (int) Math.ceil(winLenSec * (double) fsamp); //in samples
        int winInc = (int) Math.ceil(winIncSec * (double) fsamp); //in samples

        double[] dataMono = new double[blocklen];
        for (int iSample = 0; iSample < blocklen; iSample++){
            dataMono[iSample] = data[iSample][0] + data[iSample][1];
        }


        //computes 23 cochlear filterbank
        double[][] cochlearOutputs = FilterBank.ERBFilterBank(dataMono,fsamp);

        double[][] envelope = new double[cochlearOutputs
                .length][cochlearOutputs[0].length];

        //computes the envelope of the cochlearOutputs
        for(int i=0;i<envelope.length;i++){

            double[] temp = new double[envelope[0].length];
            for(int j=0;j<temp.length;j++){
                temp[j] = cochlearOutputs[i][j];
            }

            double[] temp2 = Envelope.envelope(temp);

            for(int j=0;j<temp2.length;j++){
                envelope[i][j] = temp2[j];
            }
        }

        //Modulation spectrum
        double[] modFilterCFs = computeModulationsCFs(minCf,maxCF,
                nModFilters);

        double[] window = Window.hamming(winLen); //computes a hamming window

        int numOfFrames = (int)Math.ceil( (double) dataMono.length /
                ( (double)winLen - ((double)winLen-(double)winInc)) );


        double[][][] energy = new double[numOfFrames][nCochlearFilters][nModFilters];

        for(int i=0;i<nCochlearFilters;i++){

            double[] envTemp = new double[envelope[0].length];

            //get the n envelope signal
            for(int j=0;j<envTemp.length;j++){
                envTemp[j] = envelope[i][j];
            }

            //computes a modulation filterbank
            double[][] modulationOutput = FilterBank.modulationFilterBank
                    (envTemp, modFilterCFs,fsamp,2);

            double[] modOutTemp = new double[modulationOutput[0].length];

            for(int j=0;j<nModFilters;j++){

                //get the n modulation output signal
                for(int n=0;n<modOutTemp.length;n++){
                    modOutTemp[n] = modulationOutput[j][n];
                }

                //separated signal in frames
                double[][] modOutFrame = Buffer.buffer(modOutTemp,winLen,
                        (winLen-winInc));


                //computes the window on each frame
                for(int n=0;n<modOutFrame.length;n++){
                    for(int k=0;k<modOutFrame[0].length;k++){
                        modOutFrame[n][k] *= window[k];
                    }
                }

                //energy calculation
                for(int n=0;n<modOutFrame.length;n++){
                    for(int k=0;k<modOutFrame[0].length;k++){
                        modOutFrame[n][k] = Math.pow(modOutFrame[n][k],2);
                    }
                }

                double[] energyTemp = ArrayHelper.sum(modOutFrame);

                for(int n=0;n<energyTemp.length;n++){
                    energy[n][i][j] = energyTemp[n];
                }

            }

        }


        //Modulation energy thresholding
        double[][][] peak = ArrayHelper.max(ArrayHelper.max
                (ArrayHelper.mean
                        (energy,1)));

        double peakEnergy = peak[0][0][0];
        double minEnergy = peakEnergy*0.001;

        for(int i=0;i<energy.length;i++){
            for(int j=0;j<energy[0].length;j++){
                for (int n=0;n<energy[0][0].length;n++){

                    if (energy[i][j][n] < minEnergy){
                        energy[i][j][n] = minEnergy;
                    }
                    if (energy[i][j][n] > peakEnergy){
                        energy[i][j][n] = peakEnergy;
                    }
                }
            }
        }

        // Computation of K*
        double[] cochFiltBW = FilterBank.calcERBs((double)lowFreq,(double)
                fsamp,nCochlearFilters);

        double[][][] avg_energy_3d = ArrayHelper.mean(energy,3);

        double[][] avg_energy = new double[avg_energy_3d[0]
                .length][avg_energy_3d[0][0].length];

        //copy the values from the 3D array into a 2D array;
        for(int i=0;i<avg_energy.length;i++){
            for(int j=0;j<avg_energy[0].length;j++){
                avg_energy[i][j] = avg_energy_3d[0][i][j];
            }
        }

        double total_energy = ArrayHelper.sum(ArrayHelper.sum(avg_energy,1));

        double[] ACEnergy = ArrayHelper.sum(avg_energy,2);

        double[] ACPerc = new double[ACEnergy.length];

        for(int i=0;i<ACPerc.length;i++){
            ACPerc[i] = (ACEnergy[i]*100)/total_energy;
        }

        // flip up the array values
        double[] ACPerc_flip = new double[ACPerc.length];
        for(int i=0;i<ACPerc.length;i++){
            ACPerc_flip[(ACPerc.length-1)-i] = ACPerc[i];
        }

        double[] ACPercCumSum = ArrayHelper.cumsum(ACPerc_flip);

        int[] K90perc = Find.findGreaterIdx(ACPercCumSum,90);

        double BW = cochFiltBW[K90perc[0]];

        double[] cutoffs = FilterBank.calcCutoffs(modFilterCFs,(double)fsamp,
                2);

        int Kstar = 0;

        if( (BW > cutoffs[4]) && (BW < cutoffs[5]) ){
            Kstar=5;
        }
        else if( (BW > cutoffs[5]) && (BW < cutoffs[6]) ){
            Kstar=6;
        }
        else if( (BW > cutoffs[6]) && (BW < cutoffs[7]) ){
            Kstar=7;
        }
        else if( (BW > cutoffs[7]) ){
            Kstar=8;
        }

        else if( (BW > cutoffs[5]) && (BW < cutoffs[6]) ){
            Kstar=6;
        }

        // Modulation energy ratio

        double[][] temp1 = new double[avg_energy.length][4];
        double[][] temp2 = new double[avg_energy.length][Kstar-4];


        //get range t for temp1
        for(int i=0;i<temp1.length;i++){
            for(int j=0;j<temp1[0].length;j++){
                temp1[i][j] = avg_energy[i][j];
            }
        }

        //get range t for temp2
        for(int i=0;i<temp1.length;i++){
            for(int j=4, n=0;j<Kstar;j++,n++){
                temp2[i][n] = avg_energy[i][j];
            }
        }


        double ratio = (ArrayHelper.sum(ArrayHelper.sum(temp1,1))) /
                (ArrayHelper.sum(ArrayHelper.sum(temp2,1)));

        float[] result = new float[1];
        result[0] = (float) ratio;
        appendFeature(result);

    }

    public double[] preprocessing(double[] inSig, long fsamp){

        double[] outSig = SpeechProcessing.voiceActivityDetect(inSig,fsamp);
        //outSig = SpeechProcessing.activeSpeechLevel(outSig,fsamp);
        // TODO complete ASL!

        return outSig;
    }

}
