package datapreparer;

import core.GConfigs.YAHOO_DATA_INDEX;
import core.TrainingFileGenerator;
import static core.GConfigs.RESOURCE_PATH;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.MyUtils;

public class RawDataLoader {

  // Return a map of raw data array
  // Data order(index start at 0!): 1, open; 2, high; 3, low; 4, close; 5, volume
  // TODO: import data from different files to one map
  public static ConcurrentHashMap<String, Object[]> loadRawData(String code, String modelTypePath) {
    ConcurrentHashMap<String, Object[]> raw_data_map = new ConcurrentHashMap();
    String path = RESOURCE_PATH + modelTypePath + code;
    //Check that this folder exits
    MyUtils.findOrCreateFolder(path);
    
    File file = new File(path + "//" + code + ".csv");
    if (file.isFile()) {
      try (BufferedReader reader = new BufferedReader(
              new FileReader(file))) {
        String line;
        while ((line = reader.readLine()) != null) {
          String[] parts = line.split(",");
          Object[] data = new Object[]{parts[YAHOO_DATA_INDEX.DATE.ordinal()],
            Double.parseDouble(parts[YAHOO_DATA_INDEX.OPEN.ordinal()]),
            Double.parseDouble(parts[YAHOO_DATA_INDEX.HIGH.ordinal()]),
            Double.parseDouble(parts[YAHOO_DATA_INDEX.LOW.ordinal()]),
            Double.parseDouble(parts[YAHOO_DATA_INDEX.CLOSE.ordinal()]),
            Double.parseDouble(parts[YAHOO_DATA_INDEX.VOLUME.ordinal()])};
          raw_data_map.put(parts[0], data);
        }
      } catch (Exception ex) {
        Logger.getLogger(TrainingFileGenerator.class.getName()).log(Level.SEVERE, "Error when loading csv file for raw data.", ex);
      }
    } else {
      Logger.getLogger(TrainingFileGenerator.class.getName()).log(Level.WARNING, "Data file for {0} does not exist.", code);
      return null;
    }
    return raw_data_map;
  }
  
}
