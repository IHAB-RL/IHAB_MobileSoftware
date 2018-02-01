package com.fragtest.android.pa.Questionnaire;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CompoundButtonCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.fragtest.android.pa.DataTypes.StringIntegerAndInteger;
import com.fragtest.android.pa.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ulrikkowalk on 17.02.17.
 */

public class AnswerTypeCheckBox extends AppCompatActivity {

    private static final String LOG = "AnswerTypeCheckbox";
    public final AnswerLayout mParent;
    private final Context mContext;
    private final int mQuestionId;
    private final List<StringIntegerAndInteger> mListOfAnswers;
    private final List<Integer> mListOfDefaults;
    private final Questionnaire mQuestionnaire;
    public LinearLayout.LayoutParams answerParams;
    private int mExclusiveId = -1;

    public AnswerTypeCheckBox(Context context, Questionnaire questionnaire, AnswerLayout qParent, int nQuestionId) {

        mContext = context;
        mParent = qParent;
        mQuestionId = nQuestionId;
        mQuestionnaire = questionnaire;
        mListOfDefaults = new ArrayList<>();
        mListOfAnswers = new ArrayList<>();

    }

    public boolean addAnswer(int nAnswerId, String sAnswer, int nGroup, boolean isDefault, boolean isExclusive) {
        mListOfAnswers.add(new StringIntegerAndInteger(sAnswer, nAnswerId, nGroup));
        if (isDefault) {
            mListOfDefaults.add(mListOfAnswers.size() - 1);
        }
        if (isExclusive) {
            mExclusiveId = nAnswerId;
        }
        return true;
    }

    public void buildView() {

        for (int iAnswer = 0; iAnswer < mListOfAnswers.size(); iAnswer++) {

            int currentId = mListOfAnswers.get(iAnswer).getId();
            String currentString = mListOfAnswers.get(iAnswer).getText();

            CheckBox checkBox = new CheckBox(mContext);
            checkBox.setId(currentId);
            checkBox.setText(currentString);
            checkBox.setTextSize(mContext.getResources().getDimension(R.dimen.textSizeAnswer));
            checkBox.setChecked(false);
            checkBox.setGravity(Gravity.CENTER_VERTICAL);
            //checkBox.setGravity(Gravity.START);
            checkBox.setPadding(24, 24, 24, 24);
            /*checkBox.setPadding(
                    (int) mContext.getResources().getDimension(R.dimen.answerTypeCheckBoxPadding_Left),
                    (int) mContext.getResources().getDimension(R.dimen.answerTypeCheckBoxPadding_Top),
                    (int) mContext.getResources().getDimension(R.dimen.answerTypeCheckBoxPadding_Right),
                    (int) mContext.getResources().getDimension(R.dimen.answerTypeCheckBoxPadding_Bottom)
            );*/
            checkBox.setTextColor(ContextCompat.getColor(mContext, R.color.TextColor));
            checkBox.setBackgroundColor(ContextCompat.getColor(mContext, R.color.BackgroundColor));
            int states[][] = {{android.R.attr.state_checked}, {}};
            int colors[] = {ContextCompat.getColor(mContext, R.color.JadeRed),
                    ContextCompat.getColor(mContext, R.color.JadeRed)};
            CompoundButtonCompat.setButtonTintList(checkBox, new ColorStateList(states, colors));

            // Parameters of Answer Button
            answerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            checkBox.setMinHeight((int) mContext.getResources().getDimension(R.dimen.textSizeAnswer));

            if (mListOfAnswers.get(iAnswer).getId() == 66666) {
                checkBox.setEnabled(false);
                checkBox.setVisibility(View.INVISIBLE);
            }

            mParent.layoutAnswer.addView(checkBox, answerParams);
        }
    }

    public boolean addClickListener() {

        for (int iAnswer = 0; iAnswer < mListOfAnswers.size(); iAnswer++) {

            final int group = mListOfAnswers.get(iAnswer).getGroup();
            final int currentId = mListOfAnswers.get(iAnswer).getId();
            final CheckBox checkBox = (CheckBox) mParent.layoutAnswer.findViewById(currentId);

            if (mListOfDefaults.contains(currentId)) {
                checkBox.setChecked(true);
                mQuestionnaire.addIdToEvaluationList(mQuestionId, currentId);
            }

            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (checkBox.isChecked()) {

                        if (group != -1) {
                            unCheckGroup(group);
                        }
                        if (currentId == mExclusiveId) {
                            unCheckEverythingElse();
                        } else {
                            unCheckExclusive();
                        }
                        checkBox.setChecked(true);
                        mQuestionnaire.addIdToEvaluationList(mQuestionId, currentId);

                    } else {
                        mQuestionnaire.removeIdFromEvaluationList(currentId);
                    }
                    mQuestionnaire.checkVisibility();
                }
            });
        }
        return true;
    }

    private boolean unCheckGroup(int nGroup) {
        for (int iAnswer = 0; iAnswer < mListOfAnswers.size(); iAnswer++) {
            if (mListOfAnswers.get(iAnswer).getGroup() == nGroup) {
                int currentId = mListOfAnswers.get(iAnswer).getId();
                final CheckBox checkBox = (CheckBox) mParent.layoutAnswer.findViewById(currentId);
                checkBox.setChecked(false);
                mQuestionnaire.removeIdFromEvaluationList(currentId);
            }
        }
        return true;
    }

    private void unCheckExclusive() {
        final CheckBox checkBox = (CheckBox) mParent.layoutAnswer.findViewById(mExclusiveId);
        checkBox.setChecked(false);
        mQuestionnaire.removeIdFromEvaluationList(mExclusiveId);
    }

    private void unCheckEverythingElse() {
        for (int iAnswer = 0; iAnswer < mListOfAnswers.size(); iAnswer++) {
            if (mListOfAnswers.get(iAnswer).getId() != mExclusiveId) {
                int currentId = mListOfAnswers.get(iAnswer).getId();
                final CheckBox checkBox = (CheckBox) mParent.layoutAnswer.findViewById(currentId);
                checkBox.setChecked(false);
                mQuestionnaire.removeIdFromEvaluationList(currentId);
            }
        }
    }

}
