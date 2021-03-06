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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.MyUtils;
import static util.MyUtils.roundDouble;
import weka.classifiers.Classifier;
import weka.classifiers.misc.InputMappedClassifier;
import weka.classifiers.timeseries.WekaForecaster;
import weka.core.Attribute;
import weka.core.Instances;

public class Predictor {

  // Code -> Classifier id -> [Classifier_model,Performance_array]
  private final ConcurrentHashMap<String, LinkedHashMap<String, Object[]>> m_Models
          = new ConcurrentHashMap();

  private final ConcurrentHashMap<String, ConcurrentHashMap<String, ArrayList>> m_Notables = new ConcurrentHashMap();

  private final String m_TypePath;

  public Predictor(String modelType) {
    m_TypePath = modelType + "//";
  }

  // Load all models and associated performance data of a given instrument
  private LinkedHashMap<String, Object[]> loadModelsAndPerformances(String code) throws IOException, ClassNotFoundException {
//    if (m_Models.get(code) == null) {
//      m_Models.put(code, new LinkedHashMap());
//    }
    //Classifier id -> [classifier,performance_array]
    //LinkedHashMap<String, Object[]> local_map = m_Models.get(code);
    LinkedHashMap<String, Object[]> local_map = new LinkedHashMap();

    String dir = MODEL_PATH + m_TypePath + code + "//";
    File folder = new File(dir);
    File[] files = folder.listFiles();
    for (File file : files) {
      String fname = file.getName();
      String classifier_id = fname.replaceAll(".amodel", "")
              .replaceAll(".model", "");

      Object[] store = local_map.get(classifier_id);
      if (store == null) {
        local_map.put(classifier_id, new Object[4]);
        store = local_map.get(classifier_id);
      }

      ObjectInputStream objectInputStream
              = new ObjectInputStream(new FileInputStream(file));
      if (fname.endsWith(".model")) {
        Object classifier = objectInputStream.readObject();
        Instances trainHeader = (Instances) objectInputStream.readObject();
        double[] performance = (double[]) objectInputStream.readObject();
        store[0] = classifier;
        store[1] = performance;
        store[2] = trainHeader;
      } else if (fname.endsWith(".amodel")) {
        Object assistance_classifier = objectInputStream.readObject();
        store[3] = assistance_classifier;
      }
      int jj = 0;
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

    try {
      outputNotables();
    } catch (Exception ex) {
      Logger.getLogger(Predictor.class.getName()).log(Level.SEVERE, null, ex);
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
        Logger.getLogger(Predictor.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
      }
    }
  }

  /*
   * Make sure only the right model is used for predicting right result.
   * Each model is only designed to predict one of n(36 atm) class attributes.
   */
  private void makePrediction(String code) throws Exception {
    // Class_att -> rank_mark
    if (!m_Notables.containsKey(code)) {
      m_Notables.put(code, new ConcurrentHashMap<>());
    }
    ConcurrentHashMap notables_local = m_Notables.get(code);

    Instances inputValues = MyUtils.loadInstancesFromCSV(GConfigs.RESOURCE_PATH
            + this.m_TypePath + code + "//" + code + "_Training.csv");
    inputValues.deleteAttributeAt(0);

    //System.out.println("Instrument: " + code);
    try (PrintWriter writer = new PrintWriter(new BufferedWriter(
            new FileWriter(new File(REPORT_PATH + m_TypePath + code + "_Prediction.csv"), false)))) {
      for (Entry<String, Object[]> p : loadModelsAndPerformances(code).entrySet()) {
        String identity = p.getKey();
        double result;
        double confidence = 0;
        double[] evaluations = (double[]) p.getValue()[1];
        double mae = evaluations[0];
        double true_positive = evaluations[1];
        double true_random = evaluations[2];
        Instances train_header = (Instances) p.getValue()[2];
        Attribute class_attribute = train_header.classAttribute();
        String class_name = class_attribute.name();
        //Need to distinguish forecaster and classifier
        if (p.getKey().contains("Series")) {
          WekaForecaster forecaster = (WekaForecaster) p.getValue()[0];
          forecaster.primeForecaster(train_header);
          //Find the days range in the class attribute
          int days_to_advance = MyUtils.getDaysToAdvance(class_name);
          result = (double) forecaster.forecast(days_to_advance).get(0).get(0).predicted();
        } else {
          Object model = p.getValue()[0];
          InputMappedClassifier classifier = new InputMappedClassifier();
          classifier.setClassifier((Classifier) model);
          classifier.setModelHeader(train_header);
          //classifier.setTestStructure(inputValues);
          classifier.setSuppressMappingReport(true);

          Object a_model = p.getValue()[3];
          InputMappedClassifier assistance_classifier = new InputMappedClassifier();
          assistance_classifier.setClassifier((Classifier) a_model);
          assistance_classifier.setModelHeader(train_header);
          //assistance_classifier.setTestStructure(inputValues);
          assistance_classifier.setSuppressMappingReport(true);

          result = (double) classifier
                  .classifyInstance(inputValues.get(inputValues.size() - 1));
          confidence = (double) assistance_classifier
                  .classifyInstance(inputValues.get(inputValues.size() - 1));
          evaluations[4] = confidence;
        }

        if (class_attribute.isNominal()) {//If this is a nominal class
          writer.println(class_name + "," + class_attribute.value((int) result)
                  + "," + roundDouble(confidence)
                  + "," + roundDouble(true_positive) + ","
                  + roundDouble(true_random) + "," + roundDouble(mae) + "," + identity);
          result = MyUtils.NomValueToNum(class_attribute.value((int) result));
        } else {//If this is a numeric class
          writer.println(class_name + "," + roundDouble(result)
                  + "," + roundDouble(confidence)
                  + "," + roundDouble(true_positive) + ","
                  + roundDouble(true_random) + "," + roundDouble(mae) + "," + identity);
        }

        double sig = GConfigs.getSignificanceNormal(this.m_TypePath, MyUtils.getDaysToAdvance(class_name));
        if (Math.abs(result) >= sig && confidence > 0) {
          if (!notables_local.containsKey(class_name)) {
            notables_local.put(class_name, new ArrayList());
          }
          ArrayList arr = (ArrayList) notables_local.get(class_name);
          arr.add(new Object[]{identity, evaluations, result});
        }
      }
    }
  }

  private synchronized void outputNotables() throws Exception {
    try (PrintWriter writer = new PrintWriter(new BufferedWriter(
            new FileWriter(new File(REPORT_PATH + "summary.csv"), false)))) {
      for (Entry class_att_map : this.m_Notables.entrySet()) {
        //String code = (String) class_att_map.getKey();
        boolean header = false;
        ConcurrentHashMap<String, ArrayList> map = (ConcurrentHashMap) class_att_map.getValue();
        for (Entry local_map : map.entrySet()) {
          ArrayList<Object[]> arr = (ArrayList) local_map.getValue();
          //if (arr.size() >= 3) {
          if (!header) {
            writer.println("------------------------");
            writer.println(class_att_map.getKey());
            header = true;
          }
          writer.println(local_map.getKey());
          for (Object[] oray : arr) {
            double[] evals = (double[]) oray[1];
            writer.println(roundDouble((double) oray[2]) + "," + roundDouble(evals[1])
                    + "," + roundDouble(evals[2]) + ","
                    + roundDouble(evals[4]) + "," + oray[0]);
          }
          //}
          if (header) {
            writer.println("****************");
          }
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
