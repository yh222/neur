package datapreparer.valuemaker;

import core.GlobalConfigs;
import datapreparer.StatCalculator;
import java.util.concurrent.ConcurrentHashMap;

public class TrainingValueMaker {

    protected static final int[] WEEK_MULTIPIER = new int[]{1, 2, 4, 7, 9, 12};

    // storageRow will store new items created
    public static void generateTrainingValues(String code, String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow) {
        //Input data
        addRawTrends(date, rawDataMap, storageRow);
        addVelocities(date, rawDataMap, storageRow);
        addSeasonOfYear(date, storageRow);
        addExtremeClass(date, rawDataMap, storageRow);
        addDayOfWeek(date, storageRow);
        addClusteredTrends(date, rawDataMap, storageRow);
        addCandleChartUnitCounts(code, date, rawDataMap, storageRow);
        addCandleUnitForDay(code, date, rawDataMap, storageRow);
    }

    private static void addRawTrends(String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow) {
        storageRow[GlobalConfigs.TRAINING_VALUES.RAWTREND_0d.ordinal()] = StatCalculator.CalculateRawTrend(date, rawDataMap, 0, 0);
        storageRow[GlobalConfigs.TRAINING_VALUES.RAWTREND_1d.ordinal()] = StatCalculator.CalculateRawTrend(date, rawDataMap, 1, 0);
        storageRow[GlobalConfigs.TRAINING_VALUES.RAWTREND_3d.ordinal()] = StatCalculator.CalculateRawTrend(date, rawDataMap, 3, 3);
        int index = GlobalConfigs.TRAINING_VALUES.RAWTREND_7d.ordinal();
        int days;
        for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
            days = WEEK_MULTIPIER[i] * 7;
            storageRow[index + i] = StatCalculator.CalculateRawTrend(date, rawDataMap, days, days);
        }
    }

    private static void addVelocities(String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow) {
        storageRow[GlobalConfigs.TRAINING_VALUES.VELOCITY_15d.ordinal()] = StatCalculator.CalcluateVelocity(date, rawDataMap, 15);
        // 21 day (plus weekends) duration NASDAQ velocity
        storageRow[GlobalConfigs.TRAINING_VALUES.VELOCITY_30d.ordinal()] = StatCalculator.CalcluateVelocity(date, rawDataMap, 30);
    }

    private static void addSeasonOfYear(String date, Object[] storageRow) {
        storageRow[GlobalConfigs.TRAINING_VALUES.SEASON_YEAR.ordinal()] = StatCalculator.CalculateSeasonOfYear(date);
    }

    private static void addExtremeClass(String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow) {
        int index = GlobalConfigs.TRAINING_VALUES.PASTHIGH_7d.ordinal();
        int days;
        for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
            days = WEEK_MULTIPIER[i] * 7;
            storageRow[index + i] = StatCalculator.CalculateExtremeInPeriod(date, rawDataMap, days, days, true);
        }
        index = GlobalConfigs.TRAINING_VALUES.PASTLOW_7d.ordinal();
        for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
            days = WEEK_MULTIPIER[i] * 7;
            storageRow[index + i] = StatCalculator.CalculateExtremeInPeriod(date, rawDataMap, days, days, false);
        }
    }

    private static void addDayOfWeek(String date, Object[] storageRow) {
        storageRow[GlobalConfigs.TRAINING_VALUES.DAY_OF_WEEK.ordinal()] = StatCalculator.CalculateDayOfWeek(date);
    }

    private static void addClusteredTrends(String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow) {
        int index = GlobalConfigs.TRAINING_VALUES.CLUTRENDHIGH_7d.ordinal();
        int days;
        for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
            days = WEEK_MULTIPIER[i] * 7;
            storageRow[index + i] = StatCalculator.CalculateClusteredTrend(date, rawDataMap, days, days, true);
        }
        index = GlobalConfigs.TRAINING_VALUES.CLUTRENDLOW_7d.ordinal();
        for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
            days = WEEK_MULTIPIER[i] * 7;
            storageRow[index + i] = StatCalculator.CalculateClusteredTrend(date, rawDataMap, days, days, false);
        }
    }

    private static void addCandleChartUnitCounts(String code, String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow) {
        StatCalculator.CountCandleChartUnits(code,date, rawDataMap, 7, storageRow, GlobalConfigs.TRAINING_VALUES.GreatW_7d.ordinal());
        StatCalculator.CountCandleChartUnits(code,date, rawDataMap, 15, storageRow, GlobalConfigs.TRAINING_VALUES.GreatW_15d.ordinal());
        StatCalculator.CountCandleChartUnits(code,date, rawDataMap, 30, storageRow, GlobalConfigs.TRAINING_VALUES.GreatW_30d.ordinal());
        StatCalculator.CountCandleChartUnits(code,date, rawDataMap, 60, storageRow, GlobalConfigs.TRAINING_VALUES.GreatW_60d.ordinal());
    }

    private static void addCandleUnitForDay(String code, String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow) {
        int index = GlobalConfigs.TRAINING_VALUES.CadRead_1d.ordinal();
        for (int i = 0; i < 18; i++) {
            storageRow[index + i] = StatCalculator.CalculateCandleUnitForDay(code,date, rawDataMap, i);
        }
    }

}
