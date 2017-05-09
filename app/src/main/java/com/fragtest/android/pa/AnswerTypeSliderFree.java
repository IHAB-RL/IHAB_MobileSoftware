package com.fragtest.android.pa;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ulrikkowalk on 04.04.17.
 */

public class AnswerTypeSliderFree extends AppCompatActivity {

    private static String LOG_STRING = "AnswerTypeSliderFree";
    private AnswerLayout parent;
    private Context mContext;
    private List<StringAndInteger> mListOfAnswers;
    private LinearLayout mHorizontalContainer, mAnswerListContainer;
    private View mResizeView, mRemainView;
    private int mUsableHeight, nTextViewHeight, mQuestionId;
    private int mDefaultAnswer = -1;
    private int mMinProgress = 10;
    private int[] answerLayoutPadding;
    private EvaluationList mEvaluationList;

    public AnswerTypeSliderFree(Context context, AnswerLayout qParent, int nQuestionId) {

        mContext = context;
        parent = qParent;
        mListOfAnswers = new ArrayList<>();
        mQuestionId = nQuestionId;

        // Slider Layout is predefined in XML
        LayoutInflater inflater = LayoutInflater.from(context);
        int width = Units.getScreenWidth();
        answerLayoutPadding = Units.getAnswerLayoutPadding();

        parent.scrollContent.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.f
        ));

        mUsableHeight = (new Units(mContext)).getUsableSliderHeight();

        /**
         *
         *  |           mHorizontalContainer          |
         *  | mSliderContainer | mAnswerListContainer |
         *
         * **/

        // mHorizontalContainer is parent to both slider and answer option containers
        mHorizontalContainer = (LinearLayout) inflater.inflate(
                R.layout.answer_type_slider, parent.scrollContent, false);
        mHorizontalContainer.setOrientation(LinearLayout.HORIZONTAL);
        mHorizontalContainer.setLayoutParams(new LinearLayout.LayoutParams(
                width,
                mUsableHeight,
                1.f
        ));
        mHorizontalContainer.setBackgroundColor(
                ContextCompat.getColor(mContext, R.color.BackgroundColor));

        // mSliderContainer is host to slider on the left
        RelativeLayout mSliderContainer = (RelativeLayout) mHorizontalContainer.findViewById(
                R.id.SliderContainer);
        mSliderContainer.setBackgroundColor(ContextCompat.getColor(
                mContext, R.color.BackgroundColor));

        // mAnswerListContainer is host to vertical array of answer options
        mAnswerListContainer = (LinearLayout) mHorizontalContainer.
                findViewById(R.id.AnswerTextContainer);

        mAnswerListContainer.setOrientation(LinearLayout.VERTICAL);
        mAnswerListContainer.setBackgroundColor(ContextCompat.getColor(
                mContext, R.color.BackgroundColor));
        mAnswerListContainer.setLayoutParams(new LinearLayout.LayoutParams(
                width-100,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.f
        ));

        mResizeView = mHorizontalContainer.findViewById(R.id.ResizeView);
        mRemainView = mHorizontalContainer.findViewById(R.id.RemainView);

        mResizeView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.JadeRed));
    }

    public void buildSlider() {

        // Iterate over all options and create a TextView for each one
        for (int iAnswer = 0; iAnswer < mListOfAnswers.size(); iAnswer++) {
            TextView textMark = new TextView(mContext);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1.0f);

            textParams.setMargins(
                    (int) mContext.getResources().getDimension(R.dimen.SliderFixTextBottomMargin_Left),
                    (int) mContext.getResources().getDimension(R.dimen.SliderFixTextBottomMargin_Top),
                    (int) mContext.getResources().getDimension(R.dimen.SliderFixTextBottomMargin_Right),
                    (int) mContext.getResources().getDimension(R.dimen.SliderFixTextBottomMargin_Bottom));
            textMark.setPadding(
                    (int) mContext.getResources().getDimension(R.dimen.SliderFixTextPadding_Left),
                    (int) mContext.getResources().getDimension(R.dimen.SliderFixTextPadding_Top),
                    (int) mContext.getResources().getDimension(R.dimen.SliderFixTextPadding_Right),
                    (int) mContext.getResources().getDimension(R.dimen.SliderFixTextPadding_Bottom));
            textMark.setText(mListOfAnswers.get(iAnswer).getText());
            textMark.setId(mListOfAnswers.get(iAnswer).getId());
            textMark.setTextColor(ContextCompat.getColor(mContext, R.color.TextColor));

            // Adaptive size and lightness of font of in-between tick marks
            if (mListOfAnswers.size() > 6) {
                int textSize = Units.getTextSizeAnswer() * 7 /mListOfAnswers.size();
                if (iAnswer%2 == 1){
                    textSize -= 2;
                    textMark.setTextColor(ContextCompat.getColor(mContext,R.color.TextColor_Light));
                }
                textMark.setTextSize(textSize);
            } else {
                textMark.setTextSize(Units.getTextSizeAnswer());
            }

            textMark.setGravity(Gravity.CENTER_VERTICAL);
            textMark.setLayoutParams(textParams);
            textMark.setBackgroundColor(ContextCompat.getColor(mContext, R.color.BackgroundColor));

            mAnswerListContainer.addView(textMark);
        }
        parent.layoutAnswer.addView(mHorizontalContainer);
    }

    public boolean addAnswer(int nAnswerId, String sAnswer, boolean isDefault) {
        mListOfAnswers.add(new StringAndInteger(sAnswer,nAnswerId));
        // index of default answer if present
        if (isDefault) {
            // If default present, this element is the one
            mDefaultAnswer = mListOfAnswers.size() - 1;
            // Handles default id if existent
            setProgressItem(mDefaultAnswer);
        }
        return true;
    }

    public EvaluationList addClickListener(EvaluationList evaluationList) {
        mEvaluationList = evaluationList;

        final TextView tvTemp = (TextView) mAnswerListContainer.findViewById(mListOfAnswers.get(0).getId());
        tvTemp.post(new Runnable() {
            @Override
            public void run() {
                nTextViewHeight = tvTemp.getHeight();
                // Handles default id if existent
                if (mDefaultAnswer == -1) {

                    setProgressFraction(0.5f);
                    mEvaluationList.add(mQuestionId, 0.5f);
                    //answerIds.add(mListOfAnswers.get(mListOfAnswers.size() / 2).getId());
                } else {
                    mEvaluationList.add(mQuestionId, getFractionFromProgress(
                            setProgressItem(mDefaultAnswer)));
                }
            }
        });

        // Enables clicking on option directly
        for (int iAnswer = 0; iAnswer < mListOfAnswers.size(); iAnswer++) {
            final int numAnswer = iAnswer;
            final int currentId = mListOfAnswers.get(iAnswer).getId();
            TextView tv = (TextView) mAnswerListContainer.findViewById(currentId);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mEvaluationList.removeQuestionId(mQuestionId);
                    setProgressItem(numAnswer);
                    mEvaluationList.add(mQuestionId, getFractionFromProgress());
                    //answerIds.removeAll(mListOfIds);
                    //answerIds.add(currentId);
                }
            });
        }

        // Enables dragging of slider
        mResizeView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = MotionEventCompat.getActionMasked(event);
                switch(action) {
                    case (MotionEvent.ACTION_DOWN) :
                        return true;
                    case (MotionEvent.ACTION_MOVE) :
                        rescaleSliderOnline(event);
                        return true;
                    case (MotionEvent.ACTION_UP) :
                        rescaleSliderFinal(event, mEvaluationList);
                        return true;
                    case (MotionEvent.ACTION_CANCEL) :
                        return true;
                    case (MotionEvent.ACTION_OUTSIDE) :
                        Log.d("Motion","Movement occurred outside bounds " +
                                "of current screen element");
                        return true;
                    default :
                }
                return true;
            }
        });

        // Enables clicking in area above slider (remainView) to adjust
        mRemainView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = MotionEventCompat.getActionMasked(event);
                switch(action) {
                    case (MotionEvent.ACTION_DOWN) :
                        return true;
                    case (MotionEvent.ACTION_MOVE) :
                        rescaleSliderOnline(event);
                        return true;
                    case (MotionEvent.ACTION_UP) :
                        rescaleSliderFinal(event, mEvaluationList);
                        return true;
                    case (MotionEvent.ACTION_CANCEL) :
                        return true;
                    case (MotionEvent.ACTION_OUTSIDE) :
                        Log.d("Motion","Movement occurred outside bounds " +
                                "of current screen element");
                        return true;
                    default :
                }
                return true;
            }
        });
        return mEvaluationList;
    }


    // Set progress  bar according to user input
    private void rescaleSliderFinal(MotionEvent motionEvent, EvaluationList evaluationList) {
        int nValueSelected = (int) clipValuesToRange(motionEvent.getRawY());
        //int nItem = mapValuesToItems(nValueSelected);
        //nItem = clipItemsToRange(nItem);
        try {
            setProgressPixels(nValueSelected);
            evaluationList.removeQuestionId(mQuestionId);
            evaluationList.add(mQuestionId, getFractionFromProgress());
            //setProgressItem(nItem);
            //answerIds.removeAll(mListOfIds);
            //addIdFromItem(nItem, answerIds);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Set progress  bar according to user input
    private void rescaleSliderOnline(MotionEvent motionEvent) {
        int nValueSelected = (int) clipValuesToRange(motionEvent.getRawY());
        //int nItem = mapValuesToItems(nValueSelected);
        //nItem = clipItemsToRange(nItem);
        try {
            setProgressPixels(nValueSelected);
            //setProgressItem(nItem);
            //answerIds.removeAll(mListOfIds);
            //addIdFromItem(nItem, answerIds);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Ensure values inside slider boundaries
    private float clipValuesToRange(float inVal) {
        if (inVal < Units.getScreenHeight() - mUsableHeight - answerLayoutPadding[3]) {
            inVal = Units.getScreenHeight() - mUsableHeight - answerLayoutPadding[3];
        } else if (inVal > Units.getScreenHeight() - answerLayoutPadding[3] - mMinProgress) {
            inVal = Units.getScreenHeight() - answerLayoutPadding[3] - mMinProgress;
        }
        return inVal;
    }

    // Set progress/slider according to number of selected item (counting from 0)
    private int setProgressItem(int numItem) {
        int nPixProgress = (int) ((2 * (mListOfAnswers.size() - numItem) - 1) /
                2.0f * nTextViewHeight);
        mResizeView.getLayoutParams().height = nPixProgress;
        mResizeView.setLayoutParams(mResizeView.getLayoutParams());
        return nPixProgress;
    }

    // Set progress/slider according to user input measured in pixels
    private void setProgressPixels(int nPixels) {
        mResizeView.getLayoutParams().height = Units.getScreenHeight() - answerLayoutPadding[3] - nPixels;
        mResizeView.setLayoutParams(mResizeView.getLayoutParams());
    }

    // Set progress/slider according to floating point input between 0.0 and 1.0
    private int setProgressFraction(float nFraction) {
        int nPixProgress = (int) (mUsableHeight * nFraction);
        mResizeView.getLayoutParams().height = nPixProgress;
        mResizeView.setLayoutParams(mResizeView.getLayoutParams());
        return nPixProgress;
    }

    // Obtain percentage value of user input
    private float getFractionFromProgress(int nPixels) {
        return (float) (nPixels - mMinProgress) / (mUsableHeight - mMinProgress);
    }

    // Returns a floating point number between 0.0 and 1.0 according to progress
    private float getFractionFromProgress() {
        return (float) (mResizeView.getLayoutParams().height - mMinProgress) /
                (mUsableHeight - mMinProgress);
    }
}



