package calculator;

import core.GConfigs;
import core.GConfigs.MODEL_TYPES;
import core.GConfigs.RAW_DATA_INDEX;
import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;

public class Extreme {

  public static Object getExtreme(String date, ConcurrentHashMap<String, Object[]> rawDataMap, 
          int distance, int duration, boolean getHighest) {
    Calendar start_date = StatCalculator.getUsableDate(date, rawDataMap, distance, duration, true, true);
    Calendar end_date = StatCalculator.getUsableDate(date, rawDataMap, distance, duration, false, true);
    if (start_date == null || end_date == null) {
      return null;
    }
    //

    float max = Float.NEGATIVE_INFINITY;
    float min = Float.POSITIVE_INFINITY;
    start_date.add(Calendar.DAY_OF_MONTH, 1);
    while (!start_date.after(end_date)) {
      Object[] row = rawDataMap.get(GConfigs.cldToString(start_date));
      if (row != null) {
        float templow = (float) row[GConfigs.RAW_DATA_INDEX.CLOSE.ordinal()];
        float temphigh = (float) row[GConfigs.RAW_DATA_INDEX.CLOSE.ordinal()];
        if (max < temphigh) {
          max = temphigh;
        }
        if (min > templow) {
          min = templow;
        }
      }
      start_date.add(Calendar.DAY_OF_MONTH, 1);
    }
    if (getHighest) {
      return max;
    } else {
      return min;
    }
  }

  public static Object getExtremeRatio(String date,
          ConcurrentHashMap<String, Object[]> rawDataMap, int distance, int duration, boolean getHighest) {
    Calendar start_date = StatCalculator.getUsableDate(date, rawDataMap, distance, duration, true, true);
    if(start_date==null)
      return null;
    
    float start_price;
    if (getHighest) {
      start_price = (float) rawDataMap.get(GConfigs.cldToString(start_date))[RAW_DATA_INDEX.HIGH.ordinal()];
    } else {
      start_price = (float) rawDataMap.get(GConfigs.cldToString(start_date))[RAW_DATA_INDEX.LOW.ordinal()];
    }
    Object o = getExtreme(date, rawDataMap, distance, duration, getHighest);
    if(o==null)
      return null;
    return ((float)o - start_price) / start_price;
  }

  public static String getNominalExtreme(MODEL_TYPES type, String date, 
          ConcurrentHashMap<String, Object[]> rawDataMap, int distance, int duration, boolean getHighest) {
    Object ext = Extreme.getExtremeRatio(date, rawDataMap, distance, duration, getHighest);
    if (ext == null) {
      return null;
    }
    return getHighLowClass(type, (float) ext);
  }

  public static String getHighLowClass(MODEL_TYPES type, float v) {
    float sig = GConfigs.getSignificanceNormal(type);

    
    if (Math.abs(v) < sig) {
      // (-s...s)
      return "Stay";
    } else if (v >= 2 * sig && v < 3 * sig) {
      // [2s...3s)
      return "High";
    } else if (v <= -2 * sig && v > -3 * sig) {
      // (-3s...-2s]
      return "Low";
    } else if (v >= sig && v < 2 * sig) {
      // [s...2s)
      return "Little_High";
    } else if (v <= -1 * sig && v > -2 * sig) {
      // (-2s...-s]
      return "Little_Low";
    } else if (v >= 3 * sig) {
      // [3s...inf)
      return "Very_High";
    } else if (v <= -3 * sig) {
      // (-inf...-3s]
      return "Very_Low";
    } else {
      // return null if extreme == 0, coz it usually should not happen
      return null;
    }
  }

}
