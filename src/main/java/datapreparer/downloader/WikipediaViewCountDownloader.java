package datapreparer.downloader;

import core.GConfigs;
import static core.GConfigs.DEFAULT_PATH;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikipediaViewCountDownloader {

  public static void downloadWikipeidaCounts() {
    DateFormat format = new SimpleDateFormat("yyyyMM", Locale.ENGLISH);
    ArrayList<String> instruments = GConfigs.INSTRUMENT_CODES;
    final Calendar today = Calendar.getInstance();
    for (String code : instruments) {
      Calendar start_date = Calendar.getInstance();
      String file_path = DEFAULT_PATH + "//wikipedia//" + GConfigs.MODEL_TYPES.STK.name() + "//" + code + "//" + code + "_WikiView.csv";

      if (STKCSVDownloader.isUpToDate(start_date, code, file_path)) {
        continue;
      }
      String year_month = format.format(start_date.getTime());
      int rest_count = 10;
      while (start_date.get(Calendar.MONTH) != today.get(Calendar.MONTH)
              || start_date.get(Calendar.YEAR) != today.get(Calendar.YEAR)) {
        downloadAndSaveWikiCountForMonth(start_date, year_month, GConfigs.WIKI_TITTLES.get(code), new File(file_path));
        start_date.add(Calendar.MONTH, 1);
        start_date.set(Calendar.DAY_OF_MONTH, 01);
        year_month = format.format(start_date.getTime());
        //have a rest don't attact
        if (rest_count-- <= 0) {
          rest_count = 10;
          try {
            TimeUnit.MINUTES.sleep(1);
          } catch (InterruptedException ex) {
            Logger.getLogger(WikipediaViewCountDownloader.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      }

    }

  }

  //Down
  private static void downloadAndSaveWikiCountForMonth(Calendar startDate, String yearMonth, String wikiTittle, File outFile) {
    try {
      URL url = new URL("http://stats.grok.se/en/" + yearMonth + "/" + wikiTittle);
      BufferedReader url_reader = new BufferedReader(new InputStreamReader(url.openStream()));
      //Skip the first line
      String line;
      String p = "(.*)(line1 = )(.*)";
      Pattern pattern_line = Pattern.compile(p);
      Matcher matcher;

      TreeMap<Calendar, String> temp_date = new TreeMap();

      while ((line = url_reader.readLine()) != null) {
        matcher = pattern_line.matcher(line);
        //Get the line that contains count data
        if (matcher.find()) {
          line = line.substring(0, line.length() - 3);
          //Split line into pairs
          String[] splits = line.split("],");
          String date_string, value_string;
          String p2 = "(\\d\\d\\d\\d-\\d\\d-\\d\\d)";
          Pattern pattern_date = Pattern.compile(p2);
          //For each pair, get date and value, then save to map
          for (String split : splits) {
            String[] small_split = split.split(",");
            matcher = pattern_date.matcher(small_split[0]);
            matcher.find();
            date_string = matcher.group();
            value_string = small_split[1];
            if (value_string.equals("0")) {
              //skip err data
              continue;
            }
            Calendar tempdate = Calendar.getInstance();
            tempdate.setTime(GConfigs.getDateFormat().parse(date_string));
            temp_date.put(tempdate, value_string);
          }
          break;
        }
      }

      try (PrintWriter writer = new PrintWriter(new BufferedWriter(
              new FileWriter(outFile, true)))) {
        for (Entry<Calendar, String> e : temp_date.entrySet()) {
          if (e.getKey().after(startDate)) {
            writer.println(GConfigs.getDateFormat().format(e.getKey().getTime()) + "," + e.getValue());
          }
        }
      }

    } catch (MalformedURLException ex) {
      Logger.getLogger(WikipediaViewCountDownloader.class.getName()).log(Level.SEVERE, null, ex);
    } catch (IOException | ParseException ex) {
      Logger.getLogger(WikipediaViewCountDownloader.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  public static void main(String[] args) {
    downloadWikipeidaCounts();
  }
}
