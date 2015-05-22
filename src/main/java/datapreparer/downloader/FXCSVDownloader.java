package datapreparer.downloader;

import core.GConfigs;
import static core.GConfigs.DEFAULT_START_DATE;
import static core.GConfigs.RESOURCE_PATH;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FXCSVDownloader {

  private static final String m_localPath = RESOURCE_PATH + GConfigs.MODEL_TYPES.FX.name() + "//";

  public static void downloadRecentData() {
    //1, Figure out dates to download
    Calendar lastdate = Calendar.getInstance();
    Calendar today = Calendar.getInstance();
    try (BufferedReader reader = new BufferedReader(
            new FileReader(m_localPath + "last_updated.txt"))) {
      lastdate.setTime(GConfigs.getDateFormat().parse(DEFAULT_START_DATE));
      String date = reader.readLine();
      if (date != null && date.length() >= 10) {
        lastdate.setTime(GConfigs.getDateFormat().parse(date));
      }
    } catch (Exception e) {
      Logger.getLogger(STKCSVDownloader.class
              .getName()).log(Level.SEVERE, " error when load " + m_localPath
                      + "last_updated.txt", e);
    }

    //Download data of everyday since lastdate
    while (lastdate.before(today)) {
      try {
        URL url = new URL("http://openexchangerates.org/api/historical/" + GConfigs.cldToString(lastdate)
                + ".json?app_id=c428b61934464a7dbbfb641e090f634d");
        BufferedReader url_reader = new BufferedReader(new InputStreamReader(url.openStream()));
        String ptn = "\"([A-Z][A-Z][A-Z])\":(.*),";
        Matcher matcher;
        Pattern ptn_line = Pattern.compile(ptn);

        String line;
        while ((line = url_reader.readLine()) != null) {
          matcher = ptn_line.matcher(line);
          if (matcher.find()) {
            String code = matcher.group(1);
            String price = matcher.group(2);
            if (GConfigs.FX_CODES.contains("USD" + code)) {
              writeFXPriceToFile(GConfigs.cldToString(lastdate), "USD" + code, price);
            }
          }
        }

        lastdate.add(Calendar.DAY_OF_MONTH, 1);
        //Update last_updated file. Refresh
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(
                new FileWriter(m_localPath + "last_updated.txt", false)))) {
          writer.println(GConfigs.cldToString(lastdate));
        }

      } catch (MalformedURLException ex) {
        Logger.getLogger(FXCSVDownloader.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IOException ex) {
        Logger.getLogger(FXCSVDownloader.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  private static void writeFXPriceToFile(String date, String code, String price) {
    String file_path = m_localPath + code + "//" + code + ".csv";
    File file = new File(file_path);
    File folder = file.getParentFile();
    if (!folder.isDirectory()) {
      folder.mkdir();
    }

    try (PrintWriter writer = new PrintWriter(new BufferedWriter(
            new FileWriter(file_path, true)))) {
      writer.println(date + "," + price + "," + price + "," + price + "," + price + ",0");
    } catch (IOException ex) {
      Logger.getLogger(FXCSVDownloader.class.getName()).log(Level.SEVERE, null, ex);
    }

  }

  public static void main(String[] args) {
    downloadRecentData();

  }

}
