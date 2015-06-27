package datapreparer.valuemaker;

import calculator.Extreme;
import calculator.Signal;
import static core.GConfigs.WEEK_MULTIPIER_CLASS;
import calculator.StatCalculator;
import core.GConfigs;
import core.GConfigs.MODEL_TYPES;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class STKClassValueMaker {

  private final static int DaysInWeek = 5;

  public static void generateCalssValues(String code, String date,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Object> storageRow) {
    addExtremeClass(code, date, rawDataMap, storageRow);
    //addExtremeValues(date, rawDataMap, storageRow);
    addSignals(date, rawDataMap, storageRow);
    //addClusteredTrends(code, date, rawDataMap, storageRow);
  }

  private static void addExtremeClass(String code, String date,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Object> storageRow) {
    int days;

    //Debug
//    storageRow.put(GConfigs.CLS + "Highest5d",
//            Extreme.getNominalExtreme(MODEL_TYPES.STK, date,
//                    rawDataMap, 0, 5, true));
//    storageRow.put(GConfigs.CLS + "Highest10d",
//            Extreme.getNominalExtreme(MODEL_TYPES.STK, date,
//                    rawDataMap, 0, 10, true));
//    
//        storageRow.put(GConfigs.CLS + "Lowest5d",
//            Extreme.getNominalExtreme(MODEL_TYPES.STK, date,
//                    rawDataMap, 0, 5, false));
//    storageRow.put(GConfigs.CLS + "Lowest15d",
//            Extreme.getNominalExtreme(MODEL_TYPES.STK, date,
//                    rawDataMap, 0, 15, false));
    for (int i = 0; i < WEEK_MULTIPIER_CLASS.length; i++) {
      days = WEEK_MULTIPIER_CLASS[i] * DaysInWeek;
      storageRow.put(GConfigs.CLS + "Highest" + String.format("%02d", days) + "d",
              Extreme.getNominalExtreme(MODEL_TYPES.STK, date,
                      rawDataMap, 0, days, true));
    }

    for (int i = 0; i < WEEK_MULTIPIER_CLASS.length; i++) {
      days = WEEK_MULTIPIER_CLASS[i] * DaysInWeek;
      storageRow.put(GConfigs.CLS + "Lowest" + String.format("%02d", days) + "d",
              Extreme.getNominalExtreme(MODEL_TYPES.STK, date,
                      rawDataMap, 0, days, false));
    }
  }

  private static void addClusteredTrends(String code, String date,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Object> storageRow) {
    int days;
    for (int i = 0; i < WEEK_MULTIPIER_CLASS.length; i++) {
      days = WEEK_MULTIPIER_CLASS[i] * DaysInWeek;
      storageRow.put(GConfigs.CLS + "CTrdHigh" + String.format("%02d", days) + "d",
              StatCalculator.getNominalCluTrend(MODEL_TYPES.STK, date,
                      rawDataMap, 0, days, true));

    }

    for (int i = 0; i < WEEK_MULTIPIER_CLASS.length; i++) {
      days = WEEK_MULTIPIER_CLASS[i] * DaysInWeek;
      storageRow.put(GConfigs.CLS + "CTrdLow" + String.format("%02d", days) + "d",
              StatCalculator.getNominalCluTrend(MODEL_TYPES.STK, date,
                      rawDataMap, 0, days, false));
    }
  }

  private static void addExtremeValues(String date, ConcurrentHashMap<String, Object[]> rawDataMap, HashMap<String, Object> storageRow) {
    int days;

    //Debug
//    storageRow.put(GConfigs.CLS + "Highest5dV",
//            Extreme.getExtremeRatio(date,
//                    rawDataMap, 0, 5, true));
//    storageRow.put(GConfigs.CLS + "Highest10d_series",
//            Extreme.getExtreme(date,
//                    rawDataMap, 0, 10, true));    
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

  private static void addSignals(String date, ConcurrentHashMap<String, Object[]> rawDataMap, HashMap<String, Object> storageRow) {
    storageRow.put(GConfigs.CLS + "Signal" + String.format("%02d", 5) + "d",
            Signal.getSignal(MODEL_TYPES.STK, date, rawDataMap, 5));
    
  }

}
