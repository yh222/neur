package model;

import core.GConfigs;
import java.util.concurrent.ThreadLocalRandom;
import util.MyUtils;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.timeseries.WekaForecaster;
import weka.core.Attribute;
import weka.core.Instances;

public class Evaluator {

  private static final int m_DaysForEval = 30;

  /**
   * @param code
   * @param classifier
   * @param evaluationData missing class values were removed, deep copy of
   * original data
   * @param daysToAdvance
   * @param trainingData
   * @param numOfSiblings
   * @param typePath
   * @return
   * @throws java.lang.Exception
   */
  protected static double[] evalClassifier(String code,
          Classifier classifier, Instances trainingData,
          Instances evaluationData, int daysToAdvance,
          int numOfSiblings, String typePath) throws Exception {
    Instances local_data = new Instances(trainingData);
    int size = evaluationData.size() - 1;
    double true_positive = 0;
    double positive_count = 0;
    double random_true = 0;
    double random_count = 0;

    double[] errors = new double[m_DaysForEval];
    int error_count = 0;
    double threshold = determineEvaluationThreshold(evaluationData, daysToAdvance, typePath);
    Attribute class_att = evaluationData.classAttribute();
    Classifier tclassifier = AbstractClassifier.makeCopy(classifier);
    int buffer = 0;
    for (int i = 0; i < daysToAdvance * numOfSiblings; i++) {
      local_data.remove(size - i);
    }

    size -= daysToAdvance;
    for (int i = 0, c = 0; i < m_DaysForEval; c++) {
      if (ThreadLocalRandom.current().nextInt(4) == 0) {
        if (buffer == 0) {
          for (int j = 0; j < 20 * numOfSiblings; j++) {
            local_data.remove(local_data.size() - 1);
          }
          tclassifier.buildClassifier(local_data);
          buffer = 4;
        } else {
          buffer--;
        }
        double fr = (double) tclassifier.classifyInstance(evaluationData.get(size + daysToAdvance - c));
        double av = (double) evaluationData.get(size + daysToAdvance - c).classValue();
        if (class_att.name().contains("Signal")) {
          if (class_att.isNominal()) {
            av = MyUtils.NomValueToNum(class_att.value((int) av));
            fr = MyUtils.NomValueToNum(class_att.value((int) fr));
          }
          if (fr >= threshold) {
            positive_count++;
            if (av >= threshold) {
              true_positive++;
            } else {
              errors[i] = Math.abs(av - fr);
              error_count++;
            }
          }
          if (ThreadLocalRandom.current().nextBoolean()) {
            random_count++;
            if (av >= threshold) {
              random_true++;
            }
          }
        } else if (class_att.name().contains("Stable")) {
          if (class_att.isNominal()) {
            av = MyUtils.NomValueToNum(class_att.value((int) av));
            fr = MyUtils.NomValueToNum(class_att.value((int) fr));
          }
          if (Math.abs(fr) >= Math.abs(threshold)) {
            positive_count++;
            if (fr > 0 && av >= threshold) {
              true_positive++;
            } else if (fr < 0 && av <= threshold * -1) {
              true_positive++;
            } else {
              errors[i] = Math.abs(av - fr);
              error_count++;
            }
          }
          // Count for random rate, 50% change for random to be greater than threshold
          int r;
          if ((r = ThreadLocalRandom.current().nextInt(3)) != 0) {
            random_count++;
            if (r == 1 && av >= threshold) {
              random_true++;
            } else if (r == 2 && av <= threshold * -1) {
              random_true++;
            }
          }
        }
        i++;
      }
    }
    if(code.equals("XOM")){
      int m=1;
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
      sum += i;
    }
    double[] eval = new double[5];
    eval[0] = sum / error_count;
    eval[1] = true_positive;
    eval[2] = random_true;
    return eval;
  }

  private double[] evaluateForecaster(Classifier classifier,
          String class_name, Instances originalData,
          int daysToAdvance, String typePath) throws Exception {
    int size = originalData.size() - 1;
    double false_positive = 0;
    double positive_count = 0;
    double[] errors = new double[m_DaysForEval];
    double random_count = 0;
    double random_false = 0;
    Instances local_data = new Instances(originalData);
    double threshold = determineEvaluationThreshold(originalData, daysToAdvance, typePath);
    for (int i = 0; i < m_DaysForEval; i++) {
      WekaForecaster fc = new WekaForecaster();
      fc.setBaseForecaster(classifier);
      fc.setFieldsToForecast(class_name + ",RSI50SLP5");
      local_data.remove(size - i);
      fc.buildForecaster(local_data);
      fc.primeForecaster(local_data);
      if (size + daysToAdvance - i <= size) {
        double fr = (double) (fc.forecast(daysToAdvance)
                .get(daysToAdvance - 1).get(0).predicted());
        double av = (double) originalData.get(size + daysToAdvance - i)
                .classValue();
        if (Math.abs(fr) > Math.abs(threshold)) {
          positive_count++;
          if (Math.abs(av) <= Math.abs(threshold)) {
            false_positive++;
          }
        }
        if (ThreadLocalRandom.current().nextBoolean()) {
          random_count++;
          if (Math.abs(av) <= Math.abs(threshold)) {
            random_false++;
          }
        }
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

  private static double determineEvaluationThreshold(Instances originalData,
          int daysAdv, String typePath) {
    double sig = GConfigs.getSignificanceNormal(typePath, daysAdv);
    //double half_att_amount = originalData.classAttribute().numValues() / 2;
    int multi = 1;
    if (daysAdv > 5 && daysAdv <= 20) {
      multi = 2;
    } else if (daysAdv > 20) {
      multi = 3;
    }

    //Check if the class att nominal or numeric, if nominal:
    Attribute class_att = originalData.classAttribute();
    if (class_att.name().contains("Signal")) {
      return 0.005;
    }
//    if (class_att.isNominal()) {
//      int m2 = (int) (sig / 0.01);
//      //Check class attribute's each value tag, find the one contains
//      //"-inf-0.5*0.01" or "-0.5*0.01-0.5*0.01" or "-0.5*0.01-inf"
//      String tmpstr;
//      Enumeration<?> enm = class_att.enumerateValues();
//      while (enm.hasMoreElements()) {
//        tmpstr = enm.nextElement().toString();
//        if (tmpstr.contains("-inf-0.005") || tmpstr.contains("-0.005-inf")
//                || tmpstr.contains("-0.005-0.005")) {
//          if (class_att.name().contains("Highest")) {
//            return Math.min(class_att.indexOfValue(tmpstr) + multi,
//                    class_att.numValues() - 1);
//          } else {
//            return Math.max(0, class_att.indexOfValue(tmpstr) - multi);
//          }
//        }
//      }
//    }
    //if numeric 
    return sig;
  }
}
