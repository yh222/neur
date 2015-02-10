package datapreparer.valuemaker;

import calculator.MovingAverage;
import calculator.RelativeStrengthIndex;
import static core.GlobalConfigs.DEFAULT_PATH;
import static core.GlobalConfigs.IXIC;
import static core.GlobalConfigs.GSPC;
import static core.GlobalConfigs.REVELANT_INDICIES;
import static core.GlobalConfigs.WEEK_MULTIPIER;
import calculator.StatCalculator;
import static datapreparer.valuemaker.indicie.IndicieValueMaker.loadIndicieData;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TrainingValueMaker {

    RelativeStrengthIndex m_RSI = new RelativeStrengthIndex();
    MovingAverage m_MovingAverage = new MovingAverage();
    String m_Code;
    //date, value
    ArrayList<String[]> m_DividendData;
    HashMap<String, String> m_WikiViewData;

    public TrainingValueMaker(String code) {
        m_Code = code;
        loadDividendData();
        loadWikiViewData();
    }

    public static final String nom = "N_";

    // storageRow will store new items created
    public void generateNumericTrainingValues(String date,
            ConcurrentHashMap<String, Object[]> rawDataMap,
            HashMap<String, Object> storageRow) {
        addMomentums(date, rawDataMap, storageRow);
        addExtremeValue(date, rawDataMap, storageRow);
        addClusteredTrends(date, rawDataMap, storageRow);
        addVelocities(date, rawDataMap, storageRow);
        addCandleChartUnitCounts(m_Code, date, rawDataMap, storageRow);
        addDividendData(date, storageRow);
        addIndicieInfulences(date, storageRow);
        addEMAs(date, rawDataMap, storageRow);
        addSMAs(date, rawDataMap, storageRow);
        addRSIs(date, rawDataMap, storageRow);
        //addWikiViewCount(date, storageRow);
    }

    public void generateNominalTrainingValues(String date,
            ConcurrentHashMap<String, Object[]> rawDataMap,
            HashMap<String, Object> storageRow) {
        addSeasonOfYear(date, storageRow);
        addDayOfWeek(date, storageRow);
        //addCandleUnitForDay(m_Code, date, rawDataMap, storageRow);
    }

    private void addMomentums(String date,
            ConcurrentHashMap<String, Object[]> rawDataMap,
            HashMap<String, Object> storageRow) {
        storageRow.put("Momentum_0d",
                StatCalculator.CalculateMomentum(date, rawDataMap, 0, 0));
        storageRow.put("Momentum_1d",
                StatCalculator.CalculateMomentum(date, rawDataMap, 1, 0));
        storageRow.put("Momentum_3d",
                StatCalculator.CalculateMomentum(date, rawDataMap, 3, 3));

        int days;
        for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
            days = WEEK_MULTIPIER[i] * 7;
            storageRow.put("Momentum_" + days + "d",
                    StatCalculator.CalculateMomentum(date, rawDataMap, days, days));
        }
    }

    private static void addVelocities(String date,
            ConcurrentHashMap<String, Object[]> rawDataMap,
            HashMap<String, Object> storageRow) {
        storageRow.put("Velocity_5d",
                StatCalculator.CalcluateVelocity(date, rawDataMap, 5));
        // 21 day (plus weekends) duration NASDAQ velocity
        storageRow.put("Velocity_21d",
                StatCalculator.CalcluateVelocity(date, rawDataMap, 21));
    }

    private void addSeasonOfYear(String date,
            HashMap<String, Object> storageRow) {
        storageRow.put(nom + "Season",
                StatCalculator.CalculateSeasonOfYear(date));
    }

    private void addExtremeValue(String date,
            ConcurrentHashMap<String, Object[]> rawDataMap,
            HashMap<String, Object> storageRow) {
        int days;
        for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
            days = WEEK_MULTIPIER[i] * 7;
            storageRow.put("PastHigh_" + days + "d",
                    StatCalculator.CalculateExtremeInPeriod(
                            date, rawDataMap, days, days, true));
        }

        for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
            days = WEEK_MULTIPIER[i] * 7;
            storageRow.put("PastLow_" + days + "d",
                    StatCalculator.CalculateExtremeInPeriod(
                            date, rawDataMap, days, days, false));
        }
    }

    private void addDayOfWeek(String date, HashMap<String, Object> storageRow) {
        storageRow.put(nom + "DayOfWeek",
                StatCalculator.CalculateDayOfWeek(date));
    }

    private void addClusteredTrends(String date,
            ConcurrentHashMap<String, Object[]> rawDataMap,
            HashMap<String, Object> storageRow) {
        int days;
        for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
            days = WEEK_MULTIPIER[i] * 7;
            storageRow.put("CTrendHigh_" + days + "d", StatCalculator.
                    CalculateClusteredTrend(date, rawDataMap, days, days, true));
        }

        for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
            days = WEEK_MULTIPIER[i] * 7;
            storageRow.put("CTrendLow_" + days + "d", StatCalculator.
                    CalculateClusteredTrend(date, rawDataMap, days, days, false));
        }
    }

    private void addCandleChartUnitCounts(String code, String date,
            ConcurrentHashMap<String, Object[]> rawDataMap,
            HashMap<String, Object> storageRow) {
        StatCalculator.CountCandleChartUnits(code, date, rawDataMap, 7,
                storageRow);
        StatCalculator.CountCandleChartUnits(code, date, rawDataMap, 15,
                storageRow);
        StatCalculator.CountCandleChartUnits(code, date, rawDataMap, 30,
                storageRow);
//        StatCalculator.CountCandleChartUnits(code, date, rawDataMap, 60,
//                storageRow);
    }

    private void addCandleUnitForDay(String code, String date,
            ConcurrentHashMap<String, Object[]> rawDataMap,
            HashMap<String, Object> storageRow) {
        //int index = TRAINING_VALUES_NOMINAL.CadRead_1d.ordinal();
        for (int i = 1; i <= 60; i++) {
            storageRow.put(nom + "CadRead_" + i + "d", StatCalculator.
                    CalculateCandleUnitForDay(code, date, rawDataMap, i));
        }
    }

    private void addDividendData(String date, HashMap<String, Object> storageRow) {

        storageRow.put("DividendAmount", StatCalculator.CalculateDividentAmt(
                date, m_DividendData));

        storageRow.put("DaysTillNextDividend", StatCalculator.CalculateDaysTillNextDivdnt(
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
                    Logger.getLogger(TrainingValueMaker.class.getName())
                            .log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private void loadWikiViewData() {
        if (m_WikiViewData == null) {
            File file = new File(DEFAULT_PATH + "//resources//" + m_Code
                    + "//" + m_Code + "_WikiView.csv");
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
                    Logger.getLogger(TrainingValueMaker.class.getName())
                            .log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    // Does not need rawDataMap as indicie data is get from static data keeper.
    private void addIndicieInfulences(String date,
            HashMap<String, Object> storageRow) {
        Object[] calculated_indicie_data
                = loadIndicieData(IXIC, date);
        int days;
        for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
            days = WEEK_MULTIPIER[i] * 7;
            if (calculated_indicie_data == null) {
                storageRow.put("IXICCTrendLow_" + days + "d", 0.0);

            } else {
                storageRow.put("IXICCTrendLow_" + days + "d", calculated_indicie_data[i]);
            }
        }

        calculated_indicie_data = loadIndicieData(GSPC, date);
        for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
            days = WEEK_MULTIPIER[i] * 7;
            if (calculated_indicie_data == null) {
                storageRow.put("GSPCCTrendLow_" + days + "d", 0.0);

            } else {
                storageRow.put("GSPCCTrendLow_" + days + "d", calculated_indicie_data[i]);
            }
        }

        String rev_code = REVELANT_INDICIES.get(m_Code);
        if (rev_code
                != null) {
            calculated_indicie_data
                    = loadIndicieData(rev_code, date);
        } else {
            calculated_indicie_data = new Object[WEEK_MULTIPIER.length * 2];
            for (int i = 0; i < WEEK_MULTIPIER.length * 2; i++) {
                calculated_indicie_data[i] = 0.0;
            }
        }

        for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
            days = WEEK_MULTIPIER[i] * 7;
            if (calculated_indicie_data == null) {
                storageRow.put("REVCTrendLow_" + days + "d", 0.0);

            } else {
                storageRow.put("REVCTrendLow_" + days + "d", calculated_indicie_data[i]);
            }
        }

    }

    private void addEMAs(String date, ConcurrentHashMap<String, Object[]> rawDataMap, HashMap<String, Object> storageRow) {
        try {
            storageRow.put("EMA5", m_MovingAverage.getEMA(date, 5, rawDataMap));
            storageRow.put("EMA10", m_MovingAverage.getEMA(date, 10, rawDataMap));
            storageRow.put("EMA20", m_MovingAverage.getEMA(date, 20, rawDataMap));
            storageRow.put("EMA50", m_MovingAverage.getEMA(date, 50, rawDataMap));
            storageRow.put("EMA100", m_MovingAverage.getEMA(date, 100, rawDataMap));
            //storageRow.put("EMA200", m_MovingAverage.getEMA(date, 200, rawDataMap));
        } catch (ParseException ex) {
            Logger.getLogger(TrainingValueMaker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void addSMAs(String date, ConcurrentHashMap<String, Object[]> rawDataMap, HashMap<String, Object> storageRow) {
        try {
            storageRow.put("SMA5", m_MovingAverage.getSMA(date, 5, rawDataMap));
            storageRow.put("SMA10", m_MovingAverage.getSMA(date, 10, rawDataMap));
            storageRow.put("SMA20", m_MovingAverage.getSMA(date, 20, rawDataMap));
            storageRow.put("SMA50", m_MovingAverage.getSMA(date, 50, rawDataMap));
            storageRow.put("SMA100", m_MovingAverage.getSMA(date, 100, rawDataMap));
            //storageRow.put("SMA200", m_MovingAverage.getSMA(date, 200, rawDataMap));
        } catch (ParseException ex) {
            Logger.getLogger(TrainingValueMaker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void addRSIs(String date,
            ConcurrentHashMap<String, Object[]> rawDataMap,
            HashMap<String, Object> storageRow) {
        try {
            storageRow.put("RSI10", m_RSI.getRSI(date, 10, 0, rawDataMap));
            storageRow.put("RSI20", m_RSI.getRSI(date, 20, 0, rawDataMap));
            storageRow.put("RSI30", m_RSI.getRSI(date, 30, 0, rawDataMap));
            storageRow.put("RSI50", m_RSI.getRSI(date, 50, 0, rawDataMap));
            storageRow.put("RSI10_P10", m_RSI.getRSI(date, 10, 10, rawDataMap));
            storageRow.put("RSI20_P20", m_RSI.getRSI(date, 20, 20, rawDataMap));

        } catch (ParseException ex) {
            Logger.getLogger(TrainingValueMaker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void addWikiViewCount(String date, HashMap<String, Object> storageRow) {
        storageRow.put("WikiViewCount", m_WikiViewData.get(date));
    }

}
