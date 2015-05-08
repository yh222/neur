/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package calculator;

import core.GConfigs;
import core.GConfigs.RAW_DATA_INDEX;
import static core.GConfigs.cldToString;
import static core.GConfigs.getSignificanceDaily;
import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    float mom = (float) temp;
    float momthresh = 0.015f;
    float sig = getSignificanceDaily();

    float topen = (float) raw_data[RAW_DATA_INDEX.OPEN.ordinal()];
    float tclose = (float) raw_data[RAW_DATA_INDEX.CLOSE.ordinal()];
    float thigh = (float) raw_data[RAW_DATA_INDEX.HIGH.ordinal()];
    float tlow = (float) raw_data[RAW_DATA_INDEX.LOW.ordinal()];

    float tbody = tclose - topen;
    float tutail = tbody > 0 ? (thigh - tclose) : (thigh - topen);
    float tdtail = tbody > 0 ? (topen - tlow) : (tclose - tlow);

    Calendar yesterday = StatCalculator.getUsableDate(date, rawDataMap, 1, 1, true, true);
    raw_data = rawDataMap.get(cldToString(yesterday));
    if (raw_data == null) {
      return null;
    }

    float yopen = (float) raw_data[RAW_DATA_INDEX.OPEN.ordinal()];
    float yclose = (float) raw_data[RAW_DATA_INDEX.CLOSE.ordinal()];
    float yhigh = (float) raw_data[RAW_DATA_INDEX.HIGH.ordinal()];
    float ybody = yclose - yopen;

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
    try {
      Calendar tempdate = Calendar.getInstance();
      tempdate.setTime(GConfigs.getDateFormat().parse(date));
      int buffer_count = (int) (distance * 0.6) + 3;
      for (int i = distance; i > 0; i--) {
        tempdate.add(Calendar.DAY_OF_MONTH, -1);
        if (rawDataMap.get(cldToString(tempdate)) == null) {
          i++;
          if (--buffer_count == 0) {
            break;
          }
        }
      }
      Object[] rawData = rawDataMap.get(cldToString(tempdate));
      m_READS read;
      if (rawData != null) {
        read = readCandleChartUnit(rawData);
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

  public static void getCandleChartUnits(String date, ConcurrentHashMap<String, Object[]> rawDataMap, int distance, int duration, HashMap<String, Object> targetToInsert) {
    m_READS[] reads = m_READS.values();
    Calendar start_date = StatCalculator.getUsableDate(date, rawDataMap, distance, duration, true, true);
    Calendar end_date = StatCalculator.getUsableDate(date, rawDataMap, distance, duration, false, true);
    if (start_date == null || end_date == null) {
      for (int i = 0; i < m_READS.SIZE; i++) {
        targetToInsert.put(reads[i].name() + duration + "d", null);
      }
      return;
    }
    int[] tempResult = new int[m_READS.SIZE];
    m_READS read;
    while (!start_date.after(end_date)) {
      Object[] rawData = rawDataMap.get(cldToString(start_date));
      if (rawData != null) {
        read = readCandleChartUnit(rawData);
        tempResult[read.ordinal()]++;
      }
      start_date.add(Calendar.DAY_OF_MONTH, 1);
    }
    for (int i = 0; i < m_READS.SIZE; i++) {
      targetToInsert.put(reads[i].name() + duration + "d", tempResult[i]);
    }
  }

  private static m_READS readCandleChartUnit(Object[] rawData) {
    float sig = getSignificanceDaily();
    float open = (float) rawData[RAW_DATA_INDEX.OPEN.ordinal()];
    float high = (float) rawData[RAW_DATA_INDEX.HIGH.ordinal()];
    float low = (float) rawData[RAW_DATA_INDEX.LOW.ordinal()];
    float close = (float) rawData[RAW_DATA_INDEX.CLOSE.ordinal()];
    float avg = (open + close) / 2;
    float trend = (close - open) / avg;
    float uptail = trend > 0 ? (high - close) / avg : (high - open) / avg;
    float downtail = trend > 0 ? (open - low) / avg : (close - low) / avg;
    float tail = 1 * sig;
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
    if (trend > 0) {
      if (uptail > tail && downtail > tail) {
        return m_READS.StarWTT;
      } else if (uptail > tail) {
        return m_READS.StarWU;
      } else if (downtail > tail) {
        return m_READS.StarWD;
      } else {
        return m_READS.StarW;
      }
    } else if (trend <= 0) {
      if (uptail > tail && downtail > tail) {
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
    StarW,
    StarB,
    StarWTT,
    StarBTT,
    StarWU,
    StarWD,
    StarBU,
    StarBD,
    None;

    public static final int SIZE = m_READS.values().length;
  }

}
