package calculator;

import static core.GConfigs.DEFAULT_START_DATE;
import core.GConfigs.MODEL_TYPES;
import core.GConfigs.YAHOO_DATA_INDEX;
import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import util.MyUtils;
import static util.MyUtils.parseToISO;

public class StatCalculator {

  /*
   * Velocity = volume of the date / Average volume
   */
  public static Object getVelocity(String date, ConcurrentHashMap<String, Object[]> rawDataMap, int distance) {
    //Duration should be always zero, as non-directly past velocity seems not revelant.
    LocalDate start_date = MyUtils.getUsableDate(date, rawDataMap, distance, 0, true);
    LocalDate end_date = MyUtils.getUsableDate(date, rawDataMap, 0, 0, false);
    if (start_date == null || end_date == null) {
      return null; //remain 0
    }
    int count = 0;
    double sum_volume = 0;
    //Calculate volume sum
    while (start_date.isBefore(end_date)) {
      if (rawDataMap.get(start_date.toString()) != null) {
        count++;
        sum_volume += (double) rawDataMap.get(start_date.toString())[YAHOO_DATA_INDEX.VOLUME.ordinal()];
      }
      start_date = start_date.plusDays(1);
    }
    if (rawDataMap.get(start_date.toString()) == null) {
      return null;
    }
    double current_volume = (double) rawDataMap.get(start_date.toString())[YAHOO_DATA_INDEX.VOLUME.ordinal()];
    return current_volume / (sum_volume / count);
  }

  public static Object getMomentum(String date,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          int distance, int duration) {
    LocalDate start_date = MyUtils.getUsableDate(date, rawDataMap, distance, duration, true);
    LocalDate end_date = MyUtils.getUsableDate(date, rawDataMap, distance, duration, false);
    if (start_date == null || end_date == null) {
      return null;
    }

    double trend_start = (double) rawDataMap.get(start_date.toString())[YAHOO_DATA_INDEX.OPEN.ordinal()];
    double trend_end = (double) rawDataMap.get(end_date.toString())[YAHOO_DATA_INDEX.CLOSE.ordinal()];
    return (trend_end - trend_start) / trend_start;
  }

  public static Object getClusteredTrend(String date, ConcurrentHashMap<String, Object[]> rawDataMap, int distance, int duration, boolean getHighest) {
    LocalDate start_date = MyUtils.getUsableDate(date, rawDataMap, distance, duration, true);
    LocalDate end_date = MyUtils.getUsableDate(date, rawDataMap, distance, duration, false);
    if (start_date == null || end_date == null) {
      return null;
    }
    double start_price = (double) rawDataMap.get(start_date.toString())[YAHOO_DATA_INDEX.CLOSE.ordinal()];

    double max = Double.NEGATIVE_INFINITY;
    double min = Double.POSITIVE_INFINITY;
    //Set end date to monday of the week
    int dayofWeek = end_date.getDayOfWeek().getValue();
    end_date = end_date.plusDays(-1 * (dayofWeek - DayOfWeek.MONDAY.getValue()));
    for (int i = 0; i < 5; i++) {
      Object[] row = rawDataMap.get(end_date.toString());
      if (row != null) {
        double temp = (double) row[YAHOO_DATA_INDEX.CLOSE.ordinal()];
        if (max < temp) {
          max = temp;
        }
        if (min > temp) {
          min = temp;
        }
      }
      end_date = end_date.plusDays(1);
    }
    if (getHighest) {
      return (max - start_price) / start_price;
    } else {
      return (min - start_price) / start_price;
    }
  }

  /*
   * returns Spring, Summer, Autunm or Winter
   */
  public static String getSeasonOfYear(String date) {
    LocalDate tempdate = parseToISO(date);
    int month = tempdate.getMonthValue();
    if (month <= 2) {
      return "Spring";
    } else if (month > 2 && month <= 5) {
      return "Summer";
    } else if (month > 5 && month <= 8) {
      return "Autumn";
    } else {
      return "Winter";
    }
  }

  public static Object getNominalRawTrend(MODEL_TYPES type, String date, ConcurrentHashMap<String, Object[]> rawDataMap, int distance, int duration) {
    Object rt = getMomentum(date, rawDataMap, distance, duration);
    if (rt == null) {
      return null;
    }
    return Extreme.getHighLowClass(type, (double) rt);
  }

  public static Object getNominalCluTrend(MODEL_TYPES type, String date, ConcurrentHashMap<String, Object[]> rawDataMap, int distance, int duration, boolean getHighest) {
    Object ext = getClusteredTrend(date, rawDataMap, distance, duration, getHighest);
    if (ext == null) {
      return null;
    }
    return Extreme.getHighLowClass(type, (double) ext);
  }

  public static Object getSlope(Object p1, Object p2, int distance) {
    if (p1 == null || p2 == null) {
      return null;
    }
    return ((double) p2 - (double) p1) / distance;
  }

  public static double getDividentAmt(String date, ArrayList<String[]> dividendData) {
    double r = 0.0;
    if (dividendData != null) {
      LocalDate inputDate = parseToISO(date);
      LocalDate tempDate;
      long min = Long.MAX_VALUE, diff;
      for (String[] array : dividendData) {
        tempDate = parseToISO(array[0]);
        diff = tempDate.compareTo(inputDate);
        if (diff > 0 && diff < min && diff < 120) {
          min = diff;
          r = Double.parseDouble(array[1]);
        }
      }
    }
    return r;
  }

  public static int getDaysTillNextDivdnt(String date, ArrayList<String[]> dividendData) {
    int r = 999;
    if (dividendData != null) {
      LocalDate inputDate = parseToISO(date);
      LocalDate tempDate;
      long min = Long.MAX_VALUE, diff;
      for (String[] array : dividendData) {
        tempDate = parseToISO(array[0]);
        diff = tempDate.compareTo(inputDate);
        if (diff > 0 && diff < min && diff < 120) {
          min = diff;
          r = (int) min;
        }
      }
    }
    return r;
  }

  protected static LocalDate getFirstValidDate(
          ConcurrentHashMap<String, Object[]> rawDataMap)
          throws ParseException {
    LocalDate start_date = parseToISO(DEFAULT_START_DATE);
    //Keep looping until a valid date is found
    Object[] raw_data;
    for (int i = 9999; i > 0; i--) {
      raw_data = rawDataMap.get(start_date.toString());
      if (raw_data != null) {
        break;
      }
      start_date = start_date.plusDays(1);
    }
    return start_date;
  }

  protected static double average(LinkedList<Double> queue) {
    double sum = 0;
    sum = queue.stream().map((e) -> (double) e).reduce(sum, (accumulator, _item) -> accumulator + _item);
    return sum / queue.size();
  }

  public static Object getMomtumDifference(
          ConcurrentHashMap<String, Object[]> rawDataMap1,
          ConcurrentHashMap<String, Object[]> rawDataMap2,
          String date,
          int distance,
          int duration) {
    Object indicieMomtum = StatCalculator.getMomentum(date, rawDataMap2, distance, duration);
    Object codeMomtum = StatCalculator.getMomentum(date, rawDataMap1, distance, duration);
    if (indicieMomtum == null || codeMomtum == null) {
      return null;
    }

    return ((double) indicieMomtum - (double) codeMomtum);
  }

  //Difference to historical average
  public static Object getHistoryAvgDiff(String date,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          int distance, int duration) {
    LocalDate start_date = MyUtils.getUsableDate(date, rawDataMap, distance, duration, true);
    LocalDate end_date = MyUtils.getUsableDate(date, rawDataMap, distance, duration, false);

    if (start_date == null || end_date == null) {
      return "?";
    }

    double last_price = (double) rawDataMap.get(date)[YAHOO_DATA_INDEX.CLOSE.ordinal()];

    Random rdn = new Random();
    int count = 0;
    double sum = 0;
    while (start_date.isBefore(end_date)) {
      Object[] row = rawDataMap.get(start_date.toString());
      //Using 70% of random data
      if (row != null && rdn.nextDouble() >= 0.3) {
        count++;
        sum += (double) row[YAHOO_DATA_INDEX.CLOSE.ordinal()];
      }
      start_date = start_date.plusDays(1);
    }

    return last_price / (sum / count);
  }

}
