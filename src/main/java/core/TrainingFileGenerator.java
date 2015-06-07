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
import datapreparer.RawDataLoader;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import util.MyUtils;

/**
 * Generate training data by raw .csv data downloaded by CSVDownloader
 *
 * Each data set stored as an ArrayList of double arrays
 */
public class TrainingFileGenerator {

  private final String m_TypePath;
  private final String m_LocalPath;

  public TrainingFileGenerator(String modelType) {
    m_TypePath = modelType + "//";
    m_LocalPath = GConfigs.RESOURCE_PATH + m_TypePath;
  }

  public void generateTrainingData(String type, boolean createHeaders) {
    ArrayList<String> instruments = null;

    if (type.equals(MODEL_TYPES.STK.name())) {
      instruments = GConfigs.INSTRUMENT_CODES;
    } else if (type.equals(MODEL_TYPES.FX.name())) {
      instruments = GConfigs.FX_CODES;
    }

    System.out.println("Generating training data.");

    ExecutorService executor = Executors.newFixedThreadPool(6);
    for (String code : instruments) {
      Runnable worker = new generateDataThread(code, createHeaders);
      executor.execute(worker);
    }
    executor.shutdown();
    while (!executor.isTerminated()) {
      //Todo: timeout
    }
    System.out.println("Training data generated.");
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
      ConcurrentHashMap<String, Object[]> raw_data_map = RawDataLoader.loadRawData(m_Code, m_TypePath);
      STKTrainingValueMaker tvmaker = new STKTrainingValueMaker(m_Code);
      ArrayList<LinkedHashMap<String, Object>> training_data = new ArrayList(600);
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
        //Null values are allowed for class attributes, and will be recognized as 
        // missing
        STKClassValueMaker.generateCalssValues(m_Code, date, raw_data_map, storageRow);
        training_data.add(storageRow);
      }
      //Sort according to date, ascending
      training_data.sort((p1, p2) -> MyUtils.parseToISO((String) p1.get("N_Date"))
              .compareTo(MyUtils.parseToISO((String) p2.get("N_Date"))));

      writeFullTrainingFile(m_Code, training_data, m_Headers);
      writeWekaEvaluationFile(m_Code, training_data, m_Headers);
    }

    public void writeFullTrainingFile(String code, ArrayList<LinkedHashMap<String, Object>> training_data, boolean createHeaders) {
      File training_file = new File(m_LocalPath + code + "//" + code + "_Training.csv");
      
      try (PrintWriter writer = new PrintWriter(new BufferedWriter(
              new FileWriter(training_file, false)))) {
        String temp = "";
        Set<String> keys = training_data.get(0).keySet();
        updateAttributeCount(keys);
        // Create CSV header
        if (createHeaders) {
          for (String key : keys) {
            temp += key + ",";
          }
          writer.println(temp.substring(0, temp.length() - 1));
        }

        // Concatinate items into a string line and write to file
        for (LinkedHashMap<String, Object> row : training_data) {
          temp = "";
          for (Object item : row.values()) {
            if (item == null) { //Define allowed null as missing value
              item = "?";
            }
            temp += (item.toString() + ",");
          }
          //remove last comma and write to file
          writer.println(temp.substring(0, temp.length() - 1));
        }
      } catch (IOException ex) {
        Logger.getLogger(TrainingFileGenerator.class.getName()).log(Level.SEVERE, "Failed to write to training file for: " + code, ex);
      }
      
    }

    // The evaluation file is used for manual evaluation, can be disabled if not in use.
    private void writeWekaEvaluationFile(String code, ArrayList<LinkedHashMap<String, Object>> training_data, boolean createHeaders) {
      weka.filters.unsupervised.attribute.Discretize disc=new weka.filters.unsupervised.attribute.Discretize();
      
      
      try {
        File weka_training_file = new File(m_LocalPath + code + "//" + code + "_WekaTraining.csv");
        File weka_testing_file = new File(m_LocalPath + code + "//" + code + "_WekaTesting.csv");
        PrintWriter tran_writer = new PrintWriter(new BufferedWriter(
                new FileWriter(weka_training_file, false)));
        PrintWriter test_writer = new PrintWriter(new BufferedWriter(
                new FileWriter(weka_testing_file, false)));
        String temp = "";
        Set<String> keys = training_data.get(0).keySet();
        updateAttributeCount(keys);
        // Create CSV header
        if (createHeaders) {
          for (String key : keys) {
            temp += key + ",";
          }
          tran_writer.println(temp.substring(0, temp.length() - 1));
          test_writer.println(temp.substring(0, temp.length() - 1));
        }
        int count = training_data.size();
        // Concatinate items into a string line and write to file
        for (LinkedHashMap<String, Object> row : training_data) {
          temp = "";
          for (Object item : row.values()) {
            if (item == null) { //Define allowed null as missing value
              item = "?";
            }
            temp += (item.toString() + ",");
          }
          //remove last comma and write to file
          if (count > 30) {
            tran_writer.println(temp.substring(0, temp.length() - 1));
          } else {
            test_writer.println(temp.substring(0, temp.length() - 1));
          }
          count--;
        }
        tran_writer.close();
        test_writer.close();
      } catch (IOException ex) {
        Logger.getLogger(TrainingFileGenerator.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
      }
    }
  }

  // Check if a data row contains null value
  protected static boolean hasNull(LinkedHashMap<String, Object> row) {
    for (Object o : row.values()) {
      if (o == null) {
        return true;
      }
    }
    return false;
  }

  //Count the amount of attributes. Used as memory when selecting attributes
  private void updateAttributeCount(Set<String> keys) throws IOException {
    File attCount_file = new File(m_LocalPath + "attCount");
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
