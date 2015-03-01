/* TODO: license */
package org.github.gentlewake.util;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author lorenz.fischer@gmail.com
 */
public class AlarmUtils {

    private static final String TAG = "GentleWake.AlarmUtils";

    /**
     * This method reads the time at which the next alarm is configured to be go off on the
     * device and returns the date for it.
     *
     * @param context the context to read the alarm from.
     * @return the next alarm that is configured on the phone or <code>null</code> if no configured
     * alarm could be retrieved.
     */
    public static Date getNextAlarm(Context context) {
        Date result;
        String nextAlarm;

        result = null;
        nextAlarm = Settings.System.getString(context.getContentResolver(),
                Settings.System.NEXT_ALARM_FORMATTED);

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Next alarm is set to '" + nextAlarm + "'");
        }

         /*
         * The code for this has been copied and adapted from
         * http://stackoverflow.com/questions/8133788/time-to-the-next-alarm-in-seconds-or-milliseconds
         */
        if (nextAlarm != null && nextAlarm.length() > 0) {
            DateFormat sdf;
            Date alarmDate;

            alarmDate = null;
            if (nextAlarm.toLowerCase().contains("am") || nextAlarm.toLowerCase().contains("pm")) {
                sdf = new SimpleDateFormat("EEE hh:mm aa"); // 12 hour with am/pm // test this one
            } else {
                sdf = new SimpleDateFormat("EEE HH:mm"); // 24 hour
            }

            try {
                alarmDate = sdf.parse(nextAlarm);
            } catch (ParseException e) {
                if (Log.isLoggable(TAG, Log.WARN)) {
                    Log.w(TAG, "Couldn't parse the alarmDate string '" + nextAlarm + "'");
                }
            }

            if (alarmDate != null) {
                Calendar alarm;
                int alarmDayOfWeek;
                Calendar now;
                int nowDayOfWeek;
                int daysDiff = 0;

                alarm = Calendar.getInstance();
                alarm.setTimeInMillis(alarmDate.getTime());
                alarmDayOfWeek = alarm.get(Calendar.DAY_OF_WEEK);
                now = Calendar.getInstance();
                nowDayOfWeek = now.get(Calendar.DAY_OF_WEEK);

                daysDiff = alarmDayOfWeek - nowDayOfWeek;
                if (daysDiff < 0) {
                    daysDiff += 7;
                }

                // year and month are the same as "now", only the day needs to be set
                alarm.set(Calendar.YEAR, now.get(Calendar.YEAR));
                alarm.set(Calendar.MONTH, now.get(Calendar.MONTH));
                alarm.set(Calendar.DATE, now.get(Calendar.DATE));
                alarm.add(Calendar.DATE, daysDiff);

                result = alarm.getTime();

                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Parsed date is " + result);
                }
            }
        }

        return result;
    }

}
