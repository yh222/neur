package calculator;

import core.GConfigs;
import java.util.concurrent.ConcurrentHashMap;

public class Signal {

  public static Object getSignal(GConfigs.MODEL_TYPES type,String date, ConcurrentHashMap<String, Object[]> rawDataMap, int duration) {
    Object h = Extreme.getExtremeRatio(
            date, rawDataMap, 0, duration, true);
    Object l = Extreme.getExtremeRatio(
            date, rawDataMap, 0, duration, false);
    if (h != null && l != null) {
      double highest = Math.abs((double) h);
      double lowest = Math.abs((double) l);
      if (highest > 0.03 || lowest > 0.03) {
        if (highest > lowest) {
          return 0.01;
        } else {
          return -0.01;
        }
      } else {
        return 0;
      }
    }
    return null;
  }

}
