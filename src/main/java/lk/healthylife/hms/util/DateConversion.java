package lk.healthylife.hms.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Logger;

public final class DateConversion {

    public static final String STANDARD_DATE_FORMAT_WITH_TIME = "yyyy-MM-dd HH:mm:ss";
    public static final String STANDARD_DATE_FORMAT = "yyyy-MM-dd";
    public static final String STANDARD_TIME = "HH:mm:ss";

    private DateConversion() {
        throw new IllegalStateException("Date Conversion Utility class");
    }

    /** Convert the Date to a  given Date Format */
    public static Date convertDate(Date date, String format) {
        DateFormat df = new SimpleDateFormat(format, Locale.ENGLISH);
        Date convertedDate = null;
        if(date != null){
            try {
                String formattedDate = df.format(date);
                convertedDate = df.parse(formattedDate);
                return convertedDate;
            } catch (ParseException e) {
                Logger.getLogger("Exception - Date Format : ", e.toString());
                return null;
            }
        }
        return convertedDate;
    }

    public static Date convertStringToDate(String dateToBeConverted, String pattern) {
        Date date = null;
        if (dateToBeConverted != null) {
            try {
                date = new SimpleDateFormat(pattern, Locale.ENGLISH).parse(dateToBeConverted);
            } catch (Exception e) {
                Logger.getLogger("Exception - Convert String to Date : ", e.toString());
            }
        }
        return date;
    }

    public static Integer convertDateToInteger(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        int integerDate = Integer.parseInt(sdf.format(date));
        return integerDate;
    }

    public static Date convertStringToDate(String dateToBeConverted) {
        Date date = null;
        if (dateToBeConverted != null) {
            try {
                date = new SimpleDateFormat(STANDARD_DATE_FORMAT, Locale.ENGLISH).parse(dateToBeConverted);
            } catch (Exception e) {
                Logger.getLogger("Exception - Convert String to Date : ", e.toString());
            }
        }
        return date;
    }

    public static Date convertStringToDateTime(String dateToBeConverted) {
        Date date = null;
        if (dateToBeConverted != null) {
            try {
                date = new SimpleDateFormat(STANDARD_DATE_FORMAT_WITH_TIME, Locale.ENGLISH).parse(dateToBeConverted);
            } catch (Exception e) {
                Logger.getLogger("Exception - Convert String to Date : ", e.toString());
            }
        }
        return date;
    }

    public static String convertDateToString(Date date) {

        String formattedDate;

        if (date == null) {
            return null;
        } else {
            try {
                formattedDate = new SimpleDateFormat(STANDARD_DATE_FORMAT, Locale.ENGLISH).format(date);
            } catch (Exception e) {
                Logger.getLogger("Exception - Convert Date to String : ", e.toString());
                return null;
            }
        }
        return formattedDate;
    }

    public static String convertDateToStringWithTime(Date date) {

        String formattedDate;

        if (date == null) {
            return null;
        } else {
            try {
                formattedDate = new SimpleDateFormat(STANDARD_DATE_FORMAT_WITH_TIME, Locale.ENGLISH).format(date);
            } catch (Exception e) {
                Logger.getLogger("Exception - Convert Date to String : ", e.toString());
                return null;
            }
        }
        return formattedDate;
    }

    public static LocalDateTime convertStringToLocalDateTime(String dateToConvert) {
        if (dateToConvert == null)
            return null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(STANDARD_DATE_FORMAT_WITH_TIME);
        return LocalDateTime.parse(dateToConvert, formatter);
    }

    public static Date convertLocalDateToDate(LocalDate dateToConvert) {
        if (dateToConvert == null)
            return null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(STANDARD_DATE_FORMAT);
        return convertStringToDate(dateToConvert.format(formatter));
    }

    public static Date convertLocalDateTimeToDate(LocalDateTime dateToConvert) {
        if (dateToConvert == null)
            return null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(STANDARD_DATE_FORMAT_WITH_TIME);
        return convertStringToDateTime(dateToConvert.format(formatter));
    }

    public static String convertLocalDateTimeToString(LocalDateTime dateToConvert) {
        if (dateToConvert == null)
            return null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(STANDARD_DATE_FORMAT_WITH_TIME);
        return dateToConvert.format(formatter);
    }

    public static String convertLocalDateToString(LocalDate dateToConvert) {
        if (dateToConvert == null)
            return null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(STANDARD_DATE_FORMAT);
        return dateToConvert.format(formatter);
    }

    public static String convertLocalTimeToString(LocalTime dateToConvert) {
        if (dateToConvert == null)
            return null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(STANDARD_TIME);
        return dateToConvert.format(formatter);
    }

    public static boolean isDateAfter(String dateString, String dateFormat) throws ParseException {
        Date date = new SimpleDateFormat(dateFormat).parse(dateString);
        return new Date().after(date);
    }

    public static boolean isDateAfter(String fromDate, String toDate, String dateFormat) throws ParseException {
        Date date = new SimpleDateFormat(dateFormat).parse(fromDate);
        return new SimpleDateFormat(dateFormat).parse(toDate).after(date);
    }
}
