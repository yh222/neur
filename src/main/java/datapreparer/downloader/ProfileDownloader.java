package datapreparer.downloader;

import static core.GConfigs.INSTRUMENT_CODES;
import static core.GConfigs.RESOURCE_PATH;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProfileDownloader {

  private static void downloadProfile(String code) {
    String file_path = RESOURCE_PATH +"STK//"+ "stk_profiles.txt";

    try (PrintWriter writer = new PrintWriter(new BufferedWriter(
            new FileWriter(new File(file_path), true)))) {
      URL url = new URL("https://nz.finance.yahoo.com/q/pr?s=" + code);

      BufferedReader url_reader = new BufferedReader(
              new InputStreamReader(url.openStream()));
      //Skip the first line
      String line;
      String p = "(.*)(Sector:</td><td class=\\\"yfnc_tabledata1\\\">)(.*)"
              + "(</td></tr><tr><td class=\\\"yfnc_tablehead1\\\" width=\\\"50%\\\">Industry:</td><td class=\\\"yfnc_tabledata1\\\">)"
              + "(.*)(</td></tr><tr><td class=\\\"yfnc_tablehead1\\\" width=\\\"50%\\\">Full Time Employees:)";
      Pattern pattern_line = Pattern.compile(p);
      Matcher matcher;

      while ((line = url_reader.readLine()) != null) {
        matcher = pattern_line.matcher(line);
        if (matcher.find()) {
          String sector = matcher.group(3);
          String industry = matcher.group(5);
          writer.println(code + "," + sector + "," + industry);
        }
      }

    } catch (MalformedURLException ex) {
      Logger.getLogger(ProfileDownloader.class.getName()).log(Level.SEVERE, null, ex);
    } catch (IOException ex) {
      Logger.getLogger(ProfileDownloader.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  public static void downloadAll(){
    for(String code:INSTRUMENT_CODES){
      downloadProfile(code);
    }
  }

  public static void main(String[] args) {
    downloadAll();
  }

}
