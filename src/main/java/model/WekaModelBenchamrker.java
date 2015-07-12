package model;

import core.GConfigs;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.MyUtils;
import static util.MyUtils.roundDouble;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;

public class WekaModelBenchamrker {

  private final String m_TypePath;

  // Classifier Name -> ArrayList<[fp,fr,mae]>
  private final ConcurrentHashMap<String, ConcurrentHashMap> m_Evals
          = new ConcurrentHashMap();

  public WekaModelBenchamrker(String modelType) {
    m_TypePath = modelType + "//";
  }

  public void benchmarkByClasses(String code, ArrayList<String> classifierNames,
          ArrayList<String[]> optionsList, ArrayList<String> notes)
          throws IOException {

    Instances raw_data = MyUtils.loadInstancesFromCSV(GConfigs.RESOURCE_PATH
            + this.m_TypePath + code + "//" + code + "_Training.csv");
    //Delete date
    raw_data.deleteAttributeAt(0);

    m_Evals.put(code, new ConcurrentHashMap());
    //raw_data=weightDataByDate(raw_data);
    for (int i = 0; i < GConfigs.getClassCount(m_TypePath); i++) {
      try {
        Instances prepared_data
                = Filters.prepareInstances(new Instances(raw_data), i, m_TypePath);

        benchMarkModels("Relf", prepared_data, prepared_data,
                code, classifierNames, optionsList, notes, i);

//        CorrelationAttributeEval corrlat = new CorrelationAttributeEval();
//        selected = selectAttributes(new Instances(data_with_one_class), corrlat);
//        benchMarkModels("Corrlat", filterByClassifier(selected),
//                code, classifierNames, optionsList, notes, i);
//        benchMarkModels("Relf-Sec",
//                selectUnfavoredAttributes(code + "_" + i,
//                        new Instances(data_with_one_class)),
//                code, classifierNames, optionsList, notes, i);
      } catch (Exception ex) {
        Logger.getLogger(WekaModelBenchamrker.class.getName()).
                log(Level.SEVERE, ex.getMessage() + ", " + code, ex);
      }
    }
    writeToFile(code, raw_data);
  }

  //Class index is the relative index of class value, cannot be used directly in datasource
  public HashMap<Evaluation, double[]>
          benchMarkModels(String additionInfo,
                  Instances trainingData, Instances evaluationData,
                  String code, ArrayList<String> classifierNames,
                  ArrayList<String[]> optionsList, ArrayList<String> notes,
                  int classIndex) throws Exception {
    HashMap<Evaluation, double[]> modelBenchMap = new HashMap();
    String class_name = trainingData.classAttribute().name();
    //Find the days range in the class attribute
    int days_to_advance = MyUtils.getDaysToAdvance(class_name);

    //For each classifier
    for (int i = 0; i < classifierNames.size(); i++) {
      Instances training = new Instances(trainingData);
      Instances evaluating = new Instances(evaluationData);

      // Create classifier by name and options
      Classifier classifier = AbstractClassifier
              .forName(classifierNames.get(i), optionsList.get(i).clone());
      //Identity contains class value's name, used as model's file name
      String identity = notes.get(i) + classifierNames.get(i).split("\\.")[3]
              + "-" + class_name + "-" + additionInfo;
      String cname = classifierNames.get(i);
      //Classifier name is used to merge the classifier's performences into one line
      String classifier_name = notes.get(i)
              + cname.substring(cname.lastIndexOf(".") + 1, cname.length())
              + "-" + additionInfo;
      double[] eval_result;

      //If the class attribute is numeric but the classifier is unable to handle numeric class
      // discretize the class attribute
      if (!classifier.getCapabilities()
              .handles(Capabilities.Capability.NUMERIC_CLASS)) {
        training = Filters.discretizeClassAtt(training);
        evaluating = Filters.discretizeClassAtt(evaluating);
      }

      //Skip if the classifier cannot handle nominal class value
      if (training.classAttribute().isNominal()) {
        if (!classifier.getCapabilities().handles(Capabilities.Capability.NOMINAL_CLASS)) {
          continue;
        }
      }
//      else if (classifier_name.contains("MultilayerPerceptron")
//              || classifier_name.contains("RandomForest")
//              || classifier_name.contains("HoeffdingTree")) {
//        instances = discretizeInstances(instances);
//      }
      Instances assistance_data = new Instances(evaluating.stringFreeStructure());
      eval_result = Evaluator.evalClassifier(code, classifier,
              new Instances(training), new Instances(evaluating),
              assistance_data, days_to_advance, m_TypePath);
      if (/*eval_result[1] - eval_result[2] >= 0.15 &&*/eval_result[1] > 0) {
        Classifier noise_classifier = new RandomForest();
        noise_classifier.buildClassifier(assistance_data);
        //training = purnTraining(training, noise_classifier);
        classifier.buildClassifier(training);

        MyUtils.saveModelAndPerformance(code, identity, m_TypePath,
                classifier, training.stringFreeStructure(), eval_result);
        MyUtils.saveAssistanceModel(code, identity, m_TypePath, noise_classifier);
      }
      updateEvaluationTable(code, classifier_name, class_name, eval_result);

//      System.out.println(code + ", " + identity);
//      System.out.println("Mean absolete error:" + eval_result[0]);
//      System.out.println("False positive:" + eval_result[1]);
//      System.out.println("Random rate:" + eval_result[2]);
      // evals.put(classifier_name, list);
      // Series forecasting
      classifier = AbstractClassifier.forName(classifierNames.get(i),
              optionsList.get(i).clone());
      //Skip if this classified only handles nominal value
      // as the forecaster does not support nominal forecasting
      if (!classifier.getCapabilities()
              .handles(Capabilities.Capability.NUMERIC_CLASS)) {
        continue;
      }
//      eval_result = evaluateForecaster(classifier, class_name, new Instances(instances), days_to_advance);
//      WekaForecaster forecaster = new WekaForecaster();
//      forecaster.setBaseForecaster(classifier);
//      forecaster.setFieldsToForecast(class_name);
//      forecaster.buildForecaster(instances);
//      forecaster.primeForecaster(instances);
//      System.out.println(code + ", " + identity);
//      System.out.println("Mean absolete error (Series):" + eval_result[0]);
//      System.out.println("False positive:" + eval_result[1]);
//      System.out.println("Random rate:" + eval_result[2]);
//      if (eval_result[1] < 0.5) {
//        MyUtils.saveModelAndPerformance(code, identity + "Series", m_TypePath,
//                forecaster, instances, eval_result);
//      }
    }
    return modelBenchMap;
  }

  private void writeToFile(String code, Instances rawData) {
    File file = new File(GConfigs.REPORT_PATH + "out\\6_" + code + ".csv");

    ConcurrentHashMap<String, ArrayList> map = m_Evals.get(code);

    try (PrintWriter writer = new PrintWriter(new BufferedWriter(
            new FileWriter(file, true)))) {
      writer.print("Class Value:,");
      int att_count = rawData.numAttributes();
      int class_count = GConfigs.getClassCount(m_TypePath);
      int training_count = att_count - class_count;
      for (int i = 0; i < class_count; i++) {
        writer.print(rawData.attribute(training_count + i).name() + ",,,");
      }
      writer.println();
      writer.println("Classifier Name,False Positive,False Random"
              + ",MAE,False Positive,False Random,MAE,False Positive"
              + ",False Random,MAE,False Positive,False Random"
              + ",MAE,False Positive,False Random,MAE");
      for (Entry e2 : map.entrySet()) {
        String classifier_name = (String) e2.getKey();
        ArrayList<AbstractMap.SimpleEntry> list
                = (ArrayList<AbstractMap.SimpleEntry>) e2.getValue();
        writer.print(classifier_name + ",");
        for (AbstractMap.SimpleEntry ent : list) {
          ArrayList a = (ArrayList) ent.getValue();
          double[] es = new double[5];
          for (Object a1 : a) {
            double[] os = (double[]) a1;
            for (int j = 0; j < es.length; j++) {
              es[j] += os[j];
            }
          }
          writer.print(roundDouble(es[1] / (a.size() - 1)) + ","
                  + roundDouble(es[2] / (a.size() - 1)) + ","
                  + roundDouble(es[0] / (a.size() - 1)) + ",");

        }
        writer.println();

      }
    } catch (IOException ex) {
      Logger.getLogger(WekaModelBenchamrker.class
              .getName()).log(Level.SEVERE, null, ex);
    }
  }

  private void updateEvaluationTable(String code, String classifierName,
          String className, double[] evalResult) {

    // code -> classifier_name -> ArrayList<Entry<class_name,ArrayList<different_attempts>>>
    ConcurrentHashMap<String, ArrayList> map = m_Evals.get(code);
    if (!map.containsKey(classifierName)) {
      map.put(classifierName, new ArrayList());
    }

    ArrayList<AbstractMap.SimpleEntry<String, ArrayList<double[]>>> list
            = (ArrayList) map.get(classifierName);
    boolean found = false;
    for (Entry e : list) {
      if (e.getKey().equals(className)) {
        found = true;
        ArrayList a = (ArrayList) e.getValue();
        double[] o = (double[]) a.get(0);
        if (/*eval_result[1] - eval_result[2] >= 0.15*/evalResult[1] > 0
                && evalResult[1] < o[1]) {
          a.remove(o);
          a.add(evalResult);
        }
      }
    }
    if (!found) {
      ArrayList a = new ArrayList();
      a.add(new double[5]);
      if (/*evalResult[1] - evalResult[2] >= 0.15*/evalResult[1] > 0) {
        a.add(evalResult);
      }
      list.add(new AbstractMap.SimpleEntry(className, a));
    }
  }

  private Instances purnTraining(Instances training, Classifier noise_classifier) {
    //Instances r = new Instances(training);
    for (int i = 0; i < training.size(); i++) {
      try {
        Instance ins = training.get(i);
        double c = noise_classifier.classifyInstance(ins);
        if (c < 0.6) {
          training.remove(i);
        }
      } catch (Exception ex) {
        Logger.getLogger(WekaModelBenchamrker.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    return training;
  }

}
