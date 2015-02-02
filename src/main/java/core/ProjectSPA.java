package core;

import static core.GlobalConfigs.DEFAULT_PATH;
import datapreparer.CSVDownloader;
import datapreparer.TrainingFileGenerator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import model.WekaModelBenchamrker;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.evaluation.EvaluationMetricHelper;

public class ProjectSPA {

    public static void main(String[] args) {

        CSVDownloader.updateRawDataFromYahoo("d", GlobalConfigs.INSTRUMENT_CODES);
        CSVDownloader.updateRawDataFromYahoo("d", GlobalConfigs.INDICE_CODES);
        //Update dividend data
        CSVDownloader.updateRawDataFromYahoo("v", GlobalConfigs.INSTRUMENT_CODES);
        //CSVDownloader.updateRawDataFromYahoo("",GlobalConfigs.INDICE_CODES);

        TrainingFileGenerator tdg = new TrainingFileGenerator();
        tdg.generateTrainingData(true, true, true);

        // String trainingFileName = DEFAULT_PATH + "//resources//MSFT//MSFT_Training.arff";
        ArrayList<String> classifierNames = new ArrayList();
        ArrayList<String[]> optionsList = new ArrayList();
        classifierNames.add("weka.classifiers.trees.J48");
        optionsList.add(new String[]{"-C", "0.25", "-M", "2"});

        int num_folds = 10;

        ExecutorService executor = Executors.newFixedThreadPool(6);

        ArrayList<String> instruments = GlobalConfigs.INSTRUMENT_CODES;
        for (String i : instruments) {
            Runnable worker = new ClassicifyThread(i, classifierNames, optionsList, num_folds);
            executor.execute(worker);
//            WekaModelBenchamrker.benchmarkByClasses(i,
//                    classifierNames, optionsList, num_folds);
        }
        executor.shutdown();
        while (!executor.isTerminated()) {
        }

        Performance.outputPerformanceData();

    }

    private static class ClassicifyThread implements Runnable {

        String m_Code;
        ArrayList<String> m_cNames;
        ArrayList<String[]> m_cOptions;
        int m_folds;

        public ClassicifyThread(String i, ArrayList<String> classifierNames, ArrayList<String[]> optionsList, int num_folds) {
            m_Code = i;
            m_cNames = classifierNames;
            m_cOptions = optionsList;
            m_folds = num_folds;
        }

        @Override
        public void run() {
            WekaModelBenchamrker.benchmarkByClasses(m_Code,
                    m_cNames, m_cOptions, m_folds);
        }
    }
}
