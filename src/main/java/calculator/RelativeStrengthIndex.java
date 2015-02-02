package calculator;

import static core.GlobalConfigs.DATE_FORMAT;
import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class RelativeStrengthIndex {

    private HashMap<String, Double> m_RSI10;
    private HashMap<String, Double> m_RSI20;
    private HashMap<String, Double> m_RSI30;
    private HashMap<String, Double> m_RSI50;

    public RelativeStrengthIndex() {

    }

    public Object getRSI(String date, int rsiDuration, int distance,
            ConcurrentHashMap<String, Object[]> rawDataMap) throws ParseException {
        HashMap<String, Double> rsi;
        if (rsiDuration == 10) {
            rsi = m_RSI10;
        } else if (rsiDuration == 20) {
            rsi = m_RSI20;
        } else if (rsiDuration == 30) {
            rsi = m_RSI30;
        } else if (rsiDuration == 50) {
            rsi = m_RSI50;
        } else {
            return null;
        }

        if (rsi == null) {
            if (rsiDuration == 10) {
                m_RSI10 = new HashMap();
                rsi = m_RSI10;
            } else if (rsiDuration == 20) {
                m_RSI20 = new HashMap();
                rsi = m_RSI20;
            } else if (rsiDuration == 30) {
                m_RSI30 = new HashMap();
                rsi = m_RSI30;
            } else if (rsiDuration == 50) {
                m_RSI50 = new HashMap();
                rsi = m_RSI50;
            } else {
                return null;
            }
            calculateRSI(rsiDuration, rawDataMap, rsi);
        }
        if (distance != 0) {
            Calendar adjusted_date = StatCalculator.getUsableDate(date,
                    rawDataMap, distance, 0, true, false);
            if(adjusted_date==null)
                return null;
            date=DATE_FORMAT.format(adjusted_date.getTime());
        }

        return rsi.get(date);
    }

    private void calculateRSI(int rsiDuration,
            ConcurrentHashMap<String, Object[]> rawDataMap,
            HashMap<String, Double> rsi) throws ParseException {
        Calendar start_date = StatCalculator.getFirstValidDate(rawDataMap);
        Object[] raw_data = rawDataMap.get(DATE_FORMAT.format(start_date.getTime()));
        if (raw_data == null) {
            System.err.println("Cannot find first valid date");
            return;
        }

        double loss, gain, p_avg_loss = 0, p_avg_gain = 0,
                rs, change, p, t_rsi;
        double prev = (double) raw_data[3];
        int buffer = 10;

        while (buffer > 0) {
            start_date.add(Calendar.DAY_OF_MONTH, 1);
            raw_data = rawDataMap.get(DATE_FORMAT.format(start_date.getTime()));
            if (raw_data == null) {
                buffer--;
            } else {
                buffer = 10;
                p = (double) raw_data[3];
                change = p - prev;
                prev = p;

                gain = change > 0 ? change : 0;
                loss = change < 0 ? -change : 0;

                //Use p_avg_gain/loss as temporary value to hold current avg
                p_avg_loss = (p_avg_loss * (rsiDuration - 1) + loss) / rsiDuration;
                p_avg_gain = (p_avg_gain * (rsiDuration - 1) + gain) / rsiDuration;
                rs = p_avg_loss == 0 ? 999 : p_avg_gain / p_avg_loss;
                t_rsi = (100 - 100 / (1 + rs));
                rsi.put(DATE_FORMAT.format(start_date.getTime()), t_rsi);
            }
        }
    }

}
