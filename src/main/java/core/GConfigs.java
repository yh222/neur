package core;

import datapreparer.downloader.STKCSVDownloader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * global configuration class
 */
public class GConfigs {

  //Default start date for data download
  public static final String DEFAULT_START_DATE = "2014-01-01";

  // Global Value:  project path
  public static final String DEFAULT_PATH = "D:\\Documents\\NetBeansProjects\\ProjectSPA\\";
  public static final String RESOURCE_PATH = DEFAULT_PATH + "resources\\";
  public static final String MODEL_PATH = DEFAULT_PATH + "models\\";
  public static final String TEMP_PATH = DEFAULT_PATH + "temp\\";
  public static final String REPORT_PATH = DEFAULT_PATH + "reports\\";

  public static final ArrayList<String> INSTRUMENT_CODES = loadInstrumentCodes(DEFAULT_PATH + "resources\\STK\\instrument_list.txt", 0);
  public static final ArrayList<String> INDICE_CODES = loadInstrumentCodes(DEFAULT_PATH + "resources\\STK\\indice_list.txt", 0);
  public static final ArrayList<String> FX_CODES = loadInstrumentCodes(DEFAULT_PATH + "resources\\FX\\fx_codes.txt", 0);

  //  Not using this now
  //  public static ConcurrentHashMap<String, String> WIKI_TITTLES;
  public static final String IXIC = "^IXIC";
  //public static final String GSPC = "^GSPC";
  public static final int[] WEEK_MULTIPIER_TRAIN = new int[]{2, 4, 10};
  public static final int[] WEEK_MULTIPIER_CLASS = new int[]{1, 2, 3, 5};

  //Marker for numeric class attributes
  public static final String CLS = "C_";

  // marker for Nominal attributes
  public static final String NOM = "N_";

  public static enum MODEL_TYPES {

    STK,
    COMO,
    FX;
  }

  //Matches yahoo stock data
  public static enum YAHOO_DATA_INDEX {

    DATE,
    OPEN,
    HIGH,
    LOW,
    CLOSE,
    VOLUME;
  }

  private static int m_ClassCount = 0;
  private static int m_TrainingCount = 0;

  public static int getClassCount(String modelType) {
    if (m_ClassCount == 0) {
      updateAttCounts(modelType);
    }
    return m_ClassCount;
  }

  public static int getTrainingCount(String modelType) {
    if (m_TrainingCount == 0) {
      updateAttCounts(modelType);
    }
    return m_TrainingCount;
  }

  private synchronized static void updateAttCounts(String modelTypePath) {
    m_TrainingCount = 0;
    m_ClassCount = 0;
    ClassTags = new ArrayList();
    try (BufferedReader reader = new BufferedReader(
            new FileReader(new File(RESOURCE_PATH + modelTypePath + "attCount")))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.regionMatches(0, CLS, 0, CLS.length())) {
          m_ClassCount++;
          ClassTags.add(line);
        } else {
          m_TrainingCount++;
        }
      }
    } catch (Exception ex) {
      Logger.getLogger(GConfigs.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  public static ArrayList<String> ClassTags = new ArrayList();

  private static ConcurrentHashMap<String, Float> NormalSigMap;

  public static final float getSignificanceNormal(MODEL_TYPES type) {

    if (type.equals(MODEL_TYPES.FX)) {
      return 0.004f;
    } else {
      return 0.02f;
    }

//        if (NormalSigMap == null) {
//            loadSignificanceMaps();
//        } else if (NormalSigMap.containsKey(code)) {
//            return NormalSigMap.get(code);
//        } else {
//            
//        }
  }


  private static ConcurrentHashMap<String, Float> DailySigMap;

  public static final float getSignificanceDaily() {
    return 0.015f;
  }

  private static ArrayList<String> loadInstrumentCodes(String path, int location) {
    ArrayList<String> codes = new ArrayList();
    int count = 0;
    try (BufferedReader reader = new BufferedReader(
            new FileReader(path))) {
      String line;
      String[] split;
      while ((line = reader.readLine()) != null) {
        count++;
        split = line.split(",");
        codes.add(split[location]);
      }
    } catch (FileNotFoundException fe) {
      Logger.getLogger(STKCSVDownloader.class
              .getName()).log(Level.SEVERE, path + " does not exist", fe);
    } catch (Exception e) {
      Logger.getLogger(STKCSVDownloader.class
              .getName()).log(Level.SEVERE, " error when load " + path, e);
    }
    return codes;
  }

}
