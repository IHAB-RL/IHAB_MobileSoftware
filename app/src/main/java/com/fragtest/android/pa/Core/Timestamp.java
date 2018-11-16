package com.fragtest.android.pa.Core;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Returns a timestamp as string
 */
public class Timestamp {

    public static String getTimestamp(int type ) {

        SimpleDateFormat timestamp = null;

        switch ( type ) {

            case 1:
                timestamp = new SimpleDateFormat("yyyyMMdd", Locale.US);
                break;
            case 2:
                timestamp = new SimpleDateFormat("HHmmssSSS", Locale.US);
                break;
            case 3:
                timestamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US);
                break;
            case 4:
                timestamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS", Locale.US);
                break;
        }

        return timestamp.format(new Date());

    }

}
