package core;

import static core.GlobalConfigs.DEFAULT_PATH;
import datapreparer.CSVDownloader;
import datapreparer.TrainingFileGenerator;


public class ProjectSPA {

    public static void main(String[] args) {

        CSVDownloader.updateRawDataFromYahoo();

        TrainingFileGenerator tdg = new TrainingFileGenerator();
        tdg.generateTrainingData(true, true, true);

        String trainingFileName = DEFAULT_PATH + "//resources//AAPL//AAPL_Training.csv";

        
        

    }

}
