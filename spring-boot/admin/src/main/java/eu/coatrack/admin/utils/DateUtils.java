package eu.coatrack.admin.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    private static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    public static Date parseDateStringOrGetTodayIfNull(String dateString) {
        Date date = getToday();
        if (dateString != null) {
            try {
                date = df.parse(dateString);
            } catch (ParseException ex) {
                ex.printStackTrace();
            }
        }
        return date;
    }

    public static Date getToday() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        return today.getTime();
    }

    public static String getTodayAsString() {
        Calendar today = Calendar.getInstance();
        int month = today.get(Calendar.MONTH)+1;
        String zeroPrefix = month < 10 ? "0" : "";
        String monthString = String.format("%s%d", zeroPrefix, month);
        return String.format("%d-%s-%d",
                today.get(Calendar.YEAR), monthString, today.get(Calendar.DAY_OF_MONTH));
    }

    public static String getTodayMinusOneMonthAsString() {
        Calendar today = Calendar.getInstance();
        int month = today.get(Calendar.MONTH);
        String zeroPrefix = month < 10 ? "0" : "";
        String monthString = String.format("%s%d", zeroPrefix, month);
        return String.format("%d-%s-%d",
                today.get(Calendar.YEAR), monthString, today.get(Calendar.DAY_OF_MONTH));
    }

    public static Date getDateFromString(String dateString) {
        Date date;
        try {
            date = df.parse(dateString);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return date;
    }


    public static void setFormat(String format) {
        df = new SimpleDateFormat(format);
    }

}
