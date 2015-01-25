package model;

import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.tuple.Pair;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.evaluation.EvaluationMetricHelper;
import weka.core.converters.ConverterUtils;
import weka.core.converters.ConverterUtils.DataSource;

public class WekaModelBenchamrker {

    HashMap<Evaluation, EvaluationMetricHelper> m_BenchMap = new HashMap();

    /*
     *
     */
    public void EvaluateModle(String trainFileName, String classifierName, String[] options, int classIndex, int folds) {
        try {
            DataSource trainSource = new ConverterUtils.DataSource(trainFileName);
            Evaluation eval = new Evaluation(trainSource.getDataSet(classIndex));
            EvaluationMetricHelper helper = new EvaluationMetricHelper(eval);
            eval.crossValidateModel(classifierName, trainSource.getDataSet(), folds, options, new Random());
            m_BenchMap.put(eval, helper);
        } catch (Exception ex) {
            Logger.getLogger(WekaModelBenchamrker.class.getName()).log(Level.SEVERE, "Exception while evaluating modle", ex);
        }

    }
}
