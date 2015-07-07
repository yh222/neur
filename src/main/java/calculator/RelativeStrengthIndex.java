package calculator;

import core.GConfigs;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import util.MyUtils;

public class RelativeStrengthIndex {

  // RSI_map_type(duration)-> <Date, RSI_value>
  private final HashMap<Integer, HashMap<String, Double>> m_RSIMap;

  public RelativeStrengthIndex() {
    m_RSIMap = new HashMap();
  }

  public Object getRSI(String date, int rsiDuration, int distance,
          ConcurrentHashMap<String, Object[]> rawDataMap) throws ParseException {

    HashMap<String, Double> rsi = m_RSIMap.get(rsiDuration);
    if (rsi == null) {
      m_RSIMap.put(rsiDuration, new HashMap());
      rsi = m_RSIMap.get(rsiDuration);
      calculateRSI(rsiDuration, rawDataMap, rsi);
    }

    if (distance != 0) {
      LocalDate adjusted_date = MyUtils.getUsableDate(date,
              rawDataMap, distance, 0, true);
      if (adjusted_date == null) {
        return null;
      }
      date = adjusted_date.toString();
    }

    return rsi.get(date);
  }

  private void calculateRSI(int rsiDuration,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Double> rsi) throws ParseException {
    LocalDate start_date = StatCalculator.getFirstValidDate(rawDataMap);
    Object[] raw_data = rawDataMap.get(start_date.toString());
    if (raw_data == null) {
      System.err.println("Cannot find first valid date");
      return;
    }

    double loss, gain, p_avg_loss = 0, p_avg_gain = 0,
            rs, change, p, t_rsi;
    double prev = (double) raw_data[3];
    int buffer = 10;

    while (buffer > 0) {
      start_date=start_date.plusDays(1);
      raw_data = rawDataMap.get(start_date.toString());
      if (raw_data == null) {
        buffer--;
      } else {
        buffer = 10;
        p = (double) raw_data[GConfigs.YAHOO_DATA_INDEX.CLOSE.ordinal()];
        change = p - prev;
        prev = p;

        gain = change > 0 ? change : 0;
        loss = change < 0 ? -change : 0;

        //Use p_avg_gain/loss as temporary value to hold current avg
        p_avg_loss = (p_avg_loss * (rsiDuration - 1) + loss) / rsiDuration;
        p_avg_gain = (p_avg_gain * (rsiDuration - 1) + gain) / rsiDuration;
        rs = p_avg_loss == 0 ? 999 : p_avg_gain / p_avg_loss;
        t_rsi = (100 - 100 / (1 + rs));
        rsi.put(start_date.toString(), t_rsi);
      }
    }
  }
}
