package info.nino.jpatron.helpers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DateTimeFormatUtil
{
    private static Logger logger = Logger.getLogger(DateTimeFormatUtil.class.getName());

    public static final String SQL_DATE_FORMAT = "yyyy-MM-dd";
    public static final String SQL_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String SQL_DATETIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    public static final String EU_DATE_FORMAT = "dd.MM.yyyy";
    public static final String EU_DATETIME_FORMAT = "dd.MM.yyyy HH:mm:ss";
    public static final String EU_DATETIMESTAMP_FORMAT = "dd.MM.yyyy HH:mm:ss.SSS";

    //private static TimeZone tz = TimeZone.getTimeZone("UTC");
    public static final String TIMESTAMP_PATTERN_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";  //2001-07-04T12:08:56.235-07:00
    //public static final String TIMESTAMP_PATTERN_ISO8601_ZONE = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";  //2001-07-04T12:08:56.235-0700

    public static String formatDateTime(String pattern, Date date)
    {
        return DateTimeFormatUtil.formatDateTime(pattern, date, null);
    }

    public static String formatDateTime(String pattern, Date date, TimeZone tz)
    {
        String dateString = null;

        if(date != null)
        {
            try
            {
                SimpleDateFormat df = new SimpleDateFormat(pattern);
                if(tz != null) df.setTimeZone(tz);

                dateString = df.format(date);
            }
            catch(Exception e)
            {
                logger.log(Level.WARNING, String.format("Error formatting Date '%s' value with format pattern '%s'!", String.valueOf(date), pattern));
                //e.printStackTrace();
            }
        }

        return dateString;
    }

    public static Date parseDateTime(String pattern, String dateString)
    {
        return DateTimeFormatUtil.parseDateTime(pattern, dateString, null);
    }

    public static Date parseDateTime(String pattern, String dateString, TimeZone tz)
    {
        Date date = null;

        if(dateString != null)
        {
            try
            {
                SimpleDateFormat df = new SimpleDateFormat(pattern);
                if(tz != null) df.setTimeZone(tz);

                date = df.parse(dateString);
            }
            catch(Exception e)
            {
                logger.log(Level.WARNING, String.format("Error parsing Date '%s' value with format pattern '%s'!", dateString, pattern));
                //e.printStackTrace();
            }
        }

        return date;
    }

    public static String formatDateTimeISO8601(Date datetime)
    {
        return DateTimeFormatUtil.formatDateTime(TIMESTAMP_PATTERN_ISO8601, datetime);
    }

    public static String formatDateTimeISO8601WithUTC(Date datetime)
    {
        return DateTimeFormatUtil.formatDateTime(TIMESTAMP_PATTERN_ISO8601, datetime, TimeZone.getTimeZone("UTC"));
    }

    public static Date parseDateTimeISO8601(String dateString)
    {
        return DateTimeFormatUtil.parseDateTime(TIMESTAMP_PATTERN_ISO8601, dateString);
    }
}
