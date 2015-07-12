package model;

import core.GConfigs;
import java.util.concurrent.ThreadLocalRandom;
import util.MyUtils;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.timeseries.WekaForecaster;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

public class Evaluator {

  /**
   * @param code
   * @param classifier
   * @param evaluationData missing class values were removed, deep copy of
   * original data
   * @param daysToAdvance
   * @param assistanceData
   * @param trainingData
   * @param typePath
   * @return
   * @throws java.lang.Exception
   */
  protected static double[] evalClassifier(String code,
          Classifier classifier, final Instances trainingData,
          final Instances evaluationData, Instances assistanceData,
          int daysToAdvance, String typePath) throws Exception {
    Instances local_data = new Instances(trainingData);
    int size = evaluationData.size() - 1;
    double true_positive = 0;
    double positive_count = 0;
    double random_true = 0;
    double random_count = 0;
    int days_for_eval = (int) Math.min(trainingData.size() * 0.7, 1000);

    double[] errors = new double[days_for_eval];
    int error_count = 0;
    double threshold = determineEvaluationThreshold(evaluationData, daysToAdvance, typePath);
    Attribute class_att = evaluationData.classAttribute();
    Classifier tclassifier = AbstractClassifier.makeCopy(classifier);
    Classifier noise_classifier = new RandomForest();//AbstractClassifier.makeCopy(classifier);

    int buffer = 0;
    for (int i = 0; i < daysToAdvance; i++) {
      local_data.remove(size - i);
    }

    boolean noise_learned = false;
    size -= daysToAdvance;
    for (int i = 0, c = 0; i < days_for_eval; c++, i++) {
      if (buffer == 0) {
        for (int j = 0; j < 15; j++) {
          local_data.remove(local_data.size() - 1);
        }
        tclassifier.buildClassifier(local_data);
        if (assistanceData.size() >= 60) {
          noise_classifier.buildClassifier(assistanceData);
          noise_learned = true;
        }
        buffer = 15;
      } else {
        buffer--;
      }

      Instance inst = evaluationData.get(size + daysToAdvance - c);
      double fr = tclassifier.classifyInstance(inst);
      double av = evaluationData.get(size + daysToAdvance - c).classValue();
      double is_noise = 5;
      if (noise_learned) {
        is_noise = noise_classifier.classifyInstance(inst);
      }
      
//      if (class_att.name().contains("Stable")) {
//        System.out.println("fr=" + MyUtils.roundDouble(fr)
//                + ", av=" + MyUtils.roundDouble(av)
//                + ", noise=" + is_noise);
//      }
//      if (is_noise < 0) {
//        continue;
//      }

      boolean positive_decision = false;
      boolean class_correct = false;

      if (class_att.name().contains("Signal")) {
        if (class_att.isNominal()) {
          av = MyUtils.NomValueToNum(class_att.value((int) av));
          fr = MyUtils.NomValueToNum(class_att.value((int) fr));
        }
        if (fr >= threshold) {
          positive_decision = true;
          if (av >= threshold) {
            class_correct = true;
            inst.setClassValue(1);
          } else {
            inst.setClassValue(0);
          }
          assistanceData.add(inst);
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

          positive_decision = true;
          if (fr > 0 && av >= threshold) {
            class_correct = true;
            inst.setClassValue(1);
          } else if (fr < 0 && av <= threshold * -1) {
            class_correct = true;
            inst.setClassValue(1);
          } else {
            inst.setClassValue(0);
          }
          assistanceData.add(inst);
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

      if (is_noise != 5 && is_noise > 0.6) {
        if (positive_decision) {
          positive_count++;
          if (class_correct) {
            true_positive++;
          } else {
            errors[i] = Math.abs(av - fr);
            error_count++;
          }
        }
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
    int days_for_eval = 1000;
    double[] errors = new double[days_for_eval];
    double random_count = 0;
    double random_false = 0;
    Instances local_data = new Instances(originalData);
    double threshold = determineEvaluationThreshold(originalData, daysToAdvance, typePath);
    for (int i = 0; i < days_for_eval; i++) {
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

    //Check if the class att nominal or numeric, if nominal:
    Attribute class_att = originalData.classAttribute();
    if (class_att.name().contains("Signal")) {
      return 0.005;
    }

    return sig;
  }
}
