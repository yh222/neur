package datapreparer.valuemaker;

import calculator.MovingAverage;
import calculator.RelativeStrengthIndex;
import static core.GlobalConfigs.DEFAULT_PATH;
import static core.GlobalConfigs.IXIC;
import static core.GlobalConfigs.GSPC;
import static core.GlobalConfigs.REVELANT_INDICIES;
import core.GlobalConfigs.TRAINING_VALUES_NOMINAL;
import core.GlobalConfigs.TRAINING_VALUES_NUMERIC;
import static core.GlobalConfigs.WEEK_MULTIPIER;
import calculator.StatCalculator;
import static datapreparer.valuemaker.indicie.IndicieValueMaker.loadIndicieData;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TrainingValueMaker {

    RelativeStrengthIndex m_RSI = new RelativeStrengthIndex();
    MovingAverage m_MovingAverage = new MovingAverage();
    String m_Code;
    //date, value
    ArrayList<String[]> m_DividendData;

    public TrainingValueMaker(String code) {
        m_Code = code;
        loadDividendData();
    }

    // storageRow will store new items created
    public void generateNumericTrainingValues(String date,
            ConcurrentHashMap<String, Object[]> rawDataMap,
            Object[] storageRow) {
        addRawTrends(date, rawDataMap, storageRow);
        addExtremeValue(date, rawDataMap, storageRow,
                TRAINING_VALUES_NUMERIC.PASTHIGH_7d.ordinal(),
                TRAINING_VALUES_NUMERIC.PASTLOW_7d.ordinal());
        addClusteredTrends(date, rawDataMap, storageRow);
        addVelocities(date, rawDataMap, storageRow);
        addCandleChartUnitCounts(m_Code, date, rawDataMap, storageRow);
        addDividendData(date, storageRow);
        addIndicieInfulences(date, storageRow);
        addEMAs(date, rawDataMap, storageRow);
        addSMAs(date, rawDataMap, storageRow);
        addRSIs(date, rawDataMap, storageRow);
    }

    public void generateNominalTrainingValues(String date,
            ConcurrentHashMap<String, Object[]> rawDataMap,
            Object[] storageRow) {
        addSeasonOfYear(date, storageRow);
        addDayOfWeek(date, storageRow);
        addCandleUnitForDay(m_Code, date, rawDataMap, storageRow);
    }

    private void addRawTrends(String date,
            ConcurrentHashMap<String, Object[]> rawDataMap,
            Object[] storageRow) {
        storageRow[TRAINING_VALUES_NUMERIC.RAWTREND_0d.ordinal()]
                = StatCalculator.CalculateRawTrend(date, rawDataMap, 0, 0);
        storageRow[TRAINING_VALUES_NUMERIC.RAWTREND_1d.ordinal()]
                = StatCalculator.CalculateRawTrend(date, rawDataMap, 1, 0);
        storageRow[TRAINING_VALUES_NUMERIC.RAWTREND_3d.ordinal()]
                = StatCalculator.CalculateRawTrend(date, rawDataMap, 3, 3);
        int index = TRAINING_VALUES_NUMERIC.RAWTREND_7d.ordinal();
        int days;
        for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
            days = WEEK_MULTIPIER[i] * 7;
            storageRow[index + i] = StatCalculator.
                    CalculateRawTrend(date, rawDataMap, days, days);
        }
    }

    private static void addVelocities(String date,
            ConcurrentHashMap<String, Object[]> rawDataMap,
            Object[] storageRow) {
        storageRow[TRAINING_VALUES_NUMERIC.VELOCITY_15d.ordinal()]
                = StatCalculator.CalcluateVelocity(date, rawDataMap, 15);
        // 21 day (plus weekends) duration NASDAQ velocity
        storageRow[TRAINING_VALUES_NUMERIC.VELOCITY_30d.ordinal()]
                = StatCalculator.CalcluateVelocity(date, rawDataMap, 30);
    }

    private void addSeasonOfYear(String date, Object[] storageRow) {
        storageRow[TRAINING_VALUES_NOMINAL.SEASON_YEAR.ordinal()]
                = StatCalculator.CalculateSeasonOfYear(date);
    }

    private void addExtremeValue(String date,
            ConcurrentHashMap<String, Object[]> rawDataMap,
            Object[] storageRow, int ind1, int ind2) {
        int index = ind1;
        int days;
        for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
            days = WEEK_MULTIPIER[i] * 7;
            storageRow[index + i] = StatCalculator.
                    CalculateExtremeInPeriod(
                            date, rawDataMap, days, days, true);
        }
        index = ind2;
        for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
            days = WEEK_MULTIPIER[i] * 7;
            storageRow[index + i] = StatCalculator.
                    CalculateExtremeInPeriod(
                            date, rawDataMap, days, days, false);
        }
    }

    private void addDayOfWeek(String date, Object[] storageRow) {
        storageRow[TRAINING_VALUES_NOMINAL.DAY_OF_WEEK.ordinal()]
                = StatCalculator.CalculateDayOfWeek(date);
    }

    private void addClusteredTrends(String date,
            ConcurrentHashMap<String, Object[]> rawDataMap,
            Object[] storageRow) {
        int index = TRAINING_VALUES_NUMERIC.CLUTRENDHIGH_7d.ordinal();
        int days;
        for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
            days = WEEK_MULTIPIER[i] * 7;
            storageRow[index + i] = StatCalculator.
                    CalculateClusteredTrend(date, rawDataMap, days, days, true);
        }
        index = TRAINING_VALUES_NUMERIC.CLUTRENDLOW_7d.ordinal();
        for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
            days = WEEK_MULTIPIER[i] * 7;
            storageRow[index + i] = StatCalculator.
                    CalculateClusteredTrend(
                            date, rawDataMap, days, days, false);
        }
    }

    private void addCandleChartUnitCounts(String code, String date,
            ConcurrentHashMap<String, Object[]> rawDataMap,
            Object[] storageRow) {
        StatCalculator.CountCandleChartUnits(code, date, rawDataMap, 7,
                storageRow,
                TRAINING_VALUES_NUMERIC.GreatW_7d.ordinal());
        StatCalculator.CountCandleChartUnits(code, date, rawDataMap, 15,
                storageRow,
                TRAINING_VALUES_NUMERIC.GreatW_15d.ordinal());
        StatCalculator.CountCandleChartUnits(code, date, rawDataMap, 30,
                storageRow,
                TRAINING_VALUES_NUMERIC.GreatW_30d.ordinal());
        StatCalculator.CountCandleChartUnits(code, date, rawDataMap, 60,
                storageRow,
                TRAINING_VALUES_NUMERIC.GreatW_60d.ordinal());
    }

    private void addCandleUnitForDay(String code, String date,
            ConcurrentHashMap<String, Object[]> rawDataMap,
            Object[] storageRow) {
        int index = TRAINING_VALUES_NOMINAL.CadRead_1d.ordinal();
        for (int i = 0; i < 30; i++) {
            storageRow[index + i] = StatCalculator.
                    CalculateCandleUnitForDay(code, date, rawDataMap, i);
        }
    }

    private void addDividendData(String date, Object[] storageRow) {
        storageRow[TRAINING_VALUES_NUMERIC.DividendAmount.ordinal()]
                = StatCalculator.CalculateDividentAmt(
                        date, m_DividendData);
        storageRow[TRAINING_VALUES_NUMERIC.DaysTillNextDividend.ordinal()]
                = StatCalculator.
                CalculateDaysTillNextDivdnt(date, m_DividendData);
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

    // Does not need rawDataMap as indicie data is get from static data keeper.
    private void addIndicieInfulences(String date,
            Object[] storageRow) {
        Object[] calculated_indicie_data
                = loadIndicieData(IXIC, date);
        int index = TRAINING_VALUES_NUMERIC.IXICCLUTRENDHIGH_7d.ordinal();
        for (int i = 0; i < WEEK_MULTIPIER.length * 2; i++) {
            if (calculated_indicie_data == null) {
                storageRow[index + i] = 0.0;
            } else {
                storageRow[index + i] = calculated_indicie_data[i];
            }

        }

        calculated_indicie_data = loadIndicieData(GSPC, date);
        index = TRAINING_VALUES_NUMERIC.GSPCCLUTRENDHIGH_7d.ordinal();
        for (int i = 0; i < WEEK_MULTIPIER.length * 2; i++) {
            if (calculated_indicie_data == null) {
                storageRow[index + i] = 0.0;
            } else {
                storageRow[index + i] = calculated_indicie_data[i];
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

        index = TRAINING_VALUES_NUMERIC.REVCLUTRENDHIGH_7d.ordinal();
        for (int i = 0;
                i < WEEK_MULTIPIER.length * 2; i++) {
            if (calculated_indicie_data == null) {
                storageRow[index + i] = 0.0;
            } else {
                storageRow[index + i] = calculated_indicie_data[i];
            }
        }
    }

    private void addEMAs(String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow) {
        try {
            storageRow[TRAINING_VALUES_NUMERIC.EMA5.ordinal()]
                    = m_MovingAverage.getEMA(date, 5, rawDataMap);
            storageRow[TRAINING_VALUES_NUMERIC.EMA10.ordinal()]
                    = m_MovingAverage.getEMA(date, 10, rawDataMap);
            storageRow[TRAINING_VALUES_NUMERIC.EMA20.ordinal()]
                    = m_MovingAverage.getEMA(date, 20, rawDataMap);
            storageRow[TRAINING_VALUES_NUMERIC.EMA50.ordinal()]
                    = m_MovingAverage.getEMA(date, 50, rawDataMap);
            storageRow[TRAINING_VALUES_NUMERIC.EMA100.ordinal()]
                    = m_MovingAverage.getEMA(date, 100, rawDataMap);
            storageRow[TRAINING_VALUES_NUMERIC.EMA200.ordinal()]
                    = m_MovingAverage.getEMA(date, 200, rawDataMap);
        } catch (ParseException ex) {
            Logger.getLogger(TrainingValueMaker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void addSMAs(String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow) {
        try {
            storageRow[TRAINING_VALUES_NUMERIC.SMA5.ordinal()]
                    = m_MovingAverage.getSMA(date, 5, rawDataMap);
            storageRow[TRAINING_VALUES_NUMERIC.SMA10.ordinal()]
                    = m_MovingAverage.getSMA(date, 10, rawDataMap);
            storageRow[TRAINING_VALUES_NUMERIC.SMA20.ordinal()]
                    = m_MovingAverage.getSMA(date, 20, rawDataMap);
            storageRow[TRAINING_VALUES_NUMERIC.SMA50.ordinal()]
                    = m_MovingAverage.getSMA(date, 50, rawDataMap);
            storageRow[TRAINING_VALUES_NUMERIC.SMA100.ordinal()]
                    = m_MovingAverage.getSMA(date, 100, rawDataMap);
            storageRow[TRAINING_VALUES_NUMERIC.SMA200.ordinal()]
                    = m_MovingAverage.getSMA(date, 200, rawDataMap);
        } catch (ParseException ex) {
            Logger.getLogger(TrainingValueMaker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void addRSIs(String date,
            ConcurrentHashMap<String, Object[]> rawDataMap,
            Object[] storageRow) {
        try {
            storageRow[TRAINING_VALUES_NUMERIC.RSI10.ordinal()]
                    = m_RSI.getRSI(date, 10, 0, rawDataMap);
            storageRow[TRAINING_VALUES_NUMERIC.RSI20.ordinal()]
                    = m_RSI.getRSI(date, 20, 0, rawDataMap);
            storageRow[TRAINING_VALUES_NUMERIC.RSI30.ordinal()]
                    = m_RSI.getRSI(date, 30, 0, rawDataMap);
            storageRow[TRAINING_VALUES_NUMERIC.RSI50.ordinal()]
                    = m_RSI.getRSI(date, 50, 0, rawDataMap);
            //
            storageRow[TRAINING_VALUES_NUMERIC.RSI10_P10.ordinal()]
                    = m_RSI.getRSI(date, 10, 10, rawDataMap);
            storageRow[TRAINING_VALUES_NUMERIC.RSI20_P20.ordinal()]
                    = m_RSI.getRSI(date, 20, 20, rawDataMap);

        } catch (ParseException ex) {
            Logger.getLogger(TrainingValueMaker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
