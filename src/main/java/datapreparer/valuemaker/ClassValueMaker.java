package datapreparer.valuemaker;

import core.GlobalConfigs;
import static core.GlobalConfigs.WEEK_MULTIPIER;
import calculator.StatCalculator;
import java.util.concurrent.ConcurrentHashMap;

public class ClassValueMaker {

    public static void generateCalssValues(String code, String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow) {
        addExtremeClass(code, date, rawDataMap, storageRow);
        addClusteredTrends(code, date, rawDataMap, storageRow);
    }

    private static void addExtremeClass(String code, String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow) {
        int index = GlobalConfigs.CLASS_VALUES.FHighest7d.ordinal();
        int days;
        for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
            days = WEEK_MULTIPIER[i] * 7;
            storageRow[index + i] = StatCalculator.CalculateNominalExtreme(code, date, rawDataMap, 0, days - 1, true);
        }
        index = GlobalConfigs.CLASS_VALUES.FLowest7d.ordinal();
        for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
            days = WEEK_MULTIPIER[i] * 7;
            storageRow[index + i] = StatCalculator.CalculateNominalExtreme(code, date, rawDataMap, 0, days - 1, false);
        }
    }

    private static void addClusteredTrends(String code, String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow) {
        int index = GlobalConfigs.CLASS_VALUES.FCTrdHigh7d.ordinal();
        int days;
        for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
            days = WEEK_MULTIPIER[i] * 7;
            storageRow[index + i] = StatCalculator.CalculateNominalCluTrend(code, date, rawDataMap, 0, days - 1, true);
        }

        index = GlobalConfigs.CLASS_VALUES.FCTrdLow7d.ordinal();
        for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
            days = WEEK_MULTIPIER[i] * 7;
            storageRow[index + i] = StatCalculator.CalculateNominalCluTrend(code, date, rawDataMap, 0, days - 1, false);
        }
    }
}
