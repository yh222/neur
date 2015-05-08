package calculator;

import static calculator.StatCalculator.getUsableDate;
import core.GConfigs;
import core.GConfigs.RAW_DATA_INDEX;
import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;

public class LWR {

  public static Object getLWR(String date,
          ConcurrentHashMap<String, Object[]> rawDataMap, int duration) {
    Object[] raw_data = rawDataMap.get(date);
    Object highest = Extreme.getExtreme(
            date, rawDataMap, duration, duration, true);
    Object lowest = Extreme.getExtreme(
            date, rawDataMap, duration, duration, false);
    if (highest == null || lowest == null) {
      return null;
    }

    float close = (float) raw_data[RAW_DATA_INDEX.CLOSE.ordinal()];
    double lwr = ((float) highest - close) / ((float) highest - (float) lowest) * -100;

    return lwr;
  }

  public static Object getLWR1(String date, ConcurrentHashMap<String, Object[]> rawDataMap
          , int maDuration, int duration,int distance) {

    SMovingAverage ma = new SMovingAverage(maDuration);
    // Multipy maDuration to get better ma result
    Calendar start_date = getUsableDate(date, rawDataMap, distance, maDuration, true, true);
    Calendar end_date = getUsableDate(date, rawDataMap, distance, maDuration, false, true);
    if (start_date == null || end_date == null) {
      return null;
    }

    while (!start_date.after(end_date)) {
      Object[] row = rawDataMap.get(GConfigs.getDateFormat().format(start_date.getTime()));
      if (row != null) {
        Object d = getLWR(GConfigs.getDateFormat().format(
                start_date.getTime()), rawDataMap, duration);
        if (d != null) {
          ma.newNum((double) d);
        }
      }
      start_date.add(Calendar.DAY_OF_MONTH, 1);
    }
    return (float)ma.getAvg();
  }

  public static Object getLWR2(String date,
          ConcurrentHashMap<String, Object[]> rawDataMap, int duration) {
    return null;
  }

}
