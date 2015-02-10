package model;

import core.GlobalConfigs;
import static core.GlobalConfigs.RESOURCE_PATH;

import core.Performance;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.core.converters.ConverterUtils.DataSource;

public class WekaModelBenchamrker {

  public static ArrayList<HashMap<Evaluation, double[]>>
          benchmarkByClasses(String code,
                  ArrayList<String> classifierNames,
                  ArrayList<String[]> optionsList, int folds) {
    ArrayList<HashMap<Evaluation, double[]>> list = new ArrayList();
    for (int i = 0; i < GlobalConfigs.ClassCount; i++) {
      try {
        Instances dataReadyToTrain = removeUnwantedClassValues(code, i);
        list.add(benchMarkModels("Primary",
                selectAttributes(new Instances(dataReadyToTrain)),
                code, classifierNames, optionsList, i, folds));
        //Do it again with unfavored attributes
        list.add(benchMarkModels("Secondary",
                selectUnfavoredAttributes(new Instances(dataReadyToTrain)),
                code, classifierNames, optionsList, i, folds));

      } catch (Exception ex) {
        Logger.getLogger(WekaModelBenchamrker.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    return list;
  }

  public static HashMap<Evaluation, double[]>
          benchMarkModels(String additionInfo, Instances trainingData,
                  String code, ArrayList<String> classifierNames,
                  ArrayList<String[]> optionsList, int classIndex, int folds) throws Exception {
    HashMap<Evaluation, double[]> modelBenchMap = new HashMap();
    for (int i = 0; i < classifierNames.size(); i++) {

      Instances instances = trainingData;
      Classifier classifier
              = AbstractClassifier.forName(
                      classifierNames.get(i),
                      optionsList.get(i).clone());
      Evaluation eval = new Evaluation(instances);
      eval.crossValidateModel(classifier, instances, folds, new Random());
      classifier.buildClassifier(instances);
      double[] result = generateResult(eval);

      String identify = optionsList.get(i)[3].substring(17)
              + "-" + instances.classAttribute().name() + "-" + additionInfo;

      System.out.println(identify);
      System.out.println(eval.toMatrixString());

      //however, the model for the same classifier will be different
      Performance.saveModelAndPerformance(code, identify,
              classifier, trainingData.stringFreeStructure(), result);

      //bench mark for this classifier(ignore the difference between models)
      Performance.putClassResult(code, optionsList.get(i)[3].substring(17) + "-" + additionInfo,
              result, classIndex);
    }
    return modelBenchMap;
  }

  private static Instances removeUnwantedClassValues(String code, int classIndex) {
    try {
      String trainFileName = RESOURCE_PATH + code + "//" + code + "_Training.arff";
      DataSource trainSource;
      trainSource = new ConverterUtils.DataSource(trainFileName);
      Instances trainInstances = trainSource.getDataSet();
      int target = GlobalConfigs.ClassCount;
      trainInstances.setClassIndex(GlobalConfigs.TrainingCount + classIndex);
      for (int i = 0; i < target; i++) {
        int tempind = GlobalConfigs.TrainingCount + i;
        if (trainInstances.classIndex() != tempind) {
          trainInstances.deleteAttributeAt(tempind);
          i--;
          target--;
        }
      }
      return trainInstances;
    } catch (Exception ex) {
      Logger.getLogger(WekaModelBenchamrker.class.getName()).log(Level.SEVERE, null, ex);
    }
    return null;
  }

  private static double[] generateResult(Evaluation eval) {
    String r = "";
    NumberFormat defaultFormat = NumberFormat.getPercentInstance();
    defaultFormat.setMinimumFractionDigits(2);

    double[][] cost = eval.confusionMatrix();

    double[] acc = new double[7];

    double err = 0.1;
    for (int i = 1; i < 6; i++) {
      err += cost[i][0] * i;
    }
    acc[0] = cost[0][0] / (err + cost[0][0]);

    err = 0.1;
    for (int i = 2; i < 5; i++) {
      err += cost[i][1] * (i - 1);
    }
    acc[1] = (cost[0][1] + cost[1][1]) / (err + cost[0][1] + cost[1][1]);

    err = 0.1;
    for (int i = 3; i < 4; i++) {
      err += cost[i][2] * (i - 2);
    }
    acc[2] = (cost[0][2] + cost[1][2] + cost[2][2]) / (err + cost[0][2] + cost[1][2] + cost[2][2]);

    // The accuracy for "Stay" is not considered at the moment
    acc[3] = -1;

    // 
    err = 0.1;
    for (int i = 5; i >= 0; i--) {
      err += cost[i][6] * (6 - i);
    }
    acc[6] = cost[6][6] / (err + cost[6][6]);

    err = 0.1;
    for (int i = 4; i >= 0; i--) {
      err += cost[i][5] * (5 - i);
    }
    acc[5] = (cost[6][5] + cost[5][5]) / (err + cost[6][5] + cost[5][5]);

    err = 0.1;
    for (int i = 3; i >= 0; i--) {
      err += cost[i][4] * (4 - i);
    }
    acc[4] = (cost[6][4] + cost[5][4] + cost[4][4]) / (err + cost[6][4] + cost[5][4] + cost[4][4]);

//        r += defaultFormat.format(acc[0]);//VL
//        r += "," + defaultFormat.format(acc[1]);//ML
//        r += "," + defaultFormat.format(acc[2]);//LL
//        //r += ",S: " + defaultFormat.format(acc[3]);
//        r += "," + defaultFormat.format(acc[4]);//LH
//        r += "," + defaultFormat.format(acc[5]);//MH
//        r += "," + defaultFormat.format(acc[6]);//VH
    return acc;
  }

  private static Instances selectAttributes(Instances originalData) throws Exception {
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

  private static Instances selectUnfavoredAttributes(Instances originalData) throws Exception {
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
    for (int i = selected_indexes.length - 2; i >= 0; i--) {
      originalData.deleteAttributeAt(selected_indexes[i]);
    }
    //Select attributes for second time
    selection.SelectAttributes(originalData);
    return selection.reduceDimensionality(originalData);

  }

}
