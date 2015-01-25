package datapreparer;

import java.io.File;
import org.encog.ml.data.versatile.VersatileMLDataSet;
import org.encog.ml.data.versatile.columns.ColumnDefinition;
import org.encog.ml.data.versatile.columns.ColumnType;
import org.encog.ml.data.versatile.sources.CSVDataSource;
import org.encog.util.csv.CSVFormat;

public class EncogTrainingDataPreparer {

    public static VersatileMLDataSet prepareTrainingData(String trainingFileName) {
        VersatileMLDataSet data = new VersatileMLDataSet(new CSVDataSource(new File(trainingFileName), false,
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

        
        
        return data;
    }

}
