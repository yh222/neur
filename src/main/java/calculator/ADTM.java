package calculator;

import core.GConfigs.YAHOO_DATA_INDEX;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import util.MyUtils;

public class ADTM {

  public static Object getMAADTM(String date, int maDuration, int sumBackDuration,
          ConcurrentHashMap<String, Object[]> rawDataMap) throws ParseException {
    SMovingAverage ma = new SMovingAverage(maDuration);
    LocalDate start_date = MyUtils.getUsableDate(date, rawDataMap, maDuration, maDuration, true, true);
    LocalDate end_date = MyUtils.getUsableDate(date, rawDataMap, maDuration, maDuration, false, true);
    if (start_date == null || end_date == null) {
      return null;
    }

    while (start_date.isBefore(end_date)) {
      ma.newNum((double) getADTM(start_date.toString(),
              sumBackDuration, rawDataMap));
      start_date=start_date.plusDays(1);
    }

    return ma.getAvg();
  }

  public static Object getADTM(String date, int sumBackDuration,
          ConcurrentHashMap<String, Object[]> rawDataMap) throws ParseException {

    double stm = sumBack(date, sumBackDuration, sumBackDuration, rawDataMap, new DTM());
    double sbm = sumBack(date, sumBackDuration, sumBackDuration, rawDataMap, new DBM());

    double adtm;
    if (stm > sbm) {
      adtm = (stm - sbm) / stm;
    } else if (stm == sbm) {
      adtm = 0;
    } else {
      adtm = (stm - sbm) / sbm;
    }
    return adtm;
  }

  public static double sumBack(String date, int distance, int duration,
          ConcurrentHashMap<String, Object[]> rawDataMap, Method m) throws ParseException {
    LocalDate start_date = MyUtils.getUsableDate(date, rawDataMap, distance, duration, true, true);
    LocalDate end_date = MyUtils.getUsableDate(date, rawDataMap, distance, duration, false, true);
    if (start_date == null || end_date == null) {
      return 0;
    }
    double sum = m.execute(start_date.toString(), rawDataMap);

    start_date=start_date.plusDays( 1);
    while (start_date.isBefore(end_date)) {
      Object[] row = rawDataMap.get(start_date.toString());
      if (row != null) {
        //Use highest as high, lowest as low
        sum += m.execute(start_date.toString(), rawDataMap);
      }
      start_date=start_date.plusDays(1);
    }

    return sum;
  }

  public interface Method {

    public double execute(String date, ConcurrentHashMap<String, Object[]> rawDataMap) throws ParseException;
  }

  public static class DTM implements Method {

    @Override
    public double execute(String date, ConcurrentHashMap<String, Object[]> rawDataMap) throws ParseException {
      double dtm;
      Object[] raw_data = rawDataMap.get(date);
      double open = (double) raw_data[YAHOO_DATA_INDEX.OPEN.ordinal()];
      double high = (double) raw_data[YAHOO_DATA_INDEX.HIGH.ordinal()];
      LocalDate yesterday = MyUtils.getUsableDate(date, rawDataMap, 1, 1, true, true);
      if (yesterday == null) {
        return 0;
      }
      raw_data = rawDataMap.get(yesterday.toString());
      double yopen = (double) raw_data[YAHOO_DATA_INDEX.OPEN.ordinal()];
      if (open <= yopen) {
        dtm = 0;
      } else {
        dtm = Math.max((high - open), (open - yopen));
      }
      return dtm;
    }
  }

  public static class DBM implements Method {

    @Override
    public double execute(String date, ConcurrentHashMap<String, Object[]> rawDataMap) throws ParseException {
      double dbm;
      Object[] raw_data = rawDataMap.get(date);
      double open = (double) raw_data[YAHOO_DATA_INDEX.OPEN.ordinal()];
      double low = (double) raw_data[YAHOO_DATA_INDEX.LOW.ordinal()];
      LocalDate yesterday = MyUtils.getUsableDate(date, rawDataMap, 1, 1, true, true);
      if (yesterday == null) {
        return 0;
      }
      raw_data = rawDataMap.get(yesterday.toString());
      double yopen = (double) raw_data[YAHOO_DATA_INDEX.OPEN.ordinal()];
      if (open >= yopen) {
        dbm = 0;
      } else {
        dbm = Math.max((open - low), (open - yopen));
      }
      return dbm;
    }
  }
}
