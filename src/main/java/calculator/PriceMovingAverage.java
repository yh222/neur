package calculator;

import core.GConfigs.YAHOO_DATA_INDEX;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import util.MyUtils;

public class PriceMovingAverage {

  private final HashMap<Integer, HashMap<String, Double>> m_EMAMap;
  private final HashMap<Integer, HashMap<String, Double>> m_SMAMap;
  //Standard deviation
  private final HashMap<Integer, HashMap<String, Double>> m_STDMap;

  public PriceMovingAverage() {
    m_EMAMap = new HashMap();
    m_SMAMap = new HashMap();
    m_STDMap = new HashMap();
  }

  public Double getEMA(String date, int emaDuration,
          ConcurrentHashMap<String, Object[]> rawDataMap) throws ParseException {

    Object[] raw_data = rawDataMap.get(date);
    if (raw_data == null) {
      return null;
    }
    double p = (double) raw_data[YAHOO_DATA_INDEX.CLOSE.ordinal()];
    Double ema = getRawEMA(date, emaDuration, rawDataMap);
    if (ema == null) {
      return ema;
    } else {
      return p / (double) ema;
    }
  }

  public Double getRawEMA(String date, int emaDuration,
          ConcurrentHashMap<String, Object[]> rawDataMap) throws ParseException {
    HashMap<String, Double> ema = m_EMAMap.get(emaDuration);
    if (ema == null) {
      m_EMAMap.put(emaDuration, new HashMap());
      ema = m_EMAMap.get(emaDuration);
      calculateEMA(emaDuration, rawDataMap, ema);
    }
    return ema.get(date);
  }

//  public Object getEMAClass(String date, int distance, int emaDuration,
//          ConcurrentHashMap<String, Object[]> rawDataMap) throws ParseException {
//    LocalDate target_date = MyUtils.getUsableDate(date, rawDataMap, distance * -1, 1, true);
//    if (target_date == null) {
//      return "?";
//    }
//    Double base_ema = getRawEMA(date, emaDuration, rawDataMap);
//    Double target_ema = getRawEMA(target_date.toString(), emaDuration, rawDataMap);
//    if (base_ema == null || target_ema == null) {
//      return "?";
//    }
//    return (target_ema - base_ema) / base_ema;
//  }

  public Object getSMA(String date, int smaDuration,
          ConcurrentHashMap<String, Object[]> rawDataMap) throws ParseException {

    Object[] raw_data = rawDataMap.get(date);
    if (raw_data == null) {
      return null;
    }
    double p = (double) raw_data[YAHOO_DATA_INDEX.CLOSE.ordinal()];
    Double sma = getRawSMA(date, smaDuration, rawDataMap);
    if (sma == null) {
      return sma;
    } else {
      return p / (double) sma;
    }
  }

  public Double getRawSMA(String date, int smaDuration,
          ConcurrentHashMap<String, Object[]> rawDataMap) throws ParseException {
    HashMap<String, Double> sma = m_SMAMap.get(smaDuration);
    if (sma == null) {
      m_SMAMap.put(smaDuration, new HashMap());
      sma = m_SMAMap.get(smaDuration);
      calculateSMA(smaDuration, rawDataMap, sma);
    }
    if (sma.get(date) == null) {
      return sma.get(date);
    }
    return sma.get(date);
  }

  // Standard deviation, the bone of bollinger channel
  public Double getSTD(String date, int distance, int stdDuration,
          ConcurrentHashMap<String, Object[]> rawDataMap) throws ParseException {
    LocalDate start_date = MyUtils.getUsableDate(date, rawDataMap, distance, stdDuration, false);
    if (start_date == null) {
      return null;
    }
    String start_day = start_date.toString();

    Object[] raw_data = rawDataMap.get(start_day);
    double p = (double) raw_data[YAHOO_DATA_INDEX.CLOSE.ordinal()];
    HashMap<String, Double> std = m_STDMap.get(stdDuration);
    if (std == null) {
      calculateSTD(stdDuration, rawDataMap);
      std = m_STDMap.get(stdDuration);
    }
    if (std == null || std.get(start_day) == null) {
      return null;
    }
    return std.get(start_day) / p;
  }

  public Double getMACD(String date, int shortD, int longD, int midD,
          ConcurrentHashMap<String, Object[]> rawDataMap) throws ParseException {
    Double dea = getDEA(date, shortD, longD, midD, rawDataMap);
    Double dif = getDIF(date, shortD, longD, rawDataMap);
    if (dea == null || dif == null) {
      return null;
    }
    return (dif - dea) * 2;
  }

  public Double getDEA(String date, int shortD, int longD, int midD,
          ConcurrentHashMap<String, Object[]> rawDataMap) throws ParseException {
    LocalDate start_date = MyUtils.getUsableDate(date, rawDataMap, midD, midD, true);
    LocalDate end_date = MyUtils.getUsableDate(date, rawDataMap, midD, midD, false);
    if (start_date == null || end_date == null) {
      return null;
    }
    EMovingAverage ema = new EMovingAverage(2.0 / (1.0 + midD));
    double v = 0;
    while (start_date.isBefore(end_date)) {
      Double dif = getDIF(start_date.toString(),
              shortD, longD, rawDataMap);
      if (dif != null) {
        v = (double) ema.average(dif);
      }
      start_date = start_date.plusDays(1);
    }
    return v;
  }

  public Double getDIF(String date, int shortD, int longD,
          ConcurrentHashMap<String, Object[]> rawDataMap) throws ParseException {
    Double shortema = getRawEMA(date, shortD, rawDataMap);
    Double longema = getRawEMA(date, longD, rawDataMap);
    if (shortema == null || longema == null) {
      return null;
    }
    double dif = shortema - longema;
    return dif;
  }

  private void calculateEMA(int emaDuration,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Double> ema) throws ParseException {

    LocalDate start_date = StatCalculator.getFirstValidDate(rawDataMap);
    Object[] raw_data = rawDataMap.get(start_date.toString());
    if (raw_data == null) {
      System.err.println("MA Cannot find first valid date");
      return;
    }

    EMovingAverage ma = new EMovingAverage(2.0 / (1.0 + emaDuration));
    int buffer = 15;
    while (buffer > 0) {
      String datestr = start_date.toString();
      raw_data = rawDataMap.get(datestr);
      if (raw_data == null) {
        buffer--;
      } else {
        buffer = 15;
        ema.put(datestr, (double) ma.average((double) raw_data[YAHOO_DATA_INDEX.CLOSE.ordinal()]));
      }
      start_date = start_date.plusDays(1);
    }
  }

  private void calculateSMA(int smaDuration,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Double> sma) throws ParseException {

    HashMap<String, Double> std = m_STDMap.get(smaDuration);
    if (std == null) {
      m_STDMap.put(smaDuration, new HashMap());
      std = m_STDMap.get(smaDuration);
    }

    StandardDeviation deviation = new StandardDeviation();

    LocalDate start_date = StatCalculator.getFirstValidDate(rawDataMap);
    Object[] raw_data = rawDataMap.get(start_date.toString());
    if (raw_data == null) {
      System.err.println("Cannot find first valid date");
      return;
    }

    SMovingAverage ma = new SMovingAverage(smaDuration);
    //Buffer is used to skip dates with no data
    int buffer = 15;
    while (buffer > 0) {
      String datestr = start_date.toString();
      raw_data = rawDataMap.get(datestr);
      if (raw_data == null) {
        buffer--;
      } else {
        buffer = 15;
        ma.newNum((double) raw_data[YAHOO_DATA_INDEX.CLOSE.ordinal()]);
        Object[] queue_data = ma.getWindow().toArray();
        double[] fqueue = new double[queue_data.length];
        for (int i = 0; i < queue_data.length; i++) {
          fqueue[i] = (double) queue_data[i];
        }
        std.put(datestr, (double) deviation.evaluate(fqueue));
        sma.put(datestr, (double) ma.getAvg());
      }
      start_date = start_date.plusDays(1);
    }
  }

  private void calculateSTD(int stdDuration, ConcurrentHashMap<String, Object[]> rawDataMap) throws ParseException {
    m_SMAMap.put(stdDuration, new HashMap());
    HashMap sma = m_SMAMap.get(stdDuration);
    calculateSMA(stdDuration, rawDataMap, sma);
  }

}
