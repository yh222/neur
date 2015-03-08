package calculator;

import core.GlobalConfigs;
import static core.GlobalConfigs.MODEL_PATH;
import static core.GlobalConfigs.REPORT_PATH;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import weka.classifiers.Classifier;
import weka.core.Instances;

public class Performance {

  // Structure: Code -> (ClassifierName-> Data)
  private ConcurrentHashMap<String, ConcurrentHashMap<String, float[][]>> m_Performance;
  private final String m_TypePath;

  public Performance(ArrayList<String> instruments, String modelType) {
    m_Performance = loadMap(instruments);
    m_TypePath = modelType + "//";
  }

// identification of a model  -> Performance Data
    /* Because for each class attribute, an independet classifier is used,
   * then they will be organized by different identifications.
   */
  public void saveModelAndPerformance(String code,
          String identification,
          Classifier classifier,
          Instances trainHeader,
          float[] performance) {

    String dir = MODEL_PATH + m_TypePath + code + "//";
    File theDir = new File(dir);
    if (!theDir.exists()) {
      theDir.mkdir();
    }

    String fname = identification + ".model";
    File file = new File(dir + fname);
    //Save model
    try (ObjectOutputStream objectOutputStream
            = new ObjectOutputStream(new FileOutputStream(file, false))) {
      objectOutputStream.writeObject(classifier);
      objectOutputStream.writeObject(trainHeader.stringFreeStructure());
    } catch (Exception e) {
      Logger.getLogger(Performance.class.getName()).log(Level.SEVERE, null, e);
    }

    //Save performance
    fname = identification + ".perf";
    file = new File(dir + fname);
    try (ObjectOutputStream objectOutputStream
            = new ObjectOutputStream(new FileOutputStream(file, false))) {
      objectOutputStream.writeObject(performance);
    } catch (Exception e) {
      Logger.getLogger(Performance.class.getName()).log(Level.SEVERE, null, e);
    }

  }

  private void putClassifier(String classifierName) {
    for (ConcurrentHashMap<String, float[][]> m : m_Performance.values()) {
      if (!m.containsKey(classifierName)) {
        m.put(classifierName, new float[GlobalConfigs.getClassCount(m_TypePath)][]);
      } else {
        break;
      }
    }
  }

  public void putClassResult(String code, String classifierName, float[] result, int index) {
    float[][] data = m_Performance.get(code).get(classifierName);
    if (data == null) {
      putClassifier(classifierName);
      data = m_Performance.get(code).get(classifierName);
    }
    assert data != null;
    data[index] = result;

  }

  private ConcurrentHashMap<String, ConcurrentHashMap<String, float[][]>>
          loadMap(ArrayList<String> instruments) {
    ConcurrentHashMap<String, ConcurrentHashMap<String, float[][]>> map
            = new ConcurrentHashMap();
    for (String i : instruments) {
      map.put(i, new ConcurrentHashMap<String, float[][]>());
    }
    return map;
  }

  public void outputPerformanceData() {
    String code, classifier;
    NumberFormat defaultFormat = NumberFormat.getNumberInstance();
    defaultFormat.setMinimumFractionDigits(2);

    try (PrintWriter writer = new PrintWriter(new BufferedWriter(
            new FileWriter(REPORT_PATH + m_TypePath + "_ConfidenceReport.csv", true)))) {

      for (Entry<String, ConcurrentHashMap<String, float[][]>> e1 : m_Performance.entrySet()) {
        code = e1.getKey();
        writer.println("\nCode: " + code);
        for (Entry<String, float[][]> e2 : e1.getValue().entrySet()) {
          classifier = e2.getKey();
          //String[] split;
          writer.println("Classifier: " + classifier);
          writer.println(",VL,ML,LL,LH,MH,VH");
          for (int i = 0; i < GlobalConfigs.getClassCount(m_TypePath); i++) {
            writer.println(GlobalConfigs.ClassTags.get(i)
                    + "," + defaultFormat.format(e2.getValue()[i][0])
                    + "," + defaultFormat.format(e2.getValue()[i][1])
                    + "," + defaultFormat.format(e2.getValue()[i][2])
                    + "," + defaultFormat.format(e2.getValue()[i][4])
                    + "," + defaultFormat.format(e2.getValue()[i][5])
                    + "," + defaultFormat.format(e2.getValue()[i][6])
            );
          }
        }
        writer.println();
      }
    } catch (IOException ex) {
      Logger.getLogger(Performance.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
}
