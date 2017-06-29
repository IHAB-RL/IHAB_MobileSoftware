package com.fragtest.android.pa.Questionnaire;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by ulrikkowalk on 28.02.17.
 */

public class QuestionnairePagerAdapter extends PagerAdapter {

    private String LOG_STRING = "Quest..PagerAdapter";
    // Stores all active Views
    public ArrayList<QuestionViewActive> mListOfActiveViews;
    // Stores all Views
    public ArrayList<QuestionViewActive> mListOfViewsStorage;
    public ViewPager mViewPager;
    private int mNUM_PAGES;
    private Questionnaire mQuestionnaire;
    private LinearLayout mLayout;

    public QuestionnairePagerAdapter(Context context, ViewPager viewPager) {

        mViewPager = viewPager;
        // Instantiates a Questionnaire Object based on Contents of raw XML File
        mQuestionnaire = new Questionnaire(context, this);

        mQuestionnaire.setUp();
        mNUM_PAGES = mQuestionnaire.getNumPages();
        mViewPager.setOffscreenPageLimit(mNUM_PAGES - 1);

        mListOfActiveViews = new ArrayList<>();
        mListOfViewsStorage = new ArrayList<>();

        createLayout();
        // Creates and destroys views based on filter id settings
        mQuestionnaire.checkVisibility();
    }

    private void createLayout() {
        for (int iQuestion = 0; iQuestion < mNUM_PAGES; iQuestion++) {
            // Extracts Question Details from Questionnaire and creates Question
            Question question = mQuestionnaire.createQuestion(iQuestion);
            // Inflates Question Layout based on Question Details
            mLayout = mQuestionnaire.generateView(question);
            // Sets Layout Id to Question Id
            mLayout.setId(mQuestionnaire.getId(question));
            // Adds the Layout to List carrying all ACTIVE Views
            mListOfActiveViews.add(new QuestionViewActive(mLayout, mLayout.getId(),
                    iQuestion, question.isMandatory(), question.getAnswers()));
            // Adds the Layout to List storing ALL Views
            mListOfViewsStorage.add(new QuestionViewActive(mLayout, mLayout.getId(),
                    iQuestion, question.isMandatory(), question.getAnswers()));
        }
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        View view = mListOfActiveViews.get(position).getView();
        collection.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        collection.removeView((View) view);
    }

    @Override
    public int getCount() {
        mNUM_PAGES = mListOfActiveViews.size();
        return mNUM_PAGES;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();

    }

    public int addView(View view, int position, int positionInRaw, boolean mandatory,
                       List<Answer> listOfAnswers) {
        mListOfActiveViews.add(new QuestionViewActive(view, view.getId(), positionInRaw, mandatory,
                listOfAnswers));
        // Sort the Views by their id (implicitly their determined order)
        Collections.sort(mListOfActiveViews);
        return position;
    }

    public int removeView(int position) {

        int nCurrentItem = mViewPager.getCurrentItem();
        mViewPager.setAdapter(null);
        mListOfActiveViews.remove(position);
        mViewPager.setAdapter(this);
        mViewPager.setCurrentItem(nCurrentItem);
        return position;
    }

    public QuestionViewActive getTextViewById(int id) {
        for (int iView = 0; iView < mListOfActiveViews.size(); iView++) {
            if (mListOfActiveViews.get(iView).getId() == id) {
                return mListOfActiveViews.get(iView);
            }
        }
        return null;
    }

    @Override
    public int getItemPosition(Object object) {
        int index = mListOfActiveViews.indexOf(object);

        if (index == -1)
            return POSITION_NONE;
        else
            return index;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return "";
    }

    public int getPositionFromId(int iId) {
        for (int iItem = 0; iItem < mListOfActiveViews.size(); iItem++) {
            if (mListOfActiveViews.get(iItem).getId() == iId) {
                return iItem;
            }
        }
        return -1;
    }

    public boolean clearAnswerIds() {
        return mQuestionnaire.clearAnswerIds();
    }

    public boolean clearAnswerTexts() {
        return mQuestionnaire.clearAnswerTexts();
    }

}