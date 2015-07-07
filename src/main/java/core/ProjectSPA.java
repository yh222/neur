package core;

import core.GConfigs.MODEL_TYPES;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.WekaModelBenchamrker;
import util.MyUtils;

public class ProjectSPA {

  public static void main(String[] args) {

    ArrayList<String> classifierNames = new ArrayList();
    ArrayList<String[]> optionsList = new ArrayList();
    ArrayList<String> notes = new ArrayList();

    ArrayList<String> seriesClassifierNames = new ArrayList();
    ArrayList<String[]> SeriesOptionsList = new ArrayList();

//    classifierNames.add("weka.classifiers.rules.NNge");
//    optionsList.add(new String[]{"-G", "5", "-I", "5"});
//    classifierNames.add("weka.classifiers.rules.JRip");
//    optionsList.add(new String[]{"-F", "3", "-S", "1", "-N", "2", "-O", "2"});
//    classifierNames.add("weka.classifiers.misc.VFI");
//    optionsList.add(new String[]{"-B", "0.6"});
//    classifierNames.add("weka.classifiers.functions.MultilayerPerceptronCS");
//    optionsList.add(new String[]{"-H", "a", "-N", "200", "-L", "0.03", "-M", "0.1"});
    ////////^^^^^Tested algorithms placed Above^^^^^//////
//    classifierNames.add("weka.classifiers.functions.ArtmapNew");
//    optionsList.add(new String[]{});
//
//    classifierNames.add("weka.classifiers.trees.RandomForest");
//    optionsList.add(new String[]{"-I", "40", "-depth", "10"});
//
//    classifierNames.add("weka.classifiers.functions.RBFNetwork");
//    optionsList.add(new String[]{"-B", "2", "-S", "1", "-R", "1.0E-8", "-M", "-1", "-W", "0.1"});
//
//    classifierNames.add("weka.classifiers.trees.J48");
//    optionsList.add(new String[]{"-C", "0.25", "-M", "2"});
////=============================================================
//
//    classifierNames.add("weka.classifiers.trees.HoeffdingTree");
//    optionsList.add(new String[]{"-L", "2", "-S", "1", "-E", "0.000001", "-H", "0.5", "-M", "0.01", "-G", "200", "-N", "0.0"});
//
//    classifierNames.add("weka.classifiers.rules.PART");
//    optionsList.add(new String[]{"-M", "2", "-C", "0.25", "-Q", "1"});
//
//    classifierNames.add("weka.classifiers.trees.M5P");
//    optionsList.add(new String[]{"-M", "4.0"});
//
//    classifierNames.add("weka.classifiers.functions.RBFClassifier");
//    optionsList.add(new String[]{"-N", "2", "-S", "1", "-R", "0.01", "-L", "1.0E-6", "-C", "2", "-P", "1", "-E", "1"});
//====================================
//    classifierNames.add("weka.classifiers.rules.Ridor");
//   optionsList.add(new String[]{"-F", "3", "-S", "1", "-N", "2"});
//    classifierNames.add("weka.classifiers.meta.RotationForest");
//    optionsList.add(new String[]{"-G", "3", "-H", "3", "-P", "50", "-F", "weka.filters.unsupervised.attribute.PrincipalComponents -R 1.0 -A 5 -M -1", "-S", "1", "-I", "10", "-W", "weka.classifiers.trees.J48", "--", "-C", "0.25", "-M", "2"});
//    classifierNames.add("weka.classifiers.functions.MultilayerPerceptron");
//    optionsList.add(new String[]{"-H", "a", "-N", "200", "-L", "0.03", "-M", "0.1"});
//
//    classifierNames.add("weka.classifiers.functions.MLPClassifier");
//    optionsList.add(new String[]{"-N", "2", "-R", "0.01", "-O", "1.0E-6", "-P", "1", "-E", "1", "-S", "1"});
    for (int i = 0; i < 3; i++) {
//      classifierNames.add("weka.classifiers.meta.RotationForest");
//      optionsList.add(new String[]{"-G", "4", "-H", "3", "-P", "50", "-F", "weka.filters.unsupervised.attribute.PrincipalComponents -R 1.0 -A 5 -M -1", "-W", "weka.classifiers.rules.NNge", "--", "-G", "30", "-I", "5"});
//      notes.add("");

      classifierNames.add("weka.classifiers.meta.AdditiveRegression");
      optionsList.add(new String[]{"-S", "1.0", "-I", "10", "-W",
        "weka.classifiers.trees.RandomForest", "--", "-I", "100",
        "-depth", "10"});
      notes.add("");

      classifierNames.add("weka.classifiers.trees.RandomForest");
      optionsList.add(new String[]{"-I", "100", "-depth", "10"});
      notes.add("");
//      classifierNames.add("weka.classifiers.rules.NNge");
//      optionsList.add(new String[]{"-G", "10", "-I", "5"});
//      notes.add("");

    }

//      classifierNames.add("weka.classifiers.functions.ArtmapNew");
//      optionsList.add(new String[]{});
//      classifierNames.add("weka.classifiers.functions.RBFClassifier");
//      optionsList.add(new String[]{"-N", "2", "-R", "0.01", "-L", "1.0E-6", "-C", "2"});
//      classifierNames.add("weka.classifiers.functions.MLPClassifier");
//      optionsList.add(new String[]{"-N", "2", "-R", "0.01", "-O", "1.0E-6"});
//      classifierNames.add("weka.classifiers.rules.JRip");
//      optionsList.add(new String[]{"-F", "4", "-N", "2", "-O", "2"});
//      classifierNames.add("weka.classifiers.trees.J48");
//      optionsList.add(new String[]{"-C", "0.15", "-M", "2"});
//      classifierNames.add("weka.classifiers.meta.RotationForest");
//      optionsList.add(new String[]{"-G", "4", "-H", "3", "-P", "50", "-F", "weka.filters.unsupervised.attribute.PrincipalComponents -R 1.0 -A 5 -M -1", "-S", "1", "-I", "10", "-W", "weka.classifiers.trees.J48", "--", "-C", "0.25", "-M", "2"});
//      classifierNames.add("weka.classifiers.rules.PART");
//      optionsList.add(new String[]{"-M", "3", "-C", "0.25"});
//      classifierNames.add("weka.classifiers.misc.VFI");
//      optionsList.add(new String[]{"-B", "0.8"});
//      classifierNames.add("weka.classifiers.functions.MultilayerPerceptronCS");
//      optionsList.add(new String[]{"-H", "a", "-N", "200", "-L", "0.01", "-M", "0.01"});
//      classifierNames.add("weka.classifiers.trees.RandomForest");
//      optionsList.add(new String[]{"-I", "50"});
//      classifierNames.add("weka.classifiers.rules.NNge");
//      optionsList.add(new String[]{"-G", "30", "-I", "5"});
//      classifierNames.add("weka.classifiers.trees.HoeffdingTree");
//      optionsList.add(new String[]{"-L", "2", "-E", "0.000001", "-H", "0.5", "-M", "0.01", "-G", "200", "-N", "0.0"});
    ExecutorService executor = Executors.newFixedThreadPool(8);

    ArrayList<String> instruments = GConfigs.INSTRUMENT_CODES;
    WekaModelBenchamrker benchmarker = new WekaModelBenchamrker(MODEL_TYPES.STK.name());
    MyUtils.deleteModelFolder(MODEL_TYPES.STK.name());
    for (String i : instruments) {
      Runnable worker = new ClassicifyThread(i,
              classifierNames,
              optionsList, notes, benchmarker);
      executor.execute(worker);
    }
    executor.shutdown();
    while (!executor.isTerminated()) {
    }

    //Do again
//    executor = Executors.newFixedThreadPool(6);
//    benchmarker = new WekaModelBenchamrker(MODEL_TYPES.STK.name());
//    MyUtils.deleteModelFolder(MODEL_TYPES.STK.name());
//    for (String i : instruments) {
//      Runnable worker = new ClassicifyThread(i,
//              classifierNames,
//              optionsList, notes, benchmarker);
//      executor.execute(worker);
//    }
//    executor.shutdown();
//    while (!executor.isTerminated()) {
//    }
  }

//  private static ArrayList<String[]> cloneList(ArrayList<String[]> list) {
//    ArrayList<String[]> r = new ArrayList();
//    for (String[] l : list) {
//      r.add(l);
//    }
//    return r;
//}
  private static class ClassicifyThread implements Runnable {

    String m_Code;
    ArrayList<String> m_cNames;
    ArrayList<String[]> m_cOptions;
    ArrayList<String> m_cNodes;
    WekaModelBenchamrker m_Benchmarker;

    public ClassicifyThread(String i, ArrayList<String> classifierNames,
            ArrayList<String[]> optionsList, ArrayList<String> notes,
            WekaModelBenchamrker benchmarker) {
      m_Code = i;
      m_cNames = classifierNames;
      m_cOptions = optionsList;
      m_cNodes = notes;
      m_Benchmarker = benchmarker;
    }

    @Override
    public void run() {
      try {
        m_Benchmarker.benchmarkByClasses(m_Code,
                m_cNames, m_cOptions, m_cNodes);
      } catch (IOException ex) {
        Logger.getLogger(ProjectSPA.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
      }
    }
  }
}
