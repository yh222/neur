package calculator;

import static core.GlobalConfigs.DATE_FORMAT;
import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

public class MovingAverage {

    private HashMap<String, Double> m_EMA5;
    private HashMap<String, Double> m_EMA10;
    private HashMap<String, Double> m_EMA20;
    private HashMap<String, Double> m_EMA50;
    private HashMap<String, Double> m_EMA100;
    private HashMap<String, Double> m_EMA200;

    private HashMap<String, Double> m_SMA5;
    private HashMap<String, Double> m_SMA10;
    private HashMap<String, Double> m_SMA20;
    private HashMap<String, Double> m_SMA50;
    private HashMap<String, Double> m_SMA100;
    private HashMap<String, Double> m_SMA200;

    public MovingAverage() {

    }

    public Object getEMA(String date, int emaDuration,
            ConcurrentHashMap<String, Object[]> rawDataMap) throws ParseException {
        HashMap<String, Double> ema;
        if (emaDuration == 5) {
            ema = m_EMA5;
        } else if (emaDuration == 10) {
            ema = m_EMA10;
        } else if (emaDuration == 20) {
            ema = m_EMA20;
        } else if (emaDuration == 50) {
            ema = m_EMA50;
        } else if (emaDuration == 100) {
            ema = m_EMA100;
        } else if (emaDuration == 200) {
            ema = m_EMA200;
        } else {
            return null;
        }

        if (ema == null) {
            if (emaDuration == 5) {
                m_EMA5 = new HashMap();
                ema = m_EMA5;
            } else if (emaDuration == 10) {
                m_EMA10 = new HashMap();
                ema = m_EMA10;
            } else if (emaDuration == 20) {
                m_EMA20 = new HashMap();
                ema = m_EMA20;
            } else if (emaDuration == 50) {
                m_EMA50 = new HashMap();
                ema = m_EMA50;
            } else if (emaDuration == 100) {
                m_EMA100 = new HashMap();
                ema = m_EMA100;
            } else if (emaDuration == 200) {
                m_EMA200 = new HashMap();
                ema = m_EMA200;
            } else {
                return null;
            }
            calculateEMA(emaDuration, rawDataMap, ema);
        }
        return ema.get(date);
    }

    public Object getSMA(String date, int smaDuration,
            ConcurrentHashMap<String, Object[]> rawDataMap) throws ParseException {

        HashMap<String, Double> sma;
        if (smaDuration == 5) {
            sma = m_SMA5;
        } else if (smaDuration == 10) {
            sma = m_SMA10;
        } else if (smaDuration == 20) {
            sma = m_SMA20;
        } else if (smaDuration == 50) {
            sma = m_SMA50;
        } else if (smaDuration == 100) {
            sma = m_SMA100;
        } else if (smaDuration == 200) {
            sma = m_SMA200;
        } else {
            return null;
        }

        if (sma == null) {
            if (smaDuration == 5) {
                m_SMA5 = new HashMap();
                sma = m_SMA5;
            } else if (smaDuration == 10) {
                m_SMA10 = new HashMap();
                sma = m_SMA10;
            } else if (smaDuration == 20) {
                m_SMA20 = new HashMap();
                sma = m_SMA20;
            } else if (smaDuration == 50) {
                m_SMA50 = new HashMap();
                sma = m_SMA50;
            } else if (smaDuration == 100) {
                m_SMA100 = new HashMap();
                sma = m_SMA100;
            } else if (smaDuration == 200) {
                m_SMA200 = new HashMap();
                sma = m_SMA200;
            } else {
                return null;
            }
            calculateSMA(smaDuration, rawDataMap, sma);
        }

        return sma.get(date);

    }

    private void calculateEMA(int emaDuration,
            ConcurrentHashMap<String, Object[]> rawDataMap,
            HashMap<String, Double> ema) throws ParseException {

        Calendar start_date = StatCalculator.getFirstValidDate(rawDataMap);
        Object[] raw_data = rawDataMap.get(DATE_FORMAT.format(start_date.getTime()));
        if (raw_data == null) {
            System.err.println("Cannot find first valid date");
            return;
        }
        double smooth = 2.0 / (1.0 + emaDuration);
        double past_ema = (double) 1.0;
        double current_ema;
        ema.put(DATE_FORMAT.format(start_date.getTime()), past_ema);

        int buffer = 10;
        double current_p;
        while (buffer > 0) {
            start_date.add(Calendar.DAY_OF_MONTH, 1);
            raw_data = rawDataMap.get(DATE_FORMAT.format(start_date.getTime()));
            if (raw_data == null) {
                buffer--;
            } else {
                buffer = 10;
                current_p = (double) raw_data[3];
                current_ema = (current_p * smooth)
                        + (past_ema * (1 - smooth));
                ema.put(DATE_FORMAT.format(start_date.getTime()), current_p / current_ema);
                past_ema = current_ema;
            }
        }

    }

    private void calculateSMA(int smaDuration,
            ConcurrentHashMap<String, Object[]> rawDataMap,
            HashMap<String, Double> sma) throws ParseException {

        Calendar start_date = StatCalculator.getFirstValidDate(rawDataMap);
        Object[] raw_data = rawDataMap.get(DATE_FORMAT.format(start_date.getTime()));
        if (raw_data == null) {
            System.err.println("Cannot find first valid date");
            return;
        }
        //Initialize queue with all the same values;
        LinkedList<Double> queue = new LinkedList();
        for (int i = 0; i < smaDuration; i++) {
            queue.add((Double) 1.0);
        }
        sma.put(DATE_FORMAT.format(start_date.getTime()), StatCalculator.average(queue));

        double p;
        int buffer = 10;
        while (buffer > 0) {
            start_date.add(Calendar.DAY_OF_MONTH, 1);
            raw_data = rawDataMap.get(DATE_FORMAT.format(start_date.getTime()));
            if (raw_data == null) {
                buffer--;
            } else {
                buffer = 10;
                p = (double) raw_data[3];
                queue.pop();
                queue.add(p);
                sma.put(DATE_FORMAT.format(start_date.getTime()), p / StatCalculator.average(queue));
            }
        }
    }

}
