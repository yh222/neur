package core;

import calculator.Performance;
import core.GlobalConfigs.MODEL_TYPES;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import model.WekaModelBenchamrker;

public class ProjectSPA {

  public static void main(String[] args) {

    String matrix = "["
            + "0.0 0.5 1.0 1.0 9.0 9.0 9.0; "
            + "1.0 0.0 0.5 1.0 9.0 9.0 9.0; "
            + "5.0 1.0 0.0 1.0 5.0 9.0 9.0; "
            + "9.0 5.0 1.0 0.0 1.0 5.0 9.0; "
            + "9.0 9.0 5.0 1.0 0.0 1.0 5.0; "
            + "9.0 9.0 9.0 1.0 0.5 0.0 1.0; "
            + "9.0 9.0 9.0 1.0 1.0 0.5 0.0]";
    ArrayList<String> classifierNames = new ArrayList();
    ArrayList<String[]> optionsList = new ArrayList();

    classifierNames.add("weka.classifiers.meta.CostSensitiveClassifier");
    optionsList.add(new String[]{"-cost-matrix", "\"" + matrix + "\"",
      "-W", "weka.classifiers.trees.RandomForest", "--", "-I", "40", "-depth", "10"});

//    classifierNames.add("weka.classifiers.meta.CostSensitiveClassifier");
//    optionsList.add(new String[]{"-cost-matrix", "\"" + matrix + "\"",
//      "-W", "weka.classifiers.meta.RotationForest", "--", "-I", "5"});

    int num_folds = 10;

    ExecutorService executor = Executors.newFixedThreadPool(6);

    ArrayList<String> instruments = GlobalConfigs.INSTRUMENT_CODES;
    Performance perf = new Performance(instruments, MODEL_TYPES.STK.name());
    WekaModelBenchamrker benchmarker = new WekaModelBenchamrker(perf, MODEL_TYPES.STK.name());

    for (String i : instruments) {
      Runnable worker = new ClassicifyThread(i,
              classifierNames,
              optionsList, num_folds, benchmarker);
      executor.execute(worker);
    }
    executor.shutdown();
    while (!executor.isTerminated()) {
    }
    perf.outputPerformanceData();

  }
//
//  private static ArrayList<String[]> cloneList(ArrayList<String[]> list) {
//    ArrayList<String[]> r = new ArrayList();
//    for (String[] l : list) {
//      r.add(l);
//    }
//    return r;
//  }

  private static class ClassicifyThread implements Runnable {

    String m_Code;
    ArrayList<String> m_cNames;
    ArrayList<String[]> m_cOptions;
    WekaModelBenchamrker m_Benchmarker;
    int m_folds;

    public ClassicifyThread(String i, ArrayList<String> classifierNames,
            ArrayList<String[]> optionsList, int num_folds, WekaModelBenchamrker benchmarker) {
      m_Code = i;
      m_cNames = classifierNames;
      m_cOptions = optionsList;
      m_folds = num_folds;
      m_Benchmarker = benchmarker;
    }

    @Override
    public void run() {

      m_Benchmarker.benchmarkByClasses(m_Code,
              m_cNames, m_cOptions, m_folds);
    }
  }
}
