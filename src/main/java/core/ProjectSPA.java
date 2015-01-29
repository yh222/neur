package core;

import core.GlobalConfigs.CLASS_VALUES;
import static core.GlobalConfigs.DEFAULT_PATH;
import datapreparer.CSVDownloader;
import datapreparer.TrainingFileGenerator;
import java.util.ArrayList;
import model.WekaModelBenchamrker;

public class ProjectSPA {

    public static void main(String[] args) {

        CSVDownloader.updateRawDataFromYahoo();

        TrainingFileGenerator tdg = new TrainingFileGenerator();
        tdg.generateTrainingData(true, true, true);

        String trainingFileName = DEFAULT_PATH + "//resources//MSFT//MSFT_Training.csv";

        ArrayList<String> classifierNames = new ArrayList();
        ArrayList<String[]> optionsList = new ArrayList();
        classifierNames.add("weka.classifiers.trees.J48");
        optionsList.add(new String[]{"-C", "0.25", "-M", "2"});

        
        int num_folds = 10;

        WekaModelBenchamrker.benchmarkByClasses(trainingFileName, classifierNames, optionsList, num_folds);

    }

}
