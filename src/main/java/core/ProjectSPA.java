package core;

import calculator.Performance;
import core.GConfigs.MODEL_TYPES;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.WekaModelBenchamrker;

public class ProjectSPA {

  public static void main(String[] args) {

    ArrayList<String> classifierNames = new ArrayList();
    ArrayList<String[]> optionsList = new ArrayList();

    ArrayList<String> seriesClassifierNames = new ArrayList();
    ArrayList<String[]> SeriesOptionsList = new ArrayList();

//    classifierNames.add("weka.classifiers.trees.RandomForest");
//    optionsList.add(new String[]{"-I", "40", "-depth", "10"});
    
//     classifierNames.add("weka.classifiers.meta.RotationForest");
//    optionsList.add(new String[]{"-G", "3", "-H", "3","-P","50","-F"
//            ,"weka.filters.unsupervised.attribute.PrincipalComponents -R 1.0 -A 5 -M -1","-S","1","-I","10"
//    ,"-W","weka.classifiers.trees.J48","--","-C","0.25","-M","2"});   
//        classifierNames.add("weka.classifiers.meta.RotationForest");
//    optionsList.add(new String[]{"-G", "3", "-H", "3","-P","50","-F"
//            ,"weka.filters.unsupervised.attribute.RandomProjection -N 10 -R 42 -D Sparse1","-S","1","-I","10"
//    ,"-W","weka.classifiers.trees.J48","--","-C","0.25","-M","2"}); 

    
//         classifierNames.add("weka.classifiers.meta.RotationForest");
//    optionsList.add(new String[]{"-G", "3", "-H", "3","-P","50","-F"
//            ,"weka.filters.unsupervised.attribute.RandomSubset -N 0.5 -S 1","-S","1","-I","10"
//    ,"-W","weka.classifiers.trees.RandomTree","--","-K","0","-M","1.0","-V","0.001","-S","1"});     

//        classifierNames.add("weka.classifiers.meta.RotationForest");
//    optionsList.add(new String[]{"-G", "3", "-H", "3","-P","50","-F"
//            ,"weka.filters.unsupervised.attribute.PrincipalComponents  -R 1.0 -A 5 -M -1","-S","1","-I","10"
//    ,"-W","weka.classifiers.trees.M5P","--","-M","4"}); 

    
//    classifierNames.add("weka.classifiers.functions.MultilayerPerceptron");
//    optionsList.add(new String[]{"-H", "a", "-N", "200", "-L", "0.03", "-M", "0.1"});
//        classifierNames.add("weka.classifiers.functions.MultilayerPerceptronCS");
//    optionsList.add(new String[]{"-H", "a", "-N", "200", "-L", "0.03", "-M", "0.1"});
    
//    classifierNames.add("weka.classifiers.trees.J48");
//    optionsList.add(new String[]{"-C", "0.25", "-M", "2"});

    
//    classifierNames.add("weka.classifiers.trees.HoeffdingTree");
//    optionsList.add(new String[]{"-L", "2", "-S", "1", "-E", "0.000001", "-H", "0.5"
//            , "-M", "0.01","-G","200","-N","0.0"});
    
   //classifierNames.add("weka.classifiers.rules.FURIA");
   // optionsList.add(new String[]{"-F", "3", "-N", "2.0","-O","2","-S","1","-p","0","-s","0"});
//    classifierNames.add("weka.classifiers.trees.M5P");
//    optionsList.add(new String[]{   "-M", "4.0"});

    
       classifierNames.add("weka.classifiers.rules.PART");
    optionsList.add(new String[]{   "-M", "2","-C","0.25","-Q","1"});
    
    
    
//    classifierNames.add("weka.classifiers.meta.CostSensitiveClassifier");
//    optionsList.add(new String[]{"-cost-matrix", "\"" + matrix + "\"",
//      "-W", "weka.classifiers.trees.RandomForest", "--", "-I", "40", "-depth", "10"});
//    classifierNames.add("weka.classifiers.meta.CostSensitiveClassifier");
//    optionsList.add(new String[]{"-cost-matrix", "\"" + matrix + "\"",
//      "-W", "weka.classifiers.functions.MultilayerPerceptron", "--", "-H", "a", "-N", "200"});
//    classifierNames.add("weka.classifiers.trees.RandomForest");
//    optionsList.add(new String[]{"-I", "40", "-depth", "10"});
//
//    classifierNames.add("weka.classifiers.functions.MultilayerPerceptron");
//    optionsList.add(new String[]{"-H", "a", "-N", "150"});
//    classifierNames.add("weka.classifiers.meta.CostSensitiveClassifier");
//    optionsList.add(new String[]{"-cost-matrix", "\"" + matrix + "\"",
//      "-W", "weka.classifiers.meta.RotationForest", "--", "-I", "5"});
    ExecutorService executor = Executors.newFixedThreadPool(6);

    ArrayList<String> instruments = GConfigs.INSTRUMENT_CODES;
    Performance perf = new Performance(instruments, MODEL_TYPES.STK.name());
    WekaModelBenchamrker benchmarker = new WekaModelBenchamrker(perf, MODEL_TYPES.STK.name());

    for (String i : instruments) {
      Runnable worker = new ClassicifyThread(i,
              classifierNames,
              optionsList, benchmarker);
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

    public ClassicifyThread(String i, ArrayList<String> classifierNames,
            ArrayList<String[]> optionsList, WekaModelBenchamrker benchmarker) {
      m_Code = i;
      m_cNames = classifierNames;
      m_cOptions = optionsList;
      m_Benchmarker = benchmarker;
    }

    @Override
    public void run() {
      try {
        m_Benchmarker.benchmarkByClasses(m_Code,
                m_cNames, m_cOptions);
      } catch (IOException ex) {
        Logger.getLogger(ProjectSPA.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }
}
