package com.example.fintracker.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Date Utilities - Helper methods for date operations
 */
public class DateUtils {

    /**
     * Get first day of this month at 00:00:00
     */
    public static long getThisMonthStartTimestamp() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Get first day of last month at 00:00:00
     */
    public static long getLastMonthStartTimestamp() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Add months to a timestamp
     */
    public static long addMonths(long timestamp, int months) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.add(Calendar.MONTH, months);
        return cal.getTimeInMillis();
    }

    /**
     * Get start of week (Monday) at 00:00:00
     */
    public static long getThisWeekStartTimestamp() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Get start of next week (Monday) at 00:00:00
     */
    public static long getNextWeekStartTimestamp() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.WEEK_OF_YEAR, 1);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Get start of year at 00:00:00
     */
    public static long getThisYearStartTimestamp() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_YEAR, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Get month name from timestamp
     */
    public static String getMonthName(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    /**
     * Format date range for display
     */
    public static String formatDateRange(long startDate, long endDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
        return sdf.format(new Date(startDate)) + " - " + sdf.format(new Date(endDate));
    }

    /**
     * Check if budget month is valid (This Month or Last Month only)
     */
    public static boolean isValidBudgetMonth(long monthTimestamp) {
        long thisMonth = getThisMonthStartTimestamp();
        long lastMonth = getLastMonthStartTimestamp();
        return monthTimestamp == thisMonth || monthTimestamp == lastMonth;
    }
}