package model;

import core.GlobalConfigs;
import static core.GlobalConfigs.RESOURCE_PATH;

import calculator.Performance;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.Ranker;
import weka.attributeSelection.ReliefFAttributeEval;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.core.converters.ConverterUtils.DataSource;

public class WekaModelBenchamrker {

  private final Performance m_Performance;
  private final String m_TypePath;

  public WekaModelBenchamrker(Performance perf, String modelType) {
    m_Performance = perf;
    m_TypePath = modelType + "//";
  }

  public void
          benchmarkByClasses(String code,
                  ArrayList<String> classifierNames,
                  ArrayList<String[]> optionsList, int folds) {
    for (int i = 0; i < GlobalConfigs.getClassCount(m_TypePath); i++) {
      try {
        Instances dataReadyToTrain = weightDataByDate(code);
        removeUnwantedClassValues(dataReadyToTrain, i);

        benchMarkModels("Primary",
                selectAttributes(new Instances(dataReadyToTrain)),
                code, classifierNames, optionsList, i, folds);
        //Do it again with unfavored attributes
        benchMarkModels("Secondary",
                selectUnfavoredAttributes(new Instances(dataReadyToTrain)),
                code, classifierNames, optionsList, i, folds);

      } catch (Exception ex) {
        Logger.getLogger(WekaModelBenchamrker.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    //return list;
  }

  public HashMap<Evaluation, float[]>
          benchMarkModels(String additionInfo, Instances trainingData,
                  String code, ArrayList<String> classifierNames,
                  ArrayList<String[]> optionsList, int classIndex, int folds) throws Exception {
    HashMap<Evaluation, float[]> modelBenchMap = new HashMap();
    for (int i = 0; i < classifierNames.size(); i++) {

      Instances instances = trainingData;
      Classifier classifier
              = AbstractClassifier.forName(
                      classifierNames.get(i),
                      optionsList.get(i).clone());
      Evaluation eval = new Evaluation(instances);
      eval.crossValidateModel(classifier, instances, folds, new Random());
      classifier.buildClassifier(instances);
      float[] result = generateResult(eval);

      String identify = optionsList.get(i)[3].substring(17)
              + "-" + instances.classAttribute().name() + "-" + additionInfo;

      System.out.println(identify);
      System.out.println(eval.toMatrixString());

      //however, the model for the same classifier will be different
      m_Performance.saveModelAndPerformance(code, identify,
              classifier, trainingData.stringFreeStructure(), result);

      //bench mark for this classifier(ignore the difference between models)
      m_Performance.putClassResult(code, optionsList.get(i)[3].substring(17) + "-" + additionInfo,
              result, classIndex);
    }
    return modelBenchMap;
  }

  private void removeUnwantedClassValues(Instances trainInstances, int classIndex) {

    int target = GlobalConfigs.getClassCount(m_TypePath);

    trainInstances.setClassIndex(GlobalConfigs.getTrainingCount(m_TypePath) + classIndex);
    for (int i = 0; i < target; i++) {
      int tempind = GlobalConfigs.getTrainingCount(m_TypePath) + i;
      if (trainInstances.classIndex() != tempind) {
        trainInstances.deleteAttributeAt(tempind);
        i--;
        target--;
      }
    }
    //return trainInstances;
  }

  private float[] generateResult(Evaluation eval) {
    //String r = "";
    NumberFormat defaultFormat = NumberFormat.getPercentInstance();
    defaultFormat.setMinimumFractionDigits(2);

    double[][] cost = eval.confusionMatrix();

    float[] acc = new float[7];

    float err_d = 0.1f;
    float err = err_d;
    for (int i = 1; i < 6; i++) {
      err += cost[i][0] * i * 2;
    }
    acc[0] = (float) (cost[0][0] / (err + cost[0][0]));

    err = err_d;
    for (int i = 2; i < 5; i++) {
      err += cost[i][1] * (i - 1) * 2;
    }
    acc[1] = (float) ((cost[0][1] + cost[1][1]) / (err + cost[0][1] + cost[1][1]));

    err = err_d;
    for (int i = 3; i < 4; i++) {
      err += cost[i][2] * (i - 2) * 2;
    }
    acc[2] = (float) ((cost[0][2] + cost[1][2] + cost[2][2]) / (err + cost[0][2] + cost[1][2] + cost[2][2]));

    // The accuracy for "Stay" is not considered at the moment
    acc[3] = -1;

    // 
    err = err_d;
    for (int i = 5; i >= 0; i--) {
      err += cost[i][6] * (6 - i) * 2;
    }
    acc[6] = (float) (cost[6][6] / (err + cost[6][6]));

    err = err_d;
    for (int i = 4; i >= 0; i--) {
      err += cost[i][5] * (5 - i) * 2;
    }
    acc[5] = (float) ((cost[6][5] + cost[5][5]) / (err + cost[6][5] + cost[5][5]));

    err = err_d;
    for (int i = 3; i >= 0; i--) {
      err += cost[i][4] * (4 - i) * 2;
    }
    acc[4] = (float) ((cost[6][4] + cost[5][4] + cost[4][4]) / (err + cost[6][4] + cost[5][4] + cost[4][4]));

//        r += defaultFormat.format(acc[0]);//VL
//        r += "," + defaultFormat.format(acc[1]);//ML
//        r += "," + defaultFormat.format(acc[2]);//LL
//        //r += ",S: " + defaultFormat.format(acc[3]);
//        r += "," + defaultFormat.format(acc[4]);//LH
//        r += "," + defaultFormat.format(acc[5]);//MH
//        r += "," + defaultFormat.format(acc[6]);//VH
    return acc;
  }

  private Instances selectAttributes(Instances originalData) throws Exception {
    AttributeSelection selection = new AttributeSelection();
    Ranker ranker = new Ranker();
    ranker.setOptions(new String[]{"-N", "50"});
    ReliefFAttributeEval eval = new ReliefFAttributeEval();
    eval.setOptions(new String[]{"-M", "500"});
    selection.setEvaluator(eval);
    selection.setSearch(ranker);
    selection.SelectAttributes(originalData);
    return selection.reduceDimensionality(originalData);
  }

  private Instances selectUnfavoredAttributes(Instances originalData) throws Exception {
    AttributeSelection selection = new AttributeSelection();
    Ranker ranker = new Ranker();
    ranker.setOptions(new String[]{"-N", "50"});
    ReliefFAttributeEval eval = new ReliefFAttributeEval();
    eval.setOptions(new String[]{"-M", "500"});
    selection.setEvaluator(eval);
    selection.setSearch(ranker);
    selection.SelectAttributes(originalData);
    int[] selected_indexes = selection.selectedAttributes();
    Arrays.sort(selected_indexes);
    for (int i = selected_indexes.length / 2; i >= 0; i--) {
      originalData.deleteAttributeAt(selected_indexes[i]);
    }
    //Select attributes for second time
    selection.SelectAttributes(originalData);
    return selection.reduceDimensionality(originalData);
  }

  private Instances weightDataByDate(String code) {
    try {
      String trainFileName = RESOURCE_PATH + m_TypePath + code + "//" + code + "_Training.arff";
      DataSource trainSource;
      trainSource = new ConverterUtils.DataSource(trainFileName);
      Instances data = trainSource.getDataSet();

      Calendar tempdate = Calendar.getInstance();
      Instance ins;
      for (int i = 0; i < data.numInstances(); i++) {
        ins = data.instance(i);
        tempdate.setTime(GlobalConfigs.getDateFormat().parse(ins.stringValue(1)));
        int yeardiff = Calendar.getInstance().get(Calendar.YEAR) - tempdate.get(Calendar.YEAR);
        //double weight = -0.01 * (Math.pow(yeardiff, 2)) + 1;
        double weight = -0.2 * yeardiff + 1;
        if (weight < 0) {
          weight = 0;
        }
        ins.setWeight(weight);
      }
      return data;
    } catch (Exception ex) {
      Logger.getLogger(WekaModelBenchamrker.class.getName()).log(Level.SEVERE, null, ex);
    }
    return null;
  }
}
