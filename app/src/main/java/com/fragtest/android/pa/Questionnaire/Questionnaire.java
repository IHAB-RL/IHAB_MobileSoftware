package com.fragtest.android.pa.Questionnaire;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Toast;

import com.fragtest.android.pa.BuildConfig;
import com.fragtest.android.pa.Core.EvaluationList;
import com.fragtest.android.pa.Core.FileIO;
import com.fragtest.android.pa.Core.MandatoryInfo;
import com.fragtest.android.pa.Core.MetaData;
import com.fragtest.android.pa.R;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by ulrikkowalk on 28.02.17.
 */

public class Questionnaire {

    private static final String LOG = "Questionnaire";
    // Accumulator for ids, values and texts gathered from user input
    private final EvaluationList mEvaluationList;
    // Context of QuestionnairePageAdapter for visibility
    private final QuestionnairePagerAdapter mContextQPA;
    // Context of MainActivity()
    private final Context mContext;
    // Basic information about all available questions
    private final ArrayList<QuestionInfo> mQuestionInfo;
    private final MandatoryInfo mMandatoryInfo;
    private final FileIO mFileIO;
    // Flag: display forced empty vertical spaces
    private final boolean acceptBlankSpaces = false;
    // Number of pages in questionnaire (visible and hidden)
    private int mNumPages, mViewPagerHeight;
    // ArrayList containing all questions (including all attached information)
    private ArrayList<String> mQuestionList;
    private MetaData mMetaData;
    private String mHead, mFoot, mSurveyURI, mMotivation;
    private boolean isImmersive = false;

    public Questionnaire(Context context, String head, String foot, String surveyUri,
                         String motivation, QuestionnairePagerAdapter contextQPA) {

        mContext = context;
        mContextQPA = contextQPA;
        mHead = head;
        mFoot = foot;
        mSurveyURI = surveyUri;
        mMotivation = motivation;
        mEvaluationList = new EvaluationList();
        mQuestionList = new ArrayList<>();
        mFileIO = new FileIO();
        mQuestionInfo = new ArrayList<>();
        mMandatoryInfo = new MandatoryInfo();

    }

    public void setUp(ArrayList<String> questionList) {
        //mRawInput = mFileIO.readRawTextFile();
        // offline version
        //String mRawInput = mFileIO.readRawTextFile(mContext, R.raw.question_short_eng);

        mMetaData = new MetaData(mContext, mHead, mFoot, mSurveyURI, mMotivation);
        mMetaData.initialise();
        mQuestionList = questionList;
        mNumPages = mQuestionList.size();
    }

    // Generate a Layout for Question at desired Position based on String Blueprint
    Question createQuestion(int position) {

        String sQuestionBlueprint = mQuestionList.get(position);
        Question question = new Question(sQuestionBlueprint, mContext);
        mQuestionInfo.add(new QuestionInfo(question, question.getQuestionId(),
                question.getFilterId(), position,
                question.isHidden(), question.isMandatory(), question.getAnswerIds()));
        mMetaData.addQuestion(question);
        mMandatoryInfo.add(question.getQuestionId(), question.isMandatory(), question.isHidden());

        return question;
    }

    // Builds the Layout of each Stage Question
    LinearLayout generateView(Question question, boolean immersive) {

        isImmersive = immersive;

        // Are the answers to this specific Question grouped as Radio Button Group?
        boolean isRadio = false;
        boolean isCheckBox = false;
        boolean isSliderFix = false;
        boolean isSliderFree = false;
        boolean isEmoji = false;
        boolean isText = false;
        boolean isFinish = false;
        boolean isPhotograph = false;

        LinearLayout answerContainer = new LinearLayout(mContext);
        LinearLayout.LayoutParams linearContainerParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
        answerContainer.setOrientation(LinearLayout.VERTICAL);
        answerContainer.setLayoutParams(linearContainerParams);
        answerContainer.setBackgroundColor(Color.WHITE);

        // TextView carrying the Question
        QuestionText questionText = new QuestionText(mContext, question.getQuestionId(),
                question.getQuestionText(), answerContainer);
        questionText.addQuestion();

        // Creates a Canvas for the Answer Layout
        final AnswerLayout answerLayout = new AnswerLayout(mContext);
        answerContainer.addView(answerLayout.scrollContent);

        // Format of Answer e.g. "radio", "checkbox", ...
        String sType = question.getTypeAnswer();

        final AnswerTypeRadio answerTypeRadio = new AnswerTypeRadio(
                mContext, this, answerLayout, question.getQuestionId());

        // In case of checkbox type
        final AnswerTypeCheckBox answerTypeCheckBox = new AnswerTypeCheckBox(
                mContext, this, answerLayout, question.getQuestionId());

        // In case of emoji type
        final AnswerTypeEmoji answerTypeEmoji = new AnswerTypeEmoji(
                mContext, this, answerLayout, question.getQuestionId(), isImmersive);

        // In case of sliderFix type
        final AnswerTypeSliderFix answerSliderFix = new AnswerTypeSliderFix(
                mContext, this, answerLayout, question.getQuestionId(), isImmersive);

        // In case of sliderFree type
        final AnswerTypeSliderFree answerSliderFree = new AnswerTypeSliderFree(
                mContext, this, answerLayout, question.getQuestionId(), isImmersive);

        final AnswerTypeText answerTypeText = new AnswerTypeText(
                mContext, this, answerLayout, question.getQuestionId(), isImmersive);

        final AnswerTypeFinish answerTypeFinish = new AnswerTypeFinish(
                mContext, this, answerLayout);

        /*final AnswerTypeDate answerTypeDate = new AnswerTypeDate(
                mContext, this, question.getQuestionId());*/

        final AnswerTypePhotograph answerTypePhotograph = new AnswerTypePhotograph(
                mContext, answerLayout);

        // Number of possible Answers
        int nNumAnswers = question.getNumAnswers();
        // List carrying all Answers and Answer Ids
        List<Answer> answerList = question.getAnswers();

        // Iteration over all possible Answers attributed to current question
        for (int iAnswer = 0; iAnswer < nNumAnswers; iAnswer++) {

            // Obtain Answer specific Parameters
            Answer currentAnswer = answerList.get(iAnswer);
            String sAnswer = currentAnswer.Text;
            int nAnswerId = currentAnswer.Id;
            int nAnswerGroup = currentAnswer.Group;
            boolean isDefault = currentAnswer.isDefault();
            boolean isExclusive = currentAnswer.isExclusive();

            if (((nAnswerId == 66666) && (acceptBlankSpaces)) || (nAnswerId != 66666)) {

                switch (sType) {
                    /*case "date": {
                        answerTypeDate.addAnswer(sAnswer);
                        break;
                    }*/
                    case "radio": {
                        isRadio = true;
                        answerTypeRadio.addAnswer(nAnswerId, sAnswer, isDefault);
                        break;
                    }
                    case "checkbox": {
                        isCheckBox = true;
                        answerTypeCheckBox.addAnswer(nAnswerId, sAnswer, nAnswerGroup,
                                isDefault, isExclusive);
                        break;
                    }
                    case "text": {
                        isText = true;
                        if (nNumAnswers > 0) {
                            answerTypeText.addQuestion(sAnswer);
                        }
                        break;
                    }
                    case "finish": {
                        isFinish = true;
                        answerTypeFinish.addAnswer();
                        break;
                    }
                    case "sliderFix": {
                        isSliderFix = true;
                        answerSliderFix.addAnswer(nAnswerId, sAnswer, isDefault);
                        break;
                    }
                    case "sliderFree": {
                        isSliderFree = true;
                        answerSliderFree.addAnswer(nAnswerId, sAnswer, isDefault);
                        break;
                    }
                    case "emoji": {
                        isEmoji = true;
                        answerTypeEmoji.addAnswer(nAnswerId, sAnswer, isDefault);
                        break;
                    }
                    case "photograph": {
                        isPhotograph = true;
                        answerTypePhotograph.addAnswer(sAnswer, nAnswerId);
                        break;
                    }
                    default: {
                        isRadio = false;
                        if (BuildConfig.DEBUG) {
                            Log.e(LOG, "Weird object found. Id: " +
                                    question.getQuestionId());
                        }
                        break;
                    }
                }
            }
        }

        if (isText) {
            answerTypeText.buildView();
            answerTypeText.addClickListener();
        }

        if (isCheckBox) {
            answerTypeCheckBox.buildView();
            answerTypeCheckBox.addClickListener();
        }

        if (isEmoji) {
            answerTypeEmoji.buildView();
            answerTypeEmoji.addClickListener();
        }

        if (isSliderFix) {
            answerSliderFix.buildView();
            answerSliderFix.addClickListener();
        }

        if (isSliderFree) {
            answerSliderFree.buildView();
            answerSliderFree.addClickListener();
        }

        if (isRadio) {
            answerTypeRadio.buildView();
            answerTypeRadio.addClickListener();
        }

        if (isFinish) {
            answerTypeFinish.addClickListener();
        }

        if (isPhotograph) {
            answerTypePhotograph.buildView();
            answerTypePhotograph.addClickListener();
        }

        return answerContainer;
    }

    boolean addValueToEvaluationList(int questionId, float value) {
        mEvaluationList.add(questionId, value);
        return true;
    }

    boolean addTextToEvaluationLst(int questionId, String text) {
        mEvaluationList.add(questionId, text);
        return true;
    }

    boolean addIdToEvaluationList(int questionId, int id) {
        mEvaluationList.add(questionId, id);
        return true;
    }

    boolean removeIdFromEvaluationList(int id) {
        mEvaluationList.removeAnswerId(id);
        return true;
    }

    boolean removeQuestionIdFromEvaluationList(int questionId) {
        mEvaluationList.removeQuestionId(questionId);
        return true;
    }

    boolean finaliseEvaluation() {
        mMetaData.finalise(mEvaluationList);
        mFileIO.saveDataToFile(mContext, mMetaData.getFileName(), mMetaData.getData());
        Toast.makeText(mContext, R.string.infoTextSave, Toast.LENGTH_SHORT).show();
        returnToMenu();
        return true;
    }

    int getNumPages() {
        return mNumPages;
    }

    public int getId(Question question) {
        return question.getQuestionId();
    }

    // Function checks all available pages on whether their filtering condition has been met and
    // toggles visibility by destroying or creating the views and adding them to the list of
    // views which is handled by QuestionnairePagerAdapter
    boolean checkVisibility() {

        String sid = "";
        for (int iQ = 0; iQ < mEvaluationList.size(); iQ++) {
            sid += mEvaluationList.get(iQ).getValue();
            sid += ", ";
        }

        boolean wasChanged = true;

        // Repeat until nothing changes anymore
        while (wasChanged) {
            wasChanged = false;

            for (int iPos = 0; iPos < mQuestionInfo.size(); iPos++) {

                QuestionInfo qI = mQuestionInfo.get(iPos);

                if (qI.isActive()) {                                                                    // View is active but might be obsolete

                    if (qI.isHidden()) {                                                                // View is declared "Hidden"
                        removeQuestion(iPos);
                        wasChanged = true;
                    } else if (!mEvaluationList.containsAtLeastOneAnswerId(qI.getFilterIdPositive())    // Not even 1 positive Filter Id exists OR No positive filter Ids declared
                            && qI.getFilterIdPositive().size() > 0) {
                        removeQuestion(iPos);
                        wasChanged = true;
                    } else if (mEvaluationList.containsAtLeastOneAnswerId(qI.getFilterIdNegative())) {  // At least 1 negative filter Id exists
                        removeQuestion(iPos);
                        wasChanged = true;
                    }

                } else {                                                                                // View is inactive but should possibly be active

                    if (!qI.isHidden()
                            && (mEvaluationList.containsAtLeastOneAnswerId(qI.getFilterIdPositive())    // View is not declared "Hidden"
                            || qI.getFilterIdPositive().size() == 0)                                    // && (At least 1 positive Filter Id exists OR No positive filter Ids declared)
                            && (!mEvaluationList.containsAtLeastOneAnswerId(qI.getFilterIdNegative())   // && (Not even 1 negative Filter Id exists OR No negative filter Ids declared)
                            || qI.getFilterIdNegative().size() == 0)
                            ) {
                        addQuestion(iPos);
                        wasChanged = true;
                    }
                }
            }
        }
        return true;
    }

    // Adds the question to the displayed list
    private boolean addQuestion(int iPos) {

        mQuestionInfo.get(iPos).setActive();
        // View is fetched from Storage List and added to Active List
        mContextQPA.addView(mContextQPA.mListOfViewsStorage.get(iPos).getView(),
                mContextQPA.mListOfActiveViews.size(),                                  // this is were the injection happens
                mContextQPA.mListOfViewsStorage.get(iPos).getPositionInRaw(),
                mContextQPA.mListOfViewsStorage.get(iPos).isMandatory(),
                mContextQPA.mListOfViewsStorage.get(iPos).getListOfAnswerIds());
        renewPositionsInPager();
        mContextQPA.notifyDataSetChanged();
        mContextQPA.setQuestionnaireProgressBar();

        return true;
    }

    // Removes the question from the displayed list and all given answer ids from memory
    private boolean removeQuestion(int iPos) {

        // If view is not mandatory -> can really be removed including entries in mEvaluationList
        if (!mMandatoryInfo.isMandatoryFromId(mQuestionInfo.get(iPos).getId())) {

            mQuestionInfo.get(iPos).setInactive();
            mEvaluationList.removeQuestionId(mQuestionInfo.get(iPos).getId());
            // Remove checked answers on removed questions
            String sType = mQuestionInfo.get(iPos).getQuestion().getTypeAnswer();
            List<Integer> mListOfAnswerIds = mQuestionInfo.get(iPos).getAnswerIds();

            // Visually un-check checkboxes and radio buttons
            for (int iAnswer = 0; iAnswer < mListOfAnswerIds.size(); iAnswer++) {
                if (sType.equals("checkbox") && mListOfAnswerIds.get(iAnswer) != 66666) {
                    CheckBox checkBox = (CheckBox) mContextQPA.mViewPager.findViewById(
                            mQuestionInfo.get(iPos).getAnswerIds().get(iAnswer));
                    if (checkBox != null) {
                        checkBox.setChecked(false);
                    }
                } else if (sType.equals("radio") && mListOfAnswerIds.get(iAnswer) != 66666) {
                    RadioButton radioButton = (RadioButton) mContextQPA.mViewPager.findViewById(
                            mQuestionInfo.get(iPos).getAnswerIds().get(iAnswer));
                    if (radioButton != null) {
                        radioButton.setChecked(false);
                    }
                }
            }
        }

        // Remove View from ActiveList
        mQuestionInfo.get(iPos).setInactive();
        mContextQPA.removeView(mQuestionInfo.get(iPos).getPositionInPager());
        renewPositionsInPager();
        mContextQPA.notifyDataSetChanged();
        mContextQPA.setQuestionnaireProgressBar();

        return true;
    }

    // Renews all the positions in information object gathered from actual order
    private void renewPositionsInPager() {

        for (int iItem = 0; iItem < mQuestionInfo.size(); iItem++) {
            int iId = mQuestionInfo.get(iItem).getId();
            mQuestionInfo.get(iItem).setPositionInPager(mContextQPA.getPositionFromId(iId));
        }
    }

    private void returnToMenu() {
        mContextQPA.createMenu();
    }
}