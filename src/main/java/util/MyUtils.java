package util;

import static core.GConfigs.MODEL_PATH;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import weka.core.Instances;
import weka.core.converters.CSVLoader;

public class MyUtils {

  public static void findOrCreateFolder(String folderPath) {
    File folder = new File(folderPath);
    if (!folder.isDirectory()) {
      folder.mkdir();
    }
  }

  public static LocalDate parseToISO(String dateStr) {
    return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
  }

  public static Instances loadInstancesFromCSV(String csvPath) throws IOException {
    CSVLoader loader = new CSVLoader();
    loader.setDateAttributes("first");
    loader.setDateFormat("yyyy-mm-dd");
    loader.setSource(new File(csvPath));
    Instances data = loader.getDataSet();
    return data;
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

  public static int getDaysToAdvance(String str) {
    String dayptn = "(\\d+)(d)";
    Pattern pattern_days = Pattern.compile(dayptn);
    Matcher matcher;
    matcher = pattern_days.matcher(str);
    matcher.find();
    return Integer.parseInt(matcher.group(1));
  }

  public static void saveModelAndPerformance(String code,
          String identity, String typePath,
          Object classifier, Instances dataStructure,
          double[] evaluation) {

    String dir = MODEL_PATH + typePath + code + "//";
    MyUtils.findOrCreateFolder(dir);

    String fname = identity + ".model";
    File file = new File(dir + fname);
    //Save model
    try (ObjectOutputStream objectOutputStream
            = new ObjectOutputStream(new FileOutputStream(file, false))) {
      objectOutputStream.writeObject(classifier);
      objectOutputStream.writeObject(dataStructure);
    } catch (Exception e) {
      Logger.getLogger(MyUtils.class.getName()).log(Level.SEVERE, e.getMessage(), e);
    }

    //Save performance
    fname = identity + ".perf";
    file = new File(dir + fname);
    try (ObjectOutputStream objectOutputStream
            = new ObjectOutputStream(new FileOutputStream(file, false))) {
      objectOutputStream.writeObject(evaluation);
    } catch (Exception e) {
      Logger.getLogger(MyUtils.class.getName()).log(Level.SEVERE, e.getMessage(), e);
    }
  }

  public static void deleteModelFolder(String typePath) {
    String dir = MODEL_PATH + typePath;
    MyUtils.findOrCreateFolder(dir);
    File folder = new File(dir);
    File[] files = folder.listFiles();
    if (files != null) {
      for (File f : files) {
        if (f.isDirectory()) {
          deleteDirectory(f);
        } else {
          f.delete();
        }
      }
    }
  }

  public static boolean deleteDirectory(File directory) {
    if (directory.exists()) {
      File[] files = directory.listFiles();
      if (null != files) {
        for (int i = 0; i < files.length; i++) {
          if (files[i].isDirectory()) {
            deleteDirectory(files[i]);
          } else {
            files[i].delete();
          }
        }
      }
    }
    return (directory.delete());
  }

  public static String roundDouble(double in) {
    NumberFormat defaultFormat = NumberFormat.getNumberInstance();
    defaultFormat.setMinimumFractionDigits(3);
    defaultFormat.setMaximumFractionDigits(3);
    return defaultFormat.format(in);
  }

  public static LocalDate getLastDateFromFile(File file) {
    if (file.isFile()) {
      try (final BufferedReader reader = new BufferedReader(
              new FileReader(file))) {
        String line;
        String last_line = "";
        // keep reading until the last line
        while ((line = reader.readLine()) != null) {
          last_line = line;
        }

        if (last_line.length() > 10) {
          LocalDate last_date = LocalDate.parse(last_line.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
          return last_date;
        }
      } catch (FileNotFoundException ex) {
        Logger.getLogger(MyUtils.class.getName()).log(Level.SEVERE, null, ex);
      } catch (Exception e) {
        Logger.getLogger(MyUtils.class.getName()).log(Level.SEVERE, null, e);
      }
    }
    return null;
  }
}
