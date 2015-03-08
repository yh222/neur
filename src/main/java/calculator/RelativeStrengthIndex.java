package calculator;

import core.GlobalConfigs;
import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class RelativeStrengthIndex {

  // RSI_map_type(duration)-> <Date, RSI_value>
  private final HashMap<Integer, HashMap<String, Float>> m_RSIMap;

  public RelativeStrengthIndex() {
    m_RSIMap = new HashMap();
  }

  public Object getRSI(String date, int rsiDuration, int distance,
          ConcurrentHashMap<String, Object[]> rawDataMap) throws ParseException {

    HashMap<String, Float> rsi = m_RSIMap.get(rsiDuration);
    if (rsi == null) {
      m_RSIMap.put(rsiDuration, new HashMap());
      rsi = m_RSIMap.get(rsiDuration);
      calculateRSI(rsiDuration, rawDataMap, rsi);
    }

    if (distance != 0) {
      Calendar adjusted_date = StatCalculator.getUsableDate(date,
              rawDataMap, distance, 0, true, false);
      if (adjusted_date == null) {
        return null;
      }
      date = GlobalConfigs.getDateFormat().format(adjusted_date.getTime());
    }

    return rsi.get(date);
  }

  private void calculateRSI(int rsiDuration,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Float> rsi) throws ParseException {
    Calendar start_date = StatCalculator.getFirstValidDate(rawDataMap);
    Object[] raw_data = rawDataMap.get(GlobalConfigs.getDateFormat().format(start_date.getTime()));
    if (raw_data == null) {
      System.err.println("Cannot find first valid date");
      return;
    }

    float loss, gain, p_avg_loss = 0, p_avg_gain = 0,
            rs, change, p, t_rsi;
    float prev = (float) raw_data[3];
    int buffer = 10;

    while (buffer > 0) {
      start_date.add(Calendar.DAY_OF_MONTH, 1);
      raw_data = rawDataMap.get(GlobalConfigs.getDateFormat().format(start_date.getTime()));
      if (raw_data == null) {
        buffer--;
      } else {
        buffer = 10;
        p = (float) raw_data[3];
        change = p - prev;
        prev = p;

        gain = change > 0 ? change : 0;
        loss = change < 0 ? -change : 0;

        //Use p_avg_gain/loss as temporary value to hold current avg
        p_avg_loss = (p_avg_loss * (rsiDuration - 1) + loss) / rsiDuration;
        p_avg_gain = (p_avg_gain * (rsiDuration - 1) + gain) / rsiDuration;
        rs = p_avg_loss == 0 ? 999 : p_avg_gain / p_avg_loss;
        t_rsi = (100 - 100 / (1 + rs));
        rsi.put(GlobalConfigs.getDateFormat().format(start_date.getTime()), t_rsi);
      }
    }
  }
}
