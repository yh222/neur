package calculator;

import core.GlobalConfigs;
import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

public class MovingAverage {


  private final HashMap<Integer, HashMap<String, Float>> m_EMAMap;
 
  private final HashMap<Integer, HashMap<String, Float>> m_SMAMap;

  public MovingAverage() {
    m_EMAMap = new HashMap();
    m_SMAMap = new HashMap();
  }

  public Object getEMA(String date, int emaDuration,
          ConcurrentHashMap<String, Object[]> rawDataMap) throws ParseException {
    HashMap<String, Float> ema = m_EMAMap.get(emaDuration);
    if (ema == null) {
      m_EMAMap.put(emaDuration, new HashMap());
      ema = m_EMAMap.get(emaDuration);
      calculateEMA(emaDuration, rawDataMap, ema);
    }

    return ema.get(date);
  }

  public Object getSMA(String date, int smaDuration,
          ConcurrentHashMap<String, Object[]> rawDataMap) throws ParseException {

    HashMap<String, Float> sma = m_SMAMap.get(smaDuration);
    if (sma == null) {
      m_SMAMap.put(smaDuration, new HashMap());
      sma = m_SMAMap.get(smaDuration);
      calculateSMA(smaDuration, rawDataMap, sma);
    }

    return sma.get(date);

  }

  private void calculateEMA(int emaDuration,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Float> ema) throws ParseException {

    Calendar start_date = StatCalculator.getFirstValidDate(rawDataMap);
    Object[] raw_data = rawDataMap.get(GlobalConfigs.getDateFormat().format(start_date.getTime()));
    if (raw_data == null) {
      System.err.println("Cannot find first valid date");
      return;
    }
    float smooth = 2.0f / (1.0f + emaDuration);
    float past_ema = (float) 1.0;
    float current_ema;
    ema.put(GlobalConfigs.getDateFormat().format(start_date.getTime()), past_ema);

    int buffer = 10;
    float current_p;
    while (buffer > 0) {
      start_date.add(Calendar.DAY_OF_MONTH, 1);
      raw_data = rawDataMap.get(GlobalConfigs.getDateFormat().format(start_date.getTime()));
      if (raw_data == null) {
        buffer--;
      } else {
        buffer = 10;
        current_p = (float) raw_data[3];
        current_ema = (current_p * smooth)
                + (past_ema * (1 - smooth));
        ema.put(GlobalConfigs.getDateFormat().format(start_date.getTime()), current_p / current_ema);
        past_ema = current_ema;
      }
    }

  }

  private void calculateSMA(int smaDuration,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Float> sma) throws ParseException {

    Calendar start_date = StatCalculator.getFirstValidDate(rawDataMap);
    Object[] raw_data = rawDataMap.get(GlobalConfigs.getDateFormat().format(start_date.getTime()));
    if (raw_data == null) {
      System.err.println("Cannot find first valid date");
      return;
    }
    //Initialize queue with all the same values;
    LinkedList<Float> queue = new LinkedList();
    for (int i = 0; i < smaDuration; i++) {
      queue.add((Float) 1.0f);
    }
    sma.put(GlobalConfigs.getDateFormat().format(start_date.getTime()), StatCalculator.average(queue));

    float p;
    int buffer = 10;
    while (buffer > 0) {
      start_date.add(Calendar.DAY_OF_MONTH, 1);
      raw_data = rawDataMap.get(GlobalConfigs.getDateFormat().format(start_date.getTime()));
      if (raw_data == null) {
        buffer--;
      } else {
        buffer = 10;
        p = (float) raw_data[3];
        queue.pop();
        queue.add(p);
        sma.put(GlobalConfigs.getDateFormat().format(start_date.getTime()), p / StatCalculator.average(queue));
      }
    }
  }

}
