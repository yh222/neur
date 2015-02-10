package calculator;

import static core.GlobalConfigs.DATE_FORMAT;
import static core.GlobalConfigs.DEFAULT_START_DATE;
import static core.GlobalConfigs.getSignificanceDaily;
import static core.GlobalConfigs.getSignificanceNormal;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StatCalculator {

//    protected static final double SIGNIFICANCE_NORMAL = GlobalConfigs.getSignificanceNormal(null);
//    protected static final double SIGNIFICANCE_DAILY = GlobalConfigs.SIGNIFICANCE_DAILY;
    /*
     * Velocity = volume of the date / Average volume
     */
    public static Object CalcluateVelocity(String date, ConcurrentHashMap<String, Object[]> rawDataMap, int distance) {
        //Duration should be always zero, as non-directly past velocity seems not revelant.
        Calendar start_date = getUsableDate(date, rawDataMap, distance, 0, true, true);
        Calendar end_date = getUsableDate(date, rawDataMap, 0, 0, false, true);
        if (start_date == null || end_date == null) {
            return null; //remain 0
        }
        //System.out.println("start: " + DATE_FORMAT.format(start_date.getTime()));
        //System.out.println("end: " + DATE_FORMAT.format(end_date.getTime()));
        int count = 0;
        int buffer = (int) (distance * 0.4) + 3;
        double sum_volume = 0;
        //Calculate volume sum
        for (int i = 0; i < distance; i++) {
            if (rawDataMap.get(DATE_FORMAT.format(start_date.getTime())) != null) {
                count++;
                sum_volume += (double) rawDataMap.get(DATE_FORMAT.format(start_date.getTime()))[4];
                //System.out.println(DATE_FORMAT.format(start_date.getTime()));
            } else {
                i--;
                if (--buffer == 0) {
                    break;
                }
                //System.out.println("cannot find: " + DATE_FORMAT.format(start_date.getTime()));
            }
            start_date.add(Calendar.DATE, 1);
        }
        double current_volume = (double) rawDataMap.get(DATE_FORMAT.format(end_date.getTime()))[4];
        return current_volume / (sum_volume / count);
    }

    public static Object CalculateMomentum(String date, ConcurrentHashMap<String, Object[]> rawDataMap, int distance, int duration) {
        Calendar start_date = getUsableDate(date, rawDataMap, distance, duration, true, false);
        Calendar end_date = getUsableDate(date, rawDataMap, distance, duration, false, false);
        if (start_date == null || end_date == null) {
            return null;
        }

        double trend_start = (double) rawDataMap.get(DATE_FORMAT.format(start_date.getTime()))[0];
        double trend_end = (double) rawDataMap.get(DATE_FORMAT.format(end_date.getTime()))[3];
        return (trend_end - trend_start) / trend_start;
    }

    public static Object CalculateClusteredTrend(String date, ConcurrentHashMap<String, Object[]> rawDataMap, int distance, int duration, boolean getHighest) {
        Calendar start_date = getUsableDate(date, rawDataMap, distance, duration, true, false);
        Calendar end_date = getUsableDate(date, rawDataMap, distance, duration, false, false);
        if (start_date == null || end_date == null) {
            return null;
        }
        double start_price = (double) rawDataMap.get(DATE_FORMAT.format(start_date.getTime()))[0];
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;
        //Set end date to monday of the week
        int dayofWeek = end_date.get(Calendar.DAY_OF_WEEK);
        end_date.add(Calendar.DATE, -1 * (dayofWeek - Calendar.MONDAY));
        for (int i = 0; i < 5; i++) {
            if (rawDataMap.get(DATE_FORMAT.format(end_date.getTime())) != null) {
                double templow = (double) rawDataMap.get(DATE_FORMAT.format(end_date.getTime()))[2];
                double temphigh = (double) rawDataMap.get(DATE_FORMAT.format(end_date.getTime()))[1];
                if (max < temphigh) {
                    max = temphigh;
                }
                if (min > templow) {
                    min = templow;
                }
            }
            end_date.add(Calendar.DATE, 1);
        }
        if (getHighest) {
            return (max - start_price) / start_price;
        } else {
            return (min - start_price) / start_price;
        }
    }

    /*
     * Calculate lowest drop or highest rise inside a period
     */
    public static Object CalculateExtremeInPeriod(String date, ConcurrentHashMap<String, Object[]> rawDataMap, int distance, int duration, boolean getHighest) {
        Calendar start_date = getUsableDate(date, rawDataMap, distance, duration, true, false);
        Calendar end_date = getUsableDate(date, rawDataMap, distance, duration, false, false);
        if (start_date == null || end_date == null) {
            return null;
        }
        double start_price = (double) rawDataMap.get(DATE_FORMAT.format(start_date.getTime()))[0];
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;

        while (!start_date.after(end_date)) {
            if (rawDataMap.get(DATE_FORMAT.format(start_date.getTime())) != null) {
                double templow = (double) rawDataMap.get(DATE_FORMAT.format(start_date.getTime()))[2];
                double temphigh = (double) rawDataMap.get(DATE_FORMAT.format(start_date.getTime()))[1];
                if (max < temphigh) {
                    max = temphigh;
                }
                if (min > templow) {
                    min = templow;
                }
            }
            start_date.add(Calendar.DAY_OF_MONTH, 1);
        }
        if (getHighest) {
            return (max - start_price) / start_price;
        } else {
            return (min - start_price) / start_price;
        }
    }

    /*
     * returns Spring, Summer, Autunm or Winter
     */
    public static String CalculateSeasonOfYear(String date) {
        try {
            Calendar tempdate = Calendar.getInstance();
            tempdate.setTime(DATE_FORMAT.parse(date));
            int month = tempdate.get(Calendar.MONTH);
            if (month <= 2) {
                return "Spring";
            } else if (month > 2 && month <= 5) {
                return "Summer";
            } else if (month > 5 && month <= 8) {
                return "Autumn";
            } else {
                return "Winter";
            }
        } catch (ParseException ex) {
            Logger.getLogger(StatCalculator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static String CalcluateNominalRawTrend(String code, String date, ConcurrentHashMap<String, Object[]> rawDataMap, int distance, int duration) {
        Object rt = CalculateMomentum(date, rawDataMap, distance, duration);
        if (rt == null) {
            return "?";
        }
        return getHighLowClass(code, (double) rt);
    }

    public static String CalculateNominalExtreme(String code, String date, ConcurrentHashMap<String, Object[]> rawDataMap, int distance, int duration, boolean getHighest) {
        Object ext = CalculateExtremeInPeriod(date, rawDataMap, distance, duration, getHighest);
        if (ext == null) {
            return "?";
        }
        return getHighLowClass(code, (double) ext);
    }

    public static String CalculateNominalCluTrend(String code, String date, ConcurrentHashMap<String, Object[]> rawDataMap, int distance, int duration, boolean getHighest) {
        Object ext = CalculateClusteredTrend(date, rawDataMap, distance, duration, getHighest);
        if (ext == null) {
            return "?";
        }
        return getHighLowClass(code, (double) ext);
    }

    public static void CountCandleChartUnits(String code, String date,
            ConcurrentHashMap<String, Object[]> rawDataMap,
            int duration, HashMap<String, Object> targetToInsert) {
        m_READS[] reads = m_READS.values();
        Calendar start_date = getUsableDate(date, rawDataMap, duration, duration, true, true);
        Calendar end_date = getUsableDate(date, rawDataMap, duration, duration, false, true);
        if (start_date == null || end_date == null) {
            for (int i = 0; i < m_READS.SIZE; i++) {
                targetToInsert.put(reads[i].name() + duration + "d", null);
            }
            return;
        }
        int[] tempResult = new int[m_READS.SIZE];
        m_READS read;
        while (!start_date.after(end_date)) {
            Object[] rawData = rawDataMap.get(DATE_FORMAT.format(start_date.getTime()));
            if (rawData != null) {
                read = readCandleChartUnit(code, rawData);
                tempResult[read.ordinal()]++;
            }
            start_date.add(Calendar.DAY_OF_MONTH, 1);
        }

        //Insert results into output array
        for (int i = 0; i < m_READS.SIZE; i++) {
            targetToInsert.put(reads[i].name() + duration + "d", tempResult[i]);
        }
    }

    public static String CalculateCandleUnitForDay(String code, String date, ConcurrentHashMap<String, Object[]> rawDataMap, int distance) {
        try {
            Calendar tempdate = Calendar.getInstance();
            tempdate.setTime(DATE_FORMAT.parse(date));
            //If find 10 unusable dates, return null
            int buffer_count = (int) (distance * 0.6) + 3;
            for (int i = distance; i > 0; i--) {
                tempdate.add(Calendar.DAY_OF_MONTH, -1);
                if (rawDataMap.get(DATE_FORMAT.format(tempdate.getTime())) == null) {
                    i++;
                    if (--buffer_count == 0) {
                        break;
                    }
                }
            }

            Object[] rawData = rawDataMap.get(DATE_FORMAT.format(tempdate.getTime()));
            m_READS read;
            if (rawData != null) {
                read = readCandleChartUnit(code, rawData);
            } else {
                //System.err.println("CalculateCandleUnitForDay outputed null");
                return null;
            }
            return read.name();
        } catch (ParseException ex) {
            Logger.getLogger(StatCalculator.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.err.println("CalculateCandleUnitForDay outputed null");
        return null;
    }

    /*
     * get a date 
     */
    public static String CalculateDayOfWeek(String date) {
        try {
            Calendar tempdate = Calendar.getInstance();
            tempdate.setTime(DATE_FORMAT.parse(date));
            String[] namesOfDays = DateFormatSymbols.getInstance().getShortWeekdays();
            return namesOfDays[tempdate.get(Calendar.DAY_OF_WEEK)];
        } catch (ParseException ex) {
            Logger.getLogger(StatCalculator.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static Calendar getUsableDate(String date, ConcurrentHashMap<String, Object[]> rawDataMap, int distance, int duration, boolean isStart, boolean useEffevtiveDay) {
        try {
            Calendar tempdate = Calendar.getInstance();
            tempdate.setTime(DATE_FORMAT.parse(date));

            int direction, count;
            if (isStart) {
                direction = distance <= 0 ? 1 : -1;
                count = Math.abs(distance);
            } else {
                direction = duration - distance < 0 ? -1 : 1;
                count = Math.abs(duration - distance);
            }

            int buffer = (int) (distance * 0.6) + 3;
            for (int i = 0; i <= count; i++) {
                //Keep going forward until buffer depleted 
                if (buffer == 0) {
                    break;
                }
                //Check validity when count meet
                if (i == count) {
                    if (rawDataMap.get(DATE_FORMAT.format(tempdate.getTime())) == null) {
                        buffer--;
                        i--;
                    } else {
                        return tempdate;
                    }
                }
                // Need to check everyday if going to use effective days only
                if (useEffevtiveDay) {
                    if (rawDataMap.get(DATE_FORMAT.format(tempdate.getTime())) == null) {
                        buffer--;
                        i--;
                    }
                }
                tempdate.add(Calendar.DAY_OF_MONTH, direction);
            }

            return null;
        } catch (ParseException ex) {
            Logger.getLogger(StatCalculator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static String getHighLowClass(String code, double in) {
        double sig = getSignificanceNormal(code);

        if (Math.abs(in) < sig) {
            // (-s...s)
            return "Stay";
        } else if (in >= 2 * sig && in < 3 * sig) {
            // [2s...3s)
            return "High";
        } else if (in <= - 2 * sig && in > -3 * sig) {
            // (-3s...-2s]
            return "Low";
        } else if (in >= sig && in < 2 * sig) {
            // [s...2s)
            return "Little_High";
        } else if (in <= -1 * sig && in > - 2 * sig) {
            // (-2s...-s]
            return "Little_Low";
        } else if (in >= 3 * sig) {
            // [3s...inf)
            return "Very_High";
        } else if (in <= - 3 * sig) {
            // (-inf...-3s]
            return "Very_Low";
        } else {// return null if extreme == 0, coz it usually should not happen
            return null;
        }

    }

    public static double CalculateDividentAmt(String date, ArrayList<String[]> dividendData) {
        double r = 0.0;
        try {
            if (dividendData != null) {
                Calendar inputDate = Calendar.getInstance();
                Calendar tempDate = Calendar.getInstance();
                inputDate.setTime(DATE_FORMAT.parse(date));
                long min = Long.MAX_VALUE, diff;
                for (String[] array : dividendData) {
                    tempDate.setTime(DATE_FORMAT.parse(array[0]));
                    diff = getDateDiff(inputDate.getTime(), tempDate.getTime(), TimeUnit.DAYS);
                    if (diff > 0 && diff < min && diff < 120) {
                        min = diff;
                        r = Double.parseDouble(array[1]);
                    }
                }
            }
        } catch (ParseException ex) {
            Logger.getLogger(StatCalculator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return r;
    }

    public static int CalculateDaysTillNextDivdnt(String date, ArrayList<String[]> dividendData) {
        int r = 999;
        try {
            if (dividendData != null) {
                Calendar inputDate = Calendar.getInstance();
                Calendar tempDate = Calendar.getInstance();
                inputDate.setTime(DATE_FORMAT.parse(date));
                long min = Long.MAX_VALUE, diff;
                for (String[] array : dividendData) {
                    tempDate.setTime(DATE_FORMAT.parse(array[0]));
                    diff = getDateDiff(inputDate.getTime(), tempDate.getTime(), TimeUnit.DAYS);
                    if (diff > 0 && diff < min && diff < 120) {
                        min = diff;
                        r = (int) min;
                    }
                }
            }
        } catch (ParseException ex) {
            Logger.getLogger(StatCalculator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return r;
    }

    public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
    }

    private enum m_READS {

        GreatW,
        GreatB,
        LongW,
        LongB,
        ShortW,
        ShortB,
        ShortWTT,
        ShortBTT,
        ShortWU,
        ShortWD,
        ShortBU,
        ShortBD,
        StarW,
        StarB,
        StarWTT,
        StarBTT,
        StarWU,
        StarWD,
        StarBU,
        StarBD;

        public static final int SIZE = m_READS.values().length;
    }

    private static m_READS readCandleChartUnit(String code, Object[] rawData) {
        double sig = getSignificanceDaily(code);
        double open = (double) rawData[0];
        double high = (double) rawData[1];
        double low = (double) rawData[2];
        double close = (double) rawData[3];
        double avg = (open + close) / 2;
        double trend = (close - open) / avg;
        double uptail = (high - open) / avg;
        double downtail = (close - low) / avg;
        double tail = 1 * sig;

        //Greats, If body is significantly large
        if (trend > 3.0 * sig) {
            return m_READS.GreatW;
        } else if (trend < -3.0 * sig) {
            return m_READS.GreatB;
        }
        //Longs
        if (trend > 2.0 * sig) {
            return m_READS.LongW;
        } else if (trend < -2.0 * sig) {
            return m_READS.LongB;
        }

        //Shorts
        if (trend > 0.6 * sig) {
            if (uptail > sig && downtail > tail) {
                return m_READS.ShortWTT;
            } else if (uptail > tail) {
                return m_READS.ShortWU;
            } else if (downtail > tail) {
                return m_READS.ShortWD;
            } else {
                return m_READS.ShortW;
            }
        } else if (trend < -0.6 * sig) {
            if (uptail > sig && downtail > tail) {
                return m_READS.ShortBTT;
            } else if (uptail > tail) {
                return m_READS.ShortBU;
            } else if (downtail > tail) {
                return m_READS.ShortBD;
            } else {
                return m_READS.ShortB;
            }
        }
        //Stars
        if (trend > 0) {
            if (uptail > sig && downtail > tail) {
                return m_READS.StarWTT;
            } else if (uptail > tail) {
                return m_READS.StarWU;
            } else if (downtail > tail) {
                return m_READS.StarWD;
            } else {
                return m_READS.StarW;
            }
        } else if (trend <= 0) {
            if (uptail > sig && downtail > tail) {
                return m_READS.StarBTT;
            } else if (uptail > tail) {
                return m_READS.StarBU;
            } else if (downtail > tail) {
                return m_READS.StarBD;
            } else {
                return m_READS.StarB;
            }
        }
        return null;
    }

    protected static Calendar getFirstValidDate(
            ConcurrentHashMap<String, Object[]> rawDataMap)
            throws ParseException {
        Calendar start_date = Calendar.getInstance();
        start_date.setTime(DATE_FORMAT.parse(DEFAULT_START_DATE));
        //Keep looping until a valid date is found
        Object[] raw_data;
        for (int i = 9999; i > 0; i--) {
            raw_data = rawDataMap.get(DATE_FORMAT.format(start_date.getTime()));
            if (raw_data != null) {
                break;
            }
            start_date.add(Calendar.DAY_OF_MONTH, 1);
        }
        return start_date;
    }

    protected static double average(LinkedList<Double> queue) {
        double sum = 0;
        for (Double e : queue) {
            sum += (double) e;
        }
        return sum / queue.size();
    }
}
