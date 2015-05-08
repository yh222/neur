package core;

import static core.GConfigs.MODEL_PATH;
import core.GConfigs.MODEL_TYPES;
import static core.GConfigs.REPORT_PATH;
import static core.GConfigs.TEMP_PATH;
import datapreparer.ArffHeadBuilder;
import datapreparer.RawDataLoader;
import datapreparer.valuemaker.STKTrainingValueMaker;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.text.NumberFormat;
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
  private final ConcurrentHashMap<String, LinkedHashMap<String, Object[]>> m_Models
          = new ConcurrentHashMap();

  private final ConcurrentHashMap<String, Boolean> m_Notables = new ConcurrentHashMap();

  private final String m_TypePath;

  public Predictor(String modelType) {
    m_TypePath = modelType + "//";
  }

  private LinkedHashMap<String, Object[]> loadModel(String code) throws IOException, ClassNotFoundException {
    if (m_Models.get(code) == null) {
      m_Models.put(code, new LinkedHashMap());
    }
    //Classifier id -> [classifier,performance_array]
    //LinkedHashMap<String, Object[]> local_map = m_Models.get(code);
    LinkedHashMap<String, Object[]> local_map = new LinkedHashMap();

    String dir = MODEL_PATH + m_TypePath + code + "//";
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
        float[] performance = (float[]) objectInputStream.readObject();
        store[1] = performance;
      }
    }

    return local_map;
  }

  private void generateDateAndPredictAllCodes(ArrayList<String> codes, Calendar startDate, int forDays) throws Exception {

    ArrayList<String> dates = produceDates(startDate, forDays);
    ExecutorService executor = Executors.newFixedThreadPool(6);
    for (String code : codes) {
      //makePrediction(code, makeTestingValues(code, dates));
      Runnable worker = new predictCodeThread(code, dates);
      executor.execute(worker);
    }
    executor.shutdown();
    while (!executor.isTerminated()) {
    }
  }

  private class predictCodeThread implements Runnable {

    String m_Code;
    ArrayList<String> m_Dates;

    public predictCodeThread(String code, ArrayList<String> dates) {
      m_Code = code;
      m_Dates = new ArrayList(dates);
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
  private void makePrediction(String code, Instances inputValues) throws Exception {
    // Class_att -> rank_mark
    LinkedHashMap<String, Float> infusedRanks = new LinkedHashMap();

    NumberFormat defaultFormat = NumberFormat.getNumberInstance();
    defaultFormat.setMinimumFractionDigits(2);
    defaultFormat.setMaximumFractionDigits(3);
    String ptn = "_(.*)d";
    Pattern pattern_line = Pattern.compile(ptn);
    String ptn2 = "\\d+";
    Pattern pattern_digit = Pattern.compile(ptn2);
    Matcher matcher;
    //System.out.println("Instrument: " + code);
    try (PrintWriter writer = new PrintWriter(new BufferedWriter(
            new FileWriter(new File(REPORT_PATH + m_TypePath + code + "_Prediction.csv"), false)))) {
      for (Entry<String, Object[]> p : loadModel(code).entrySet()) {
        String id = p.getKey();
        matcher = pattern_line.matcher(id);
        matcher.find();
        String classAtt = matcher.group();
        //Remove  '_'
        classAtt = classAtt.substring(1, classAtt.length());
        if (!infusedRanks.containsKey(classAtt)) {
          infusedRanks.put(classAtt, 0.0f);
        }
        Instances trainHeader = (Instances) p.getValue()[2];
        matcher = pattern_digit.matcher(classAtt);
        matcher.find();
        int days = Integer.parseInt(matcher.group());

        //if (classAtt.equals(trainHeader.classAttribute().name())) {
        InputMappedClassifier c = (InputMappedClassifier) p.getValue()[0];
        c.setTestStructure(inputValues);
        c.setSuppressMappingReport(true);
        float[] performance = (float[]) p.getValue()[1];
        for (Instance i : inputValues) {
          float result = (float) c.classifyInstance(i);
          if (performance[0] != -1) {//If this is a nominal class
            float classifier_performance = performance[(int) result];
            writer.println(classAtt + "," + parseResultToClasses((int) result)
                    + "," + defaultFormat.format(classifier_performance) + "," + id);

            //e.g. rank for VC_Highest5Day = 2 (little_low)
            float rank = infusedRanks.get(classAtt);
            if ((result == 0) || (result == 1)
                    || (result == 5) || (result == 6)) {
              rank += 1;
            } else if (result == 2 || result == 4) {
              rank += 1;
            }
            infusedRanks.put(classAtt, rank);
//              if ((days <= 7 && result != 3) || (result == 0) || (result == 1)
//                      || (result == 5) || (result == 6)) {
//                float rank = infusedRanks.get(classAtt);
//                rank += (result - 3) * classifier_performance;
//                infusedRanks.put(classAtt, rank);
//              }

          } else {//If this is a numeric class
            float rootMeanSquaredError = performance[1];
            float rootRelativeSquaredError = performance[2];
            writer.println(classAtt + "," + defaultFormat.format(result)
                    + "," + defaultFormat.format(rootMeanSquaredError) + ","
                    + defaultFormat.format(rootRelativeSquaredError) + "," + id);

            float rank = infusedRanks.get(classAtt);
            if (result <= -2 * GConfigs.getSignificanceNormal(code)
                    || result >= 2 * GConfigs.getSignificanceNormal(code)) {
              rank += 1;
            } else if (result <= -1 * GConfigs.getSignificanceNormal(code)
                    || result >= GConfigs.getSignificanceNormal(code)) {
              rank += 1;
            }
            infusedRanks.put(classAtt, rank);
          }
          //  }
        }
      }

      try (PrintWriter writer2 = new PrintWriter(new BufferedWriter(
              new FileWriter(new File(REPORT_PATH + m_TypePath + code + "_InfusedPrediction.csv"), false)))) {
        boolean notable = false;
        for (Entry<String, Float> e : infusedRanks.entrySet()) {
          //writer2.println(e.getKey() + "," + defaultFormat.format(e.getValue()));
          if (e.getValue() / (inputValues.size()) == 4) {
            notable = true;
          }
        }
        if (notable) {
          System.out.println(code + " is notable");
        }
      }
    }
  }

  /*
   * Generate a temporary arff file for prediction according to the input dates,
   * then immediately load the file as Istances object for further prediction.
   */
  private Instances makeTestingValues(String code, ArrayList<String> dates) throws IOException, Exception {
    ConcurrentHashMap<String, Object[]> raw_data_map = RawDataLoader.loadRawDataFromFile(code, m_TypePath);
    STKTrainingValueMaker tvmaker = new STKTrainingValueMaker(code);
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
    ArffHeadBuilder.buildArffHeader(prediction_data.get(0).keySet(), arff, code);
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

  private ArrayList<String> produceDates(Calendar startDate, int forDays) {
    ArrayList<String> dates = new ArrayList();
    for (int i = 0; i < forDays; i++) {
      if (startDate.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
              || startDate.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
        i--;
      } else {
        dates.add(GConfigs.getDateFormat().format(startDate.getTime()));
      }
      startDate.add(Calendar.DAY_OF_MONTH, -1);
    }
    return dates;
  }

  private String parseResultToClasses(int i) {
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

  public static void main(String[] args) {
    try {
      Predictor predictor = new Predictor(MODEL_TYPES.STK.name());

      //loadModels(GlobalConfigs.INSTRUMENT_CODES);
      predictor.generateDateAndPredictAllCodes(GConfigs.INSTRUMENT_CODES, Calendar.getInstance(), 2);
//      ArrayList<String> dates = new ArrayList();
//      dates.add("2015-02-06");
//      dates.add("2015-02-05");
//      dates.add("2015-02-04");
//      dates.add("2015-02-03");
//      dates.add("2015-02-02");
//      Instances testing = makeTestingValues("AAPL", dates);
//      makePrediction("AAPL", testing);

//      System.out.println("Notables:");
//      for (String s : m_Notables.keySet()) {
//        System.out.println(s);
//      }
    } catch (Exception ex) {
      Logger.getLogger(Predictor.class
              .getName()).log(Level.SEVERE, null, ex);
    }

  }

}
