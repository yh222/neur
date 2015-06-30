/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package calculator;

import core.GConfigs.YAHOO_DATA_INDEX;
import static core.GConfigs.getSignificanceDaily;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import util.MyUtils;

/**
 *
 * @author Yichen
 */
public class CandleStick {

  public static String getCandlePatternForDay(String date,
          ConcurrentHashMap<String, Object[]> rawDataMap) {
    Object[] raw_data = rawDataMap.get(date);
    Object temp = StatCalculator.getMomentum(date, rawDataMap, 3, 3);
    if (raw_data == null || temp == null) {
      return null;
    }
    double mom = (double) temp;
    double momthresh = 0.015;
    double sig = getSignificanceDaily();

    double topen = (double) raw_data[YAHOO_DATA_INDEX.OPEN.ordinal()];
    double tclose = (double) raw_data[YAHOO_DATA_INDEX.CLOSE.ordinal()];
    double thigh = (double) raw_data[YAHOO_DATA_INDEX.HIGH.ordinal()];
    double tlow = (double) raw_data[YAHOO_DATA_INDEX.LOW.ordinal()];

    double tbody = tclose - topen;
    double tutail = tbody > 0 ? (thigh - tclose) : (thigh - topen);
    double tdtail = tbody > 0 ? (topen - tlow) : (tclose - tlow);

    LocalDate yesterday = MyUtils.getUsableDate(date, rawDataMap, 1, 1, true, true);
    raw_data = rawDataMap.get(yesterday.toString());
    if (raw_data == null) {
      return null;
    }

    double yopen = (double) raw_data[YAHOO_DATA_INDEX.OPEN.ordinal()];
    double yclose = (double) raw_data[YAHOO_DATA_INDEX.CLOSE.ordinal()];
    double yhigh = (double) raw_data[YAHOO_DATA_INDEX.HIGH.ordinal()];
    double ybody = yclose - yopen;

    // Bearish Belt Holt: short down shadow, period bullish, long neg body
    if (mom > momthresh && tbody < sig * -2 && tdtail < sig * 0.5 && tutail == 0) {
      return m_PATTERNS.BBeltHolt.name();
    }

    // Bullish Belt Holt: no down shadow, period bearish, long pos body
    if (mom < -1 * momthresh && tbody > sig * 2 && tutail < sig * 0.5 && tdtail == 0) {
      return m_PATTERNS.BuBeltHolt.name();
    }

    //Hammer: down tail, beriod bearish, little up tail
    // abs.body should not be too narrow
    if (mom < -1 * momthresh && tutail < Math.abs(tbody)
            && tdtail > Math.abs(tbody) * 2 && Math.abs(tbody) > sig * 0.5) {
      return m_PATTERNS.Hammer.name();
    }

    //Hanging Man: down tail, period bullish,little up tail
    // abs.body should not be too narrow
    if (mom > momthresh && tutail < Math.abs(tbody)
            && tdtail > Math.abs(tbody) * 2 && Math.abs(tbody) > sig * 0.5) {
      return m_PATTERNS.HangingMan.name();
    }

    // Bearish Engulfing: today engulf yesterday, 
    //period bullish, today long neg body, yesterday positive body
    if (mom > momthresh && tbody < sig * -2 && ybody > 0 && tclose < yopen && topen > yclose) {
      return m_PATTERNS.BEngulfing.name();
    }

    //Bullish Engulfing: today engulf yesterday
    // beriod bearish, today long pos body, yesterday neg body
    if (mom < -1 * momthresh && tbody > sig * 2 && ybody < 0 && tclose > yopen && topen < yclose) {
      return m_PATTERNS.BuEngulfing.name();
    }

    // Bearish Harami: yesterday engulf today, 
    //period bullish, neg today body, long pos yesterday body
    if (mom > momthresh && tbody < 0 && ybody > sig * 2 && tclose > yopen && topen < yclose) {
      return m_PATTERNS.BHarami.name();
    }

    // Bullish Harami: yesterday engulf today, 
    // period bearish,long neg yesterday body, pos today body
    if (mom < -1 * momthresh && tbody > 0 && ybody < sig * -2 && tclose < yopen && topen > yclose) {
      return m_PATTERNS.BuHarami.name();
    }

    //Dark Cloud Cover: neg body today, pos body yesterday,
    //period bullish, topen>yhigh, tclose>yclose
    if (mom > momthresh && tbody < sig * -0.6 && ybody > sig * 0.6 && topen > yhigh && tclose > yclose) {
      return m_PATTERNS.DarkCloud.name();
    }

    return m_PATTERNS.None.name();

  }

  private enum m_PATTERNS {

    None,
    HangingMan,
    Hammer,
    DarkCloud,
    BHarami,
    BuHarami,
    BEngulfing,
    BuEngulfing,
    BuBeltHolt,
    BBeltHolt;
  }

  public static String getCandleUnitForDay(String date,
          ConcurrentHashMap<String, Object[]> rawDataMap, int distance) {

    LocalDate tempdate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
    int buffer_count = (int) (distance * 0.6) + 3;
    for (int i = distance; i > 0; i--) {
      tempdate = tempdate.plusDays(-1);
      if (rawDataMap.get(tempdate.toString()) == null) {
        i++;
        if (--buffer_count == 0) {
          break;
        }
      }
    }
    Object[] rawData = rawDataMap.get(tempdate.toString());
    m_READS read;
    if (rawData != null) {
      read = readCandleChartUnit(rawData);
    } else {
      //System.err.println("CalculateCandleUnitForDay outputed null");
      return null;
    }
    return read.name();
  }

  public static void getCandleChartUnits(String date, ConcurrentHashMap<String, Object[]> rawDataMap, int distance, int duration, HashMap<String, Object> targetToInsert) {
    m_READS[] reads = m_READS.values();
    LocalDate start_date = MyUtils.getUsableDate(date, rawDataMap, distance, duration, true, true);
    LocalDate end_date = MyUtils.getUsableDate(date, rawDataMap, distance, duration, false, true);
    if (start_date == null || end_date == null) {
      for (int i = 0; i < m_READS.SIZE; i++) {
        targetToInsert.put(reads[i].name() + duration + "d", null);
      }
      return;
    }
    int[] tempResult = new int[m_READS.SIZE];
    m_READS read;
    while (start_date.isBefore(end_date)) {
      Object[] rawData = rawDataMap.get(start_date.toString());
      if (rawData != null) {
        read = readCandleChartUnit(rawData);
        tempResult[read.ordinal()]++;
      }
      start_date = start_date.plusDays(1);
    }
    for (int i = 0; i < m_READS.SIZE; i++) {
      targetToInsert.put(reads[i].name() + duration + "d", tempResult[i]);
    }
  }

  private static m_READS readCandleChartUnit(Object[] rawData) {
    double sig = getSignificanceDaily();
    double open = (double) rawData[YAHOO_DATA_INDEX.OPEN.ordinal()];
    double high = (double) rawData[YAHOO_DATA_INDEX.HIGH.ordinal()];
    double low = (double) rawData[YAHOO_DATA_INDEX.LOW.ordinal()];
    double close = (double) rawData[YAHOO_DATA_INDEX.CLOSE.ordinal()];
    double avg = (open + close) / 2;
    double trend = (close - open) / avg;
    double uptail = trend > 0 ? (high - close) / avg : (high - open) / avg;
    double downtail = trend > 0 ? (open - low) / avg : (close - low) / avg;
    double tail = 1 * sig;
    if (trend > 3.0 * sig) {
      return m_READS.GreatW;
    } else if (trend < -3.0 * sig) {
      return m_READS.GreatB;
    }
    if (trend > 2.0 * sig) {
      return m_READS.LongW;
    } else if (trend < -2.0 * sig) {
      return m_READS.LongB;
    }
    if (trend > 0.6 * sig) {
      if (uptail > tail && downtail > tail) {
        return m_READS.ShortW;
      } else if (uptail > tail) {
        return m_READS.ShortWU;
      } else if (downtail > tail) {
        return m_READS.ShortWD;
      } else {
        return m_READS.ShortW;
      }
    } else if (trend < -0.6 * sig) {
      if (uptail > tail && downtail > tail) {
        return m_READS.ShortB;
      } else if (uptail > tail) {
        return m_READS.ShortBU;
      } else if (downtail > tail) {
        return m_READS.ShortBD;
      } else {
        return m_READS.ShortB;
      }
    }
    return m_READS.Star;
  }

  private enum m_READS {

    GreatW,
    GreatB,
    LongW,
    LongB,
    ShortW,
    ShortB,
    ShortWU,
    ShortWD,
    ShortBU,
    ShortBD,
    Star,
    None;

    public static final int SIZE = m_READS.values().length;
  }

}
