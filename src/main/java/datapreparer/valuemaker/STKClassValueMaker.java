package datapreparer.valuemaker;

import static core.GlobalConfigs.WEEK_MULTIPIER_CLASS;
import calculator.StatCalculator;
import core.GlobalConfigs;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class STKClassValueMaker {

  private final static int DaysInWeek = 5;

  public static void generateCalssValues(String code, String date,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Object> storageRow) {
    addExtremeClass(code, date, rawDataMap, storageRow);
    addClusteredTrends(code, date, rawDataMap, storageRow);
  }

  private static void addExtremeClass(String code, String date,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Object> storageRow) {
    int days;

    storageRow.put(GlobalConfigs.CLS + "Highest02d",
            StatCalculator.getNominalExtreme(code, date,
                    rawDataMap, 0, 2 - 1, true));
    storageRow.put(GlobalConfigs.CLS + "Highest03d",
            StatCalculator.getNominalExtreme(code, date,
                    rawDataMap, 0, 3 - 1, true));

    for (int i = 0; i < WEEK_MULTIPIER_CLASS.length; i++) {
      days = WEEK_MULTIPIER_CLASS[i] * DaysInWeek;
      storageRow.put(GlobalConfigs.CLS + "Highest" + String.format("%02d", days) + "d",
              StatCalculator.getNominalExtreme(code, date,
                      rawDataMap, 0, days - 1, true));
    }

    storageRow.put(GlobalConfigs.CLS + "Lowest02d",
            StatCalculator.getNominalExtreme(code, date,
                    rawDataMap, 0, 2 - 1, false));
    storageRow.put(GlobalConfigs.CLS + "Lowest03d",
            StatCalculator.getNominalExtreme(code, date,
                    rawDataMap, 0, 3 - 1, false));

    for (int i = 0; i < WEEK_MULTIPIER_CLASS.length; i++) {
      days = WEEK_MULTIPIER_CLASS[i] * DaysInWeek;
      storageRow.put(GlobalConfigs.CLS + "Lowest" + String.format("%02d", days) + "d",
              StatCalculator.getNominalExtreme(code, date,
                      rawDataMap, 0, days - 1, false));
    }
  }

  private static void addClusteredTrends(String code, String date,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Object> storageRow) {
    int days;
    for (int i = 0; i < WEEK_MULTIPIER_CLASS.length; i++) {
      days = WEEK_MULTIPIER_CLASS[i] * DaysInWeek;
      storageRow.put(GlobalConfigs.CLS + "CTrdHigh" + String.format("%02d", days) + "d",
              StatCalculator.getNominalCluTrend(code, date,
                      rawDataMap, 0, days - 1, true));

    }

    for (int i = 0; i < WEEK_MULTIPIER_CLASS.length; i++) {
      days = WEEK_MULTIPIER_CLASS[i] * DaysInWeek;
      storageRow.put(GlobalConfigs.CLS + "CTrdLow" + String.format("%02d", days) + "d",
              StatCalculator.getNominalCluTrend(code, date,
                      rawDataMap, 0, days - 1, false));
    }
  }

}
