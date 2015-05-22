package core;

import core.GConfigs.MODEL_TYPES;
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
import static core.GConfigs.RESOURCE_PATH;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
  public void generateTrainingData(String type, boolean createHeaders) {
    ArrayList<String> instruments = null;
    if (type.equals(MODEL_TYPES.STK.name())) {
      instruments = GConfigs.INSTRUMENT_CODES;
    } else if (type.equals(MODEL_TYPES.FX.name())) {
      instruments = GConfigs.FX_CODES;
    }

    System.out.println("Starting to generate training data.");
    //The code currently being processed
    //String currentCode;
    ExecutorService executor = Executors.newFixedThreadPool(6);
    for (String code : instruments) {
      Runnable worker = new generateDataThread(code, createHeaders);
      executor.execute(worker);
//      //currentCode = code;
//      //Get raw data organized by date for each instrument
//      ConcurrentHashMap<String, Object[]> raw_data_map = RawDataLoader.loadRawDataFromFile(code,m_TypePath);
//      STKTrainingValueMaker tvmaker = new STKTrainingValueMaker(code);
//      ArrayList<LinkedHashMap<String, Object>> training_data = new ArrayList(3000);
//      //Start data processing
//      for (String date : raw_data_map.keySet()) {
//        LinkedHashMap<String, Object> storageRow = new LinkedHashMap();
//        tvmaker.generateNominalTrainingValues(date, raw_data_map, storageRow);
//        tvmaker.generateNumericTrainingValues(date, raw_data_map, storageRow);
//        STKClassValueMaker.generateCalssValues(code, date, raw_data_map, storageRow);
//
//        //remove line with null value
//        //This is for bad entries, not for missing values. Missing values should be filled rather than leave null
//        if (hasNull(storageRow)) {
//          continue;
//        }
//        training_data.add(storageRow);
//      }
//
//        writeToFile(code, training_data, createHeaders);
    }
    executor.shutdown();
    while (!executor.isTerminated()) {
    }
    System.out.println("Training data successfully generated.");
  }

  private class generateDataThread implements Runnable {

    String m_Code;
    boolean m_Headers;

    public generateDataThread(String code, boolean headers) {
      m_Code = code;
      m_Headers = headers;
    }

    @Override
    public void run() {
      //currentCode = code;
      //Get raw data organized by date for each instrument
      ConcurrentHashMap<String, Object[]> raw_data_map = RawDataLoader.loadRawDataFromFile(m_Code, m_TypePath);
      STKTrainingValueMaker tvmaker = new STKTrainingValueMaker(m_Code);
      ArrayList<LinkedHashMap<String, Object>> training_data = new ArrayList(3000);
      //Start data processing
      for (String date : raw_data_map.keySet()) {
        LinkedHashMap<String, Object> storageRow = new LinkedHashMap();
        tvmaker.generateNominalTrainingValues(date, raw_data_map, storageRow);
        tvmaker.generateNumericTrainingValues(date, raw_data_map, storageRow);
        //remove line with null value
        //This is for bad entries, not for missing values. Missing values should be filled rather than leave null
        if (hasNull(storageRow)) {
          continue;
        }
        STKClassValueMaker.generateCalssValues(m_Code, date, raw_data_map, storageRow);
        training_data.add(storageRow);
      }

      writeToFile(m_Code, training_data, m_Headers);
    }
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
          if(item==null)
            item="?";
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
      GConfigs.ClassTags = new ArrayList();
      for (String key : keys) {
        writer.println(key);
      }
    }
  }

  public static void main(String[] args) {
//    TrainingFileGenerator tdg = new TrainingFileGenerator(MODEL_TYPES.FX.name());
//    tdg.generateTrainingData(MODEL_TYPES.FX.name(), true);
    TrainingFileGenerator tdg = new TrainingFileGenerator(MODEL_TYPES.STK.name());
    tdg.generateTrainingData(MODEL_TYPES.STK.name(), true);
  }
}
