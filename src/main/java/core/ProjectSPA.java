package core;

import datapreparer.TrainingFileGenerator;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import model.WekaModelBenchamrker;

public class ProjectSPA {

  public static void main(String[] args) {

    TrainingFileGenerator tdg = new TrainingFileGenerator();
    tdg.generateTrainingData(true, true, true);

    String matrix = "["
            + "0.0 0.5 1.0 1.0 5.0 5.0 5.0; "
            + "1.0 0.0 0.5 1.0 5.0 5.0 5.0; "
            + "3.0 1.0 0.0 1.0 3.0 5.0 5.0; "
            + "5.0 3.0 2.0 0.0 2.0 3.0 5.0; "
            + "5.0 5.0 3.0 1.0 0.0 1.0 3.0; "
            + "5.0 5.0 5.0 1.0 0.5 0.0 1.0; "
            + "5.0 5.0 5.0 1.0 1.0 0.5 0.0]";
    ArrayList<String> classifierNames = new ArrayList();
    ArrayList<String[]> optionsList = new ArrayList();
//        classifierNames.add("weka.classifiers.meta.CostSensitiveClassifier");
//        optionsList.add(new String[]{"-cost-matrix", "\"" + matrix + "\"", "-W", "weka.classifiers.trees.J48"});
    classifierNames.add("weka.classifiers.meta.CostSensitiveClassifier");
    optionsList.add(new String[]{"-cost-matrix", "\"" + matrix + "\"", "-W"
            ,"weka.classifiers.trees.RandomForest", "--", "-I", "40", "-depth", "15"});
    //optionsList.add(new String[]{"-C", "matrix.cost", "-N", "D:\\Documents\\NetBeansProjects\\ProjectSPA", "-W", "weka.classifiers.trees.RandomForest", "--", "-num-slots", "6"});
    int num_folds = 10;

    ExecutorService executor = Executors.newFixedThreadPool(6);

    ArrayList<String> instruments = GlobalConfigs.INSTRUMENT_CODES;
    for (String i : instruments) {
      Runnable worker = new ClassicifyThread(i, (ArrayList<String>) classifierNames.clone(), cloneList(optionsList), num_folds);
      executor.execute(worker);
    }
    executor.shutdown();
    while (!executor.isTerminated()) {
    }
    Performance.outputPerformanceData();

  }

  private static ArrayList<String[]> cloneList(ArrayList<String[]> list) {
    ArrayList<String[]> r = new ArrayList();
    for (String[] l : list) {
      r.add(l);
    }
    return r;
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
