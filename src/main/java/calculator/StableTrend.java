package calculator;

import core.GConfigs;
import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import util.MyUtils;

public class StableTrend {

  //stable trend, use date A's past x day's average close, 
  //divided by a target date B's past x day's average close
  public static Object getSTrendRatio(String date, int distance, int duration,
          ConcurrentHashMap<String, Object[]> rawDataMap) {
    LocalDate target_date = MyUtils.getUsableDate(date, rawDataMap, distance, 0, true);
    if (target_date == null) {
      return "?";
    }
    Double base = (Double) rawDataMap.get(date)[GConfigs.YAHOO_DATA_INDEX.CLOSE.ordinal()];
    Double target_avg = getAverage(target_date.toString(), duration, rawDataMap);

    if (base == null || target_avg == null) {
      return "?";
    }

    return (target_avg - base) / base * 5;
  }

  private static Double getAverage(String date, int duration,
          ConcurrentHashMap<String, Object[]> rawDataMap) {
    LocalDate start_date = MyUtils.parseToISO(date);

    int buffer = 10;
    double sum = 0;
    double count = 0;

    for (int i = 0; i < duration; i++) {
      Object[] row = rawDataMap.get(start_date.toString());
      if (row != null) {
        count++;
        sum += (double) row[GConfigs.YAHOO_DATA_INDEX.CLOSE.ordinal()];
      } else if (buffer > 0) {
        i--;
        buffer--;
      } else {
        return null;
      }
    }
    return sum / count;
  }

}
