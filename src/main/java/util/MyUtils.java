package util;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

public class MyUtils {

  public static void findOrCreateFolder(String folderPath) {
    File folder = new File(folderPath);
    if (!folder.isDirectory()) {
      folder.mkdir();
    }
  }

  public static LocalDate parseToISO(String dateStr){
    return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
  }
  
  public static LocalDate getUsableDate(String date, ConcurrentHashMap<String, Object[]> rawDataMap, int distance, int duration, boolean isStart, boolean useEffevtiveDay) {
    LocalDate tempdate = parseToISO(date);
    int direction;
    int count;
    if (isStart) {
      direction = distance <= 0 ? 1 : -1;
      count = Math.abs(distance);
    } else {
      direction = duration - distance < 0 ? -1 : 1;
      count = Math.abs(duration - distance);
    }
    int buffer = (int) ((duration + distance) * 0.6) + 5;
    for (int i = 0; i <= count; i++) {
      if (buffer <= 0) {
        break;
      }
      if (i == count) {
        if (rawDataMap.get(tempdate.toString()) == null) {
          buffer--;
          i--;
        } else {
          return tempdate;
        }
      }
      if (useEffevtiveDay) {
        if (rawDataMap.get(tempdate.toString()) == null) {
          buffer--;
          i--;
        }
      }
      tempdate = tempdate.plusDays(direction);
    }
    return null;
  }

}
