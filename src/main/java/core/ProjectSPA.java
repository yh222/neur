package core;

import static core.GlobalConfigs.DEFAULT_PATH;
import datapreparer.CSVDownloader;
import datapreparer.TrainingDataGenerator;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.encog.ConsoleStatusReportable;
import org.encog.Encog;
import org.encog.ml.MLMethod;
import org.encog.ml.MLRegression;
import org.encog.ml.data.MLData;
import org.encog.ml.data.versatile.NormalizationHelper;
import org.encog.ml.data.versatile.VersatileMLDataSet;
import org.encog.ml.data.versatile.columns.ColumnDefinition;
import org.encog.ml.data.versatile.columns.ColumnType;
import org.encog.ml.data.versatile.sources.CSVDataSource;
import org.encog.ml.data.versatile.sources.VersatileDataSource;
import org.encog.ml.factory.MLMethodFactory;
import org.encog.ml.factory.MLTrainFactory;
import org.encog.ml.model.EncogModel;
import org.encog.util.csv.CSVFormat;
import org.encog.util.csv.ReadCSV;

public class ProjectSPA {

    public static void main(String[] args) {

        CSVDownloader.updateRawDataFromYahoo();

        TrainingDataGenerator tdg = new TrainingDataGenerator();
        tdg.generateTrainingData(true, true);

        String inputFileName = DEFAULT_PATH + "//resources//AAPL//AAPL_Training.csv";
        VersatileMLDataSet data = new VersatileMLDataSet(new CSVDataSource(new File(inputFileName), false,
                CSVFormat.EG_FORMAT));
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
        //       data.defineSourceColumn("ft7", 11, ColumnType.continuous);
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
//        data.defineInput(outputColumnin11);
        ColumnDefinition outputColumn5 = data.defineSourceColumn("fs1", 15,
                ColumnType.nominal);
        //data.defineOutput(outputColumn5);
        data.defineSingleOutputOthersInput(outputColumn5);
        data.analyze();
        ArrayList configs = new ArrayList();
        configs.add(new String[]{MLMethodFactory.TYPE_FEEDFORWARD,
            "?:B->TANH->20:B->TANH->?",
            MLTrainFactory.PROPERTY_C,
            ""});
//        configs.add(new String[]{MLMethodFactory.TYPE_BAYESIAN,
//            "",
//            MLTrainFactory.TYPE_BAYESIAN,
//            ""});
        HashMap map = ModelBenchmarker.benchmarkModels(data, configs, 0.8);

        NormalizationHelper helper = data.getNormHelper();
        System.out.println(helper.toString());

        for (Object method : map.keySet().toArray()) {
            System.out.println("model: " + method + ", score:" + map.get(method));
            // Display the final model.
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
                //line[11] = csv.get(11);
                //String d7r = csv.get(11);
                String d14r = csv.get(12);
                String d24r = csv.get(13);
                String d49r = csv.get(14);
                String d7fs = csv.get(15);
                helper.normalizeInputVector(line, input.getData(), false);
                MLData output = ((MLRegression) method).compute(input);
                String day7fs = helper.denormalizeOutputVectorToString(output)[0];

                result.append(" -> 7d fs: ");
                result.append(day7fs);
                result.append("(correct: ");
                result.append(d7fs);
                result.append(")");
                if (!day7fs.equals("Stay")) {
                    System.out.println(result.toString());
                }
            }
        }
        Encog.getInstance().shutdown();

    }

}
