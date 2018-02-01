package com.fragtest.android.pa.Menu;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fragtest.android.pa.ControlService;
import com.fragtest.android.pa.Questionnaire.QuestionnairePagerAdapter;
import com.fragtest.android.pa.R;

/**
 * Created by ul1021 on 30.06.2017.
 */

public class MenuPage extends AppCompatActivity {

    private final static String LOG = "MenuPage";
    private String mCountDownString;
    private Context mContext;
    private QuestionnairePagerAdapter mContextQPA;
    private String StartText;
    private TextView mCountDownRemaining, mStartQuestionnaire;
    private String[] mTempTextCountDownRemaining;

    public MenuPage(Context context, QuestionnairePagerAdapter contextQPA) {

        mContext = context;
        mContextQPA = contextQPA;
        StartText = "";
        mCountDownString = mContext.getResources().getString(R.string.timeRemaining);
        mTempTextCountDownRemaining = mCountDownString.split("%");

    }

    public LinearLayout generateView() {

        // Parent Layout of the menu
        LinearLayout menuLayout = new LinearLayout(mContext);
        menuLayout.setBackgroundColor(ContextCompat.getColor(mContext,R.color.BackgroundColor));
        menuLayout.setOrientation(LinearLayout.VERTICAL);
        //menuLayout.setBackgroundColor(Color.GREEN);

        // Top View carrying countdown
        mCountDownRemaining = new TextView(mContext);
        LinearLayout.LayoutParams tempTopParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 0.3f);
        mCountDownRemaining.setText(mCountDownString);
        mCountDownRemaining.setGravity(View.TEXT_ALIGNMENT_CENTER);
        mCountDownRemaining.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        mCountDownRemaining.setTextColor(ContextCompat.getColor(mContext,R.color.TextColor_Light));
        mCountDownRemaining.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);

        // Layout patch carrying "Take Survey" Text/Button
        LinearLayout centerLayout = new LinearLayout(mContext);
        centerLayout.setBackgroundColor(ContextCompat.getColor(mContext,R.color.BackgroundColor));
        LinearLayout.LayoutParams centerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 0.7f
        );
        centerLayout.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);

        // The actual Text/Button "Take Survey"
        mStartQuestionnaire = new TextView(mContext);
        mStartQuestionnaire.setText("");
        mStartQuestionnaire.setTextSize(mContext.getResources().getDimension(R.dimen.textSizeAnswer));
        mStartQuestionnaire.setTypeface(Typeface.DEFAULT);
        //mStartQuestionnaire.setMaxWidth(1000);
        mStartQuestionnaire.setTextColor(ContextCompat.getColor(mContext, R.color.JadeRed));
        mStartQuestionnaire.setBackgroundColor(Color.WHITE);
        mStartQuestionnaire.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mContextQPA.sendMessage(ControlService.MSG_MANUAL_QUESTIONNAIRE);
            }
        });

        // Bottom View (blank)
        View tempViewBottom = new View(mContext);

        centerLayout.addView(mStartQuestionnaire);
        menuLayout.addView(mCountDownRemaining, tempTopParams);
        menuLayout.addView(centerLayout, centerParams);
        menuLayout.addView(tempViewBottom, tempTopParams);

        return menuLayout;
    }
    // Simply increases text size of "Start Questionnaire" item in user menu
    public void increaseStartTextSize() {

        mStartQuestionnaire.setText(formatString(StartText));
        mStartQuestionnaire.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        mStartQuestionnaire.setTextSize(mContext.getResources().
                getDimension(R.dimen.textSizeProposed));
        mStartQuestionnaire.setTypeface(null, Typeface.BOLD);
        mStartQuestionnaire.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mContextQPA.sendMessage(ControlService.MSG_PROPOSITION_ACCEPTED);
            }
        });
    }

    public void setText(String text) {

        StartText = text;
        mStartQuestionnaire.setText(cleanUpString(text));
        mStartQuestionnaire.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
    }

    public void setText() {
        mStartQuestionnaire.setText(mCountDownString);
    }

    public void resetStartTextSize() {
        mStartQuestionnaire.setText(cleanUpString(StartText));
        mStartQuestionnaire.setTextSize(mContext.getResources().
                getDimension(R.dimen.textSizeAnswer));
        mStartQuestionnaire.setTypeface(Typeface.DEFAULT);
    }

    // Handles update of visible text countdown
    public void updateCountdownText(int seconds) {
        int minutesRemaining = seconds / 60;
        int secondsRemaining = seconds - minutesRemaining * 60;
        mCountDownRemaining.setText("" + mTempTextCountDownRemaining[0] + minutesRemaining +
                mTempTextCountDownRemaining[1] + secondsRemaining + mTempTextCountDownRemaining[2]);
    }

    private String cleanUpString(String inString) {
        return inString.replace("-","");
    }

    private String formatString(String inString) {
        return inString.replace(" ","\n").replace("-","-\n");
    }

    public void updateCountDownText(String text) {
        mCountDownRemaining.setText(text);
    }
}