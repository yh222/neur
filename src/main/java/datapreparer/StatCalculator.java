package datapreparer;

import static core.GlobalConfigs.DATE_FORMAT;
import java.text.ParseException;
import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StatCalculator {

    /*
     * Velocity = volume of the date / Average volume
     */
    public static Object CalcluateVelocity(String date, ConcurrentHashMap<String, Object[]> rawDataMap, int distance) {
        //Duration should be always zero, as non-directly past velocity seems not revelant.
        Calendar start_date = getUsableDate(date, rawDataMap, distance, 0, true);
        Calendar end_date = getUsableDate(date, rawDataMap, 0, 0, false);
        if (start_date == null || end_date == null) {
            return null; //remain 0
        }
        //System.out.println("start: " + DATE_FORMAT.format(start_date.getTime()));
        //System.out.println("end: " + DATE_FORMAT.format(end_date.getTime()));
        int count = 0;
        double sum_volume = 0;
        //Calculate volume sum
        for (int i = 0; i < distance; i++) {
            if (rawDataMap.get(DATE_FORMAT.format(start_date.getTime())) != null) {
                count++;
                sum_volume += (double) rawDataMap.get(DATE_FORMAT.format(start_date.getTime()))[4];
                //System.out.println(DATE_FORMAT.format(start_date.getTime()));
            } else {
                //System.out.println("cannot find: " + DATE_FORMAT.format(start_date.getTime()));
            }
            start_date.add(Calendar.DATE, 1);
        }
        double current_volume = (double) rawDataMap.get(DATE_FORMAT.format(end_date.getTime()))[4];
        return current_volume / (sum_volume / count);
    }

    public static Object CalculateRawTrend(String date, ConcurrentHashMap<String, Object[]> rawDataMap, int distance, int duration) {
        Calendar start_date = getUsableDate(date, rawDataMap, distance, duration, true);
        Calendar end_date = getUsableDate(date, rawDataMap, distance, duration, false);
        if (start_date == null || end_date == null) {
            return null; 
        }

        double trend_start = (double) rawDataMap.get(DATE_FORMAT.format(start_date.getTime()))[0];
        double trend_end = (double) rawDataMap.get(DATE_FORMAT.format(end_date.getTime()))[3];
        return (trend_end - trend_start) / trend_start;
    }

    /*
     * returns Spring, Summer, Autunm or Winter
     */
    public static String CalculateSeasonOfYear(String date) {
        try {
            Calendar tempdate = Calendar.getInstance();
            tempdate.setTime(DATE_FORMAT.parse(date));
            int month = tempdate.get(Calendar.MONTH);
            if (month <= 2) {
                return "Spring";
            } else if (month > 2 && month <= 5) {
                return "Summer";
            } else if (month > 5 && month <= 8) {
                return "Autumn";
            } else {
                return "Winter";
            }
        } catch (ParseException ex) {
            Logger.getLogger(StatCalculator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /*
    * get a date 
    */
    private static Calendar getUsableDate(String date, ConcurrentHashMap<String, Object[]> rawDataMap, int distance, int duration, boolean isStart) {
        try {
            Calendar tempdate = Calendar.getInstance();
            tempdate.setTime(DATE_FORMAT.parse(date));

            int direction;
            if (isStart) {
                direction = 1;
            } else {
                tempdate.add(Calendar.DAY_OF_MONTH, duration);
                direction = -1;
            }

            //Sundays and Saturdays will be ommited
            tempdate.add(Calendar.DAY_OF_MONTH, distance * -1);
            int count = 6;
            while (rawDataMap.get(DATE_FORMAT.format(tempdate.getTime())) == null) {
                tempdate.add(Calendar.DAY_OF_MONTH, direction);
                if (--count < 0) {
                    //System.out.println("Cannot find data on date:" + tempdate.getTime() + " for code: " + currentCode + "\n Initial date string: " + date + ", direction:" + direction);
                    return null;
                }
            }
            return tempdate;
        } catch (ParseException ex) {
            Logger.getLogger(StatCalculator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static String CalcluateSituation(String date, ConcurrentHashMap<String, Object[]> rawDataMap, int distance, int duration, double significance) {
        Object rt = CalculateRawTrend(date, rawDataMap, distance, duration);
        if (rt == null) {
            return null;
        }
        double raw_trend = (double) rt;

        if (Math.abs(raw_trend) < significance) {
            return "Stay";
        } else if (raw_trend > 0) {
            return "Rise";
        } else if (raw_trend < 0) {
            return "Down";
        } else {// return null if trend == 0, coz it usually should not happen
            return null;
        }
    }
}
