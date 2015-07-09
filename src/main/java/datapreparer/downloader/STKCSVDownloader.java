package datapreparer.downloader;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.time.LocalDate;
import java.util.logging.*;
import core.GConfigs;
import static core.GConfigs.RESOURCE_PATH;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import util.MyUtils;

/**
 * Download raw instrument data from web-server in .csv format
 *
 * Downloads dividend data as well.
 *
 * Need "instrument_list.txt" to get list of instrument codes
 *
 * If target .csv file already exists, append new data to the end of the .csv
 * file.
 */
public class STKCSVDownloader {

  public static void updateRawDataFromYahoo(String yhooParameter, ArrayList<String> codeList) {
    String type = "";
    if (yhooParameter.equals("v")) {
      type = "_Dividend";
    }

    ArrayList<String> instruments = codeList;
    LocalDate today = LocalDate.now();
    if (GConfigs.END_DATE != "") {
      today = LocalDate.parse(GConfigs.END_DATE);
      today = today.minusMonths(1);
    }

    for (String code : instruments) {
      String file_path = RESOURCE_PATH + GConfigs.MODEL_TYPES.STK.name() + "//" + code + "//" + code + type + ".csv";
      LocalDate start_date = LocalDate.now();

      File file = new File(file_path);
      MyUtils.findOrCreateFolder(file.getParentFile().getAbsolutePath());

      start_date = MyUtils.getLastDateFromFile(file);
      if (start_date != null) {
        start_date = start_date.plusDays(1);
        if (start_date.toString().equals(LocalDate.now().toString())) {
          //if the date is today, skip G
          System.out.println(code + " is up to date.");
        }
      } else {
        start_date = LocalDate.parse(GConfigs.DEFAULT_START_DATE);
      }

      try {
        int dayDiff = (int) ChronoUnit.DAYS.between(start_date, today);

        if (!yhooParameter.equals("")) {
          //download data from yahoo
          URL url = new URL("http://ichart.finance.yahoo.com/table.csv?s="
                  + code + "&a=" + (start_date.get(ChronoField.MONTH_OF_YEAR) - 1) + "&b=" + start_date.get(ChronoField.DAY_OF_MONTH) + "&c=" + start_date.get(ChronoField.YEAR)
                  + "&d=" + today.get(ChronoField.MONTH_OF_YEAR) + "&e=" + today.get(ChronoField.DAY_OF_MONTH) + "&f=" + today.get(ChronoField.YEAR)
                  + "&g=" + yhooParameter + "&ignore=.csv");
          System.out.println("Downloading data for " + code + " from " + url);
          downloadDataFromURL(url, new File(file_path), true, dayDiff);
        } else {
          //download from quotemedia
          URL url = new URL("http://app.quotemedia.com/quotetools/getHistoryDownload.csv?&webmasterId=501&startDay="
                  + start_date.get(ChronoField.DAY_OF_MONTH) + "&startMonth=" + start_date.get(ChronoField.MONTH_OF_YEAR) + "&startYear=" + start_date.get(ChronoField.YEAR)
                  + "&endDay=" + today.get(ChronoField.DAY_OF_MONTH) + "&endMonth=" + today.get(ChronoField.MONTH_OF_YEAR) + "&endYear=" + today.get(ChronoField.YEAR)
                  + "&isRanged=false&symbol=" + code + "&ignore=.csv");
          System.out.println("Downloading data for " + code + " from " + url);
          downloadDataFromURL(url, new File(file_path), false, dayDiff);
        }
      } catch (MalformedURLException ex) {
        Logger.getLogger(STKCSVDownloader.class.getName()).log(Level.SEVERE, null, ex);
      }

    }
  }

  private static void downloadDataFromURL(URL url, File file, boolean isYhoo, int dayDiff) {
    try {
      BufferedReader url_reader = new BufferedReader(new InputStreamReader(url.openStream()));
      //Skip the first line
      url_reader.readLine();
      ArrayList<String> temp = new ArrayList();
      String line;
      while ((line = url_reader.readLine()) != null) {
        temp.add(line);
      }
      // Append new data at reversed order
      try (PrintWriter writer = new PrintWriter(new BufferedWriter(
              new FileWriter(file, true)))) {
        String new_line;
        for (int j = temp.size() - 1; j >= 0; j--) {
          if (isYhoo) {
            new_line = handleStockSplit(temp.get(j));
          } else {
            new_line = reformatQMdata(temp.get(j));
          }
          writer.println(new_line);
        }
      }
    } catch (FileNotFoundException ex) {
      //Display error only if date difference is high
      if (dayDiff > 2) {
        Logger.getLogger(STKCSVDownloader.class.getName()).log(Level.SEVERE, " failed to fetch data from: " + url, ex);
      }
    } catch (MalformedURLException ex) {
      Logger.getLogger(STKCSVDownloader.class.getName()).log(Level.SEVERE, null, ex);
    } catch (IOException ex) {
      Logger.getLogger(STKCSVDownloader.class.getName()).log(Level.SEVERE, " error reading from url", ex);
    }
  }

  //Check if this stock had split, and adjust price according to split ratio
  private static String handleStockSplit(String raw) {
    String[] parts = raw.split(",");
    if (parts.length < 7 || parts[4].equals(parts[6])) {
      return raw;
    }
    double split = Double.parseDouble(parts[4]) / Double.parseDouble(parts[6]);
    double open = Double.parseDouble(parts[1]) / split;
    double high = Double.parseDouble(parts[2]) / split;
    double low = Double.parseDouble(parts[3]) / split;
    double close = Double.parseDouble(parts[4]) / split;
    double vol = Double.parseDouble(parts[5]) / split;

    return parts[0] + "," + open + "," + high + "," + low + "," + close + "," + vol + "," + parts[6];
  }

  private static String reformatQMdata(String raw) {
    String[] parts = raw.split(",");
    return parts[0] + "," + parts[1] + "," + parts[2] + "," + parts[3] + "," + parts[4] + "," + parts[5] + "," + parts[8];
  }

  public static void main(String[] args) {
    STKCSVDownloader.updateRawDataFromYahoo("d", GConfigs.INSTRUMENT_CODES);
  //STKCSVDownloader.updateRawDataFromYahoo("d", GConfigs.INDICE_CODES);
    //Update dividend data
    //STKCSVDownloader.updateRawDataFromYahoo("v", GConfigs.INSTRUMENT_CODES);
  }

}
