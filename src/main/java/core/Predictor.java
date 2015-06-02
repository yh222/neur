package core;

import static core.GConfigs.MODEL_PATH;
import core.GConfigs.MODEL_TYPES;
import static core.GConfigs.REPORT_PATH;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import util.MyUtils;
import weka.classifiers.Classifier;
import weka.classifiers.misc.InputMappedClassifier;
import weka.classifiers.timeseries.WekaForecaster;
import weka.core.Attribute;
import weka.core.Instances;

public class Predictor {

  // Code -> Classifier id -> [Classifier_model,Performance_array]
  private final ConcurrentHashMap<String, LinkedHashMap<String, Object[]>> m_Models
          = new ConcurrentHashMap();

  private final ConcurrentHashMap<String, Boolean> m_Notables = new ConcurrentHashMap();

  private final String m_TypePath;

  public Predictor(String modelType) {
    m_TypePath = modelType + "//";
  }

  // Load all models and associated performance data of a given instrument
  private LinkedHashMap<String, Object[]> loadModelsAndPerformances(String code) throws IOException, ClassNotFoundException {
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
        Object classifier = objectInputStream.readObject();
        Instances trainHeader = (Instances) objectInputStream.readObject();
        store[0] = classifier;
        store[2] = trainHeader;
      } else if (fname.endsWith(".perf")) {
        float[] performance = (float[]) objectInputStream.readObject();
        store[1] = performance;
      }
    }
    return local_map;
  }

  private void predictForLastAvaliableDate(ArrayList<String> codes) {
    ExecutorService executor = Executors.newFixedThreadPool(6);
    for (String code : codes) {
      //makePrediction(code, makeTestingValues(code, dates));
      Runnable worker = new predictCodeThread(code);
      executor.execute(worker);
    }
    executor.shutdown();
    while (!executor.isTerminated()) {
    }
  }

  private class predictCodeThread implements Runnable {

    String m_Code;

    public predictCodeThread(String code) {
      m_Code = code;
    }

    @Override
    public void run() {
      try {
        makePrediction(m_Code);
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
  private void makePrediction(String code) throws Exception {
    // Class_att -> rank_mark
    LinkedHashMap<String, Float> infusedRanks = new LinkedHashMap();
    Instances inputValues = MyUtils.loadInstancesFromCSV(GConfigs.RESOURCE_PATH
            + this.m_TypePath + code + "//" + code + "_Training.csv");
    NumberFormat defaultFormat = NumberFormat.getNumberInstance();
    defaultFormat.setMinimumFractionDigits(2);
    defaultFormat.setMaximumFractionDigits(3);

    //System.out.println("Instrument: " + code);
    try (PrintWriter writer = new PrintWriter(new BufferedWriter(
            new FileWriter(new File(REPORT_PATH + m_TypePath + code + "_Prediction.csv"), false)))) {
      for (Entry<String, Object[]> p : loadModelsAndPerformances(code).entrySet()) {
        String identity = p.getKey();
        float result;
        float[] evaluations = (float[]) p.getValue()[1];
        float false_positive = evaluations[1];
        Instances train_header = (Instances) p.getValue()[2];
        Attribute class_attribute = train_header.classAttribute();
        //Need to distinguish forecaster and classifier
        if (p.getKey().contains("Series")) {
          WekaForecaster forecaster = (WekaForecaster) p.getValue()[0];
          forecaster.primeForecaster(train_header);
          String class_name = train_header.classAttribute().name();
          String dayptn = "(\\d+)(d)";
          Pattern pattern_days = Pattern.compile(dayptn);
          Matcher matcher;
          matcher = pattern_days.matcher(class_name);
          matcher.find();
          //Find the days range in the class attribute
          int days_to_advance = Integer.parseInt(matcher.group(1));
          result = (float) forecaster.forecast(days_to_advance).get(0).get(0).predicted();
        } else {
          Object o = p.getValue()[0];
          InputMappedClassifier classifier = new InputMappedClassifier();
          classifier.setClassifier((Classifier) o);
          classifier.setModelHeader(train_header);
          classifier.setTestStructure(inputValues);
          classifier.setSuppressMappingReport(true);
          result = (float) classifier.classifyInstance(inputValues.get(inputValues.size() - 1));

        }
        if (class_attribute.isNominal()) {//If this is a nominal class
          writer.println(class_attribute.name() + "," + class_attribute.value((int) result)
                  + "," + defaultFormat.format(false_positive) + "," + identity);
        } else {//If this is a numeric class
          writer.println(class_attribute.name() + "," + defaultFormat.format(result)
                  + "," + defaultFormat.format(false_positive) + "," + identity);
        }

      }

      // Filter notable instruments
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

  public static void main(String[] args) {
    try {
      Predictor predictor = new Predictor(MODEL_TYPES.STK.name());
      predictor.predictForLastAvaliableDate(GConfigs.INSTRUMENT_CODES);

    } catch (Exception ex) {
      Logger.getLogger(Predictor.class
              .getName()).log(Level.SEVERE, null, ex);
    }

  }

}
