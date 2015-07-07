package model;

import core.GConfigs;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.MyUtils;
import static util.MyUtils.roundDouble;
import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.CorrelationAttributeEval;
import weka.attributeSelection.Ranker;
import weka.attributeSelection.ReliefFAttributeEval;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.timeseries.WekaForecaster;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;

public class WekaModelBenchamrker {

  private final String m_TypePath;
  private final int m_DaysForEval = 60;

  // Classifier Name -> ArrayList<[fp,fr,mae]>
  private final ConcurrentHashMap<String, ConcurrentHashMap> m_Evals = new ConcurrentHashMap();

  public WekaModelBenchamrker(String modelType) {
    m_TypePath = modelType + "//";
  }

  public void benchmarkByClasses(String code, ArrayList<String> classifierNames, ArrayList<String[]> optionsList, ArrayList<String> notes) throws IOException {

    Instances raw_data = MyUtils.loadInstancesFromCSV(GConfigs.RESOURCE_PATH
            + this.m_TypePath + code + "//" + code + "_Training.csv");
    //Delete date
    raw_data.deleteAttributeAt(0);

    m_Evals.put(code, new ConcurrentHashMap());
    //raw_data=weightDataByDate(raw_data);
    for (int i = 0; i < GConfigs.getClassCount(m_TypePath); i++) {
      try {
        Instances data_with_one_class
                = removeUnwantedClassAtt(new Instances(raw_data), i);
        //data_with_one_class=interquartileRange(data_with_one_class);
        ReliefFAttributeEval relf = new ReliefFAttributeEval();
        relf.setOptions(new String[]{"-M", "50"});
        benchMarkModels("Relf",
                selectAttributes(new Instances(data_with_one_class), relf),
                code, classifierNames, optionsList, notes, i);

        CorrelationAttributeEval corrlat = new CorrelationAttributeEval();
        benchMarkModels("Corrlat",
                selectAttributes(new Instances(data_with_one_class), corrlat),
                code, classifierNames, optionsList, notes, i);

        benchMarkModels("Relf-Sec",
                selectUnfavoredAttributes(code + "_" + i, new Instances(data_with_one_class)),
                code, classifierNames, optionsList, notes, i);

      } catch (Exception ex) {
        Logger.getLogger(WekaModelBenchamrker.class.getName()).
                log(Level.SEVERE, ex.getMessage(), ex);
      }
    }
    writeToFile(code);
  }

  //Class index is the relative index of class value, cannot be used directly in datasource
  public HashMap<Evaluation, double[]>
          benchMarkModels(String additionInfo, Instances trainingData,
                  String code, ArrayList<String> classifierNames,
                  ArrayList<String[]> optionsList, ArrayList<String> notes,
                  int classIndex) throws Exception {
    HashMap<Evaluation, double[]> modelBenchMap = new HashMap();
    String class_name = trainingData.classAttribute().name();
    //Find the days range in the class attribute
    int days_to_advance = MyUtils.getDaysToAdvance(class_name);

    //For each classifier
    for (int i = 0; i < classifierNames.size(); i++) {
      Instances instances = new Instances(trainingData);
      instances.deleteWithMissingClass();

      // Create classifier by name and options
      Classifier classifier = AbstractClassifier
              .forName(classifierNames.get(i), optionsList.get(i).clone());
      String identity = notes.get(i) + classifierNames.get(i).split("\\.")[3]
              + "-" + class_name + "-" + additionInfo;
      String cname = classifierNames.get(i);
      String classifier_name = notes.get(i)
              + cname.substring(cname.lastIndexOf(".") + 1, cname.length())
              + "-" + additionInfo;
      double[] eval_result;

      //If the class attribute is numeric but the classifier is unable to handle numeric class
      // discretize the class attribute
      if (!classifier.getCapabilities().handles(Capabilities.Capability.NUMERIC_CLASS)) {
        instances = discretizeClassAtt(instances);
      }
//      else if (classifier_name.contains("MultilayerPerceptron")
//              || classifier_name.contains("RandomForest")
//              || classifier_name.contains("HoeffdingTree")) {
//        instances = discretizeInstances(instances);
//      }

      eval_result = evalClassifier(classifier, new Instances(instances), days_to_advance);
      classifier.buildClassifier(instances);
//      System.out.println(code + ", " + identity);
//      System.out.println("Mean absolete error:" + eval_result[0]);
//      System.out.println("False positive:" + eval_result[1]);
//      System.out.println("Random rate:" + eval_result[2]);

      if (eval_result[1] - eval_result[2] >= 0.15 && eval_result[1] > 0) {
        MyUtils.saveModelAndPerformance(code, identity, m_TypePath,
                classifier, instances.stringFreeStructure(), eval_result);
      }

      // code -> classifier_name -> ArrayList<Entry<class_name,ArrayList<different_attempts>>>
      ConcurrentHashMap<String, ArrayList> map = m_Evals.get(code);
      if (!map.containsKey(classifier_name)) {
        map.put(classifier_name, new ArrayList());
      }

      ArrayList<AbstractMap.SimpleEntry<String, ArrayList<double[]>>> list
              = (ArrayList) map.get(classifier_name);
      boolean found = false;
      for (Entry e : list) {
        if (e.getKey().equals(class_name)) {
          found = true;
          ArrayList a = (ArrayList) e.getValue();
          if (eval_result[1] - eval_result[2] >= 0.15) {
            a.add(eval_result);
          }
        }
      }
      if (!found) {
        ArrayList a = new ArrayList();
        a.add(new double[5]);
        if (eval_result[1] - eval_result[2] >= 0.15) {
          a.add(eval_result);
        }
        list.add(new AbstractMap.SimpleEntry(class_name, a));
      }
     // evals.put(classifier_name, list);

      // Series forecasting
      classifier = AbstractClassifier.forName(classifierNames.get(i), optionsList.get(i).clone());
      //Skip if this classified only handles nominal value
      // as the forecaster does not support nominal forecasting
      if (!classifier.getCapabilities().handles(Capabilities.Capability.NUMERIC_CLASS)) {
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

  // Only one class attribute to be predicted at once, so the other class attributes will be removed        
  private Instances removeUnwantedClassAtt(Instances trainInstances, int classIndex) {
    int target = GConfigs.getClassCount(m_TypePath);
    int att_count = trainInstances.numAttributes();
    int training_count = att_count - target;
    trainInstances.setClassIndex(training_count + classIndex);
    for (int i = 0; i < target; i++) {
      int tempind = training_count + i;
      if (trainInstances.classIndex() != tempind) {
        trainInstances.deleteAttributeAt(tempind);
        i--;
        target--;
      }
    }
    return trainInstances;
  }

  private Instances selectAttributes(Instances originalData, ASEvaluation evaluator) throws Exception {
    if (!evaluator.getCapabilities().handles(Capabilities.Capability.NUMERIC_CLASS)) {
      originalData = discretizeClassAtt(originalData);
    }

    AttributeSelection selection = new AttributeSelection();
    Ranker ranker = new Ranker();
    ranker.setOptions(new String[]{"-N", "20"});
    selection.setEvaluator(evaluator);
    selection.setSearch(ranker);
    selection.SelectAttributes(originalData);
    return selection.reduceDimensionality(originalData);
  }

  private Instances selectUnfavoredAttributes(String tittle, Instances originalData) throws Exception {
    AttributeSelection selection = new AttributeSelection();
    Ranker ranker = new Ranker();
    ranker.setOptions(new String[]{"-N", "20"});
    ReliefFAttributeEval eval = new ReliefFAttributeEval();
    eval.setOptions(new String[]{"-M", "50"});
    selection.setEvaluator(eval);
    selection.setSearch(ranker);
    selection.SelectAttributes(originalData);
    int[] selected_indexes = selection.selectedAttributes();
    Arrays.sort(selected_indexes);
    printSelectedAtts("primary_" + tittle, selected_indexes, originalData);
    for (int i = selected_indexes.length - 2; i >= 0; i--) {
      originalData.deleteAttributeAt(selected_indexes[i]);
    }
    //Select attributes for second time
    selection.SelectAttributes(originalData);
    selected_indexes = selection.selectedAttributes();
    Arrays.sort(selected_indexes);
    printSelectedAtts("secondary_" + tittle, selected_indexes, originalData);
    return selection.reduceDimensionality(originalData);
  }

  private Instances weightDataByDate(Instances data) {
    LocalDate tempdate;
    Instance ins;
    for (int i = 0; i < data.numInstances(); i++) {
      ins = data.instance(i);
      tempdate = MyUtils.parseToISO(ins.stringValue(0));
      int yeardiff = LocalDate.now().getYear() - tempdate.getYear();
      //double weight = -0.01 * (Math.pow(yeardiff, 2)) + 1;
      double weight = -0.2 * yeardiff + 1;
      if (weight < 0) {
        weight = 0;
      }
      ins.setWeight(weight);
    }
    return data;
  }

  private Instances discretizeClassAtt(Instances instances) throws Exception {
    weka.filters.unsupervised.attribute.Discretize disc = new weka.filters.unsupervised.attribute.Discretize();
    disc.setAttributeIndices("last");
    disc.setIgnoreClass(true);
    disc.setUseEqualFrequency(true);
    disc.setInputFormat(instances);
    return Filter.useFilter(instances, disc);
  }

  private Instances discretizeTrainingAtt(Instances instances) throws Exception {
    weka.filters.unsupervised.attribute.Discretize disc = new weka.filters.unsupervised.attribute.Discretize();
    disc.setAttributeIndices("first-last");
    disc.setIgnoreClass(false);
    disc.setUseEqualFrequency(true);
    disc.setInputFormat(instances);
    return Filter.useFilter(instances, disc);
  }

  private Instances interquartileRange(Instances instances) throws Exception {
    weka.filters.unsupervised.attribute.InterquartileRange disc
            = new weka.filters.unsupervised.attribute.InterquartileRange();
    disc.setAttributeIndices("first-last");
    disc.setInputFormat(instances);
    return Filter.useFilter(instances, disc);
  }

  /**
   * @param originalData missing class values were removed, deep copy of
   * original data
   */
  private double[] evalClassifier(Classifier classifier,
          Instances originalData, int daysToAdvance) throws Exception {
    Instances local_data = new Instances(originalData);
    int size = originalData.size() - 1;
    double true_positive = 0;
    double positive_count = 0;
    double random_true = 0;
    double random_count = 0;
    double[] errors = new double[m_DaysForEval];
    double threshold = determineEvaluationThreshold(originalData, daysToAdvance);
    Attribute class_att = originalData.classAttribute();
    //double sig = GConfigs.getSignificanceNormal(m_TypePath);
    Classifier tclassifier = AbstractClassifier.makeCopy(classifier);

    int buffer = 0;
    for (int i = 0; i < daysToAdvance; i++) {
      local_data.remove(size - i);
    }
    size -= daysToAdvance;

    //c=number of days visited, i=number of days evaluated
    for (int i = 0, c = 0; i < m_DaysForEval; c++) {
      //Draw 25% of the total data
      if (ThreadLocalRandom.current().nextInt(4) == 0) {
        if (buffer == 0) {
          for (int j = 0; j < 20; j++) {
            local_data.remove(local_data.size() - 1);
          }
          tclassifier.buildClassifier(local_data);
          buffer = 4;
        } else {
          buffer--;
        }
        double fr = (double) tclassifier.classifyInstance(originalData.get(size + daysToAdvance - c));
        double av = (double) originalData.get(size + daysToAdvance - c).classValue();

        if (class_att.name().contains("Signal")) {
          // Signals are always positive
          if (class_att.isNumeric()) {
            if (fr >= 0.005) {
              positive_count++;
              if (av >= 0.005) {
                true_positive++;
              }
            }
            if (ThreadLocalRandom.current().nextBoolean()) {
              random_count++;
              if (av >= 0.005) {
                random_true++;
              }
            }

          } else {
            if (fr != 0) {
              positive_count++;
              if (fr == av) {
                true_positive++;
              }
            }

            int ran = ThreadLocalRandom.current().nextInt(2);
            if (ran != 0) {
              random_count++;
              if (av == ran) {
                random_true++;
              }
            }
          }
        } else {

          if (class_att.isNumeric()) {
            // If |av|<|t| && |fr|>|t|
            if (Math.abs(fr) >= Math.abs(threshold)) {
              positive_count++;
              if (Math.abs(av) >= threshold) {
                true_positive++;
              }
            }
            // Count for random rate, 50% change for random to be greater than threshold
            if (ThreadLocalRandom.current().nextBoolean()) {
              random_count++;
              if (Math.abs(fr) >= threshold) {
                random_true++;
              }
            }

          } else {

            break;
          }
        }
        errors[i] = Math.abs(av) - Math.abs(fr);
        i++;
        //System.out.println("fr=" + fr + ", av=" + av);
      }
    }

    if (positive_count >= 5) {
      true_positive = true_positive / positive_count;
    } else {
      true_positive = 0;
    }

    if (random_count >= 5) {
      random_true = random_true / random_count;
    } else {
      random_true = 0;
    }

    double sum = 0;
    for (double i : errors) {
      sum += Math.abs(i);
    }
    double[] eval = new double[5];
    eval[0] = sum / errors.length;
    eval[1] = true_positive;
    eval[2] = random_true;

    return eval;
  }

  private double[] evaluateForecaster(Classifier classifier,
          String class_name, Instances originalData, int daysToAdvance) 
          throws Exception {
    int size = originalData.size() - 1;
    double false_positive = 0;
    double positive_count = 0;
    double[] errors = new double[m_DaysForEval];
    double random_count = 0;
    double random_false = 0;

    Instances local_data = new Instances(originalData);
    double threshold = determineEvaluationThreshold(originalData, daysToAdvance);

    for (int i = 0; i < m_DaysForEval; i++) {
      WekaForecaster fc = new WekaForecaster();
      fc.setBaseForecaster(classifier);
      fc.setFieldsToForecast(class_name + ",RSI50SLP5");
//      fc.getTSLagMaker().setMinLag(1);
//      fc.getTSLagMaker().setMaxLag(10);
//      fc.getTSLagMaker().setAverageConsecutiveLongLags(false);
//      fc.getTSLagMaker().setLagRange("");
//      fc.getTSLagMaker().setPrimaryPeriodicFieldName("");
      local_data.remove(size - i);
      fc.buildForecaster(local_data);
      fc.primeForecaster(local_data);

      if (size + daysToAdvance - i <= size) {
        double fr = (double) (fc.forecast(daysToAdvance)
                .get(daysToAdvance - 1).get(0).predicted());
        double av = (double) originalData.get(size + daysToAdvance - i).classValue();
        // If |av|<|t| && |fr|>|t|
        if (Math.abs(fr) > Math.abs(threshold)) {
          positive_count++;
          if (Math.abs(av) <= Math.abs(threshold)) {
            false_positive++;
          }
        }
        // Count for random rate, 50% change for random to be greater than threshold
        if (ThreadLocalRandom.current().nextBoolean()) {
          random_count++;
          if (Math.abs(av) <= Math.abs(threshold)) {
            random_false++;
          }
        }

        //System.out.println("fr=" + fr + ", av=" + av);
        errors[i] = Math.abs(av) - Math.abs(fr);
      }
    }

    double sum = 0;
    for (double i : errors) {
      sum += Math.abs(i);
    }
    double[] eval = new double[5];

    if (positive_count >= 5) {
      false_positive = false_positive / positive_count;
    } else {
      false_positive = 0;
    }

    random_false = random_false / random_count;

    eval[0] = sum / errors.length;
    eval[1] = false_positive;
    eval[2] = random_false;

    return eval;
  }

  private double determineEvaluationThreshold(Instances originalData, int daysAdv) {
    double sig = GConfigs.getSignificanceNormal(m_TypePath, daysAdv);
    //double half_att_amount = originalData.classAttribute().numValues() / 2;
    int multi = 1;
    if (daysAdv > 5 && daysAdv <= 20) {
      multi = 2;
    } else if (daysAdv > 20) {
      multi = 3;
    }

    //Check if the class att nominal or numeric, if nominal:
    Attribute class_att = originalData.classAttribute();
    if (class_att.isNominal()) {
      int m2 = (int) (sig / 0.01);
      //Check class attribute's each value tag, find the one contains
      //"-inf-0.5*0.01" or "-0.5*0.01-0.5*0.01" or "-0.5*0.01-inf"
      String tmpstr;
      Enumeration<?> enm = class_att.enumerateValues();
      while (enm.hasMoreElements()) {
        tmpstr = enm.nextElement().toString();
        if (tmpstr.contains("-inf-0.005") || tmpstr.contains("-0.005-inf")
                || tmpstr.contains("-0.005-0.005")) {
          if (class_att.name().contains("Highest")) {
            return Math.min(class_att.indexOfValue(tmpstr) + multi,
                    class_att.numValues() - 1);
          } else {
            return Math.max(0, class_att.indexOfValue(tmpstr) - multi);
          }
        }
      }
    }
    //if numeric 
    return sig;
  }

  private void printSelectedAtts(String tittle, int[] selected, Instances header) {
    File file = new File(GConfigs.REPORT_PATH + "out\\Selected_" + tittle + ".csv");
    try (PrintWriter writer = new PrintWriter(new BufferedWriter(
            new FileWriter(file, true)))) {
      writer.println("----");
      for (int index : selected) {
        writer.println(header.attribute(index).name());

      }
    } catch (Exception ex) {
      Logger.getLogger(WekaModelBenchamrker.class
              .getName()).log(Level.SEVERE, null, ex);
    }
  }

  private void writeToFile(String code) {
    File file = new File(GConfigs.REPORT_PATH + "out\\6_" + code + ".csv");

    ConcurrentHashMap<String, ArrayList> map = m_Evals.get(code);

    try (PrintWriter writer = new PrintWriter(new BufferedWriter(
            new FileWriter(file, true)))) {
      writer.println("Class Value:,C_Highest05d,,,C_Highest10d,,,C_Highest15d,,,C_Highest20d,,,C_Highest25d");
      writer.println("Classifier Name,False Positive,False Random,MAE,False Positive,False Random,MAE,False Positive,False Random,MAE,False Positive,False Random,MAE,False Positive,False Random,MAE");
      for (Entry e2 : map.entrySet()) {
        String classifier_name = (String) e2.getKey();
        ArrayList<AbstractMap.SimpleEntry> list = (ArrayList<AbstractMap.SimpleEntry>) e2.getValue();
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
          writer.print(roundDouble(es[1] / (a.size() - 1)) + "," + roundDouble(es[2] / (a.size() - 1)) + "," + roundDouble(es[0] / (a.size() - 1)) + ",");

        }
        writer.println();

      }
    } catch (IOException ex) {
      Logger.getLogger(WekaModelBenchamrker.class
              .getName()).log(Level.SEVERE, null, ex);
    }
  }

}
