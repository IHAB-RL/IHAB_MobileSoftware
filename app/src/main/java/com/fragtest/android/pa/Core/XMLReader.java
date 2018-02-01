package com.fragtest.android.pa.Core;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Central class for XML sheet info extraction tasks
 */

public class XMLReader {

    private static String LOG = "XMLReader";
    private Context mContext;
    private FileIO mFileIO;
    private String mHead, mFoot, mSurveyURI, KEY_NEW_LINE;
    private int mTimerMean, mTimerDeviation, mTimerInterval;
    // List containing all questions (including attached information)
    private ArrayList<String> mQuestionList;

    public XMLReader(Context context, String fileName) {

        mContext = context;
        mFileIO = new FileIO();
        mQuestionList = new ArrayList<>();
        String rawInput = mFileIO.readRawTextFile(fileName);
        KEY_NEW_LINE = "\n";

        // offline version
        //String rawInput = mFileIO.readRawTextFile(mContext, R.raw.questionnairecheckboxgroup);
        String[] timerTemp = rawInput.split("<timer|</timer>");

        // timerTemp.length == 0 means no timer information can be found
        if (timerTemp.length > 1) {

            //isTimerPresent = true;

            if (timerTemp[1].split("mean").length > 1) {
                try {
                    mTimerMean = Integer.parseInt(timerTemp[1].split("\"")[1]);
                } catch (Exception e) {
                    mTimerMean = 30 * 60;
                    Log.e(LOG, "Invalid entry. Timer mean set to " + mTimerMean + " seconds.");
                }
            }

            if (timerTemp[1].split("deviation").length > 1) {
                try {
                    mTimerDeviation = Integer.parseInt(timerTemp[1].split("\"")[3]);
                } catch (Exception e) {
                    mTimerDeviation = 5 * 60;
                    Log.e(LOG, "Invalid entry. Timer mean set to 300 seconds.");
                }
            }
        } else {
            mTimerMean = 0;
            mTimerDeviation = 0;
        }

        // Split basis data into question segments
        String[] questionnaire = rawInput.split("<question|</question>|<finish>|</finish>");
        mHead = extractHead(rawInput);
        mFoot = extractFoot(rawInput);
        mSurveyURI = extractSurveyURI(rawInput);
        mQuestionList = stringArrayToListString(questionnaire);
        mQuestionList = thinOutList(mQuestionList);
    }

    private String extractHead(String rawInput) {
        String head = "";
        String[] tempHead = rawInput.split("<|>");

        head += "<";
        head += tempHead[1];
        head +=">";
        head += KEY_NEW_LINE;
        head +="<";
        head += tempHead[3];
        head += ">";

        return head;
    }

    private String extractSurveyURI(String rawInput) {
        return rawInput.split("<survey uri=\"")[1].split("\">")[0];
    }

    private String extractFoot(String rawInput) {
        String[] rawInputLines = rawInput.split("\n");
        return rawInputLines[rawInputLines.length - 1];
    }

    public int getNewTimerInterval() {
        mTimerInterval = ThreadLocalRandom.current().nextInt(
                mTimerMean - mTimerDeviation,
                mTimerMean + mTimerDeviation + 1);

        return mTimerInterval;
    }

    public boolean getQuestionnaireHasTimer() {
        return (mTimerMean != 0);
    }

    public String getHead() {
        return mHead;
    }

    public String getFoot() {
        return mFoot;
    }

    public String getSurveyURI() {
        return mSurveyURI;
    }

    public ArrayList<String> getQuestionList() {
        return mQuestionList;
    }

    private ArrayList<String> thinOutList(ArrayList<String> mQuestionList) {
        // Removes irrelevant data from question sheet

        for (int iItem = mQuestionList.size() - 1; iItem >= 0; iItem = iItem - 2) {
            mQuestionList.remove(iItem);
        }
        return mQuestionList;
    }

    private ArrayList<String> stringArrayToListString(String[] stringArray) {
        // Turns an array of Strings into a List of Strings
        ArrayList<String> listString = new ArrayList<>();
        Collections.addAll(listString, stringArray);
        return listString;
    }
}
