package datapreparer.valuemaker.indicie;

import calculator.Extreme;
import static core.GConfigs.WEEK_MULTIPIER_TRAIN;
import calculator.StatCalculator;
import core.GConfigs.MODEL_TYPES;
import static datapreparer.RawDataLoader.loadRawDataFromFile;
import java.util.concurrent.ConcurrentHashMap;

public class IndicieValueMaker {

  private static final ConcurrentHashMap<String, ConcurrentHashMap<String, Object[]>> indicieExtremes = new ConcurrentHashMap();
  private static final ConcurrentHashMap<String, ConcurrentHashMap<String, Object[]>> indicieRawData = new ConcurrentHashMap();

  public static Object[] loadIndiciePastExtreme(String indicieCode, String date) {

    if (!indicieExtremes.containsKey(indicieCode)) {
      ConcurrentHashMap<String, Object[]> raw_data_map
              = loadRawDataFromFile(indicieCode, MODEL_TYPES.STK.name() + "//");
      ConcurrentHashMap date_to_data = new ConcurrentHashMap();

      Object[] temp_row;
      for (String d : raw_data_map.keySet()) {
        int days;
        temp_row = new Object[WEEK_MULTIPIER_TRAIN.length];
        for (int i = 0; i < WEEK_MULTIPIER_TRAIN.length; i++) {
          days = WEEK_MULTIPIER_TRAIN[i] * 7;
          Object r = Extreme.getExtremeRatio(
                  d, raw_data_map, days, days, false);
          if (r == null) {
            r = "?";
          }
          temp_row[i] = r;
        }
        date_to_data.put(d, temp_row);
      }
      indicieExtremes.put(indicieCode, date_to_data);
    }
    return indicieExtremes.get(indicieCode).get(date);
  }

  public static Object getMomtumDifference(String indicieCode,
          ConcurrentHashMap<String, Object[]> rawDataMap_stk,
          String date,
          int distance,
          int duration) {
    ConcurrentHashMap<String, Object[]> rawDataMap_ind = indicieRawData.get(indicieCode);
    if (rawDataMap_ind == null) {
      rawDataMap_ind = loadRawDataFromFile(indicieCode, MODEL_TYPES.STK.name() + "//");
      indicieRawData.put(indicieCode, rawDataMap_ind);
    }

    Object r = StatCalculator.getMomtumDifference(
            rawDataMap_stk, rawDataMap_ind, date, distance, duration);
    if (r == null) {
      r = "?";
    }
    return r;

  }

}
