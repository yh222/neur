package datapreparer;

import core.GConfigs;
import core.TrainingFileGenerator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.AccessDB;
import util.MyUtils;
import util.Tag;

public class SectorTrainingDataMerger {

  private static final String m_LocalPath = GConfigs.RESOURCE_PATH + "STK\\_Sectors\\";
  private static final String m_CSVPath = GConfigs.RESOURCE_PATH + "STK\\";

  public static void mergeTrainingDataByIndustry() {
    MyUtils.findOrCreateFolder(m_LocalPath);
    Set<String> industries = AccessDB.loadIndustries();
    for (String industry : industries) {
      //For each industry, find all tags associated to it, add all data to 
      //a list that to be sorted by date
      ArrayList<Tag> tags = AccessDB.queryTagsByIndustry(industry);
      TreeSet<SimpleEntry<String, String>> sorted_set
              = new TreeSet<>((p1, p2) -> p1.getKey().compareTo(p2.getKey()));
      String header = "";
      for (Tag t : tags) {
        try (BufferedReader reader = new BufferedReader(
                new FileReader(m_CSVPath + "\\" + t.m_Name + "\\"
                        + t.m_Name + "_Training.csv"))) {
          String line;
          while ((line = reader.readLine()) != null) {
            String s1 = line.split(",")[0];
            if (s1.equals("N_Date")) {
              header = line;
            } else {
              sorted_set.add(new SimpleEntry(s1 + t.m_Name, line));
            }
          }
        } catch (Exception ex) {
          Logger.getLogger(TrainingFileGenerator.class.getName())
                  .log(Level.SEVERE, "Error when loading csv file for raw data.", ex);
        }
      }

      try (PrintWriter writer = new PrintWriter(new BufferedWriter(
              new FileWriter(m_LocalPath + industry + ".csv", false)))) {
        writer.println(header);
        for (SimpleEntry s : sorted_set) {
          writer.println(s.getValue());
        }
      } catch (IOException ex) {
        Logger.getLogger(SectorTrainingDataMerger.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  //Test
  public static void main(String[] args) {
    mergeTrainingDataByIndustry();
    int l = 1;
  }
}
