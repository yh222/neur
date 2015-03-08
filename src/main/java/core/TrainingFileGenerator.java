package core;

import core.GlobalConfigs.MODEL_TYPES;
import datapreparer.valuemaker.STKClassValueMaker;
import datapreparer.valuemaker.STKTrainingValueMaker;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import datapreparer.ArffHeadBuilder;
import datapreparer.RawDataLoader;
import static core.GlobalConfigs.RESOURCE_PATH;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * Generate training data by raw .csv data downloaded by CSVDownloader
 *
 * Each data set stored as an ArrayList of float arrays
 */
public class TrainingFileGenerator {

  private final String m_TypePath;

  public TrainingFileGenerator(String modelType) {
    m_TypePath = modelType + "//";
  }

  //static ConcurrentHashMap<String, ArrayList> m_TrainingDataMap = new ConcurrentHashMap();
  public void generateTrainingData(boolean writeToFile, boolean createHeaders) {
    ArrayList<String> instruments = GlobalConfigs.INSTRUMENT_CODES;
    System.out.println("Starting to generate training data.");
    //The code currently being processed
    //String currentCode;
    for (String code : instruments) {
      //currentCode = code;
      //Get raw data organized by date for each instrument
      ConcurrentHashMap<String, Object[]> raw_data_map = RawDataLoader.loadRawDataFromFile(code,m_TypePath);
      STKTrainingValueMaker tvmaker = new STKTrainingValueMaker(code);
      ArrayList<LinkedHashMap<String, Object>> training_data = new ArrayList(3000);
      //Start data processing
      for (String date : raw_data_map.keySet()) {
        LinkedHashMap<String, Object> storageRow = new LinkedHashMap();
        tvmaker.generateNominalTrainingValues(date, raw_data_map, storageRow);
        tvmaker.generateNumericTrainingValues(date, raw_data_map, storageRow);
        STKClassValueMaker.generateCalssValues(code, date, raw_data_map, storageRow);

        //remove line with null value
        //This is for bad entries, not for missing values. Missing values should be filled rather than leave null
        if (hasNull(storageRow)) {
          continue;
        }
        training_data.add(storageRow);
      }

      if (writeToFile) {
        writeToFile(code, training_data, createHeaders);
      }
    }
    System.out.println("Training data successfully generated.");
  }

  public void writeToFile(String code, ArrayList<LinkedHashMap<String, Object>> training_data, boolean createHeaders) {
    File training_file = new File(RESOURCE_PATH + m_TypePath + code + "//" + code + "_Training.csv");
    try (PrintWriter writer = new PrintWriter(new BufferedWriter(
            new FileWriter(training_file, false)))) {
      String temp = "";
      Set<String> keys = training_data.get(0).keySet();
      updateAttributeCount(keys);
      if (createHeaders) {
        for (String key : keys) {
          temp += key + ",";
        }
        writer.println(temp.substring(0, temp.length() - 1));
      }
      String arff = RESOURCE_PATH + m_TypePath + code + "//" + code + "_Training.arff";
      ArffHeadBuilder.buildArffHeader(keys, arff, code);
      PrintWriter arffWriter = new PrintWriter(new BufferedWriter(
              new FileWriter(arff, true)));

      for (LinkedHashMap<String, Object> row : training_data) {
        temp = "";
        for (Object item : row.values()) {
          temp += (item.toString() + ",");
        }
        //remove last comma and write to file
        writer.println(temp.substring(0, temp.length() - 1));
        arffWriter.println(temp.substring(0, temp.length() - 1));
      }
      arffWriter.flush();
    } catch (IOException ex) {
      Logger.getLogger(TrainingFileGenerator.class.getName()).log(Level.SEVERE, "Failed to write to training file for: " + code, ex);
    }
  }

  /*
   * Check if a data row contains null value
   */
  public static boolean hasNull(LinkedHashMap<String, Object> row) {
    for (Object o : row.values()) {
      if (o == null) {
        return true;
      }
    }
//
//    if (row.size() > max) {
//      max = row.size();
//    } else if (row.size() < max) {
//      return true;
//    }
    return false;
  }

//    private Object[] jointArray(Object[] array1, Object[] array2, Object[] array3) {
//        Object[] r = new Object[array1.length + array2.length + array3.length];
//
//        System.arraycopy(array1, 0, r, 0, array1.length);
//        System.arraycopy(array2, 0, r, array1.length, array2.length);
//        System.arraycopy(array3, 0, r, array1.length + array2.length, array3.length);
//
//        return r;
//    }
  private void updateAttributeCount(Set<String> keys) throws IOException {
    File attCount_file = new File(RESOURCE_PATH + m_TypePath + "attCount");
    try (PrintWriter writer = new PrintWriter(new BufferedWriter(
            new FileWriter(attCount_file, false)))) {
      GlobalConfigs.ClassTags = new ArrayList();
      for (String key : keys) {
        writer.println(key);
      }
    }
  }

  public static void main(String[] args) {
    TrainingFileGenerator tdg = new TrainingFileGenerator(MODEL_TYPES.STK.name());
    tdg.generateTrainingData(true, true);
  }
}
