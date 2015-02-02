package model;

import core.GlobalConfigs.CLASS_VALUES;
import static core.GlobalConfigs.RESOURCE_PATH;
import core.GlobalConfigs.TRAINING_VALUES_NOMINAL;
import core.GlobalConfigs.TRAINING_VALUES_NUMERIC;
import core.Performance;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.evaluation.EvaluationMetricHelper;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.core.converters.ConverterUtils.DataSource;

public class WekaModelBenchamrker {

    public static ArrayList<HashMap<Evaluation, EvaluationMetricHelper>> benchmarkByClasses(String code, ArrayList<String> classifierNames, ArrayList<String[]> optionsList, int folds) {
        ArrayList<HashMap<Evaluation, EvaluationMetricHelper>> list = new ArrayList();
        for (int i = 0; i < CLASS_VALUES.SIZE; i++) {
            benchMarkModels(code, classifierNames, optionsList, i, folds);
        }
        return list;
    }

    public static HashMap<Evaluation, EvaluationMetricHelper> benchMarkModels(String code, ArrayList<String> classifierNames, ArrayList<String[]> optionsList, int classIndex, int folds) {
        HashMap<Evaluation, EvaluationMetricHelper> modelBenchMap = new HashMap();
        double[] results;
        for (int i = 0; i < classifierNames.size(); i++) {
            results = evaluateModel(removeUnwantedClassValues(code, classIndex), classifierNames.get(i), optionsList.get(i), folds, modelBenchMap);
            Performance.putClassResult(code, classifierNames.get(i), results, classIndex);
        }
        return modelBenchMap;
    }

    public static double[] evaluateModel(Instances trainInstances, String classifierName, String[] options, int folds, HashMap<Evaluation, EvaluationMetricHelper> modelBenchMap) {
        try {
            Performance.putClassifier(classifierName);

            Instances instances = trainInstances;
            Classifier classifier = AbstractClassifier.forName(classifierName, options);
            Evaluation eval = new Evaluation(instances);
            EvaluationMetricHelper helper = new EvaluationMetricHelper(eval);
            eval.crossValidateModel(classifier, instances, folds, new Random());
            if (modelBenchMap != null) {
                modelBenchMap.put(eval, helper);
            }

//            double[][] d = eval.confusionMatrix();
//            System.out.println(classifierName);
            System.out.println(instances.classAttribute().name());
//            System.out.println(eval.toSummaryString());
            System.out.println(eval.toMatrixString());
            return generateResult(eval);

        } catch (weka.core.UnsupportedAttributeTypeException ex) {
            Logger.getLogger(WekaModelBenchamrker.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(WekaModelBenchamrker.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private static Instances removeUnwantedClassValues(String code, int classIndex) {
        try {
            String trainFileName = RESOURCE_PATH + code + "//" + code + "_Training.arff";
            DataSource trainSource;
            trainSource = new ConverterUtils.DataSource(trainFileName);
            Instances trainInstances = trainSource.getDataSet();
            int target = CLASS_VALUES.SIZE;
            trainInstances.setClassIndex(classIndex + TRAINING_VALUES_NUMERIC.SIZE + TRAINING_VALUES_NOMINAL.SIZE);
            for (int i = 0; i < target; i++) {
                int tempind = TRAINING_VALUES_NUMERIC.SIZE + TRAINING_VALUES_NOMINAL.SIZE + i;
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

}
