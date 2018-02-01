package com.fragtest.android.pa.Questionnaire;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.support.v4.BuildConfig;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.fragtest.android.pa.ControlService;
import com.fragtest.android.pa.Core.Units;
import com.fragtest.android.pa.MainActivity;
import com.fragtest.android.pa.Menu.Help;
import com.fragtest.android.pa.Menu.MenuPage;
import com.fragtest.android.pa.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by ulrikkowalk on 28.02.17.
 */

public class QuestionnairePagerAdapter extends PagerAdapter {

    private static String LOG = "Quest..PagerAdapter";
    final ViewPager mViewPager;
    private final MainActivity MainActivity;
    private final Context mContext;
    private final Handler mCountDownHandler = new Handler();
    private final int mUpdateRate = 1000;
    // Stores all active Views
    ArrayList<QuestionViewActive> mListOfActiveViews;
    // Stores all Views
    ArrayList<QuestionViewActive> mListOfViewsStorage;
    private boolean isCountDownRunning = false;
    private boolean isTimer = false;
    private boolean isInForeGround = false;
    private boolean isMenu = false;
    //private boolean isQuestionnaireActive = false;
    private boolean needsIncreasing = false;
    private boolean isPrefsInForeGround = false;
    private boolean isQuestionnairePresent = false;
    private int mCountDownInterval = 30;
    private int mNUM_PAGES;
    private int mFinalCountdown = -255;
    private int mSecondsRemaining = 120;
    private String mHead, mFoot, mSurveyURI;
    private Questionnaire mQuestionnaire;
    private MenuPage mMenuPage;
    private Help mHelpScreen;
    private boolean isImmersive = false;
    private Units mUnits;
    private float batteryPlaceholderWeight;

    private IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    private Intent batteryStatus;

    private final Runnable mCountDownRunnable = new Runnable() {
        @Override
        public void run() {
            if (isCountDownRunning) {
                mSecondsRemaining = mFinalCountdown - (int) (System.currentTimeMillis() / 1000);
                updateCountDown();
                mCountDownHandler.postDelayed(this, mUpdateRate);

                //setBatteryLogo();
            }
        }
    };
    private final Runnable mHideProgressBarRunnable = new Runnable() {
        @Override
        public void run() {
                hideQuestionnaireProgressBar();

        }
    };
    private String mMotivation = "";
    private ArrayList<String> mQuestionList;

    public QuestionnairePagerAdapter(Context context, ViewPager viewPager, boolean immersive) {
        mContext = context;
        MainActivity = (MainActivity) context;
        mViewPager = viewPager;
        isImmersive = immersive;
        mUnits = new Units(mContext);
        batteryPlaceholderWeight = mContext.getResources().getIntArray(R.array.battery_placeholder_weight)[0]*0.01f;
        handleControls();
    }

    public void noQuestionnaires() {
        isQuestionnairePresent = false;
        isTimer = false;
        mMenuPage.setText(mContext.getResources().getString(R.string.noQuestionnaires));
        mMenuPage.updateCountDownText("");
    }

    public void noTimer() {
        isTimer = false;
        stopCountDown();
        mMenuPage.updateCountDownText("");
        mCountDownHandler.post(mHideProgressBarRunnable);
        if (BuildConfig.DEBUG) {
            Log.i(LOG, "Timer offline.");
        }
    }

    public void questionnairePresent() {
        isQuestionnairePresent = true;
    }

    // Calculation of remaining time and visual update
    private void updateCountDown() {

        if (mSecondsRemaining >= 0 && isTimer) {
            mMenuPage.updateCountdownText(mSecondsRemaining);
            setQuestionnaireProgressBar((float) mSecondsRemaining / mCountDownInterval);
        } else {
            stopCountDown();
        }
    }

    // Instate new countdown target
    public void setFinalCountDown(int finalCountDown, int countDownInterval) {
        mFinalCountdown = finalCountDown;
        mCountDownInterval = countDownInterval;

        // Not the most beautiful and holistic approach but easiest this way
        if (mCountDownInterval > 0) {
            isTimer = true;
            if (BuildConfig.DEBUG) {
                Log.i(LOG, "Final Countdown set: " + finalCountDown);
            }
        }
    }

    public void displayManualStart() {
        if (isQuestionnairePresent) {
            mMenuPage.setText(mContext.getResources().getString(R.string.menuText));
        } else {
            mMenuPage.setText(mContext.getResources().getString(R.string.noQuestionnaires));
        }
    }

    // Start/restart countdown and determine validity
    public void startCountDown() {
        mMenuPage.resetStartTextSize();
        mMenuPage.setText(mContext.getResources().getString(R.string.menuText));
        Log.i(LOG, "Text set to: " + mContext.getResources().getString(R.string.menuText));

        if (isTimer)
            if ((mFinalCountdown - System.currentTimeMillis() / 1000) >= 0) {
                mCountDownHandler.post(mCountDownRunnable);
                isCountDownRunning = true;
                if (BuildConfig.DEBUG) {
                    Log.i(LOG, "Countdown started.");
                }
            } else {
                if (BuildConfig.DEBUG) {
                    Log.e(LOG, "Countdown out of time.");
                }
                stopCountDown();
                setQuestionnaireProgressBar(0f);
            }
        }

    // End/Stop countdown
    private void stopCountDown() {
        isCountDownRunning = false;
        mCountDownHandler.removeCallbacks(mCountDownRunnable);
        if (BuildConfig.DEBUG) {
            Log.i(LOG, "CountDown stopped.");
        }
    }

    private void setBatteryLogo() {
        LinearLayout.LayoutParams regparams = new LinearLayout.LayoutParams(
                mUnits.convertDpToPixels(12),
                0,
                (1.0f-2* batteryPlaceholderWeight)*(1.0f - getbatteryInfo())
        );
        regparams.leftMargin = mUnits.convertDpToPixels(0);
        regparams.rightMargin = mUnits.convertDpToPixels(10);
        MainActivity.mBatteryReg.setLayoutParams(regparams);

        LinearLayout.LayoutParams progparams = new LinearLayout.LayoutParams(
                mUnits.convertDpToPixels(12),
                0,
                (1.0f-2* batteryPlaceholderWeight)*getbatteryInfo()
        );
        progparams.leftMargin = mUnits.convertDpToPixels(0);
        progparams.rightMargin = mUnits.convertDpToPixels(0);
        MainActivity.mBatteryProg.setLayoutParams(progparams);
    }

    private float getbatteryInfo() {

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        return level / (float)scale;
    }

    // Initialise menu with visible countdown
    public void createMenu() {

        batteryStatus = mContext.registerReceiver(null, ifilter);

        isMenu = true;
        needsIncreasing = false;
        sendMessage(ControlService.MSG_ISMENU);
        // Instantiates a MenuPage Object based on Contents of raw XML File
        mMenuPage = new MenuPage(MainActivity, this);
        mNUM_PAGES = 1;
        mViewPager.setOffscreenPageLimit(0);

        mListOfActiveViews = new ArrayList<>();
        mListOfViewsStorage = new ArrayList<>();

        createMenuLayout();
        setControlsMenu();

        setBatteryLogo();

        sendMessage(ControlService.MSG_QUESTIONNAIRE_INACTIVE);
    }

    public void backToMenu() {

        isMenu = true;
        sendMessage(ControlService.MSG_ISMENU);
        // Instantiates a MenuPage Object based on Contents of raw XML File
        mMenuPage = new MenuPage(MainActivity, this);
        mNUM_PAGES = 1;
        mViewPager.setOffscreenPageLimit(0);

        mListOfActiveViews = new ArrayList<>();
        mListOfViewsStorage = new ArrayList<>();

        createMenuLayout();
        setControlsMenu();

        onResume();

        if (!isQuestionnairePresent) {
                noQuestionnaires();
        }

    }

    public void createHelpScreen() {

        // Instantiates a MenuPage Object based on Contents of raw XML File
        mHelpScreen = new Help(MainActivity, this);
        mNUM_PAGES = 1;
        mViewPager.setOffscreenPageLimit(0);

        mListOfActiveViews = new ArrayList<>();
        mListOfViewsStorage = new ArrayList<>();

        createHelpLayout();
    }

    private void createHelpLayout() {

        LinearLayout layout = mHelpScreen.generateView();

        layout.setId(0);
        // Adds the Layout to List carrying all ACTIVE Views
        mListOfActiveViews.add(new QuestionViewActive(layout, layout.getId(),
                0, true, null));
        // Adds the Layout to List storing ALL Views
        mListOfViewsStorage.add(new QuestionViewActive(layout, layout.getId(),
                0, true, null));

        notifyDataSetChanged();
        mViewPager.setCurrentItem(0);
    }

    // Initialise questionnaire based on new input parameters
    public void createQuestionnaire(ArrayList<String> questionList, String head, String foot,
                                    String surveyUri, String motivation) {

        isMenu = false;
        stopCountDown();
        sendMessage(ControlService.MSG_QUESTIONNAIRE_ACTIVE);
        //isQuestionnaireActive = true;
        mQuestionList = questionList;
        mHead = head;
        mFoot = foot;
        mSurveyURI = surveyUri;
        mMotivation = motivation;

        // Instantiates a Questionnaire Object based on Contents of raw XML File
        mQuestionnaire = new Questionnaire(MainActivity, mHead, mFoot,
                mSurveyURI, mMotivation, this);
        mQuestionnaire.setUp(questionList);
        mNUM_PAGES = mQuestionnaire.getNumPages();
        mViewPager.setOffscreenPageLimit(1);

        mListOfActiveViews = new ArrayList<>();
        mListOfViewsStorage = new ArrayList<>();

        createQuestionnaireLayout();
        setControlsQuestionnaire();
        // Creates and destroys views based on filter id settings
        mQuestionnaire.checkVisibility();

        notifyDataSetChanged();
        mViewPager.setCurrentItem(0);
        setArrows(0);
        setQuestionnaireProgressBar();
    }

    // Initialise questionnaire based on last input parameters (only used in case of reversion)
    private void createQuestionnaire() {

        isMenu = false;
        stopCountDown();
        sendMessage(ControlService.MSG_QUESTIONNAIRE_ACTIVE);
        // Instantiates a Questionnaire Object based on Contents of raw XML File
        mQuestionnaire = new Questionnaire(MainActivity, mHead, mFoot, mSurveyURI,
                mMotivation, this);
        mQuestionnaire.setUp(mQuestionList);
        mNUM_PAGES = mQuestionnaire.getNumPages();
        mViewPager.setOffscreenPageLimit(1);

        mListOfActiveViews = new ArrayList<>();
        mListOfViewsStorage = new ArrayList<>();

        createQuestionnaireLayout();
        setControlsQuestionnaire();
        // Creates and destroys views based on filter id settings
        mQuestionnaire.checkVisibility();

        notifyDataSetChanged();
        mViewPager.setCurrentItem(0);
        setArrows(0);
        setQuestionnaireProgressBar();
        needsIncreasing = false;
    }

    // Simply increases text size of "Start Questionnaire" item in user menu
    public void proposeQuestionnaire() {
        backToMenu();
        needsIncreasing = true;
        if (isInForeGround) {
            mMenuPage.increaseStartTextSize();
        }
    }

    private void hideQuestionnaireProgressBar() {
        View progress = MainActivity.mProgress;
        View regress = MainActivity.mRegress;

        float nProgress = 0;
        float nRegress = 1;

        LinearLayout.LayoutParams progParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                nRegress
        );
        LinearLayout.LayoutParams regParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                nProgress
        );

        progress.setLayoutParams(progParams);
        regress.setLayoutParams(regParams);
    }

    // Set the horizontal Indicator at the Top to follow Page Position
    public void setQuestionnaireProgressBar(int position) {

        View progress = MainActivity.mProgress;
        View regress = MainActivity.mRegress;

        int nAccuracy = 100;
        float nProgress = (float) (position + 1) / mViewPager.getAdapter().getCount() * nAccuracy;
        float nRegress = (nAccuracy - nProgress);

        LinearLayout.LayoutParams progParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                nRegress
        );
        LinearLayout.LayoutParams regParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                nProgress
        );

        progress.setLayoutParams(progParams);
        regress.setLayoutParams(regParams);
    }

    // Set the horizontal Indicator at the Top to follow Page Position
    void setQuestionnaireProgressBar() {

        int nAccuracy = 100;

        View progress = MainActivity.mProgress;
        View regress = MainActivity.mRegress;

        float nProgress = (float) (mViewPager.getCurrentItem() + 1) /
                mViewPager.getAdapter().getCount() * nAccuracy;
        float nRegress = (nAccuracy - nProgress);

        LinearLayout.LayoutParams progParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                nRegress
        );
        LinearLayout.LayoutParams regParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                nProgress
        );

        progress.setLayoutParams(progParams);
        regress.setLayoutParams(regParams);
    }

    // Set the horizontal Indicator at the Top to follow Page Position
    private void setQuestionnaireProgressBar(float fraction) {

        View progress = MainActivity.mProgress;
        View regress = MainActivity.mRegress;

        float nProgress = fraction;
        float nRegress = 1f - nProgress;

        LinearLayout.LayoutParams progParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                nProgress
        );
        LinearLayout.LayoutParams regParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                nRegress
        );

        progress.setLayoutParams(progParams);
        regress.setLayoutParams(regParams);
    }

    // Adjust visibility of navigation symbols to given state
    public void setArrows(int position) {

        if (position == 0) {
            MainActivity.mArrowBack.setVisibility(View.INVISIBLE);
        } else if (MainActivity.mArrowBack.getVisibility() == View.INVISIBLE) {
            MainActivity.mArrowBack.setVisibility(View.VISIBLE);
        }

        if (position == mViewPager.getAdapter().getCount() - 1) {
            MainActivity.mArrowForward.setVisibility(View.INVISIBLE);
        } else if (MainActivity.mArrowForward.getVisibility() == View.INVISIBLE) {
            MainActivity.mArrowForward.setVisibility(View.VISIBLE);
        }
    }

    // Add new page to display
    public int addView(View view, int position, int positionInRaw, boolean mandatory,
                       List<Answer> listOfAnswers) {

        mListOfActiveViews.add(new QuestionViewActive(view, view.getId(), positionInRaw, mandatory,
                listOfAnswers));
        // Sort the Views by their id (implicitly their determined order)
        Collections.sort(mListOfActiveViews);
        return position;
    }

    // Inserts contents in blank menu
    private void createMenuLayout() {

        LinearLayout layout = mMenuPage.generateView();
        mMenuPage.updateCountDownText("");
        mMenuPage.setText("");

        layout.setId(0);
        // Adds the Layout to List carrying all ACTIVE Views
        mListOfActiveViews.add(new QuestionViewActive(layout, layout.getId(),
                0, true, null));
        // Adds the Layout to List storing ALL Views
        mListOfViewsStorage.add(new QuestionViewActive(layout, layout.getId(),
                0, true, null));

        notifyDataSetChanged();
        mViewPager.setCurrentItem(0);
    }

    public void resetMenu() {
        mMenuPage.resetStartTextSize();
    }

    // Sets up visible control elements for menu i.e. status bar
    private void setControlsMenu() {

        MainActivity.mLogo.setText(R.string.menuHelp);
        MainActivity.mArrowForward.setVisibility(View.INVISIBLE);
        MainActivity.mArrowBack.setVisibility(View.INVISIBLE);
        MainActivity.mRevert.setVisibility(View.INVISIBLE);
        MainActivity.mProgress.setBackgroundColor(
                ContextCompat.getColor(mContext, R.color.JadeRed));
        MainActivity.mRegress.setBackgroundColor(
                ContextCompat.getColor(mContext, R.color.JadeGray));
        MainActivity.mLogo.setEnabled(true);
    }

    // Sets up visible control elements for questionnaire i.e. navigation symbols
    private void setControlsQuestionnaire() {

        MainActivity.mLogo.setText("IHAB");
        MainActivity.mArrowForward.setVisibility(View.VISIBLE);
        MainActivity.mArrowBack.setVisibility(View.VISIBLE);
        MainActivity.mRevert.setVisibility(View.VISIBLE);
        MainActivity.mProgress.setBackgroundColor(
                ContextCompat.getColor(mContext, R.color.JadeRed));
        MainActivity.mRegress.setBackgroundColor(
                ContextCompat.getColor(mContext, R.color.JadeGray));
        MainActivity.mLogo.setEnabled(true);
    }

    // Inserts contents into questionnaire and appoints recycler
    private void createQuestionnaireLayout() {
        // Generate a view for each page/question and collect them in ArrayList
        for (int iQuestion = 0; iQuestion < mNUM_PAGES; iQuestion++) {
            // Extracts Question Details from Questionnaire and creates Question
            Question question = mQuestionnaire.createQuestion(iQuestion);
            // Inflates Question Layout based on Question Details
            LinearLayout layout = mQuestionnaire.generateView(question, isImmersive);
            // Sets Layout Id to Question Id
            layout.setId(mQuestionnaire.getId(question));
            // Adds the Layout to List carrying all ACTIVE Views
            mListOfActiveViews.add(new QuestionViewActive(layout, layout.getId(),
                    iQuestion, question.isMandatory(), question.getAnswers()));
            // Adds the Layout to List storing ALL Views
            mListOfViewsStorage.add(new QuestionViewActive(layout, layout.getId(),
                    iQuestion, question.isMandatory(), question.getAnswers()));
        }
    }

    // Set up control elements on top menu
    private void handleControls() {

        MainActivity.mLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //isPrefsInForeGround = true;
                //setPrefsInForeGround(isPrefsInForeGround);
                if (isMenu) {
                    createHelpScreen();
                } else {
                    //createMenu();
                    //startCountDown();
                }
            }
        });
        MainActivity.mArrowBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mViewPager.getCurrentItem() != 0) {
                    mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
                }
            }
        });
        MainActivity.mArrowForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mViewPager.getCurrentItem() < mViewPager.getAdapter().getCount() - 1) {
                    mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
                }
            }
        });
        MainActivity.mRevert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity, R.string.infoTextRevert, Toast.LENGTH_SHORT).show();
                createQuestionnaire();
            }
        });
    }

    // Removes specific view from list and updates viewpager
    int removeView(int position) {

        int nCurrentItem = mViewPager.getCurrentItem();
        mViewPager.setAdapter(null);
        mListOfActiveViews.remove(position);
        mViewPager.setAdapter(this);
        mViewPager.setCurrentItem(nCurrentItem);
        return position;
    }

    // Returns position of element with given id in viewpager
    int getPositionFromId(int iId) {
        for (int iItem = 0; iItem < mListOfActiveViews.size(); iItem++) {
            if (mListOfActiveViews.get(iItem).getId() == iId) {
                return iItem;
            }
        }
        return -1;
    }

    // Takes view out of viewpager and includes it in displayable collection
    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        View view = mListOfActiveViews.get(position).getView();
        collection.addView(view);
        return view;
    }

    // Removes view from displayable collection
    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        collection.removeView((View) view);
    }

    // Returns number of pages in viewpager
    @Override
    public int getCount() {
        if (!(mListOfActiveViews == null) && !(mListOfActiveViews.size() == 0)) {
            mNUM_PAGES = mListOfActiveViews.size();
        } else {
            mNUM_PAGES = 0;
        }
        return mNUM_PAGES;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    // Announce change in contents and invoke rehash
    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    // Returns position of object in displayed list
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

    // Information about current state (menu/questionnaire)
    public boolean isMenu() {
        return isMenu;
    }

    public boolean isInForeGround() { return isInForeGround; }

    // Message service for communication with ControlService
    public void sendMessage(int what) {
        MainActivity.messageService(what);
    }

    public void setPrefsInForeGround(boolean state) {
        isPrefsInForeGround = state;
    }

    /**
     * Lifecycle methods
     **/

    public void onCreate() {
        isCountDownRunning = false;
    }

    public void onStart() {
        isCountDownRunning = false;
    }

    public void onResume() {

        Log.i(LOG, "onResume");

        if(isMenu && isTimer) {
            isCountDownRunning = false;
            if (isQuestionnairePresent) {
                startCountDown();
            }
        } else {
            // This should not be needed but is due to some weird behaviour.
            mCountDownHandler.post(mHideProgressBarRunnable);
        }

        if (isMenu && needsIncreasing) {
            mMenuPage.increaseStartTextSize();
            mMenuPage.updateCountdownText(0);
            setQuestionnaireProgressBar(0f);
        }

        setBatteryLogo();

        isInForeGround = true;
    }

    public void onPause() {
        isCountDownRunning = false;
        stopCountDown();
        isInForeGround = false;
    }

    public void onStop() {
        isCountDownRunning = false;
        //if (isMenu && !isPrefsInForeGround) {
            //sendMessage(ControlService.MSG_QUESTIONNAIRE_INACTIVE);
        //}
        if (!isMenu && !isPrefsInForeGround) {
            sendMessage(ControlService.MSG_QUESTIONNAIRE_ACTIVE);
        }
    }
}