package datapreparer.valuemaker.indicie;

import static core.GlobalConfigs.WEEK_MULTIPIER;
import calculator.StatCalculator;
import static datapreparer.RawDataLoader.loadRawDataFromFile;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class IndicieValueMaker {

    private static ConcurrentHashMap<String, HashMap<String, Object[]>> indicieData;

    public static Object[] loadIndicieData(String indicieCode, String date) {
        if (indicieData == null) {
            indicieData = new ConcurrentHashMap();
        }

        if (!indicieData.containsKey(indicieCode)) {
            ConcurrentHashMap<String, Object[]> raw_data_map
                    = loadRawDataFromFile(indicieCode);
            HashMap date_to_data = new HashMap();

            Object[] temp_row;
            for (String d : raw_data_map.keySet()) {
                int days;
                temp_row = new Object[WEEK_MULTIPIER.length];
                for (int i = 0; i < WEEK_MULTIPIER.length; i++) {
                    days = WEEK_MULTIPIER[i] * 7;
                    temp_row[i] = StatCalculator.
                            CalculateExtremeInPeriod(
                                    d, raw_data_map, days, days, false);
                }
                date_to_data.put(d, temp_row);
            }
            indicieData.put(indicieCode, date_to_data);
        }
        return indicieData.get(indicieCode).get(date);
    }

}
