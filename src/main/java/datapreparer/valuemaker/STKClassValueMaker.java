package datapreparer.valuemaker;

import calculator.Extreme;
import calculator.PriceMovingAverage;
import calculator.Signal;
import calculator.StableTrend;
import static core.GConfigs.WEEK_MULTIPIER_CLASS;
import calculator.StatCalculator;
import core.GConfigs;
import core.GConfigs.MODEL_TYPES;
import java.text.ParseException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class STKClassValueMaker {

  PriceMovingAverage m_MovingAverage = new PriceMovingAverage();

  public void generateCalssValues(String code, String date,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Object> storageRow) {
    //addExtremeClass(code, date, rawDataMap, storageRow);
    //addExtremeValues(date, rawDataMap, storageRow);
    addStableTrends(date, rawDataMap, storageRow);
    addSignals(date, rawDataMap, storageRow);
    //addClusteredTrends(code, date, rawDataMap, storageRow);
  }

  private void addExtremeClass(String code, String date,
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
//        storageRow.put(GConfigs.CLS + "Lowest10d",
//            Extreme.getNominalExtreme(MODEL_TYPES.STK, date,
//                    rawDataMap, 0, 10, false));
//    storageRow.put(GConfigs.CLS + "Lowest15d",
//            Extreme.getNominalExtreme(MODEL_TYPES.STK, date,
//                    rawDataMap, 0, 15, false));
//    for (int i = 0; i < WEEK_MULTIPIER_CLASS.length; i++) {
//      days = WEEK_MULTIPIER_CLASS[i] * DaysInWeek;
//      storageRow.put(GConfigs.CLS + "Highest" + String.format("%02d", days) + "d",
//              Extreme.getNominalExtreme(MODEL_TYPES.STK, date,
//                      rawDataMap, 0, days, true));
//    }
//
//    for (int i = 0; i < WEEK_MULTIPIER_CLASS.length; i++) {
//      days = WEEK_MULTIPIER_CLASS[i] * DaysInWeek;
//      storageRow.put(GConfigs.CLS + "Lowest" + String.format("%02d", days) + "d",
//              Extreme.getNominalExtreme(MODEL_TYPES.STK, date,
//                      rawDataMap, 0, days, false));
//    }
  }

//  private static void addClusteredTrends(String code, String date,
//          ConcurrentHashMap<String, Object[]> rawDataMap,
//          HashMap<String, Object> storageRow) {
//    int days;
//    for (int i = 0; i < WEEK_MULTIPIER_CLASS.length; i++) {
//      days = WEEK_MULTIPIER_CLASS[i] * DaysInWeek;
//      storageRow.put(GConfigs.CLS + "CTrdHigh" + String.format("%02d", days) + "d",
//              StatCalculator.getNominalCluTrend(MODEL_TYPES.STK, date,
//                      rawDataMap, 0, days, true));
//
//    }
//
//    for (int i = 0; i < WEEK_MULTIPIER_CLASS.length; i++) {
//      days = WEEK_MULTIPIER_CLASS[i] * DaysInWeek;
//      storageRow.put(GConfigs.CLS + "CTrdLow" + String.format("%02d", days) + "d",
//              StatCalculator.getNominalCluTrend(MODEL_TYPES.STK, date,
//                      rawDataMap, 0, days, false));
//    }
//  }
  private void addExtremeValues(String date,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Object> storageRow) {
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

  private void addStableTrends(String date,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Object> storageRow) {
    storageRow.put(GConfigs.CLS + "Stable05d",
            StableTrend.getSTrendRatio(date, -6, 5, rawDataMap));
    storageRow.put(GConfigs.CLS + "Stable11d",
            StableTrend.getSTrendRatio(date, -11, 5, rawDataMap));
    storageRow.put(GConfigs.CLS + "Stable15d",
            StableTrend.getSTrendRatio(date, -16, 5, rawDataMap));
    storageRow.put(GConfigs.CLS + "Stable30d",
            StableTrend.getSTrendRatio(date, -31, 5, rawDataMap));
  }

  private void addSignals(String date,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Object> storageRow) {
    storageRow.put(GConfigs.CLS + "UpSignal" + String.format("%02d", 10) + "d",
            Signal.getUpSignal(MODEL_TYPES.STK, date, rawDataMap, 10));
    storageRow.put(GConfigs.CLS + "DownSignal" + String.format("%02d", 10) + "d",
            Signal.getDownSignal(MODEL_TYPES.STK, date, rawDataMap, 10));
  }

}
