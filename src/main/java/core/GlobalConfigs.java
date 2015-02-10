package core;

import datapreparer.downloader.CSVDownloader;
import java.io.BufferedReader;
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
public class GlobalConfigs {

  public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
  //Default start date for data download
  public static final String DEFAULT_START_DATE = "2000-01-01";

  // Global Value:  project path
  public static final String DEFAULT_PATH = "D:\\Documents\\NetBeansProjects\\ProjectSPA\\";
  public static final String RESOURCE_PATH = DEFAULT_PATH + "resources\\";
  public static final String MODEL_PATH = DEFAULT_PATH + "models\\";
  public static final String TEMP_PATH = DEFAULT_PATH + "temp\\";
  public static final String REPORT_PATH = DEFAULT_PATH + "reports\\";

  public static final ArrayList<String> INSTRUMENT_CODES = loadInstrumentCodes(DEFAULT_PATH + "resources\\instrument_list.txt");
  public static final ArrayList<String> INDICE_CODES = loadInstrumentCodes(DEFAULT_PATH + "resources\\indice_list.txt");
  public static ConcurrentHashMap<String, String> REVELANT_INDICIES;
  public static ConcurrentHashMap<String, String> WIKI_TITTLES;
  public static final String IXIC = "^IXIC";
  public static final String GSPC = "^GSPC";
  public static final int[] WEEK_MULTIPIER = new int[]{1, 2, 3, 4, 5, 6};

  public static int ClassCount = 1;
  public static int TrainingCount = 0;
  public static ArrayList<String> ClassTags = new ArrayList();

  private static ConcurrentHashMap<String, Double> NormalSigMap;

  public static final double getSignificanceNormal(String code) {
    return 0.04;
//        if (NormalSigMap == null) {
//            loadSignificanceMaps();
//        } else if (NormalSigMap.containsKey(code)) {
//            return NormalSigMap.get(code);
//        } else {
//            
//        }
  }

  private static ConcurrentHashMap<String, Double> DailySigMap;

  public static final double getSignificanceDaily(String code) {
    return 0.015;
//        if (DailySigMap == null) {
//            loadSignificanceMaps();
//        } else if (DailySigMap.containsKey(code)) {
//            return DailySigMap.get(code);
//        } else {
//
//        }
  }

//    private static void loadSignificanceMaps() {
//        ObjectInputStream inputStream = null;
//        try {
//            String fileName = DEFAULT_PATH + "resources\\NormalSigMap.bin";
//            inputStream = new ObjectInputStream(new FileInputStream(fileName));
//            NormalSigMap = (ConcurrentHashMap<String, Double>) inputStream.readObject();
//            fileName = DEFAULT_PATH + "resources\\DailySigMap.bin";
//            inputStream = new ObjectInputStream(new FileInputStream(fileName));
//            DailySigMap = (ConcurrentHashMap<String, Double>) inputStream.readObject();
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(GlobalConfigs.class.getName()).log(Level.SEVERE, null, ex);
//            NormalSigMap = new ConcurrentHashMap();
//            DailySigMap = new ConcurrentHashMap();
//        } catch (IOException | ClassNotFoundException ex) {
//            Logger.getLogger(GlobalConfigs.class.getName()).log(Level.SEVERE, null, ex);
//        } finally {
//            try {
//                inputStream.close();
//            } catch (IOException ex) {
//                Logger.getLogger(GlobalConfigs.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//    }
  private static ArrayList<String> loadInstrumentCodes(String path) {
    if (REVELANT_INDICIES == null) {
      REVELANT_INDICIES = new ConcurrentHashMap();
    }
    if (WIKI_TITTLES == null) {
      WIKI_TITTLES = new ConcurrentHashMap();
    }

    ArrayList<String> codes = new ArrayList();
    int count = 0;
    try (BufferedReader reader = new BufferedReader(
            new FileReader(path))) {
      String line;
      String[] split;
      while ((line = reader.readLine()) != null) {
        count++;
        split = line.split(",");
        codes.add(split[0]);
        if (split.length > 1) {
          REVELANT_INDICIES.put(split[0], split[1]);
          WIKI_TITTLES.put(split[0], split[2]);
        }
      }
    } catch (FileNotFoundException fe) {
      Logger.getLogger(CSVDownloader.class
              .getName()).log(Level.SEVERE, path + " does not exist", fe);
    } catch (Exception e) {
      Logger.getLogger(CSVDownloader.class
              .getName()).log(Level.SEVERE, " error when load " + path, e);
    }

    System.out.println(count + " codes loaded.");
    return codes;
  }
}
