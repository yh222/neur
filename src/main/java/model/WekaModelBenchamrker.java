package model;

import core.GlobalConfigs.CLASS_VALUES;
import core.GlobalConfigs.TRAINING_VALUES;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.evaluation.EvaluationMetricHelper;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.core.converters.ConverterUtils.DataSource;

public class WekaModelBenchamrker {

    public static HashMap<Evaluation, EvaluationMetricHelper> benchMarkModels(String trainFileName, ArrayList<String> classifierNames, ArrayList<String[]> optionsList, int classIndex, int folds) {
        HashMap<Evaluation, EvaluationMetricHelper> modelBenchMap = new HashMap();
        for (int i = 0; i < classifierNames.size(); i++) {
            evaluateModel(removeUnwantedClassValues(trainFileName, classIndex), classifierNames.get(i), optionsList.get(i), folds, modelBenchMap);
        }
        return modelBenchMap;
    }

    /*
     *
     */
    public static void evaluateModel(Instances trainInstances, String classifierName, String[] options, int folds, HashMap<Evaluation, EvaluationMetricHelper> modelBenchMap) {
        try {
            Evaluation eval = new Evaluation(trainInstances);
            EvaluationMetricHelper helper = new EvaluationMetricHelper(eval);
            eval.crossValidateModel(classifierName, trainInstances, folds, options, new Random());
            if (modelBenchMap != null) {
                modelBenchMap.put(eval, helper);
            }
            System.out.println(eval.toSummaryString());
        } catch (Exception ex) {
            Logger.getLogger(WekaModelBenchamrker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static Instances removeUnwantedClassValues(String trainFileName, int classIndex) {
        try {
            DataSource trainSource;
            trainSource = new ConverterUtils.DataSource(trainFileName);
            Instances trainInstances = trainSource.getDataSet();
            int target = CLASS_VALUES.SIZE;
            trainInstances.setClassIndex(classIndex);
            for (int i = 0; i < target; i++) {
                int tempind = TRAINING_VALUES.SIZE + i;
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

}
