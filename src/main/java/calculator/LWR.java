package calculator;

import core.GConfigs.YAHOO_DATA_INDEX;
import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import util.MyUtils;

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

    float close = (float) raw_data[YAHOO_DATA_INDEX.CLOSE.ordinal()];
    double lwr = ((float) highest - close) / ((float) highest - (float) lowest) * -100;

    return lwr;
  }

  public static Object getLWR1(String date, ConcurrentHashMap<String, Object[]> rawDataMap, int maDuration, int duration, int distance) {

    SMovingAverage ma = new SMovingAverage(maDuration);
    // Multipy maDuration to get better ma result
    LocalDate start_date = MyUtils.getUsableDate(date, rawDataMap, distance, maDuration, true, true);
    LocalDate end_date = MyUtils.getUsableDate(date, rawDataMap, distance, maDuration, false, true);
    if (start_date == null || end_date == null) {
      return null;
    }

    while (start_date.isBefore(end_date)) {
      Object[] row = rawDataMap.get(start_date.toString());
      if (row != null) {
        Object d = getLWR(start_date.toString(), rawDataMap, duration);
        if (d != null) {
          ma.newNum((double) d);
        }
      }
      start_date=start_date.plusDays(1);
    }
    return (float) ma.getAvg();
  }

  public static Object getLWR2(String date,
          ConcurrentHashMap<String, Object[]> rawDataMap, int duration) {
    return null;
  }

}
