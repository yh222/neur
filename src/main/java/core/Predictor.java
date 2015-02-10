package core;

import static core.GlobalConfigs.DATE_FORMAT;
import static core.GlobalConfigs.MODEL_PATH;
import static core.GlobalConfigs.REPORT_PATH;
import static core.GlobalConfigs.TEMP_PATH;
import datapreparer.ArffBuilder;
import datapreparer.RawDataLoader;
import datapreparer.TrainingFileGenerator;
import datapreparer.valuemaker.TrainingValueMaker;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import weka.classifiers.Classifier;
import weka.classifiers.misc.InputMappedClassifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

public class Predictor {

  // Code -> Classifier id -> [Classifier_model,Performance_array]
  static ConcurrentHashMap<String, LinkedHashMap<String, Object[]>> m_Models
          = new ConcurrentHashMap();

  /*
   * 
   */
  static void loadModels(ArrayList<String> codes) {
    ExecutorService executor = Executors.newFixedThreadPool(6);
    for (String code : codes) {
//        loadModel(code);
      Runnable worker = new loadModelThread(code);
      executor.execute(worker);
    }
    executor.shutdown();
    while (!executor.isTerminated()) {
    }
  }

  private static class loadModelThread implements Runnable {

    String m_Code;

    public loadModelThread(String code) {
      m_Code = code;
    }

    @Override
    public void run() {
      try {
        loadModel(m_Code);
      } catch (IOException | ClassNotFoundException ex) {
        Logger.getLogger(Predictor.class.getName()).log(Level.SEVERE, null, ex);
      }
    }

  }

  static void loadModel(String code) throws IOException, ClassNotFoundException {
    if (m_Models.get(code) == null) {
      m_Models.put(code, new LinkedHashMap());
    }
    //Classifier id -> [classifier,performance_array]
    LinkedHashMap<String, Object[]> local_map = m_Models.get(code);

    String dir = MODEL_PATH + code + "//";
    File folder = new File(dir);
    File[] files = folder.listFiles();
    for (File file : files) {
      String fname = file.getName();
      String classifier_id = fname.replaceAll(".model", "").replaceAll(".perf", "");

      Object[] store = local_map.get(classifier_id);
      if (store == null) {
        local_map.put(classifier_id, new Object[3]);
        store = local_map.get(classifier_id);
      }

      ObjectInputStream objectInputStream
              = new ObjectInputStream(new FileInputStream(file));
      if (fname.endsWith(".model")) {
        Classifier classifier = (Classifier) objectInputStream.readObject();
        Instances trainHeader = (Instances) objectInputStream.readObject();
        InputMappedClassifier temp = new InputMappedClassifier();
        temp.setClassifier(classifier);
        temp.setModelHeader(trainHeader);
        store[0] = temp;
        store[2] = trainHeader;
      } else if (fname.endsWith(".perf")) {
        double[] performance = (double[]) objectInputStream.readObject();
        store[1] = performance;
      }
    }
  }

  static void generateDateAndPredictAllCodes(ArrayList<String> codes, Calendar startDate, int forDays) throws Exception {

    ArrayList<String> dates = produceDates(startDate, forDays);
    //ExecutorService executor = Executors.newFixedThreadPool(6);
    for (String code : codes) {
      makePrediction(code, makeTestingValues(code, dates));
      //Runnable worker = new predictCodeThread(code, dates);
      //executor.execute(worker);
    }
    //executor.shutdown();
    //while (!executor.isTerminated()) {
    //}
  }

  private static class predictCodeThread implements Runnable {

    String m_Code;
    ArrayList<String> m_Dates;

    public predictCodeThread(String code, ArrayList<String> dates) {
      m_Code = code;
      m_Dates = dates;
    }

    @Override
    public void run() {
      try {
        makePrediction(m_Code, makeTestingValues(m_Code, m_Dates));
      } catch (IOException | ClassNotFoundException ex) {
        Logger.getLogger(Predictor.class.getName()).log(Level.SEVERE, null, ex);
      } catch (Exception ex) {
        Logger.getLogger(Predictor.class.getName()).log(Level.SEVERE, null, ex);
      }
    }

  }

  /*
   * Make sure only the right model is used for predicting right result.
   * Each model is only designed to predict one of n(36 atm) class attributes.
   */
  static void makePrediction(String code, Instances inputValues) throws Exception {
    String ptn = "-(.*)-";
    Pattern pattern_line = Pattern.compile(ptn);
    Matcher matcher;
    System.out.println("Instrument: " + code);
    try (PrintWriter writer = new PrintWriter(new BufferedWriter(
            new FileWriter(new File(REPORT_PATH + code + "_Prediction.csv"), false)))) {
      for (Entry<String, Object[]> p : m_Models.get(code).entrySet()) {
        String id = p.getKey();
        matcher = pattern_line.matcher(id);
        matcher.find();
        String classAtt = matcher.group();
        //Remove  '-'
        classAtt = classAtt.substring(1, classAtt.length() - 1);
        Instances trainHeader = (Instances) p.getValue()[2];

        if (classAtt.equals(trainHeader.classAttribute().name())) {
          InputMappedClassifier c = (InputMappedClassifier) p.getValue()[0];
          c.setTestStructure(inputValues);
          c.setSuppressMappingReport(true);
          double[] performance = (double[]) p.getValue()[1];
          for (Instance i : inputValues) {
            int result = (int) c.classifyInstance(i);
            System.out.print(classAtt + ", " + parseResult(result));
            System.out.println(", " + performance[result]);
            writer.println(classAtt + "," + parseResult(result)
                    + "," + performance[result] + "," + id);
          }
        }
      }
    }
  }

  /*
   * Generate a temporary arff file for prediction according to the input dates,
   * then immediately load the file as Istances object for further prediction.
   */
  static Instances makeTestingValues(String code, ArrayList<String> dates) throws IOException, Exception {
    ConcurrentHashMap<String, Object[]> raw_data_map = RawDataLoader.loadRawDataFromFile(code);
    TrainingValueMaker tvmaker = new TrainingValueMaker(code);
    ArrayList<LinkedHashMap<String, Object>> prediction_data = new ArrayList();
    LinkedHashMap<String, Object> storageRow;
    for (String date : dates) {
      storageRow = new LinkedHashMap();
      tvmaker.generateNominalTrainingValues(date, raw_data_map, storageRow);
      tvmaker.generateNumericTrainingValues(date, raw_data_map, storageRow);
      if (TrainingFileGenerator.hasNull(storageRow)) {
        continue;
      }
      prediction_data.add(storageRow);
    }

    String arff = TEMP_PATH + code + "_Prediction.arff";
    ArffBuilder.buildArffHeader(prediction_data.get(0).keySet(), arff, code);
    PrintWriter arffWriter = new PrintWriter(new BufferedWriter(
            new FileWriter(arff, true)));
    for (LinkedHashMap<String, Object> row : prediction_data) {
      String temp = "";
      for (Object item : row.values()) {
        temp += (item.toString() + ",");
      }
      arffWriter.println(temp.substring(0, temp.length() - 1));
      arffWriter.flush();
    }
    ConverterUtils.DataSource trainSource;
    trainSource = new ConverterUtils.DataSource(arff);
    Instances testInstances = trainSource.getDataSet();
    return testInstances;
  }

  public static void main(String[] args) {
    try {
      loadModels(GlobalConfigs.INSTRUMENT_CODES);
      generateDateAndPredictAllCodes(GlobalConfigs.INSTRUMENT_CODES, Calendar.getInstance(), 3);
//      ArrayList<String> dates = new ArrayList();
//      dates.add("2015-02-06");
//      dates.add("2015-02-05");
//      dates.add("2015-02-04");
//      dates.add("2015-02-03");
//      dates.add("2015-02-02");
//
//      Instances testing = makeTestingValues("AAPL", dates);
//      makePrediction("AAPL", testing);

    } catch (Exception ex) {
      Logger.getLogger(Predictor.class.getName()).log(Level.SEVERE, null, ex);
    }

  }

  private static ArrayList<String> produceDates(Calendar startDate, int forDays) {
    ArrayList<String> dates = new ArrayList();
    for (int i = 0; i < forDays; i++) {
      if (startDate.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
              || startDate.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
        i--;
      } else {
        dates.add(DATE_FORMAT.format(startDate.getTime()));
      }
      startDate.add(Calendar.DAY_OF_MONTH, -1);
    }
    return dates;
  }

  private static String parseResult(int i) {
    switch (i) {
      case 0:
        return "Very_Low";
      case 1:
        return "Low";
      case 2:
        return "Little_Low";
      case 3:
        return "Stay";
      case 4:
        return "Little_High";
      case 5:
        return "High";
      case 6:
        return "Very_High";
      default:
        return "Invalid index";
    }
  }
}
