package datapreparer;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.*;
import core.GlobalConfigs;
import static core.GlobalConfigs.DATE_FORMAT;
import calculator.StatCalculator;
import static core.GlobalConfigs.DEFAULT_START_DATE;
import java.text.ParseException;
import java.util.concurrent.TimeUnit;

/**
 * Download raw instrument data from web-server in .csv format
 *
 * Need "instrument_list.txt" to get list of instrument codes
 *
 * If target .csv file already exists, append new data to the end of the .csv
 * file.
 */
public class CSVDownloader {

    static String DEFAULT_PATH = GlobalConfigs.DEFAULT_PATH + "resources\\";

    public static void updateRawDataFromYahoo(String yhooParameter, ArrayList<String> codeList) {
        String type = "";
        if (yhooParameter.equals("v")) {
            type = "_Dividend";
        }

        ArrayList<String> instruments = codeList;
        final Calendar today = Calendar.getInstance();
        for (String code : instruments) {
            Calendar start_date = Calendar.getInstance();
            
            try {
                start_date.setTime(DATE_FORMAT.parse(DEFAULT_START_DATE));
            } catch (ParseException ex) {
                Logger.getLogger(CSVDownloader.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            File folder = new File(DEFAULT_PATH + code);
            if (!folder.isDirectory()) {
                folder.mkdir();
            }
            File file = new File(DEFAULT_PATH + code + "//" + code + type + ".csv");
            if (file.isFile()) {
                try (BufferedReader reader = new BufferedReader(
                        new FileReader(file))) {
                    String line;
                    String last_line = "";
                    while ((line = reader.readLine()) != null) {
                        last_line = line;
                    }
                    if (last_line.length() > 10) {
                        start_date.setTime(DATE_FORMAT.parse(last_line.substring(0, 10)));
                        start_date.add(Calendar.DATE, 1);
                        if (DATE_FORMAT.format(start_date.getTime()).equals(DATE_FORMAT.format(today.getTime()))) {
                            //if the date is today, skip
                            System.out.println(code + " is up to date, skip.");
                            continue;
                        }
                    }
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(CSVDownloader.class.getName()).log(Level.SEVERE, " failed to create data file for: " + code, ex);
                } catch (Exception e) {
                    Logger.getLogger(CSVDownloader.class.getName()).log(Level.SEVERE, " error when load data file:" + code, e);
                }
            } else {  // Create a new datafile
                try {
                    file.createNewFile();
                } catch (IOException ex) {
                    Logger.getLogger(CSVDownloader.class.getName()).log(Level.SEVERE, " failed to create data file for: " + code, ex);
                }
            }
            try {
                int dayDiff = (int) StatCalculator.getDateDiff(start_date.getTime(), today.getTime(), TimeUnit.DAYS);

                if (!yhooParameter.equals("")) {
                    //download data from yahoo
                    URL url = new URL("http://ichart.finance.yahoo.com/table.csv?s=" + code + "&a=" + start_date.get(Calendar.MONTH) + "&b=" + start_date.get(Calendar.DAY_OF_MONTH) + "&c=" + start_date.get(Calendar.YEAR)
                            + "&d=" + today.get(Calendar.MONTH) + "&e=" + today.get(Calendar.DAY_OF_MONTH) + "&f=" + today.get(Calendar.YEAR)
                            + "&g=" + yhooParameter + "&ignore=.csv");
                    System.out.println("Downloading data for " + code + " from " + url);
                    downloadDataFromURL(url, file, true, dayDiff);
                } else {
                    //download from quotemedia
                    URL url = new URL("http://app.quotemedia.com/quotetools/getHistoryDownload.csv?&webmasterId=501&startDay=" + start_date.get(Calendar.DAY_OF_MONTH) + "&startMonth=" + start_date.get(Calendar.MONTH) + "&startYear=" + start_date.get(Calendar.YEAR)
                            + "&endDay=" + today.get(Calendar.DAY_OF_MONTH) + "&endMonth=" + today.get(Calendar.MONTH) + "&endYear=" + today.get(Calendar.YEAR)
                            + "&isRanged=false&symbol=" + code + "&ignore=.csv");
                    System.out.println("Downloading data for " + code + " from " + url);
                    downloadDataFromURL(url, file, false, dayDiff);
                }
            } catch (MalformedURLException ex) {
                Logger.getLogger(CSVDownloader.class.getName()).log(Level.SEVERE, null, ex);
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
                Logger.getLogger(CSVDownloader.class.getName()).log(Level.SEVERE, " failed to fetch data from: " + url, ex);
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(CSVDownloader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CSVDownloader.class.getName()).log(Level.SEVERE, " error reading from url", ex);
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

}
