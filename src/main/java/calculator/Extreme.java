package calculator;

import core.GConfigs;
import core.GConfigs.MODEL_TYPES;
import core.GConfigs.YAHOO_DATA_INDEX;
import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import util.MyUtils;

public class Extreme {

  public static Object getExtreme(String date, ConcurrentHashMap<String, Object[]> rawDataMap,
          int distance, int duration, boolean getHighest) {
    LocalDate start_date = MyUtils.getUsableDate(date, rawDataMap, distance, duration, true);
    LocalDate end_date = MyUtils.getUsableDate(date, rawDataMap, distance, duration, false);
    if (start_date == null || end_date == null) {
      return null;
    }

    double max = Double.NEGATIVE_INFINITY;
    double min = Double.POSITIVE_INFINITY;
    start_date = start_date.plusDays(1);
    while (start_date.isBefore(end_date)) {
      Object[] row = rawDataMap.get(start_date.toString());
      if (row != null) {
        double close = (double) row[GConfigs.YAHOO_DATA_INDEX.CLOSE.ordinal()];
        if (max < close) {
          max = close;
        }
        if (min > close) {
          min = close;
        }
      }
      start_date = start_date.plusDays(1);
    }
    if (getHighest) {
      return max;
    } else {
      return min;
    }
  }

  public static Object getExtremeRatio(String date,
          ConcurrentHashMap<String, Object[]> rawDataMap, int distance, int duration, boolean getHighest) {
    LocalDate start_date = MyUtils.getUsableDate(date, rawDataMap, distance, duration, true);
    if (start_date == null) {
      return null;
    }

    double start_price = (double) rawDataMap.get(start_date.toString())[YAHOO_DATA_INDEX.CLOSE.ordinal()];
    Object ext = getExtreme(date, rawDataMap, distance, duration, getHighest);
    if (ext == null) {
      return null;
    }
    return ((double) ext - start_price) / start_price;
  }

  // Was use nominal, but now shift to a semi-nominal numeric representation.
  public static Object getNominalExtreme(MODEL_TYPES type, String date,
          ConcurrentHashMap<String, Object[]> rawDataMap, int distance, int duration, boolean getHighest) {
    Object ext = Extreme.getExtremeRatio(date, rawDataMap, distance, duration, getHighest);
    if (ext == null) {
      return null;
    }
    return getHighLowClass(type, (double) ext);
  }

  public static double getHighLowClass(MODEL_TYPES type, double v) {
    double sig = GConfigs.getSignificanceNormal(type.name(),5);
    //positive or negative
    boolean sign = v >= 0;
    int i = (int) Math.ceil(Math.abs(v) / sig) - 1;
    
    //i=Math.round(i);
    if (sign) {
      return i*sig;
    } else {
      return -1 * i*sig;
    }
  }
}
