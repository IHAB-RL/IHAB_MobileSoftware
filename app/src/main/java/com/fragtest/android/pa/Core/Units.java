package com.fragtest.android.pa.Core;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;

import com.fragtest.android.pa.R;

/**
 * Created by ulrikkowalk on 01.03.17.
 */

public class Units extends AppCompatActivity {

    private static int SCREEN_SIZE_HEIGHT;
    private static int SCREEN_SIZE_WIDTH;
    private Resources mResources;
    private DisplayMetrics mMetrics;
    private Context mContext;

    public Units(Context context){
        mContext = context;
        mResources = context.getResources();
        mMetrics = mResources.getDisplayMetrics();
        SCREEN_SIZE_WIDTH = mMetrics.widthPixels;
        SCREEN_SIZE_HEIGHT = mMetrics.heightPixels;
    }

    public static int getScreenHeight() {
        return SCREEN_SIZE_HEIGHT;
    }

    public static int getScreenWidth() { return SCREEN_SIZE_WIDTH; }

    public int getUsableSliderHeight() {
        return getScreenHeight() -
                (int) mContext.getResources().getDimension(R.dimen.questionTextHeight) -
                convertDpToPixels(32) -
                convertDpToPixels(6) -
                (int) mContext.getResources().getDimension(R.dimen.answerLayoutPadding_Top) -
                (int) mContext.getResources().getDimension(R.dimen.answerLayoutPadding_Bottom) -
                getStatusBarHeight() -
                (int) mContext.getResources().getDimension(R.dimen.answerLayoutPadding_Top);
    }

    public int getStatusBarHeight() {
        return (int) (24 * mMetrics.density);
    }

    public int convertDpToPixels(float dp){
        return (int) (dp * ((float) mMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public int convertPixelsToDp(float px){
        return (int) (px / ((float) mMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public int convertPixelsToSp(int px) {
        return (int) (px / mMetrics.scaledDensity);
    }

    public int convertSpToPixels(int sp) {
        return (int) (sp * mMetrics.scaledDensity);
    }

}
