package core;

import static core.GlobalConfigs.DEFAULT_PATH;
import datapreparer.CSVDownloader;
import datapreparer.TrainingDataGenerator;
import java.io.File;
import java.util.Arrays;
import org.encog.ConsoleStatusReportable;
import org.encog.Encog;
import org.encog.ml.MLRegression;
import org.encog.ml.data.MLData;
import org.encog.ml.data.versatile.NormalizationHelper;
import org.encog.ml.data.versatile.VersatileMLDataSet;
import org.encog.ml.data.versatile.columns.ColumnDefinition;
import org.encog.ml.data.versatile.columns.ColumnType;
import org.encog.ml.data.versatile.sources.CSVDataSource;
import org.encog.ml.data.versatile.sources.VersatileDataSource;
import org.encog.ml.factory.MLMethodFactory;
import org.encog.ml.model.EncogModel;
import org.encog.util.csv.CSVFormat;
import org.encog.util.csv.ReadCSV;
import org.encog.util.simple.EncogUtility;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.data.DataSetRow;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.util.TransferFunctionType;
import org.neuroph.util.data.norm.DecimalScaleNormalizer;
import org.neuroph.util.data.norm.MaxMinNormalizer;
import org.neuroph.util.data.norm.RangeNormalizer;

/**
 *
 */
public class ProjectSPA {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        CSVDownloader.updateRawDataFromYahoo();

        TrainingDataGenerator tdg = new TrainingDataGenerator();
        tdg.generateTrainingData(true, true, true);

        String inputFileName = DEFAULT_PATH + "//resources//AAPL//AAPL_Training.csv";

        // create MultiLayerPerceptron neural network
//        MultiLayerPerceptron neuralNet = new MultiLayerPerceptron(TransferFunctionType.TANH,11, 50, 4);
//        DataSet dataSet = DataSet.createFromFile(inputFileName, 11, 4, ",", false);
//        DecimalScaleNormalizer dsn=new DecimalScaleNormalizer();
//        dsn.normalize(dataSet);
//        neuralNet.learn(dataSet);
        VersatileDataSource source = new CSVDataSource(new File(inputFileName), false,
                CSVFormat.EG_FORMAT);
        VersatileMLDataSet data = new VersatileMLDataSet(source);
        ColumnDefinition outputColumnin1 = data.defineSourceColumn("rt1", 0, ColumnType.continuous);
        ColumnDefinition outputColumnin2 = data.defineSourceColumn("rt2", 1, ColumnType.continuous);
        ColumnDefinition outputColumnin3 = data.defineSourceColumn("rt3", 2, ColumnType.continuous);
        ColumnDefinition outputColumnin4 = data.defineSourceColumn("rt4", 3, ColumnType.continuous);
        ColumnDefinition outputColumnin5 = data.defineSourceColumn("rt5", 4, ColumnType.continuous);
        ColumnDefinition outputColumnin6 = data.defineSourceColumn("rt6", 5, ColumnType.continuous);
        ColumnDefinition outputColumnin7 = data.defineSourceColumn("rt7", 6, ColumnType.continuous);
        ColumnDefinition outputColumnin8 = data.defineSourceColumn("rt8", 7, ColumnType.continuous);
        ColumnDefinition outputColumnin9 = data.defineSourceColumn("rt9", 8, ColumnType.continuous);
        ColumnDefinition outputColumnin10 = data.defineSourceColumn("season", 9, ColumnType.nominal);
        ColumnDefinition outputColumnin11 = data.defineSourceColumn("velo", 10, ColumnType.continuous);
        data.defineInput(outputColumnin1);
        data.defineInput(outputColumnin2);
        data.defineInput(outputColumnin3);
        data.defineInput(outputColumnin4);
        data.defineInput(outputColumnin5);
        data.defineInput(outputColumnin6);
        data.defineInput(outputColumnin7);
        data.defineInput(outputColumnin8);
        data.defineInput(outputColumnin9);
        data.defineInput(outputColumnin10);
        data.defineInput(outputColumnin11);

//        ColumnDefinition outputColumn1 = data.defineSourceColumn("ft1", 11,
//                ColumnType.continuous);
//        ColumnDefinition outputColumn2 = data.defineSourceColumn("ft2", 12,
//                ColumnType.continuous);
//        ColumnDefinition outputColumn3 = data.defineSourceColumn("ft3", 13,
//                ColumnType.continuous);
//        ColumnDefinition outputColumn4 = data.defineSourceColumn("ft4", 14,
//                ColumnType.continuous);
        ColumnDefinition outputColumn5 = data.defineSourceColumn("fs1", 15,
                ColumnType.nominal);


//        data.defineOutput(outputColumn1);
//        data.defineOutput(outputColumn2);
//        data.defineOutput(outputColumn3);
//        data.defineOutput(outputColumn4);
        //data.defineOutput(outputColumn5);
        data.defineSingleOutputOthersInput(outputColumn5);

        // Analyze the data, determine the min/max/mean/sd of every column.
        data.analyze();

        EncogModel model = new EncogModel(data);
        model.selectMethod(data, MLMethodFactory.TYPE_FEEDFORWARD);
        //model.selectMethod(data, MLMethodFactory.TYPE_NEAT);
        // Send any output to the console.
        model.setReport(new ConsoleStatusReportable());
        data.normalize();

        model.holdBackValidation(0.3, true, 1001);

        // Choose whatever is the default training type for this model.
        model.selectTrainingType(data);

        // Use a 5-fold cross-validated train.  Return the best method found.
        MLRegression bestMethod = (MLRegression) model.crossvalidate(3, true);
        System.out.println("Training error: " + EncogUtility.calculateRegressionError(bestMethod, model.getTrainingDataset()));
        System.out.println("Validation error: " + EncogUtility.calculateRegressionError(bestMethod, model.getValidationDataset()));

        // Display our normalization parameters.
        NormalizationHelper helper = data.getNormHelper();
        System.out.println(helper.toString());

        // Display the final model.
        System.out.println("Final model: " + bestMethod);
        ReadCSV csv = new ReadCSV(new File(inputFileName), false, CSVFormat.DECIMAL_POINT);
        String[] line = new String[11];
        MLData input = helper.allocateInputVector();

        while (csv.next()) {
            StringBuilder result = new StringBuilder();
            line[0] = csv.get(0);
            line[1] = csv.get(1);
            line[2] = csv.get(2);
            line[3] = csv.get(3);
            line[4] = csv.get(4);
            line[5] = csv.get(5);
            line[6] = csv.get(6);
            line[7] = csv.get(7);
            line[8] = csv.get(8);
            line[9] = csv.get(9);
            line[10] = csv.get(10);
            String d7r = csv.get(11);
            String d14r = csv.get(12);
            String d24r = csv.get(13);
            String d49r = csv.get(14);
            String d7fs = csv.get(15);

            helper.normalizeInputVector(line, input.getData(), false);
            MLData output = bestMethod.compute(input);
            //String day7 = helper.denormalizeOutputVectorToString(output)[0];
//            String day49 = helper.denormalizeOutputVectorToString(output)[3];
            String day7fs = helper.denormalizeOutputVectorToString(output)[0];
            //result.append(Arrays.toString(line));
//            result.append(" -> 7d: ");
//            result.append(day7);
//            result.append("(correct: ");
//            result.append(d7r);
//            result.append(")");

//            result.append(" -> 49d: ");
//            result.append(day49);
//            result.append("(correct: ");
//            result.append(d49r);
//            result.append(")");
            result.append(" -> 7d fs: ");
            result.append(day7fs);
            result.append("(correct: ");
            result.append(d7fs);
            result.append(")");

            System.out.println(result.toString());
        }

        Encog.getInstance().shutdown();
//        testNeuralNetwork(neuralNet, dataSet);

    }

    public static void testNeuralNetwork(NeuralNetwork neuralNet, DataSet testSet) {

        for (DataSetRow testSetRow : testSet.getRows()) {
            neuralNet.setInput(testSetRow.getInput());
            neuralNet.calculate();
            double[] networkOutput = neuralNet.getOutput();

            System.out.println("Input: " + Arrays.toString(testSetRow.getInput()));
            System.out.println(" Output: " + Arrays.toString(networkOutput));
        }
    }

}
