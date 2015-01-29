package model;

import core.GlobalConfigs.CLASS_VALUES;
import core.GlobalConfigs.TRAINING_VALUES;
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
    
    public static ArrayList<HashMap<Evaluation, EvaluationMetricHelper>> benchmarkByClasses(String trainFileName, ArrayList<String> classifierNames, ArrayList<String[]> optionsList, int folds) {
        ArrayList<HashMap<Evaluation, EvaluationMetricHelper>> list = new ArrayList();
        for (int i = 0; i < CLASS_VALUES.SIZE; i++) {
            benchMarkModels(trainFileName, classifierNames, optionsList, i, folds);
        }
        return list;
    }
    
    public static HashMap<Evaluation, EvaluationMetricHelper> benchMarkModels(String trainFileName, ArrayList<String> classifierNames, ArrayList<String[]> optionsList, int classIndex, int folds) {
        HashMap<Evaluation, EvaluationMetricHelper> modelBenchMap = new HashMap();
        for (int i = 0; i < classifierNames.size(); i++) {
            evaluateModel(removeUnwantedClassValues(trainFileName, classIndex), classifierNames.get(i), optionsList.get(i), folds, modelBenchMap);
        }
        return modelBenchMap;
    }
    
    public static void evaluateModel(Instances trainInstances, String classifierName, String[] options, int folds, HashMap<Evaluation, EvaluationMetricHelper> modelBenchMap) {
        try {
            Instances instances = trainInstances;
            Classifier classifier = AbstractClassifier.forName(classifierName, options);
//            if (!classifier.getCapabilities().handles(NUMERIC_CLASS)) {
//                System.out.println("This classifier:" + classifierName + " does not handle numeric class value.");
//                System.out.println("Transforming numeric class into nominal.");
//                Discretize disTransform = new Discretize();
//                disTransform.setAttributeIndices("last");
//                //disTransform.setUseEqualFrequency(true);
//                int t=instances.classIndex();
//                instances.setClassIndex(-1);
//                disTransform.setInputFormat(instances);
//                disTransform.setBins(5);
//                instances = Filter.useFilter(instances, disTransform);
//                instances.setClassIndex(t);
//            }
            
            Evaluation eval = new Evaluation(instances);
            EvaluationMetricHelper helper = new EvaluationMetricHelper(eval);
            eval.crossValidateModel(classifier, instances, folds, new Random());
            if (modelBenchMap != null) {
                modelBenchMap.put(eval, helper);
            }
            System.out.println(classifierName);
            System.out.println(instances.classAttribute().name());
            System.out.println(eval.toSummaryString());
            System.out.println(eval.toMatrixString());
        } catch (weka.core.UnsupportedAttributeTypeException ex) {
            Logger.getLogger(WekaModelBenchamrker.class.getName()).log(Level.SEVERE, null, ex);
            //System.out.println("This classifier:" + classifierName + " does not handle numeric class value.");
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
            trainInstances.setClassIndex(classIndex + TRAINING_VALUES.SIZE);
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
