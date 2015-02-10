package datapreparer.valuemaker;

import static core.GlobalConfigs.WEEK_MULTIPIER;
import calculator.StatCalculator;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class ClassValueMaker {

    //Special marker for class values. used to seperate them from training values.
    public static final String cls = "C_";

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
        storageRow.put(cls + "Highest3d",
                StatCalculator.CalculateNominalExtreme(code, date,
                        rawDataMap, 0, 3 - 1, true));
        for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
            days = WEEK_MULTIPIER[i] * 7;
            storageRow.put(cls + "Highest" + days + "d",
                    StatCalculator.CalculateNominalExtreme(code, date,
                            rawDataMap, 0, days - 1, true));
        }
        
        storageRow.put(cls + "Lowest3d",
                StatCalculator.CalculateNominalExtreme(code, date,
                        rawDataMap, 0, 3 - 1, false));
        for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
            days = WEEK_MULTIPIER[i] * 7;
            storageRow.put(cls + "Lowest" + days + "d",
                    StatCalculator.CalculateNominalExtreme(code, date,
                            rawDataMap, 0, days - 1, false));
        }
    }

    private static void addClusteredTrends(String code, String date,
            ConcurrentHashMap<String, Object[]> rawDataMap,
            HashMap<String, Object> storageRow) {
        int days;
        for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
            days = WEEK_MULTIPIER[i] * 7;
            storageRow.put(cls + "CTrdHigh" + days + "d",
                    StatCalculator.CalculateNominalCluTrend(code, date,
                            rawDataMap, 0, days - 1, true));

        }

        for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
            days = WEEK_MULTIPIER[i] * 7;
            storageRow.put(cls + "CTrdLow" + days + "d",
                    StatCalculator.CalculateNominalCluTrend(code, date,
                            rawDataMap, 0, days - 1, false));
        }
    }

}
