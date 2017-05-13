package com.fragtest.android.pa;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CompoundButtonCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by ulrikkowalk on 17.02.17.
 */

public class AnswerTypeRadio extends AppCompatActivity {

    private String LOG_STRING = "AnswerTypeRadio";
    private RadioGroup mRadioGroup;
    private AnswerLayout mParent;
    private Context mContext;
    private Questionnaire mQuestionnaire;
    private int mQuestionId;
    private List<StringAndInteger> mListOfAnswers;
    private int mDefault = -1;


    public AnswerTypeRadio(Context context, Questionnaire questionnaire, AnswerLayout parent, int Id) {

        mContext = context;
        mQuestionnaire = questionnaire;
        mParent = parent;
        mQuestionId = Id;

        mListOfAnswers = new ArrayList<>();

        // Answer Buttons of type "radio" are grouped and handled together
        mRadioGroup = new RadioGroup(mContext);
        mRadioGroup.setOrientation(RadioGroup.VERTICAL);
    }

    public boolean addAnswer(int nAnswerId, String sAnswer, boolean isDefault) {
        mListOfAnswers.add(new StringAndInteger(sAnswer, nAnswerId));
        if (isDefault) {
            mDefault = mListOfAnswers.size() - 1;
        }
        return true;
    }

    public boolean buildView() {

        for (int iAnswer = 0; iAnswer < mListOfAnswers.size(); iAnswer++) {
            RadioButton button = new RadioButton(mContext);
            button.setId(mListOfAnswers.get(iAnswer).getId());
            button.setText(mListOfAnswers.get(iAnswer).getText());
            button.setTextSize(mContext.getResources().getDimension(R.dimen.textSizeAnswer));
            button.setChecked(false);
            button.setGravity(Gravity.CENTER_VERTICAL);
            button.setTextColor(ContextCompat.getColor(mContext, R.color.TextColor));
            button.setBackgroundColor(ContextCompat.getColor(mContext, R.color.BackgroundColor));
            int states[][] = {{android.R.attr.state_checked}, {}};
            int colors[] = {ContextCompat.getColor(mContext, R.color.JadeRed),
                    ContextCompat.getColor(mContext, R.color.JadeRed)};
            CompoundButtonCompat.setButtonTintList(button, new ColorStateList(states, colors));
            button.setMinHeight(Units.getRadioMinHeight());
            button.setPadding(24, 24, 24, 24);

            if (iAnswer == mDefault) {
                button.setChecked(true);
                mQuestionnaire.addIdToEvaluationList(mQuestionId, mListOfAnswers.get(mDefault).getId());
            }

            // Parameters of Answer Button
            LinearLayout.LayoutParams answerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);

            mRadioGroup.addView(button, answerParams);
        }
        mParent.layoutAnswer.addView(mRadioGroup);
        return true;
    }

    public void addClickListener() {

        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // In Case of Radio Buttons checking one means un-checking all other Elements
                // Therefore onClickListening must be handled on Group Level
                // listOfRadioIds contains all Ids of current Radio Group
                mQuestionnaire.removeQuestionIdFromEvaluationList(mQuestionId);
                // mEvaluationList.removeQuestionId(mQuestionId);
                mQuestionnaire.addIdToEvaluationList(mQuestionId, checkedId);
                //mEvaluationList.add(mQuestionId, checkedId);
                mRadioGroup.check(checkedId);

                // Toggle Visibility of suited/unsuited frames
                mQuestionnaire.checkVisibility();
            }
        });
    }
}
