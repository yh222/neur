package model;

import java.util.ArrayList;
import java.util.HashMap;
import org.encog.ml.MLRegression;
import org.encog.ml.data.versatile.VersatileMLDataSet;
import org.encog.ml.model.EncogModel;
import org.encog.util.simple.EncogUtility;

public class EncogModelBenchmarker {

    /**
     * @param maxError The threshold to filter models below the maximum error.
     */
    public static HashMap<MLRegression, Double> benchmarkModels(VersatileMLDataSet trainingData, ArrayList<String[]> modelConfigs, double maxError) {
        HashMap<MLRegression, Double> modelScoreMap = new HashMap();
        double score_sum = 0;
        for (String[] confg : modelConfigs) {
            EncogModel temp_model = createModel(
                    trainingData,
                    confg[0],
                    confg[1],
                    confg[2],
                    confg[3]);
            // Train the model
            MLRegression temp_method = (MLRegression) temp_model.crossvalidate(5, true);

            // Get the benchmark score
            double error = EncogUtility.calculateRegressionError(temp_method, temp_model.getValidationDataset());
            System.out.println("Training error: " + EncogUtility.calculateRegressionError(temp_method, temp_model.getTrainingDataset()));
            System.out.println("Validation error: " + EncogUtility.calculateRegressionError(temp_method, temp_model.getValidationDataset()));
//            if (error > maxError) {
//                continue;
//            }
            //Use maxError - error for normalization, to make better methods to have siginificantly higher weight in the future;
            modelScoreMap.put(temp_method, (maxError - error));
            score_sum += (maxError - error);
        }

        //Replace old error with normalized error. Sum of normalized errors will be 1
        for (MLRegression key : modelScoreMap.keySet()) {
            modelScoreMap.put(key, (double) modelScoreMap.get(key) / score_sum);
        }

        return modelScoreMap;
    }

    public static EncogModel createModel(
            VersatileMLDataSet trainingData,
            String methodName,
            String methodArgs,
            String trainerName,
            String trainerArgs) {

        // Create the model
        EncogModel model = new EncogModel(trainingData);
        model.selectMethod(model.getDataset(), methodName, methodArgs, trainerName, trainerArgs);
        //model.getDataset().normalize();
        
        model.holdBackValidation(0.3, true, 5548);

        return model;
    }
}

