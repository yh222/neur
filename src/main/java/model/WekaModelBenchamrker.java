package model;

import core.GConfigs;

import calculator.Performance;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.MyUtils;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.Ranker;
import weka.attributeSelection.ReliefFAttributeEval;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.timeseries.WekaForecaster;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;

public class WekaModelBenchamrker {

  private final Performance m_Performance;
  private final String m_TypePath;
  private final int m_DaysForEval = 90;

  public WekaModelBenchamrker(Performance perf, String modelType) {
    m_Performance = perf;
    m_TypePath = modelType + "//";
  }

  public void benchmarkByClasses(String code,
          ArrayList<String> classifierNames,
          ArrayList<String[]> optionsList) throws IOException {
    Instances raw_data = loadInstancesFromCSV(code);
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

      eval_result = EvalClassifier(classifier, new Instances(instances));
      classifier.buildClassifier(instances);
      System.out.println(code + ", " + identity);
      System.out.println("Mean absolete error:" + eval_result[0]);
      System.out.println("False positive:" + eval_result[1]);

      //System.out.println("Mean absolete error:" + eval_result[0]);
      //Skip if this classified only handles nominal value
      // as the forecaster does not support nominal forecasting
      if (!classifier.getCapabilities().handles(Capabilities.Capability.NUMERIC_CLASS)) {
        continue;
      }
      classifier = AbstractClassifier.forName(classifierNames.get(i), optionsList.get(i));
      eval_result = evaluateForecaster(classifier, class_name, new Instances(instances));
      WekaForecaster forecaster = new WekaForecaster();
      forecaster.setBaseForecaster(classifier);
      forecaster.setFieldsToForecast(class_name);
      forecaster.buildForecaster(instances, System.out);
      forecaster.primeForecaster(instances);
      System.out.println(code + ", " + identity);
      System.out.println("Mean absolete error series:" + eval_result[0]);
      System.out.println("False positive:" + eval_result[1]);
    }

//      //Create classifier and cross evaluate it
//      Evaluation eval = new Evaluation(instances);
//      eval.crossValidateModel(classifier, instances, 1, new Random());
//      float[] result;
//      if (trainingData.attribute(47).isNumeric()) {//If it is numeric
//        result = generateResultForNumeric(eval);
//        System.out.println(eval.rootMeanSquaredError());
//        System.out.println(eval.rootRelativeSquaredError());
//      } else {
//        result = generateResultForNominal(eval);
//        System.out.println(eval.toMatrixString());
//      }
//
//      //however, the model for the same classifier will be different
//      m_Performance.saveModelAndPerformance(code, identify,
//              classifier, trainingData.stringFreeStructure(), result);
//      m_Performance.putClassResult(code, optionsList.get(i)[3].substring(17) + "-" + additionInfo,
//              result, classIndex);
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

  private Instances loadInstancesFromCSV(String code) throws IOException {
    String trainFileName = GConfigs.RESOURCE_PATH + m_TypePath + code + "//" + code + "_Training.csv";
    CSVLoader loader = new CSVLoader();
    loader.setDateAttributes("first");
    loader.setDateFormat("yyyy-mm-dd");
    loader.setSource(new File(trainFileName));
    Instances data = loader.getDataSet();
    return data;
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
  private float[] EvalClassifier(Classifier classifier, Instances originalData) throws Exception {
    Instances local_data = new Instances(originalData);
    int size = originalData.size() - 1;
    float false_positive = 0;
    float[] results = new float[m_DaysForEval];
    for (int i = 0; i < m_DaysForEval; i++) {
      Classifier tclassifier = AbstractClassifier.makeCopy(classifier);
      local_data.remove(size - i);
      tclassifier.buildClassifier(local_data);
      double fr = tclassifier.classifyInstance(originalData.get(size - i));
      double av = originalData.get(size - i).classValue();
      //fr=new Random().nextInt(2);
      
      //check if the class att nominal or numeric, if nominal:
      //class attribute's each value tag, find the one contains "-inf-0.5*sig" or "-0.5*sig-0.5*sig" or
      // "-0.5*sig-inf"
      
      //if numeric
      
      
      if (av < 1 && fr >= 1) {
        false_positive++;
      }
      results[i] = (float) (av - fr);
      System.out.println("fr=" + fr + ", av=" + av);
    }
    
    false_positive = false_positive / m_DaysForEval;
    float sum = 0;
    for (float i : results) {
      sum += Math.abs(i);
    }
    float[] eval = new float[3];
    eval[0] = sum / results.length;
    eval[1] = (float) false_positive;

    return eval;
  }

  private float[] evaluateForecaster(Classifier classifier,
          String class_name, Instances originalData) throws Exception {
    int size = originalData.size() - 1;
    float false_positive = 0;
    float[] results = new float[m_DaysForEval];
    Instances local_data = new Instances(originalData);

    PrintStream ops = new PrintStream(new OutputStream() {
      @Override
      public void write(int b) throws IOException {
      }

      public void println(String s) {
      }
    });

    for (int i = 0; i < m_DaysForEval; i++) {
      WekaForecaster fc = new WekaForecaster();
      fc.setBaseForecaster(classifier);
      fc.setFieldsToForecast(class_name + ",RSI50SLP5");
      Instance r = local_data.remove(size - i);
//      fc.getTSLagMaker().setMinLag(1);
//      fc.getTSLagMaker().setMaxLag(10);
//      fc.getTSLagMaker().setAverageConsecutiveLongLags(false);
//      fc.getTSLagMaker().setLagRange("");
//      fc.getTSLagMaker().setPrimaryPeriodicFieldName("");

      fc.buildForecaster(local_data, ops);
      fc.primeForecaster(local_data);
      double fr = (fc.forecast(1, ops).get(0).get(0).predicted());
      if (size - i < size) {
        double av = r.classValue();
        if (av < 1 && fr >= 1) {
          false_positive++;
        }
        //System.out.println("fr=" + fr + ", av=" + av);
        results[i] = (float) (fr - av);
      }
    }

    float sum = 0;
    for (float i : results) {
      sum += Math.abs(i);
    }
    float[] eval = new float[3];
    false_positive = false_positive / m_DaysForEval;

    eval[0] = sum / results.length;
    eval[1] = (float) false_positive;

    return eval;
  }

}
