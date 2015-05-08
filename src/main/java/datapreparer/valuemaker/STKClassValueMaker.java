package datapreparer.valuemaker;

import calculator.Extreme;
import static core.GConfigs.WEEK_MULTIPIER_CLASS;
import calculator.StatCalculator;
import core.GConfigs;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class STKClassValueMaker {

  private final static int DaysInWeek = 5;

  public static void generateCalssValues(String code, String date,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Object> storageRow) {
    addExtremeClass(code, date, rawDataMap, storageRow);
    addExtremeValues(date, rawDataMap, storageRow);
    //addClusteredTrends(code, date, rawDataMap, storageRow);
  }

  private static void addExtremeClass(String code, String date,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Object> storageRow) {
    int days;
    
    //Debug
    storageRow.put(GConfigs.NCLS + "Highest10d",
            Extreme.getNominalExtreme(code, date,
                    rawDataMap, 0, 10, true));

//    for (int i = 0; i < WEEK_MULTIPIER_CLASS.length; i++) {
//      days = WEEK_MULTIPIER_CLASS[i] * DaysInWeek;
//      storageRow.put(GConfigs.NCLS + "Highest" + String.format("%02d", days) + "d",
//              Extreme.getNominalExtreme(code, date,
//                      rawDataMap, 0, days, true));
//    }
//
//    for (int i = 0; i < WEEK_MULTIPIER_CLASS.length; i++) {
//      days = WEEK_MULTIPIER_CLASS[i] * DaysInWeek;
//      storageRow.put(GConfigs.NCLS + "Lowest" + String.format("%02d", days) + "d",
//              Extreme.getNominalExtreme(code, date,
//                      rawDataMap, 0, days, false));
//    }
  }

  private static void addClusteredTrends(String code, String date,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Object> storageRow) {
    int days;
    for (int i = 0; i < WEEK_MULTIPIER_CLASS.length; i++) {
      days = WEEK_MULTIPIER_CLASS[i] * DaysInWeek;
      storageRow.put(GConfigs.VCLS + "CTrdHigh" + String.format("%02d", days) + "d",
              StatCalculator.getNominalCluTrend(code, date,
                      rawDataMap, 0, days, true));

    }

    for (int i = 0; i < WEEK_MULTIPIER_CLASS.length; i++) {
      days = WEEK_MULTIPIER_CLASS[i] * DaysInWeek;
      storageRow.put(GConfigs.VCLS + "CTrdLow" + String.format("%02d", days) + "d",
              StatCalculator.getNominalCluTrend(code, date,
                      rawDataMap, 0, days, false));
    }
  }

  private static void addExtremeValues(String date, ConcurrentHashMap<String, Object[]> rawDataMap, HashMap<String, Object> storageRow) {
    int days;

    //Debug
    storageRow.put(GConfigs.VCLS + "Highest10dV",
            Extreme.getExtremeRatio(date,
                    rawDataMap, 0, 10, true));

//    for (int i = 0; i < WEEK_MULTIPIER_CLASS.length; i++) {
//      days = WEEK_MULTIPIER_CLASS[i] * DaysInWeek;
//      storageRow.put(GConfigs.VCLS +"Highest" + String.format("%02d", days) + "dV",
//              Extreme.getExtremeRatio(date,
//                      rawDataMap, 0, days, true));
//    }
//    
//    for (int i = 0; i < WEEK_MULTIPIER_CLASS.length; i++) {
//      days = WEEK_MULTIPIER_CLASS[i] * DaysInWeek;
//      storageRow.put(GConfigs.VCLS +"Lowest" + String.format("%02d", days) + "dV",
//              Extreme.getExtremeRatio(date,
//                      rawDataMap, 0, days, false));
//    }
  }

}
