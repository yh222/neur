package datapreparer.valuemaker;

import calculator.ADTM;
import calculator.CandleStick;
import calculator.Extreme;
import calculator.LWR;
import calculator.PriceMovingAverage;
import calculator.RelativeStrengthIndex;
import static core.GConfigs.DEFAULT_PATH;
import static core.GConfigs.IXIC;
import static core.GConfigs.WEEK_MULTIPIER_TRAIN;
import calculator.StatCalculator;
import core.GConfigs;
import datapreparer.valuemaker.indicie.IndicieValueMaker;
import static datapreparer.valuemaker.indicie.IndicieValueMaker.loadIndiciePastExtreme;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class STKTrainingValueMaker {

  private final static int DaysInWeek = 5;

  RelativeStrengthIndex m_RSI = new RelativeStrengthIndex();
  PriceMovingAverage m_MovingAverage = new PriceMovingAverage();
  String m_Code;
  //date, value
  ArrayList<String[]> m_DividendData;
  HashMap<String, String> m_WikiViewData;

  public STKTrainingValueMaker(String code) {
    m_Code = code;
    loadDividendData();
  }

  // storageRow will store new items created
  public void generateNumericTrainingValues(String date,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Object> storageRow) {
    addMomentums(date, rawDataMap, storageRow);
    addSupportResistance(date, rawDataMap, storageRow);
    addClusteredTrends(date, rawDataMap, storageRow);
    addVelocities(date, rawDataMap, storageRow);
    addCandleChartUnitCounts(date, rawDataMap, storageRow);
    //addCandlePattern(date, rawDataMap, storageRow);
    //addDividendData(date, storageRow);
    //addIndicieInfulences(date, storageRow);
    addEMAs(date, rawDataMap, storageRow);
    addSMAs(date, rawDataMap, storageRow);
    addSTDs(date, rawDataMap, storageRow);
    addRSIs(date, rawDataMap, storageRow);
    addMomtumDiff(date, rawDataMap, storageRow);
    addADTM(date, rawDataMap, storageRow);
    addLWR(date, rawDataMap, storageRow);
    addMACD(date, rawDataMap, storageRow);
    //addWikiViewCount(date, storageRow);
    addHistoryAvgDiff(date, rawDataMap, storageRow);
  }

  public void generateNominalTrainingValues(String date,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Object> storageRow) {
    storageRow.put(GConfigs.NOM + "Date", date);
    addSeasonOfYear(date, storageRow);
    //addDayOfWeek(date, storageRow);
    //addCandleUnitForDay(m_Code, date, rawDataMap, storageRow);
  }

  private void addMomentums(String date,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Object> storageRow) {

    int days = 3;
    for (int i = 0; i < 6; i++) {
      storageRow.put("Momentum_" + days + "d",
              StatCalculator.getMomentum(date, rawDataMap, days, 3));
      days = days + 3;
    }
    for (int i = 0; i < 4; i++) {
      days = days + 10;
      storageRow.put("Momentum_" + days + "d",
              StatCalculator.getMomentum(date, rawDataMap, days, 10));
    }
//    for (int i = 0; i < 3; i++) {
//      days = days + 30;
//      storageRow.put("Momentum_" + days + "d",
//              StatCalculator.getMomentum(date, rawDataMap, days, 30));
//    }

  }

  private static void addVelocities(String date,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Object> storageRow) {
//    storageRow.put("Velocity_5d",
//            StatCalculator.getVelocity(date, rawDataMap, 5));
    // 21 day (plus weekends) duration NASDAQ velocity
    storageRow.put("Velocity_21d",
            StatCalculator.getVelocity(date, rawDataMap, 21));
  }

  private void addSeasonOfYear(String date,
          HashMap<String, Object> storageRow) {
    storageRow.put(GConfigs.NOM + "Season",
            StatCalculator.getSeasonOfYear(date));
  }

  private void addSupportResistance(String date,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Object> storageRow) {

    storageRow.put("Resistance30d",
            Extreme.getExtremeRatio(
                    date, rawDataMap, 30, 20, true));
//    storageRow.put("Resistance60d",
//            Extreme.getExtremeRatio(
//                    date, rawDataMap, 60, 45, true));
//    storageRow.put("Resistance120d",
//            Extreme.getExtremeRatio(
//                    date, rawDataMap, 120, 100, true));
//    storageRow.put("Resistance240d",
//            Extreme.getExtremeRatio(
//                    date, rawDataMap, 240, 200, true));

    storageRow.put("Support30d",
            Extreme.getExtremeRatio(
                    date, rawDataMap, 30, 20, false));
//    storageRow.put("Support60d",
//            Extreme.getExtremeRatio(
//                    date, rawDataMap, 60, 45, false));
//    storageRow.put("Support120d",
//            Extreme.getExtremeRatio(
//                    date, rawDataMap, 120, 100, false));
//    storageRow.put("Support240d",
//            Extreme.getExtremeRatio(
//                    date, rawDataMap, 240, 200, false));

  }

  private void addClusteredTrends(String date,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Object> storageRow) {
    int days;
    for (int i = 0; i < WEEK_MULTIPIER_TRAIN.length; i++) {
      days = WEEK_MULTIPIER_TRAIN[i] * DaysInWeek;
      storageRow.put("CTrendHigh_" + days + "d",
              StatCalculator.getClusteredTrend(date, rawDataMap, days, days, true));
    }

    for (int i = 0; i < WEEK_MULTIPIER_TRAIN.length; i++) {
      days = WEEK_MULTIPIER_TRAIN[i] * DaysInWeek;
      storageRow.put("CTrendLow_" + days + "d",
              StatCalculator.getClusteredTrend(date, rawDataMap, days, days, false));
    }
  }

  private void addCandleChartUnitCounts(String date,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Object> storageRow) {
//    StatCalculator.getCandleChartUnits(code, date, rawDataMap, 7, 7,
//            storageRow);
    CandleStick.getCandleChartUnits(date, rawDataMap, 15, 15,
            storageRow);
    CandleStick.getCandleChartUnits(date, rawDataMap, 30, 30,
            storageRow);
//        StatCalculator.CountCandleChartUnits(code, date, rawDataMap, 60,
//                storageRow);
  }

  private void addCandlePattern(String date, ConcurrentHashMap<String, Object[]> rawDataMap, HashMap<String, Object> storageRow) {
    storageRow.put(GConfigs.NOM + "CandlePattern", CandleStick.getCandlePatternForDay(
            date, rawDataMap));
  }

  private void addCandleUnitForDay(String date,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Object> storageRow) {
    //int index = TRAINING_VALUES_NOMINAL.CadRead_1d.ordinal();
    for (int i = 1; i <= 3; i++) {
      storageRow.put(GConfigs.NOM + "CadRead_" + i + "d", CandleStick.getCandleUnitForDay(date, rawDataMap, i));
    }
  }

  private void addDividendData(String date, HashMap<String, Object> storageRow) {
    storageRow.put("DividendAmount", StatCalculator.getDividentAmt(
            date, m_DividendData));

    storageRow.put("DaysTillNextDividend", StatCalculator.getDaysTillNextDivdnt(
            date, m_DividendData));
  }

  private void loadDividendData() {
    if (m_DividendData == null) {
      File file = new File(DEFAULT_PATH + "//resources//" + m_Code
              + "//" + m_Code + "_Dividend.csv");
      ArrayList<String[]> l = new ArrayList();
      if (file.isFile()) {
        try (BufferedReader reader
                = new BufferedReader(new FileReader(file))) {
          String line;
          while ((line = reader.readLine()) != null) {
            String[] parts = line.split(",");
            // Data order: 1, date 2,dividend
            String[] data = new String[]{parts[0], parts[1]};
            l.add(data);
          }
          m_DividendData = l;
        } catch (Exception ex) {
          Logger.getLogger(STKTrainingValueMaker.class.getName())
                  .log(Level.SEVERE, null, ex);
        }
      }
    }
  }

  private void loadWikiViewData() {
    if (m_WikiViewData == null) {
      File file = new File(DEFAULT_PATH + "wikipedia//" + GConfigs.MODEL_TYPES.STK.name() + "//" + m_Code + "//" + m_Code + "_WikiView.csv");
      HashMap<String, String> map = new HashMap();
      if (file.isFile()) {
        try (BufferedReader reader
                = new BufferedReader(new FileReader(file))) {
          String line;
          while ((line = reader.readLine()) != null) {
            String[] parts = line.split(",");
            // Data order: 1, date 2, view count
            map.put(parts[0], parts[1]);
          }
          m_WikiViewData = map;
        } catch (Exception ex) {
          Logger.getLogger(STKTrainingValueMaker.class.getName())
                  .log(Level.SEVERE, null, ex);
        }
      }
    }
  }

  // Does not need rawDataMap as indicie data is get from static data keeper.
  private void addIndicieInfulences(String date,
          HashMap<String, Object> storageRow) {
    Object[] calculated_indicie_data = loadIndiciePastExtreme(IXIC, date);
    int days;
    for (int i = 0; i < WEEK_MULTIPIER_TRAIN.length; i++) {
      days = WEEK_MULTIPIER_TRAIN[i] * DaysInWeek;
      if (calculated_indicie_data == null) {
        storageRow.put("IXICCTrendLow_" + days + "d", 0.0);
      } else {
        storageRow.put("IXICCTrendLow_" + days + "d", calculated_indicie_data[i]);
      }
    }
  }

  private void addEMAs(String date, ConcurrentHashMap<String, Object[]> rawDataMap, HashMap<String, Object> storageRow) {
    try {
//      storageRow.put("EMA5", m_MovingAverage.getEMA(date, 5, rawDataMap));
//      storageRow.put("EMA10", m_MovingAverage.getEMA(date, 10, rawDataMap));
      storageRow.put("EMA20", m_MovingAverage.getEMA(date, 20, rawDataMap));
      storageRow.put("EMA50", m_MovingAverage.getEMA(date, 50, rawDataMap));
      //storageRow.put("EMA100", m_MovingAverage.getEMA(date, 100, rawDataMap));
      //storageRow.put("EMA200", m_MovingAverage.getEMA(date, 200, rawDataMap));
    } catch (ParseException ex) {
      Logger.getLogger(STKTrainingValueMaker.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  private void addSMAs(String date, ConcurrentHashMap<String, Object[]> rawDataMap, HashMap<String, Object> storageRow) {
    try {
      storageRow.put("SMA5", m_MovingAverage.getSMA(date, 5, rawDataMap));
      storageRow.put("SMA10", m_MovingAverage.getSMA(date, 10, rawDataMap));
      storageRow.put("SMA20", m_MovingAverage.getSMA(date, 20, rawDataMap));
      storageRow.put("SMA50", m_MovingAverage.getSMA(date, 50, rawDataMap));
      // storageRow.put("SMA100", m_MovingAverage.getSMA(date, 100, rawDataMap));
      //storageRow.put("SMA200", m_MovingAverage.getSMA(date, 200, rawDataMap));
    } catch (ParseException ex) {
      Logger.getLogger(STKTrainingValueMaker.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  private void addSTDs(String date, ConcurrentHashMap<String, Object[]> rawDataMap, HashMap<String, Object> storageRow) {
    try {
      Object std20_20 = m_MovingAverage.getSTD(date, 20, 20, rawDataMap);
      Object std20_25 = m_MovingAverage.getSTD(date, 25, 20, rawDataMap);
      storageRow.put("STD20", std20_20);
      storageRow.put("STDSLP20", StatCalculator.getSlope(std20_25, std20_20, 5));
      storageRow.put("STD50", m_MovingAverage.getSTD(date, 50, 50, rawDataMap));
      // storageRow.put("STD100", m_MovingAverage.getSTD(date, 100, 100, rawDataMap));
    } catch (ParseException ex) {
      Logger.getLogger(STKTrainingValueMaker.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  private void addRSIs(String date,
          ConcurrentHashMap<String, Object[]> rawDataMap,
          HashMap<String, Object> storageRow) {
    try {
      storageRow.put("RSI6", m_RSI.getRSI(date, 6, 6, rawDataMap));
      Object rsi24 = m_RSI.getRSI(date, 24, 24, rawDataMap);
      Object rsi24_5 = m_RSI.getRSI(date, 24, 29, rawDataMap);
      storageRow.put("RSI24SLP5", StatCalculator.getSlope(rsi24, rsi24_5, 5));
      storageRow.put("RSI24", m_RSI.getRSI(date, 24, 24, rawDataMap));
      //storageRow.put("RSI30", m_RSI.getRSI(date, 30, 0, rawDataMap));
      Object rsi50 = m_RSI.getRSI(date, 50, 50, rawDataMap);
      Object rsi50_5 = m_RSI.getRSI(date, 55, 50, rawDataMap);
      storageRow.put("RSI50SLP5", StatCalculator.getSlope(rsi50, rsi50_5, 5));
      storageRow.put("RSI50", m_RSI.getRSI(date, 50, 50, rawDataMap));
      storageRow.put("RSI10_P10", m_RSI.getRSI(date, 20, 10, rawDataMap));
      storageRow.put("RSI20_P20", m_RSI.getRSI(date, 40, 20, rawDataMap));

    } catch (ParseException ex) {
      Logger.getLogger(STKTrainingValueMaker.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  private void addWikiViewCount(String date, HashMap<String, Object> storageRow) {
    loadWikiViewData();
    Object o = m_WikiViewData.get(date);
    if (o == null) {
      o = "?";
    }
    storageRow.put("WikiViewCount", o);
  }

  private void addMomtumDiff(String date, ConcurrentHashMap<String, Object[]> rawDataMap, HashMap<String, Object> storageRow) {

    int days;
    for (int i = 0; i < WEEK_MULTIPIER_TRAIN.length; i++) {
      days = WEEK_MULTIPIER_TRAIN[i] * DaysInWeek;
      storageRow.put("IXICMomDiff_" + days, IndicieValueMaker.getMomtumDifference(
              IXIC, rawDataMap, date, days, days));
    }

//    String rev_code = REVELANT_INDICIES.get(m_Code);
//    for (int i = 0; i < WEEK_MULTIPIER_TRAIN.length; i++) {
//      days = WEEK_MULTIPIER_TRAIN[i] * DaysInWeek;
//      storageRow.put("REVMomDiff_" + days, IndicieValueMaker.getMomtumDifference(
//              rev_code, rawDataMap, date, days, days));
//    }
  }

  private void addHistoryAvgDiff(String date, ConcurrentHashMap<String, Object[]> rawDataMap, HashMap<String, Object> storageRow) {
//    storageRow.put("HistoryAvgDiff_10", StatCalculator.getHistoryAvgDiff(
//            date, rawDataMap, 10));

    storageRow.put("HistoryAvgDiff30", StatCalculator.getHistoryAvgDiff(
            date, rawDataMap, 30, 30));

   // storageRow.put("HistoryAvgDiff90", StatCalculator.getHistoryAvgDiff(
    //       date, rawDataMap, 90, 90));
    //storageRow.put("HistoryAvgDiff90_90", StatCalculator.getHistoryAvgDiff(
    //        date, rawDataMap, 180, 90));
   // storageRow.put("HistoryAvgDiff180", StatCalculator.getHistoryAvgDiff(
    //       date, rawDataMap, 180, 180));
   // storageRow.put("HistoryAvgDiff180_90", StatCalculator.getHistoryAvgDiff(
    //         date, rawDataMap, 270, 180));
  }

  private void addADTM(String date, ConcurrentHashMap<String, Object[]> rawDataMap, HashMap<String, Object> storageRow) {
    try {
      Object adtm23 = ADTM.getADTM(date, 23, rawDataMap);
      Object maadtm23 = ADTM.getMAADTM(date, 8, 23, rawDataMap);
      storageRow.put("ADTM23day", adtm23);
      storageRow.put("MAADTM8ma23day", maadtm23);
    } catch (ParseException ex) {
      Logger.getLogger(STKTrainingValueMaker.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  private void addLWR(String date, ConcurrentHashMap<String, Object[]> rawDataMap, HashMap<String, Object> storageRow) {
    Object lwr9 = LWR.getLWR1(date, rawDataMap, 3, 9, 9);
    storageRow.put("LWR3ma9day", lwr9);
//    Object lwr9_14 = LWR.getLWR1(date, rawDataMap, 3, 9, 14);
//    storageRow.put("LWR3ma9daySLP", StatCalculator.getSlope(lwr9, lwr9_14, 5));
  }

  private void addMACD(String date, ConcurrentHashMap<String, Object[]> rawDataMap, HashMap<String, Object> storageRow) {
    try {
      storageRow.put("MACD12To26m9", m_MovingAverage.getMACD(date, 12, 26, 9, rawDataMap));
      storageRow.put("DIF12To26", m_MovingAverage.getDIF(date, 12, 26, rawDataMap));
      storageRow.put("DEA12To26m9", m_MovingAverage.getDEA(date, 12, 26, 9, rawDataMap));
    } catch (ParseException ex) {
      Logger.getLogger(STKTrainingValueMaker.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

}
