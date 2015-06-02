package model;

import core.GConfigs;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import util.MyUtils;
import weka.attributeSelection.AttributeSelection;
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
  private final int m_DaysForEval = 90;

  public WekaModelBenchamrker(String modelType) {
    m_TypePath = modelType + "//";
  }

  public void benchmarkByClasses(String code,
          ArrayList<String> classifierNames,
          ArrayList<String[]> optionsList) throws IOException {
    Instances raw_data = MyUtils.loadInstancesFromCSV(GConfigs.RESOURCE_PATH
            + this.m_TypePath + code + "//" + code + "_Training.csv");
    //raw_data=weightDataByDate(raw_data);

    for (int i = 0; i < GConfigs.getClassCount(m_TypePath); i++) {
      try {
        Instances data_with_one_class
                = removeUnwantedClassAtt(new Instances(raw_data), i);
        benchMarkModels("Primary",
                selectAttributes(data_with_one_class),
                code, classifierNames, optionsList, i);

      } catch (Exception ex) {
        Logger.getLogger(WekaModelBenchamrker.class.getName()).
                log(Level.SEVERE, ex.getMessage(), ex);
      }
    }
  }

  //Class index is the relative index of class value, cannot be used directly in datasource
  public HashMap<Evaluation, float[]>
          benchMarkModels(String additionInfo, Instances trainingData,
                  String code, ArrayList<String> classifierNames,
                  ArrayList<String[]> optionsList, int classIndex) throws Exception {
    HashMap<Evaluation, float[]> modelBenchMap = new HashMap();
    String class_name = trainingData.classAttribute().name();
    String dayptn = "(\\d+)(d)+";
    Pattern pattern_days = Pattern.compile(dayptn);
    Matcher matcher;
    matcher = pattern_days.matcher(class_name);
    matcher.find();
    //Find the days range in the class attribute
    int days_to_advance = Integer.parseInt(matcher.group(1));

    //For each classifier
    for (int i = 0; i < classifierNames.size(); i++) {
      Instances instances = removeMissingClass(new Instances(trainingData));

      // Create classifier by name and options
      Classifier classifier = AbstractClassifier.forName(classifierNames.get(i), optionsList.get(i).clone());
      String identity = classifierNames.get(i).split("\\.")[3]
              + "-" + class_name + "-" + additionInfo;

      float[] eval_result;

      //If the class attribute is numeric but the classifier is unable to handle numeric class
      // discretize the class attribute
      if (!classifier.getCapabilities().handles(Capabilities.Capability.NUMERIC_CLASS)) {
        instances = discretizeInstances(instances);
      }

      eval_result = EvalClassifier(classifier, new Instances(instances), days_to_advance);
      classifier.buildClassifier(instances);
      System.out.println(code + ", " + identity);
      System.out.println("Mean absolete error:" + eval_result[0]);
      System.out.println("False positive:" + eval_result[1]);
      System.out.println("Random rate:" + eval_result[2]);

      MyUtils.saveModelAndPerformance(code, identity, m_TypePath,
              classifier, trainingData.stringFreeStructure(), eval_result);

      // Series forecasting
      classifier = AbstractClassifier.forName(classifierNames.get(i), optionsList.get(i).clone());
      //Skip if this classified only handles nominal value
      // as the forecaster does not support nominal forecasting
      if (!classifier.getCapabilities().handles(Capabilities.Capability.NUMERIC_CLASS)) {
        continue;
      }
      eval_result = evaluateForecaster(classifier, class_name, new Instances(instances), days_to_advance);
      WekaForecaster forecaster = new WekaForecaster();
      forecaster.setBaseForecaster(classifier);
      forecaster.setFieldsToForecast(class_name);
      forecaster.buildForecaster(instances);
      forecaster.primeForecaster(instances);
      System.out.println(code + ", " + identity);
      System.out.println("Mean absolete error (Series):" + eval_result[0]);
      System.out.println("False positive:" + eval_result[1]);
      System.out.println("Random rate:" + eval_result[2]);
      MyUtils.saveModelAndPerformance(code, identity + "Series", m_TypePath,
              forecaster, instances, eval_result);
    }
    return modelBenchMap;
  }

  private float[] generateResultForNumeric(Evaluation eval) {
    float[] acc = new float[7];
    acc[0] = -1;
    acc[1] = (float) eval.rootMeanSquaredError();

    try {
      acc[2] = (float) eval.relativeAbsoluteError();

    } catch (Exception ex) {
      Logger.getLogger(WekaModelBenchamrker.class
              .getName()).log(Level.SEVERE, null, ex);
    }

    return acc;
  }

  private float[] generateResultForNominal(Evaluation eval) {
    //String r = "";
    NumberFormat defaultFormat = NumberFormat.getPercentInstance();
    defaultFormat.setMinimumFractionDigits(2);

    double[][] cost = eval.confusionMatrix();
    eval.weightedFalseNegativeRate();
    float[] acc = new float[7];

    return acc;
  }

  // Only one class attribute to be predicted at once, so the other class attributes will be removed        
  private Instances removeUnwantedClassAtt(Instances trainInstances, int classIndex) {
    int target = GConfigs.getClassCount(m_TypePath);

    trainInstances.setClassIndex(GConfigs.getTrainingCount(m_TypePath) + classIndex);
    for (int i = 0; i < target; i++) {
      int tempind = GConfigs.getTrainingCount(m_TypePath) + i;
      if (trainInstances.classIndex() != tempind) {
        trainInstances.deleteAttributeAt(tempind);
        i--;
        target--;
      }
    }
    return trainInstances;
  }

  private Instances selectAttributes(Instances originalData) throws Exception {
    AttributeSelection selection = new AttributeSelection();
    Ranker ranker = new Ranker();
    ranker.setOptions(new String[]{"-N", "30"});
    ReliefFAttributeEval eval = new ReliefFAttributeEval();
    eval.setOptions(new String[]{"-M", "500"});
    selection.setEvaluator(eval);
    selection.setSearch(ranker);
    selection.SelectAttributes(originalData);
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

  private Instances discretizeInstances(Instances instances) throws Exception {
    weka.filters.unsupervised.attribute.Discretize disc = new weka.filters.unsupervised.attribute.Discretize();
    disc.setAttributeIndices("last");
    disc.setIgnoreClass(true);
    disc.setUseEqualFrequency(true);
    disc.setInputFormat(instances);
    return Filter.useFilter(instances, disc);
  }

  private Instances removeMissingClass(Instances instances) throws Exception {
    weka.filters.unsupervised.instance.RemoveWithValues filter = new weka.filters.unsupervised.instance.RemoveWithValues();
    filter.setAttributeIndex("last");
    filter.setNominalIndices("");
    filter.setMatchMissingValues(true);
    filter.setInputFormat(instances);
    return Filter.useFilter(instances, filter);
  }

  /**
   * @param originalData missing class values were removed, deep copy of
   * original data
   */
  private float[] EvalClassifier(Classifier classifier,
          Instances originalData, int daysToAdvance) throws Exception {
    Instances local_data = new Instances(originalData);
    int size = originalData.size() - 1;
    float false_positive = 0;
    float positive_count = 0;
    float random_false = 0;
    float random_count = 0;
    float[] errors = new float[m_DaysForEval];
    double threshold = determineEvaluationThreshold(originalData);
    Attribute class_att = originalData.classAttribute();
    double half_att_amount = class_att.numValues() / 2;

    for (int i = 0; i < daysToAdvance; i++) {
      local_data.remove(size - i);
    }
    size -= daysToAdvance;
    for (int i = 0; i < m_DaysForEval; i++) {
      Classifier tclassifier = AbstractClassifier.makeCopy(classifier);
      local_data.remove(size - i);
      tclassifier.buildClassifier(local_data);
      double fr = tclassifier.classifyInstance(originalData.get(size + daysToAdvance - i));
      double av = originalData.get(size + daysToAdvance - i).classValue();

      if (class_att.isNumeric()) {
        // If |av|<|t| && |fr|>|t|
        if (Math.abs(fr) > Math.abs(threshold)) {
          positive_count++;
          if (Math.abs(av) <= Math.abs(threshold)) {
            false_positive++;
          }
        }
        // Count for random rate, 50% change for random to be greater than threshold
        if (new Random().nextBoolean()) {
          random_count++;
          if (Math.abs(av) <= Math.abs(threshold)) {
            random_false++;
          }
        }

      } else if (threshold > half_att_amount) { //predicting negative
        if (fr < threshold) {
          positive_count++;
          if (av >= threshold && fr < threshold) {
            false_positive++;
          }
        }
        // Count for random rate, 50% change for fr to be greater than threshold
        if (new Random().nextBoolean()) {
          random_count++;
          if (av >= threshold) {
            random_false++;
          }
        }

      } else if (threshold < half_att_amount) { //predicting positive
        if (fr > threshold) {
          positive_count++;
          if (av <= threshold) {
            false_positive++;
          }
        }
        // Count for random rate, 50% change for fr to be greater than threshold
        if (new Random().nextBoolean()) {
          random_count++;
          if (av <= threshold) {
            random_false++;
          }
        }
      }

      errors[i] = (float) (av - fr);
      //System.out.println("fr=" + fr + ", av=" + av);
    }

    false_positive = false_positive / positive_count;
    random_false = random_false / random_count;
    float sum = 0;
    for (float i : errors) {
      sum += Math.abs(i);
    }
    float[] eval = new float[5];
    eval[0] = sum / errors.length;
    eval[1] = false_positive;
    eval[2] = random_false;

    return eval;
  }

  private float[] evaluateForecaster(Classifier classifier,
          String class_name, Instances originalData, int daysToAdvance) throws Exception {
    int size = originalData.size() - 1;
    float false_positive = 0;
    float positive_count = 0;
    float[] errors = new float[m_DaysForEval];
    float random_count = 0;
    float random_false = 0;

    Instances local_data = new Instances(originalData);
    double threshold = determineEvaluationThreshold(originalData);

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
        double fr = (fc.forecast(daysToAdvance).get(daysToAdvance - 1).get(0).predicted());
        double av = originalData.get(size + daysToAdvance - i).classValue();
        // If |av|<|t| && |fr|>|t|
        if (Math.abs(fr) > Math.abs(threshold)) {
          positive_count++;
          if (Math.abs(av) <= Math.abs(threshold)) {
            false_positive++;
          }
        }
        // Count for random rate, 50% change for random to be greater than threshold
        if (new Random().nextBoolean()) {
          random_count++;
          if (Math.abs(av) <= Math.abs(threshold)) {
            random_false++;
          }
        }

        //System.out.println("fr=" + fr + ", av=" + av);
        errors[i] = (float) (fr - av);
      }
    }

    float sum = 0;
    for (float i : errors) {
      sum += Math.abs(i);
    }
    float[] eval = new float[5];
    false_positive = false_positive / positive_count;
    random_false = random_false / random_count;

    eval[0] = sum / errors.length;
    eval[1] = false_positive;
    eval[2] = random_false;

    return eval;
  }

  private double determineEvaluationThreshold(Instances originalData) {
    double sig = GConfigs.getSignificanceNormal(m_TypePath);
    //Check if the class att nominal or numeric, if nominal:
    Attribute class_att = originalData.classAttribute();
    if (class_att.isNominal()) {
      //Check class attribute's each value tag, find the one contains
      //"-inf-0.5*sig" or "-0.5*sig-0.5*sig" or "-0.5*sig-inf"
      String half = String.valueOf(0.5 * sig);
      String neghalf = String.valueOf(-0.5 * sig);
      String tmpstr;
      Enumeration<?> enm = class_att.enumerateValues();
      while (enm.hasMoreElements()) {
        tmpstr = enm.nextElement().toString();
        if (tmpstr.contains("-inf-" + half) || tmpstr.contains(neghalf + "-inf")
                || tmpstr.contains(neghalf + "-" + half)) {
          return class_att.indexOfValue(tmpstr);
        }
      }
    }
    //if numeric, 
    return sig;

  }

}
