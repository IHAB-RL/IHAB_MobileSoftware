package com.fragtest.android.pa.Questionnaire;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.fragtest.android.pa.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by ulrikkowalk on 28.02.17.
 */

public class Question extends AppCompatActivity {

    private final String LOG = "Question";
    private final String mQuestionBlueprint;
    private final String mQuestionText;
    private final String mTypeAnswer;
    private final int mNumAnswers;
    private final int mQuestionId;
    private final boolean mHidden;
    //private boolean mFilterCondition;
    private final boolean mMandatory;
    private final List<String> ListOfNonTypicalAnswerTypes = Arrays.asList("text", "date");
    private final List<Integer> mListOfAnswerIds = new ArrayList<>();
    private List<Answer> mAnswers;
    private ArrayList<Integer> mFilterId;
    private Context mContext;

    // Public Constructor
    public Question(String sQuestionBlueprint, Context context) {

        mQuestionBlueprint = sQuestionBlueprint;
        mFilterId = new ArrayList<>();
        mContext = context;

        if (isFinish()) {
            mQuestionId = 99999;
            mQuestionText = extractQuestionTextFinish();
            mMandatory = true;
            mTypeAnswer = "finish";
            mNumAnswers = 1;
            mHidden = false;
            mAnswers = new ArrayList<>();
            mAnswers.add(new Answer(mContext.getResources().
                    getString(R.string.buttonTextFinish), -1, 99999));

        } else {
            // Obtain Question Id
            mQuestionId = extractQuestionId();
            // Obtain Question Text
            mQuestionText = extractQuestionText();

            // Obtain Filter Id
            mFilterId = extractFilterId();
            // Obtain Answer Type (e.g. Radio, Button, Slider,...)
            mTypeAnswer = extractTypeAnswers();
            // Obtain information whether question is mandatory
            mMandatory = extractMandatory();

            // Create List of Answers
            mAnswers = extractAnswerList();
            // In case of real text input no answer text is given
            if (mAnswers.size() == 0) {
                mAnswers = new ArrayList<>();
                mAnswers.add(new Answer("", 33333, -1, false, false));
            }

            // Obtain Number of Answers
            mNumAnswers = extractNumAnswers();
            // Determine whether Element is hidden
            mHidden = extractHidden();


            Log.e(LOG, "----------------------");
            Log.i(LOG, "Question id: "+getQuestionId());
            Log.i(LOG, ""+mQuestionText);
            for (int iId = 0; iId<mFilterId.size(); iId++) {
                Log.e(LOG,"Filter id: "+mFilterId.get(iId));
            }
        }
    }

    private int extractQuestionId() {
        // Obtain Question Id from Questionnaire
        return Integer.parseInt((mQuestionBlueprint.split("id=\"")[1].split("\"")[0]).replace("_",""));
    }

    private String extractQuestionText() {
        // Obtain Question Text from Questionnaire
        return (mQuestionBlueprint.split("<label>|</label>")[1].split("<text>|</text>")[1]);
    }

    private String extractQuestionTextFinish() {
        // Obtain Question Text from Questionnaire
        return (mQuestionBlueprint.split("\\r?\\n")[1].split("<text>|</text>")[1]);
    }

    private ArrayList<Integer> extractFilterId() {
        ArrayList<Integer> listOfFilterIds = new ArrayList<>();

        if (mQuestionBlueprint.split("filter=\"").length > 1) {
            String[] arrayTmp = mQuestionBlueprint.split("filter=\"")[1].split("\"")[0].replaceAll("\\s+","").split(",");
            for (int iId = 0; iId < arrayTmp.length; iId++) {

                // Negative factor represents EXCLUSION filter
                int nFactor = 1;
                if (arrayTmp[iId].startsWith("!")) {
                    nFactor = -1;
                }

                listOfFilterIds.add(Integer.parseInt(
                        arrayTmp[iId].replace("_","").replace("!","")) * nFactor);
            }
        }
        return listOfFilterIds;
    }

    private boolean extractMandatory() {
        return mQuestionBlueprint.contains("mandatory=\"true\"");
    }

    private int extractNumAnswers() {
        if (nonTypicalAnswer(mTypeAnswer)) {
            return 1;
        } else {
            // Obtain Number of Answers
            return mAnswers.size();
        }
    }

    private String extractTypeAnswers() {
        // Obtain Answer Type (e.g. Radio, Button, Slider,...)
        return mQuestionBlueprint.split("type=\"")[1].split("\"")[0];
    }

    private List<Answer> extractAnswerList() {

        // List of Answers
        List<Answer> listAnswers = new ArrayList<>();
        String[] stringArray = mQuestionBlueprint.split("<option|<default");

        for (int iA = 1; iA < stringArray.length; iA++) {

            String answerString = "";
            int answerId = -1;
            int answerGroup = -1;
            String sGroupTmp;
            boolean isDefault = false;
            boolean isExclusive = false;

            if (stringArray[iA].contains("option")) {
                isDefault = false;
                if (stringArray[iA].contains("id=") && stringArray[iA].split("id=\"|\"").length > 1) {
                    answerId = Integer.parseInt((stringArray[iA].split("id=\"|\"")[1]).replace("_",""));
                }
                if (stringArray[iA].contains("group=") && stringArray[iA].split("group=\"|\"").length > 1){
                    sGroupTmp = stringArray[iA].split("group=\"")[1].split("\"")[0];
                    answerGroup = Integer.parseInt(sGroupTmp);
                }
                if (stringArray[iA].split("<text>|</text>").length > 1) {
                    answerString = stringArray[iA].split("<text>|</text>")[1];
                }
                if (stringArray[iA].contains("condition=\"exclusive\"")) {
                    isExclusive = true;
                }

                listAnswers.add(new Answer(
                        answerString,
                        answerId,
                        answerGroup,
                        isDefault,
                        isExclusive
                ));
            }

            if (stringArray[iA].contains("default")) {
                isDefault = true;
                if (stringArray[iA].contains("id=") && stringArray[iA].split("id=\"|\"").length > 1) {
                    answerId = Integer.parseInt((stringArray[iA].split("id=\"|\"")[1]).replace("_",""));
                }
                if (stringArray[iA].contains("group=") && stringArray[iA].split("group=\"|\"").length > 1){
                    sGroupTmp = stringArray[iA].split("group=\"")[1].split("\"")[0];
                    answerGroup = Integer.parseInt(sGroupTmp);
                }
                if (stringArray[iA].split("<text>|</text>").length > 1) {
                    answerString = stringArray[iA].split("<text>|</text>")[1];
                }
                if (stringArray[iA].contains("condition=\"exclusive\"")) {
                    isExclusive = true;
                }
                listAnswers.add(new Answer(
                        answerString,
                        answerId,
                        answerGroup,
                        isDefault,
                        isExclusive
                ));
            }
        }
        return listAnswers;
    }

    private boolean extractHidden() {
        return mQuestionBlueprint.contains("hidden=\"true\"");
    }

    public boolean isFinish() {
        // String Array carrying introductory Line with Id, Type, Filter
        String[] introductoryLine = mQuestionBlueprint.split("\"");
        return introductoryLine.length == 1;
    }

    public boolean isHidden() {
        return mHidden;
    }

    public boolean isMandatory() {
        return mMandatory;
    }

    private boolean nonTypicalAnswer(String sTypeAnswer) {
        return ListOfNonTypicalAnswerTypes.contains(sTypeAnswer);
    }

    public String getQuestionText() {
        return mQuestionText;
    }

    public int getQuestionId() {
        return mQuestionId;
    }

    public ArrayList<Integer> getFilterId() {
        return mFilterId;
    }

    public String getTypeAnswer() {
        return mTypeAnswer;
    }

    public int getNumAnswers() {
        return mNumAnswers;
    }

    public List<Answer> getAnswers() {
        return mAnswers;
    }

    public List<Integer> getAnswerIds() {
        if (mNumAnswers > 0) {
            for (int iAnswer = 0; iAnswer < mNumAnswers; iAnswer++) {
                mListOfAnswerIds.add(mAnswers.get(iAnswer).Id);
            }
        }
        return mListOfAnswerIds;
    }
}
