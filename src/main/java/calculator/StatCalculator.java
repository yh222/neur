package calculator;

import core.GConfigs;
import static core.GConfigs.DEFAULT_START_DATE;
import core.GConfigs.RAW_DATA_INDEX;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StatCalculator {

  /*
   * Velocity = volume of the date / Average volume
   */
  public static Object getVelocity(String date, ConcurrentHashMap<String, Object[]> rawDataMap, int distance) {
    //Duration should be always zero, as non-directly past velocity seems not revelant.
    Calendar start_date = getUsableDate(date, rawDataMap, distance, 0, true, true);
    Calendar end_date = getUsableDate(date, rawDataMap, 0, 0, false, true);
    if (start_date == null || end_date == null) {
      return null; //remain 0
    }
    //System.out.println("start: " + GlobalConfigs.getDateFormat().format(start_date.getTime()));
    //System.out.println("end: " + GlobalConfigs.getDateFormat().format(end_date.getTime()));
    int count = 0;
    int buffer = (int) (distance * 0.4) + 3;
    float sum_volume = 0;
    //Calculate volume sum
    for (int i = 0; i < distance; i++) {
      if (rawDataMap.get(GConfigs.getDateFormat().format(start_date.getTime())) != null) {
        count++;
        sum_volume += (float) rawDataMap.get(GConfigs.getDateFormat().format(start_date.getTime()))[RAW_DATA_INDEX.VOLUME.ordinal()];
        //System.out.println(GlobalConfigs.getDateFormat().format(start_date.getTime()));
      } else {
        i--;
        if (--buffer == 0) {
          break;
        }
        //System.out.println("cannot find: " + GlobalConfigs.getDateFormat().format(start_date.getTime()));
      }
      start_date.add(Calendar.DATE, 1);
    }
    float current_volume = (float) rawDataMap.get(GConfigs.getDateFormat().format(end_date.getTime()))[RAW_DATA_INDEX.VOLUME.ordinal()];
    return current_volume / (sum_volume / count);
  }

  public static Object getMomentum(String date,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          int distance, int duration) {
    Calendar start_date = getUsableDate(date, rawDataMap, distance, duration, true, true);
    Calendar end_date = getUsableDate(date, rawDataMap, distance, duration, false, true);
    if (start_date == null || end_date == null) {
      return null;
    }

    float trend_start = (float) rawDataMap.get(GConfigs.getDateFormat().format(start_date.getTime()))[RAW_DATA_INDEX.OPEN.ordinal()];
    float trend_end = (float) rawDataMap.get(GConfigs.getDateFormat().format(end_date.getTime()))[RAW_DATA_INDEX.CLOSE.ordinal()];
    return (trend_end - trend_start) / trend_start;
  }

  public static Object getClusteredTrend(String date, ConcurrentHashMap<String, Object[]> rawDataMap, int distance, int duration, boolean getHighest) {
    Calendar start_date = getUsableDate(date, rawDataMap, distance, duration, true, true);
    Calendar end_date = getUsableDate(date, rawDataMap, distance, duration, false, true);
    if (start_date == null || end_date == null) {
      return null;
    }
    float start_price;
    if (getHighest) {
      start_price = (float) rawDataMap.get(GConfigs.getDateFormat().format(start_date.getTime()))[RAW_DATA_INDEX.HIGH.ordinal()];
    } else {
      start_price = (float) rawDataMap.get(GConfigs.getDateFormat().format(start_date.getTime()))[RAW_DATA_INDEX.LOW.ordinal()];
    }

    float max = Float.NEGATIVE_INFINITY;
    float min = Float.POSITIVE_INFINITY;
    //Set end date to monday of the week
    int dayofWeek = end_date.get(Calendar.DAY_OF_WEEK);
    end_date.add(Calendar.DATE, -1 * (dayofWeek - Calendar.MONDAY));
    for (int i = 0; i < 5; i++) {
      Object[] row = rawDataMap.get(GConfigs.getDateFormat().format(end_date.getTime()));
      if (row != null) {
        float templow = (float) row[RAW_DATA_INDEX.LOW.ordinal()];
        float temphigh = (float) row[RAW_DATA_INDEX.HIGH.ordinal()];
        if (max < temphigh) {
          max = temphigh;
        }
        if (min > templow) {
          min = templow;
        }
      }
      end_date.add(Calendar.DATE, 1);
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
    try {
      Calendar tempdate = Calendar.getInstance();
      tempdate.setTime(GConfigs.getDateFormat().parse(date));
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

  public static String getNominalRawTrend(String code, String date, ConcurrentHashMap<String, Object[]> rawDataMap, int distance, int duration) {
    Object rt = getMomentum(date, rawDataMap, distance, duration);
    if (rt == null) {
      return null;
    }
    return Extreme.getHighLowClass(code, (float) rt);
  }


  public static String getNominalCluTrend(String code, String date, ConcurrentHashMap<String, Object[]> rawDataMap, int distance, int duration, boolean getHighest) {
    Object ext = getClusteredTrend(date, rawDataMap, distance, duration, getHighest);
    if (ext == null) {
      return null;
    }
    return Extreme.getHighLowClass(code, (float) ext);
  }


  /*
   * get a date 
   */
  public static String getDayOfWeek(String date) {
    try {
      Calendar tempdate = Calendar.getInstance();
      tempdate.setTime(GConfigs.getDateFormat().parse(date));
      String[] namesOfDays = DateFormatSymbols.getInstance().getShortWeekdays();
      return namesOfDays[tempdate.get(Calendar.DAY_OF_WEEK)];
    } catch (ParseException ex) {
      Logger.getLogger(StatCalculator.class.getName()).log(Level.SEVERE, null, ex);
      return null;
    }
  }

  public static Calendar getUsableDate(String date, ConcurrentHashMap<String, Object[]> rawDataMap, int distance, int duration, boolean isStart, boolean useEffevtiveDay) {
    try {
      Calendar tempdate = Calendar.getInstance();
      tempdate.setTime(GConfigs.getDateFormat().parse(date));

      int direction, count;
      if (isStart) {
        direction = distance <= 0 ? 1 : -1;
        count = Math.abs(distance);
      } else {
        direction = duration - distance < 0 ? -1 : 1;
        count = Math.abs(duration - distance);
      }

      int buffer = (int) ((duration + distance) * 0.6) + 5;
      for (int i = 0; i <= count; i++) {
        //Keep going forward until buffer depleted 
        if (buffer <= 0) {
          break;
        }
        //Check validity when count meet
        if (i == count) {
          if (rawDataMap.get(GConfigs.getDateFormat().format(tempdate.getTime())) == null) {
            buffer--;
            i--;
          } else {
            return (Calendar) tempdate.clone();
          }
        }
        // Need to check everyday if going to use effective days only
        if (useEffevtiveDay) {
          if (rawDataMap.get(GConfigs.getDateFormat().format(tempdate.getTime())) == null) {
            buffer--;
            i--;
          }
        }
        tempdate.add(Calendar.DAY_OF_MONTH, direction);
      }

      return null;
    } catch (ParseException ex) {
      Logger.getLogger(StatCalculator.class.getName()).log(Level.SEVERE, null, ex);
    }
    return null;
  }

  public static Object getSlope(Object p1, Object p2, int distance) {
    if (p1 == null || p2 == null) {
      return null;
    }    
    return ((float) p2 - (float) p1) / distance;
  }


  public static float getDividentAmt(String date, ArrayList<String[]> dividendData) {
    float r = 0.0f;
    try {
      if (dividendData != null) {
        Calendar inputDate = Calendar.getInstance();
        Calendar tempDate = Calendar.getInstance();
        inputDate.setTime(GConfigs.getDateFormat().parse(date));
        long min = Long.MAX_VALUE, diff;
        for (String[] array : dividendData) {
          tempDate.setTime(GConfigs.getDateFormat().parse(array[0]));
          diff = getDateDiff(inputDate.getTime(), tempDate.getTime(), TimeUnit.DAYS);
          if (diff > 0 && diff < min && diff < 120) {
            min = diff;
            r = Float.parseFloat(array[1]);
          }
        }
      }
    } catch (ParseException ex) {
      Logger.getLogger(StatCalculator.class.getName()).log(Level.SEVERE, null, ex);
    }
    return r;
  }

  public static int getDaysTillNextDivdnt(String date, ArrayList<String[]> dividendData) {
    int r = 999;
    try {
      if (dividendData != null) {
        Calendar inputDate = Calendar.getInstance();
        Calendar tempDate = Calendar.getInstance();
        inputDate.setTime(GConfigs.getDateFormat().parse(date));
        long min = Long.MAX_VALUE, diff;
        for (String[] array : dividendData) {
          tempDate.setTime(GConfigs.getDateFormat().parse(array[0]));
          diff = getDateDiff(inputDate.getTime(), tempDate.getTime(), TimeUnit.DAYS);
          if (diff > 0 && diff < min && diff < 120) {
            min = diff;
            r = (int) min;
          }
        }
      }
    } catch (ParseException ex) {
      Logger.getLogger(StatCalculator.class.getName()).log(Level.SEVERE, null, ex);
    }
    return r;
  }

  public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
    long diffInMillies = date2.getTime() - date1.getTime();
    return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
  }

  protected static Calendar getFirstValidDate(
          ConcurrentHashMap<String, Object[]> rawDataMap)
          throws ParseException {
    Calendar start_date = Calendar.getInstance();
    start_date.setTime(GConfigs.getDateFormat().parse(DEFAULT_START_DATE));
    //Keep looping until a valid date is found
    Object[] raw_data;
    for (int i = 9999; i > 0; i--) {
      raw_data = rawDataMap.get(GConfigs.getDateFormat().format(start_date.getTime()));
      if (raw_data != null) {
        break;
      }
      start_date.add(Calendar.DAY_OF_MONTH, 1);
    }
    return start_date;
  }

  protected static float average(LinkedList<Float> queue) {
    float sum = 0;
    for (Float e : queue) {
      sum += (float) e;
    }
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

    return ((float) indicieMomtum - (float) codeMomtum);
  }

  //Difference to historical average
  public static Object getHistoryAvgDiff(String date,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          int distance, int duration) {
    Calendar start_date = getUsableDate(date, rawDataMap, distance, duration, true, true);
    Calendar end_date = getUsableDate(date, rawDataMap, distance, duration, false, true);

    Calendar input_date = getUsableDate(date, rawDataMap, 0, 0, false, true);

    if (start_date == null || end_date == null || input_date == null) {
      return "?";
    }

    float last_price = (float) rawDataMap.get(GConfigs.getDateFormat().format(input_date.getTime()))[4];

    Random rdn = new Random();
    int count = 0;
    float sum = 0.0f;
    while (!start_date.after(end_date)) {
      Object[] row = rawDataMap.get(GConfigs.getDateFormat().format(start_date.getTime()));
      //Using 70% of random data
      if (row != null && rdn.nextFloat() >= 0.3f) {
        count++;
        sum += (float) row[RAW_DATA_INDEX.CLOSE.ordinal()];
      }
      start_date.add(Calendar.DAY_OF_MONTH, 1);
    }

    return last_price / (sum / count);
  }

}
